package com.vajraworld.defender.domain.engine

import java.net.URI
import java.util.Locale
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

data class LocalUrlAnalysisResult(
    val url: String,
    val riskScore: Int,
    val confidence: Float,
    val signals: List<String>,
    val brandDeception: String?,
    val recommendedAction: String,
    val entropy: Float
)

object UrlRuleEngine {

    private val HIGH_ABUSE_TLDS = setOf(
        "xyz", "top", "buzz", "cc", "tk", "online", "work", "click", "link",
        "club", "surf", "gq", "cf", "ml", "ga", "rest", "icu", "cam", "live",
        "loan", "stream", "download", "win", "bid", "racing", "vip", "fit",
        "monster", "tokyo", "bar", "party"
    )

    private val DANGEROUS_EXTENSIONS = listOf(
        ".apk", ".exe", ".scr", ".bat", ".dex", ".vbs", ".msi", ".jar",
        ".cmd", ".ps1", ".sh", ".com", ".pif", ".hta"
    )

    data class ProtectedBrand(
        val brandName: String,
        val officialDomain: String,
        val token: String
    )

    private val PROTECTED_BRANDS = listOf(
        ProtectedBrand("PayPal", "paypal.com", "paypal"),
        ProtectedBrand("Google", "google.com", "google"),
        ProtectedBrand("Apple", "apple.com", "apple"),
        ProtectedBrand("Microsoft", "microsoft.com", "microsoft"),
        ProtectedBrand("Amazon", "amazon.com", "amazon"),
        ProtectedBrand("Chase", "chase.com", "chase"),
        ProtectedBrand("Wells Fargo", "wellsfargo.com", "wellsfargo"),
        ProtectedBrand("Bank of America", "bankofamerica.com", "bankofamerica"),
        ProtectedBrand("Netflix", "netflix.com", "netflix"),
        ProtectedBrand("Facebook", "facebook.com", "facebook")
    )

    private val URGENT_LURE_KEYWORDS = listOf(
        "account blocked",
        "verify otp",
        "immediate action",
        "account suspended",
        "unauthorized transaction",
        "kyc update",
        "urgent",
        "security alert",
        "password reset",
        "lottery won",
        "confirm identity",
        "deactivation warning",
        "action required"
    )

    private val IPV4_REGEX = Regex("""^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$""")
    private val IPV6_REGEX = Regex("""^(?:[0-9a-fA-F]{1,4}:){2,7}[0-9a-fA-F]{1,4}$""")

    /**
     * Computes Shannon Character Entropy H(X) = - sum(p(x) * log2(p(x)))
     */
    fun calculateEntropy(input: String): Float {
        if (input.isEmpty()) return 0f
        val freqMap = HashMap<Char, Int>()
        for (c in input) {
            freqMap[c] = (freqMap[c] ?: 0) + 1
        }
        val len = input.length.toDouble()
        var entropy = 0.0
        val log2 = ln(2.0)
        for ((_, count) in freqMap) {
            val p = count / len
            entropy -= p * (ln(p) / log2)
        }
        return entropy.toFloat()
    }

    /**
     * Normalized Levenshtein distance between two strings.
     * Returns a similarity ratio between 0.0 (totally different) and 1.0 (identical).
     */
    fun normalizedLevenshteinSimilarity(s1: String, s2: String): Float {
        val dist = levenshteinDistance(s1.lowercase(Locale.ROOT), s2.lowercase(Locale.ROOT))
        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 1.0f
        return 1.0f - (dist.toFloat() / maxLen.toFloat())
    }

    fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Check if a string contains unicode homoglyphs or mixed scripts (e.g. Cyrillic or Greek lookalikes mixed with Latin).
     */
    fun detectUnicodeHomoglyphs(host: String): Boolean {
        var hasLatin = false
        var hasCyrillicOrGreek = false

        for (c in host) {
            val code = c.code
            // Latin
            if ((code in 0x0041..0x005A) || (code in 0x0061..0x007A)) {
                hasLatin = true
            }
            // Cyrillic (0x0400..0x04FF) or Greek (0x0370..0x03FF)
            if ((code in 0x0400..0x04FF) || (code in 0x0370..0x03FF)) {
                hasCyrillicOrGreek = true
            }
        }

        // Mixed script in domain is a classic homograph spoofing signature
        return hasCyrillicOrGreek && hasLatin
    }

    /**
     * Offline URL Analysis Engine
     */
    fun analyze(rawUrl: String, context: String? = null): LocalUrlAnalysisResult {
        val trimmedUrl = rawUrl.trim()
        val signals = mutableListOf<String>()
        var riskScore = 5
        var brandDeception: String? = null

        val parsedHost: String
        val parsedPath: String
        try {
            val normalized = if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
                "https://$trimmedUrl"
            } else {
                trimmedUrl
            }
            val uri = URI(normalized)
            parsedHost = uri.host?.lowercase(Locale.ROOT) ?: ""
            parsedPath = (uri.path ?: "") + (if (uri.query != null) "?${uri.query}" else "")
        } catch (_: Exception) {
            signals.add("Malformed or unparseable URL syntax")
            return LocalUrlAnalysisResult(
                url = rawUrl,
                riskScore = 80,
                confidence = 0.95f,
                signals = signals,
                brandDeception = null,
                recommendedAction = "Block & Quarantine",
                entropy = calculateEntropy(rawUrl)
            )
        }

        // 1. Shannon Entropy
        val hostEntropy = calculateEntropy(parsedHost)
        val fullUrlEntropy = calculateEntropy(trimmedUrl)
        if (hostEntropy >= 3.8f || fullUrlEntropy >= 4.5f) {
            riskScore += 20
            signals.add("High character entropy (${String.format(Locale.US, "%.2f", hostEntropy)}) - potential DGA or obfuscated link")
        }

        // 2. Punycode check (xn--)
        if (parsedHost.contains("xn--")) {
            riskScore += 40
            signals.add("Punycode domain detected (xn--) - internationalized domain homograph attack risk")
        }

        // 3. Unicode Homoglyph detection
        if (detectUnicodeHomoglyphs(parsedHost)) {
            riskScore += 45
            signals.add("Unicode homoglyph / mixed script detected in domain - visual deception technique")
        }

        // 4. IP-literal host matching
        val isIpv4 = IPV4_REGEX.matches(parsedHost)
        val isIpv6 = IPV6_REGEX.matches(parsedHost.removePrefix("[").removeSuffix("]"))
        if (isIpv4 || isIpv6) {
            riskScore += 40
            signals.add("Direct IP-literal host ($parsedHost) - bypasses DNS reputation filtering")
        }

        // 5. High-abuse TLDs
        val tld = parsedHost.substringAfterLast('.', "")
        if (tld.isNotEmpty() && HIGH_ABUSE_TLDS.contains(tld)) {
            riskScore += 30
            signals.add("High-abuse top-level domain detected: .$tld")
        }

        // 6. Dangerous executable/package extensions
        val lowerPath = parsedPath.lowercase(Locale.ROOT)
        for (ext in DANGEROUS_EXTENSIONS) {
            if (lowerPath.contains(ext)) {
                riskScore += 50
                signals.add("Dangerous executable or package extension detected ($ext)")
                break
            }
        }

        // 7. Brand similarity comparison & typosquatting detection
        val sld = extractSecondLevelDomain(parsedHost)
        for (brand in PROTECTED_BRANDS) {
            val isOfficial = parsedHost == brand.officialDomain || parsedHost.endsWith(".${brand.officialDomain}")
            if (!isOfficial) {
                val similarity = normalizedLevenshteinSimilarity(sld, brand.token)
                val distance = levenshteinDistance(sld, brand.token)

                // Subdomain spoofing: e.g. paypal.com.verify-account.top or paypal-login.net
                val containsBrand = parsedHost.contains(brand.token)

                if (distance in 1..2 || (similarity >= 0.75f && similarity < 1.0f)) {
                    riskScore += 50
                    val deceptionMsg = "Deceptive typosquatting impersonating ${brand.brandName} (similarity: ${String.format(Locale.US, "%.0f%%", similarity * 100)})"
                    brandDeception = deceptionMsg
                    signals.add(deceptionMsg)
                    break
                } else if (containsBrand) {
                    riskScore += 45
                    val deceptionMsg = "Brand deception: unauthorized domain referencing ${brand.brandName} token"
                    brandDeception = deceptionMsg
                    signals.add(deceptionMsg)
                    break
                }
            }
        }

        // 8. Context correlation (Urgent lures)
        if (!context.isNullOrBlank()) {
            val lowerContext = context.lowercase(Locale.ROOT)
            val matchedLures = URGENT_LURE_KEYWORDS.filter { lowerContext.contains(it) }
            if (matchedLures.isNotEmpty()) {
                riskScore += 30
                signals.add("Urgent lure context correlated: \"${matchedLures.first()}\"")
            }
        }

        val clampedScore = riskScore.coerceIn(0, 100)
        val confidence = when {
            signals.size >= 3 -> 0.98f
            signals.size == 2 -> 0.92f
            signals.size == 1 -> 0.85f
            else -> 0.75f
        }

        val recommendedAction = when {
            clampedScore >= 70 -> "Block & Quarantine"
            clampedScore >= 40 -> "Caution - Warn User"
            else -> "Allow"
        }

        return LocalUrlAnalysisResult(
            url = trimmedUrl,
            riskScore = clampedScore,
            confidence = confidence,
            signals = signals,
            brandDeception = brandDeception,
            recommendedAction = recommendedAction,
            entropy = fullUrlEntropy
        )
    }

    private fun extractSecondLevelDomain(host: String): String {
        val parts = host.split('.')
        return if (parts.size >= 2) {
            parts[parts.size - 2]
        } else {
            host
        }
    }
}
