package com.vajraworld.defender.domain.engine

import java.util.Locale

/**
 * Offline SHA-256 Allowlist and Reputation Manager.
 * Provides instant O(1) clean-verdict short-circuit for verified system binaries,
 * popular Android libraries, and standard benign reference files.
 */
object AllowlistManager {

    // Known-good SHA-256 hashes (lowercase hex)
    private val KNOWN_CLEAN_HASHES = mapOf(
        // Common Android system stubs / platform test artifacts
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855" to "Standard zero-byte empty file",
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad" to "Standard verified test string payload",
        "4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945" to "Official Android Google Play Services Client library",
        "13b863001859ef09210a47d25e0bc08deec85871b6d194c7b8417c80214eb192" to "Verified AndroidX core platform runtime",
        "822c54ee9f090b83b38c2c069502ab64b1f4133405c75ffaebe408a2fc2521c7" to "Standard Android Support library component"
    )

    private val TRUSTED_PACKAGE_PREFIXES = listOf(
        // Platform & System Infrastructure
        "com.google.",
        "com.android.",
        "com.miui.",
        "com.xiaomi.",
        "cn.wps.",
        "com.vajraworld.defender",
        "org.chromium.",
        "androidx.",
        "com.qualcomm.",
        "com.mediatek.",
        "com.sec.android.",
        "com.samsung.",
        // Reputable Global Publishers & Productivity
        "com.microsoft.",
        "com.adobe.",
        "org.mozilla.",
        "com.whatsapp",
        "org.telegram.",
        "org.thoughtcrime.securesms",
        "com.slack",
        "us.zoom.",
        "com.spotify.",
        "com.x8bit.bitwarden",
        "keepass2android.",
        "org.videolan.vlc"
    )

    // Legitimate Remote Support, Screen Sharing & Automation Utilities
    // These applications legitimately require Accessibility Service and Screen Overlay
    private val TRUSTED_REMOTE_UTILITIES = listOf(
        "com.anydesk.",
        "com.teamviewer.",
        "com.splashtop.",
        "com.logmein.",
        "com.google.android.marvin.talkback",
        "com.arlosoft.macrodroid",
        "net.dinglisch.android.taskerm",
        "com.teslacoilsw.launcher"
    )

    /**
     * Checks if a file's SHA-256 hash is in the known-clean allowlist.
     */
    fun isAllowlisted(sha256: String): Boolean {
        if (sha256.isBlank()) return false
        return KNOWN_CLEAN_HASHES.containsKey(sha256.lowercase(Locale.ROOT))
    }

    /**
     * Gets the description/reason for an allowlisted file.
     */
    fun getAllowlistReason(sha256: String): String? {
        return KNOWN_CLEAN_HASHES[sha256.lowercase(Locale.ROOT)]
    }

    /**
     * Checks if an APK package name or filename belongs to trusted system/platform infrastructure.
     */
    fun isTrustedSystemPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val lower = packageName.lowercase(Locale.ROOT)
        return TRUSTED_PACKAGE_PREFIXES.any { lower.startsWith(it) || lower.contains(it) }
    }

    /**
     * Checks if an APK package name or filename belongs to a known, legitimate
     * remote support or automation utility.
     */
    fun isTrustedRemoteUtility(identifier: String?): Boolean {
        if (identifier.isNullOrBlank()) return false
        val lower = identifier.lowercase(Locale.ROOT)
        return TRUSTED_REMOTE_UTILITIES.any { lower.startsWith(it) || lower.contains(it) } ||
                lower.contains("anydesk") || lower.contains("teamviewer") ||
                lower.contains("quicksupport") || lower.contains("talkback")
    }

    /**
     * Checks if a package name or filename belongs to any reputable publisher or verified utility.
     */
    fun isTrustedPackage(identifier: String?): Boolean {
        if (identifier.isNullOrBlank()) return false
        return isTrustedSystemPackage(identifier) || isTrustedRemoteUtility(identifier)
    }
}
