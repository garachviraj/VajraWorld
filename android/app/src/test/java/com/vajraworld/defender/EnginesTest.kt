package com.vajraworld.defender

import com.vajraworld.defender.domain.engine.ClipboardSecretEngine
import com.vajraworld.defender.domain.engine.FileInspector
import com.vajraworld.defender.domain.engine.UrlRuleEngine
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EnginesTest {

    @Test
    fun testUrlRuleEngine_EntropyAndPunycode() {
        val entropyLow = UrlRuleEngine.calculateEntropy("aaaaa")
        assertEquals(0.0f, entropyLow, 0.001f)

        val entropyHigh = UrlRuleEngine.calculateEntropy("abcdefghijklmnop")
        assertTrue(entropyHigh > 3.5f)

        val punycodeResult = UrlRuleEngine.analyze("http://xn--pypal-4ve.com/login")
        assertTrue(punycodeResult.signals.any { it.contains("Punycode") })
        assertTrue(punycodeResult.riskScore >= 40)
    }

    @Test
    fun testUrlRuleEngine_IpLiteralAndHighAbuseTld() {
        val ipResult = UrlRuleEngine.analyze("http://192.168.1.50/admin")
        assertTrue(ipResult.signals.any { it.contains("IP-literal") })
        assertTrue(ipResult.riskScore >= 40)

        val tldResult = UrlRuleEngine.analyze("https://malicious-tracker.xyz/auth")
        assertTrue(tldResult.signals.any { it.contains(".xyz") })
    }

    @Test
    fun testUrlRuleEngine_DangerousExtensions() {
        val apkResult = UrlRuleEngine.analyze("https://download-center.com/payload.apk")
        assertTrue(apkResult.signals.any { it.contains(".apk") })
        assertTrue(apkResult.riskScore >= 50)
    }

    @Test
    fun testUrlRuleEngine_BrandDeceptionTyposquatting() {
        val paypalResult = UrlRuleEngine.analyze("https://paypa1.com/signin")
        assertTrue(paypalResult.brandDeception != null)
        assertTrue(paypalResult.brandDeception!!.contains("PayPal"))
        assertTrue(paypalResult.riskScore >= 50)

        val contextResult = UrlRuleEngine.analyze(
            "https://chase-security.net/verify",
            context = "Account blocked immediately. Verify OTP within 5 minutes."
        )
        assertTrue(contextResult.signals.any { it.contains("Urgent lure context") })
        assertTrue(contextResult.riskScore >= 70)
        assertEquals("Block & Quarantine", contextResult.recommendedAction)
    }

    @Test
    fun testClipboardSecretEngine_AwsAndGithubTokens() {
        val awsText = "Found credentials: AKIAIOSFODNN7EXAMPLE"
        val awsResult = ClipboardSecretEngine.analyze(awsText)
        assertTrue(awsResult.isSensitive)
        assertTrue(awsResult.detectedTypes.contains("AWS Access Key ID"))
        assertFalse(awsResult.valueStored)
        assertTrue(awsResult.riskScore >= 90)

        val ghText = "ghp_111122223333444455556666777788889999"
        val ghResult = ClipboardSecretEngine.analyze(ghText)
        assertTrue(ghResult.isSensitive)
        assertTrue(ghResult.detectedTypes.contains("GitHub Personal Access Token"))
    }

    @Test
    fun testClipboardSecretEngine_CreditCardLuhn() {
        // Genuine Luhn test: 49927398716 is a known valid Luhn number
        assertTrue(ClipboardSecretEngine.isValidLuhn("49927398716"))
        assertFalse(ClipboardSecretEngine.isValidLuhn("49927398717"))

        // Standard test Visa number
        val visaResult = ClipboardSecretEngine.analyze("My card is 4111 1111 1111 1111 thanks")
        assertTrue(visaResult.isSensitive)
        assertTrue(visaResult.detectedTypes.any { it.contains("Credit Card") })
        assertFalse(visaResult.valueStored)
    }

    @Test
    fun testClipboardSecretEngine_MnemonicSeedPhrase() {
        val mnemonic = "abandon ability able about above absent absorb abstract absurd abuse access accident"
        val mnemonicResult = ClipboardSecretEngine.analyze(mnemonic)
        assertTrue(mnemonicResult.isSensitive)
        assertTrue(mnemonicResult.detectedTypes.any { it.contains("Mnemonic") })
        assertEquals(100, mnemonicResult.riskScore)
        assertFalse(mnemonicResult.valueStored)
    }

    @Test
    fun testFileInspector_StreamingSha256() {
        val content = "VajraWorld Guardian Security Test".toByteArray(Charsets.UTF_8)
        val hash = FileInspector.calculateStreamingSha256(ByteArrayInputStream(content))
        assertNotNull(hash)
        assertEquals(64, hash.length) // 64 hex chars for SHA-256
    }

    @Test
    fun testFileInspector_ToxicCombinationDetection_ConfirmedMalware() {
        // Construct mock APK with toxic permissions AND confirmed Banking Trojan bytecode signature
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // AndroidManifest.xml
            zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
            val manifestContent = "android.permission.BIND_ACCESSIBILITY_SERVICE android.permission.SYSTEM_ALERT_WINDOW android.permission.INTERNET"
            zos.write(manifestContent.toByteArray(Charsets.ISO_8859_1))
            zos.closeEntry()

            // classes.dex with Dalvik header and string pool containing trojan signature
            zos.putNextEntry(ZipEntry("classes.dex"))
            val dexBytes = "dex\n035\u0000" + "\u0000".repeat(105) +
                    "AccessibilityNodeInfo;->performAction ACTION_CLICK TYPE_APPLICATION_OVERLAY"
            zos.write(dexBytes.toByteArray(Charsets.ISO_8859_1))
            zos.closeEntry()
        }

        val apkBytes = baos.toByteArray()
        val result = FileInspector.inspectStream(
            filename = "confirmed_banking_trojan.apk",
            inputStream = ByteArrayInputStream(apkBytes),
            fileSize = apkBytes.size.toLong()
        )

        assertTrue(result.isApk)
        assertTrue(result.whyPoints.any { it.contains("Confirmed Banking Trojan") })
        assertEquals(90, result.riskScore)
        assertTrue(result.permissions.contains("android.permission.BIND_ACCESSIBILITY_SERVICE"))
        assertTrue(result.permissions.contains("android.permission.SYSTEM_ALERT_WINDOW"))
    }

    @Test
    fun testFileInspector_BenignRemoteUtility_NotQuarantined() {
        // Legitimate remote support or accessibility automation utility without trojan bytecode
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
            val manifestContent = "package=\"org.example.automation\" android.permission.BIND_ACCESSIBILITY_SERVICE android.permission.SYSTEM_ALERT_WINDOW android.permission.INTERNET"
            zos.write(manifestContent.toByteArray(Charsets.ISO_8859_1))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("classes.dex"))
            val dexBytes = "dex\n035\u0000" + "\u0000".repeat(105) + "clean user utility code"
            zos.write(dexBytes.toByteArray(Charsets.ISO_8859_1))
            zos.closeEntry()
        }

        val apkBytes = baos.toByteArray()
        val result = FileInspector.inspectStream(
            filename = "custom_screen_helper.apk",
            inputStream = ByteArrayInputStream(apkBytes),
            fileSize = apkBytes.size.toLong()
        )

        assertTrue(result.isApk)
        // Must NOT be quarantined (riskScore < 70)
        assertTrue("Ambiguous accessibility tool must not cross quarantine threshold (70)", result.riskScore < 70)
        assertEquals(45, result.riskScore)
        assertTrue(result.whyPoints.any { it.contains("Elevated Review") })
    }

    @Test
    fun testFileInspector_TrustedPublisher_CleanVerdict() {
        // AnyDesk or TeamViewer QuickSupport style reputable publisher
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
            val manifestContent = "package=\"com.anydesk.anydeskandroid\" android.permission.BIND_ACCESSIBILITY_SERVICE android.permission.SYSTEM_ALERT_WINDOW android.permission.INTERNET"
            zos.write(manifestContent.toByteArray(Charsets.ISO_8859_1))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("classes.dex"))
            val dexBytes = "dex\n035\u0000" + "\u0000".repeat(105) + "anydesk remote engine"
            zos.write(dexBytes.toByteArray(Charsets.ISO_8859_1))
            zos.closeEntry()
        }

        val apkBytes = baos.toByteArray()
        val result = FileInspector.inspectStream(
            filename = "anydesk.apk",
            inputStream = ByteArrayInputStream(apkBytes),
            fileSize = apkBytes.size.toLong()
        )

        assertTrue(result.isApk)
        assertEquals(20, result.riskScore)
        assertTrue(result.whyPoints.any { it.contains("trusted remote-support") })
    }
}
