package com.vajraworld.defender.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.vajraworld.defender.data.local.SecurityEventEntity
import com.vajraworld.defender.data.local.VajraDatabase
import com.vajraworld.defender.domain.engine.UrlRuleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

class VajraNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private val SCAM_LURE_REGEX = Regex(
            """\b(account\s+(?:blocked|suspended|deactivated|frozen|terminated)|immediate\s+action\s+required|verify\s+(?:kyc|pan|identity|card)|unauthorized\s+(?:transaction|login|debit|access)|lottery\s+(?:won|winner)|claim\s+(?:reward|cashback|prize|refund)|income\s+tax\s+refund|card\s+(?:will\s+be\s+)?blocked|urgent\s+security\s+alert)\b""",
            RegexOption.IGNORE_CASE
        )

        private val OTP_FORWARD_LURE_REGEX = Regex(
            """\b(?:(?:your\s+)?otp\s+is\s*[:\-]?\s*(\d{4,8})|verification\s+code\s*(?:is\s*[:\-]?\s*)?(\d{4,8})|do\s+not\s+share\s+this\s+otp|forward\s+this\s+(?:message|sms)|share\s+code\s+to\s+cancel)\b""",
            RegexOption.IGNORE_CASE
        )

        private val URL_REGEX = Regex("""https?://[^\s<>"{}|\\^`]+""")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val currentSbn = sbn ?: return
        val packageName = currentSbn.packageName ?: "unknown"

        // Ignore notifications from our own application
        if (packageName == applicationContext.packageName) return

        val notification = currentSbn.notification ?: return
        val extras = notification.extras ?: return

        // In-memory text extraction
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        val rawCombined = "$title $text $bigText $subText".trim()
        if (rawCombined.isBlank()) return

        // STRICT PRIVACY RULE:
        // 1. Calculate SHA-256 of raw message for cryptographic audit trail
        // 2. Discard plaintext message content from memory after analysis
        // 3. value_stored = false: NEVER store raw notification text or secrets in Room or logs
        val rawContentHash = computeSha256(rawCombined)

        // In-flight pattern matching
        val hasScamLure = SCAM_LURE_REGEX.containsMatchIn(rawCombined)
        val hasOtpLure = OTP_FORWARD_LURE_REGEX.containsMatchIn(rawCombined)
        val matchedUrls = URL_REGEX.findAll(rawCombined).map { it.value }.toList()

        var riskScore = 0
        var confidence = 0.80f
        val explanationParts = mutableListOf<String>()
        var eventType = "NOTIFICATION_SUSPICIOUS"

        if (hasOtpLure) {
            riskScore = maxOf(riskScore, 65)
            explanationParts.add("Potential OTP / 2FA credential event detected in incoming notification")
            eventType = "NOTIFICATION_OTP_INTERCEPT"
            confidence = 0.90f
        }

        if (hasScamLure) {
            riskScore = maxOf(riskScore, 75)
            val lureMatch = SCAM_LURE_REGEX.find(rawCombined)?.value ?: "urgent account alert"
            explanationParts.add("High-pressure urgency lure matched: '$lureMatch'")
            eventType = "NOTIFICATION_SCAM_LURE"
            confidence = 0.92f
        }

        // Check embedded URLs
        for (url in matchedUrls) {
            val urlResult = UrlRuleEngine.analyze(url, context = rawCombined)
            if (urlResult.riskScore >= 40) {
                riskScore = maxOf(riskScore, urlResult.riskScore)
                explanationParts.add("Suspicious URL detected: ${urlResult.recommendedAction} (risk ${urlResult.riskScore})")
                eventType = "NOTIFICATION_MALICIOUS_LINK"
                confidence = maxOf(confidence, urlResult.confidence)
            }
        }

        // Persist to Room DB if risk >= 50 or OTP detected
        if (riskScore >= 50 || hasOtpLure) {
            val explanation = explanationParts.joinToString(" | ")

            serviceScope.launch {
                try {
                    val db = VajraDatabase.getInstance(applicationContext)
                    val event = SecurityEventEntity(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        eventType = eventType,
                        source = packageName,
                        risk = riskScore.toFloat(),
                        confidence = confidence,
                        explanation = explanation,
                        rawContentHash = rawContentHash,
                        isSynthetic = false
                    )
                    db.dao().insertSecurityEvent(event)
                } catch (_: Exception) {
                    // Fail-safe: service does not crash on local DB write failure
                }
            }
        }
    }

    private fun computeSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
