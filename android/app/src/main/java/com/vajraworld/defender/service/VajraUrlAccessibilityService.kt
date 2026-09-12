package com.vajraworld.defender.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.vajraworld.defender.VajraApplication
import com.vajraworld.defender.data.local.IncidentEntity
import com.vajraworld.defender.data.local.ScanResultEntity
import com.vajraworld.defender.data.local.SecurityEventEntity
import com.vajraworld.defender.domain.engine.UrlRuleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class VajraUrlAccessibilityService : AccessibilityService() {

    private val supportedBrowsers = setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "com.sec.android.app.sbrowser",
        "org.mozilla.firefox",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.duckduckgo.mobile.android"
    )

    private var lastAnalyzedUrl: String = ""
    private var lastAnalyzedTime: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (!supportedBrowsers.contains(pkg)) return

        val rootNode = rootInActiveWindow ?: event.source ?: return

        try {
            val url = extractUrlFromNode(rootNode)
            if (!url.isNullOrBlank() && isValidUrlString(url)) {
                val now = SystemClock.elapsedRealtime()
                if (url != lastAnalyzedUrl || (now - lastAnalyzedTime > 5000)) {
                    lastAnalyzedUrl = url
                    lastAnalyzedTime = now
                    evaluateUrl(url, pkg)
                }
            }
        } catch (e: Exception) {
            Log.e("VajraAccessibility", "Error inspecting browser node: ${e.message}")
        } finally {
            try {
                rootNode.recycle()
            } catch (_: Exception) {}
        }
    }

    private fun extractUrlFromNode(node: AccessibilityNodeInfo): String? {
        // Direct check by common browser view IDs
        val commonIds = listOf(
            "url_bar", "search_box_text", "location_bar_edit_text",
            "toolbar", "address_bar", "omnibox"
        )

        for (id in commonIds) {
            val nodes = node.findAccessibilityNodeInfosByViewId("${node.packageName}:id/$id")
            if (nodes.isNotEmpty()) {
                val text = nodes[0].text?.toString()
                if (!text.isNullOrBlank() && (text.contains(".") || text.startsWith("http"))) {
                    return text.trim()
                }
            }
        }

        // Recursive search for text matching URL format
        return findUrlRecursive(node)
    }

    private fun findUrlRecursive(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null
        val text = node.text?.toString()
        if (!text.isNullOrBlank() && isValidUrlString(text.trim())) {
            return text.trim()
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val res = findUrlRecursive(child)
            if (res != null) return res
        }
        return null
    }

    private fun isValidUrlString(candidate: String): Boolean {
        if (candidate.contains(" ") || candidate.length < 4) return false
        if (candidate.startsWith("http://") || candidate.startsWith("https://")) return true
        if (candidate.contains(".") && (
            candidate.endsWith(".com") || candidate.endsWith(".org") ||
            candidate.endsWith(".xyz") || candidate.endsWith(".top") ||
            candidate.endsWith(".net") || candidate.endsWith(".info") ||
            candidate.endsWith(".ru") || candidate.endsWith(".cn") ||
            candidate.contains(".com/") || candidate.contains(".org/")
        )) return true
        return false
    }

    private fun evaluateUrl(rawUrl: String, browserPkg: String) {
        val url = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) "https://$rawUrl" else rawUrl
        val analysis = UrlRuleEngine.analyze(url)

        if (analysis.riskScore >= 40 || analysis.brandDeception != null) {
            // Malicious or high-risk phishing URL detected
            val title = if (analysis.brandDeception != null) "Deceptive Brand Phishing Link" else "Suspicious Web Threat"
            val message = if (analysis.brandDeception != null) {
                "${analysis.brandDeception}. High risk of credential theft."
            } else {
                "Risky link detected in browser: ${analysis.signals.firstOrNull() ?: "High entropy domain / deceptive structure"}."
            }

            VajraNotificationManager.sendThreatAlert(
                context = applicationContext,
                title = title,
                message = message,
                targetId = url,
                targetType = "URL",
                riskScore = analysis.riskScore
            )

            // Log event and create incident in database
            val app = application as? VajraApplication
            app?.let { vajraApp ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dao = vajraApp.database.dao()
                        val scanResult = ScanResultEntity(
                            id = UUID.randomUUID().toString(),
                            target = url,
                            scanType = "URL",
                            riskScore = analysis.riskScore,
                            confidence = analysis.confidence,
                            signalsJson = com.google.gson.Gson().toJson(analysis.signals),
                            sha256 = null,
                            createdAt = System.currentTimeMillis()
                        )
                        dao.insertScanResult(scanResult)

                        val event = SecurityEventEntity(
                            id = UUID.randomUUID().toString(),
                            timestamp = System.currentTimeMillis(),
                            eventType = "PHISHING_URL_INTERCEPTED",
                            source = browserPkg,
                            risk = analysis.riskScore.toFloat(),
                            confidence = analysis.confidence,
                            explanation = "Intercepted in $browserPkg: ${analysis.signals.joinToString("; ")}",
                            rawContentHash = "url_${System.currentTimeMillis()}",
                            isSynthetic = false
                        )
                        dao.insertSecurityEvent(event)

                        val incident = IncidentEntity(
                            incidentId = "INC-URL-${System.currentTimeMillis() % 10000}",
                            title = "Phishing URL Intercepted: $url",
                            status = "OPEN",
                            severity = if (analysis.riskScore >= 75) "CRITICAL" else "HIGH",
                            risk = analysis.riskScore / 100f,
                            confidence = analysis.confidence,
                            etaSeconds = 30,
                            predictedStage = "Credential Phishing Attempt",
                            affectedAssetsJson = com.google.gson.Gson().toJson(listOf(url, browserPkg)),
                            evidenceJson = com.google.gson.Gson().toJson(analysis.signals),
                            recommendedAction = "Close browser tab immediately and avoid entering credentials",
                            acknowledged = false,
                            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        )
                        dao.insertIncidents(listOf(incident))
                    } catch (e: Exception) {
                        Log.e("VajraAccessibility", "Failed to persist URL threat: ${e.message}")
                    }
                }
            }
        }
    }

    override fun onInterrupt() {}
}
