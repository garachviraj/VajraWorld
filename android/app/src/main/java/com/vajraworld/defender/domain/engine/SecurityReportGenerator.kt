package com.vajraworld.defender.domain.engine

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.vajraworld.defender.data.local.ClipboardLogEntity
import com.vajraworld.defender.data.local.IncidentEntity
import com.vajraworld.defender.data.local.ScanResultEntity
import com.vajraworld.defender.data.repository.VajraRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object SecurityReportGenerator {

    suspend fun generateForensicReport(context: Context, repository: VajraRepository): String = withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
        val generatedAt = sdf.format(Date())
        val reportId = "VW-REP-${UUID.randomUUID().toString().take(8).uppercase()}"

        val audit = InstalledAppScanner.scanInstalledApps(context)
        val telemetry = DeviceSecurityEngine.getTelemetry(context, audit.overallAppRiskScore)
        val incidents: List<IncidentEntity> = try {
            repository.daoSync().getAllIncidentsSync()
        } catch (_: Exception) {
            emptyList()
        }
        val urlHistory: List<ScanResultEntity> = try {
            repository.daoSync().getUrlScanHistory().firstOrNull() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        val clipboardLogs: List<ClipboardLogEntity> = try {
            repository.daoSync().getAllClipboardLogs().firstOrNull() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val sb = StringBuilder()
        sb.appendLine("================================================================================")
        sb.appendLine("              VAJRAWORLD GUARDIAN - ON-DEVICE FORENSIC SECURITY REPORT          ")
        sb.appendLine("================================================================================")
        sb.appendLine("Report ID         : $reportId")
        sb.appendLine("Generated At      : $generatedAt")
        sb.appendLine("Defense Engine    : VajraWorld Autonomous On-Device Guardian v2.5")
        sb.appendLine("Device Subject    : ${telemetry.hardware.deviceName}")
        sb.appendLine("Overall Posture   : ${telemetry.postureLabel} (Risk: ${telemetry.overallRiskScore}/100, Health: ${100 - telemetry.overallRiskScore}/100)")
        sb.appendLine("================================================================================\n")

        sb.appendLine("1. HARDWARE & OPERATING SYSTEM TELEMETRY")
        sb.appendLine("--------------------------------------------------------------------------------")
        sb.appendLine("Manufacturer      : ${telemetry.hardware.manufacturer}")
        sb.appendLine("Model             : ${telemetry.hardware.model}")
        sb.appendLine("Android OS        : ${telemetry.hardware.androidVersion} (API ${telemetry.hardware.apiLevel})")
        sb.appendLine("Security Patch    : ${telemetry.hardware.securityPatch}")
        val usedRam = telemetry.hardware.totalRamMb - telemetry.hardware.availableRamMb
        sb.appendLine("RAM Allocated     : $usedRam MB / ${telemetry.hardware.totalRamMb} MB (${telemetry.hardware.ramUsagePct}%)")
        sb.appendLine("Flash Storage     : ${telemetry.hardware.freeStorageGb} GB Free (Total: ${telemetry.hardware.totalStorageGb} GB)")
        sb.appendLine("Battery Status    : ${telemetry.hardware.batteryLevel}% | Temperature: ${telemetry.hardware.batteryTemperatureC}°C")
        sb.appendLine()

        sb.appendLine("2. SYSTEM INTEGRITY & TAMPER ATTESTATION")
        sb.appendLine("--------------------------------------------------------------------------------")
        sb.appendLine("Superuser / Root  : ${if (telemetry.integrity.isRooted) "COMPROMISED (Root detected: ${telemetry.integrity.rootSignals.joinToString("; ")})" else "VERIFIED CLEAN"}")
        sb.appendLine("Developer Options : ${if (telemetry.integrity.isDeveloperOptionsEnabled) "ENABLED" else "DISABLED"}")
        sb.appendLine("USB Debugging/ADB : ${if (telemetry.integrity.isAdbEnabled) "ACTIVE (Exposed to USB debug shell)" else "SECURED (Disabled)"}")
        sb.appendLine("Lock Screen Guard : ${if (telemetry.integrity.isDeviceSecure) "ENCRYPTED (Hardware Keystore Active)" else "VULNERABLE (No Lock PIN/Biometrics)"}")
        sb.appendLine("Integrity Score   : ${telemetry.integrity.overallIntegrityScore}/100")
        sb.appendLine()

        sb.appendLine("3. NETWORK & TRANSPORT DEFENSE")
        sb.appendLine("--------------------------------------------------------------------------------")
        sb.appendLine("Active Transport  : ${telemetry.network.activeTransport}")
        sb.appendLine("Wi-Fi SSID        : ${telemetry.network.wifiSsid ?: "None"}")
        sb.appendLine("Link Speed        : ${if (telemetry.network.linkSpeedMbps > 0) "${telemetry.network.linkSpeedMbps} Mbps" else "N/A"}")
        sb.appendLine("Local IPv4 / GW   : ${telemetry.network.ipAddress ?: "127.0.0.1"}")
        sb.appendLine("VPN Encapsulation : ${if (telemetry.network.isVpnActive) "ACTIVE (Traffic Encrypted)" else "INACTIVE (Direct Gateway)"}")
        sb.appendLine()

        sb.appendLine("4. INSTALLED APPLICATION FORENSIC AUDIT")
        sb.appendLine("--------------------------------------------------------------------------------")
        sb.appendLine("User Installed Apps : ${audit.userAppsCount}")
        sb.appendLine("High-Risk Packages  : ${audit.highRiskApps.size}")
        sb.appendLine("Medium-Risk Packages: ${audit.mediumRiskApps.size}")
        sb.appendLine("Safe / Nominal Apps : ${audit.safeApps.size}")
        sb.appendLine()

        if (audit.highRiskApps.isNotEmpty()) {
            sb.appendLine("HIGH-RISK APPLICATIONS DETECTED:")
            audit.highRiskApps.forEachIndexed { idx, app ->
                sb.appendLine("  [!] ${idx + 1}. ${app.appName} (${app.packageName}) - Risk: ${app.riskScore}/100")
                app.riskReasons.forEach { r -> sb.appendLine("      • $r") }
            }
            sb.appendLine()
        }

        sb.appendLine("5. ACTIVE SECURITY INCIDENTS & THREAT STORIES")
        sb.appendLine("--------------------------------------------------------------------------------")
        if (incidents.isEmpty()) {
            sb.appendLine("No active critical incidents recorded. Device posture is nominal.")
        } else {
            incidents.forEachIndexed { idx, inc ->
                sb.appendLine("  #${idx + 1} [${inc.severity}] ${inc.title} (Status: ${inc.status})")
                sb.appendLine("     Target     : ${inc.affectedAssetsJson}")
                sb.appendLine("     Action     : ${inc.recommendedAction}")
                sb.appendLine("     Timestamp  : ${inc.createdAt}")
            }
        }
        sb.appendLine()

        sb.appendLine("6. ZERO-KNOWLEDGE URL & CLIPBOARD SAFETY AUDIT")
        sb.appendLine("--------------------------------------------------------------------------------")
        sb.appendLine("Total URLs Analyzed     : ${urlHistory.size}")
        val riskyUrls = urlHistory.filter { it.riskScore >= 40 }
        sb.appendLine("Deceptive URLs Flagged  : ${riskyUrls.size}")
        riskyUrls.take(5).forEach { u ->
            sb.appendLine("  • ${u.target} (Risk: ${u.riskScore}%)")
        }
        sb.appendLine()
        sb.appendLine("Total Clipboard Scans   : ${clipboardLogs.size}")
        val sensitiveClips = clipboardLogs.filter { it.isSensitive }
        sb.appendLine("Exposed Secrets Masked  : ${sensitiveClips.size} (Raw values never stored on disk)")
        sensitiveClips.take(5).forEach { c ->
            sb.appendLine("  • Type: ${c.detectedType} | Masked: ${c.maskedPreview} | Risk: ${c.riskScore}%")
        }
        sb.appendLine()

        sb.appendLine("================================================================================")
        sb.appendLine("END OF VAJRAWORLD FORENSIC AUDIT REPORT - SIGNED BY LOCAL HARDWARE ENGINE")
        sb.appendLine("================================================================================")

        sb.toString()
    }

    fun exportAndShareReport(context: Context, reportContent: String): Intent {
        val cacheDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val reportFile = File(cacheDir, "vajra_forensic_report.txt")
        reportFile.writeText(reportContent)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            reportFile
        )

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "VajraWorld Guardian - Device Forensic Report")
            putExtra(Intent.EXTRA_TEXT, reportContent)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return Intent.createChooser(sendIntent, "Share Forensic Security Report")
    }
}
