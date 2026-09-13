package com.vajraworld.defender.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.vajraworld.defender.data.local.IncidentEntity
import com.vajraworld.defender.data.local.SecurityEventEntity
import com.vajraworld.defender.data.local.VajraDatabase
import com.vajraworld.defender.domain.engine.InstalledAppScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AppInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_PACKAGE_ADDED && action != Intent.ACTION_PACKAGE_REPLACED) return

        val packageName = intent.data?.schemeSpecificPart ?: return
        if (packageName == context.packageName) return

        // CRITICAL: Hold broadcast receiver lifecycle active during background inspection
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val report = InstalledAppScanner.scanSinglePackage(context, packageName) ?: return@launch
                val db = VajraDatabase.getInstance(context)

                val isHighRisk = report.riskScore >= 40 || report.riskReasons.isNotEmpty()
                val eventType = if (isHighRisk) "NEW_APP_RISK_DETECTED" else "NEW_APP_INSTALL_VERIFIED"

                val event = SecurityEventEntity(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    eventType = eventType,
                    source = packageName,
                    risk = report.riskScore.toFloat(),
                    confidence = 0.95f,
                    explanation = "Newly installed app '${report.appName}' audited: ${
                        if (report.riskReasons.isNotEmpty()) report.riskReasons.joinToString("; ")
                        else "Verified clean - no toxic permission or bytecode signatures"
                    }",
                    rawContentHash = "pkg_${packageName}_${System.currentTimeMillis()}",
                    isSynthetic = false
                )
                db.dao().insertSecurityEvent(event)

                // Persist Incident in Room Database
                val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val severity = when {
                    report.riskScore >= 60 -> "CRITICAL"
                    report.riskScore >= 40 -> "HIGH"
                    report.riskScore >= 20 -> "MEDIUM"
                    else -> "LOW"
                }
                val predictedStage = when {
                    report.riskScore >= 60 -> "Toxic Privilege Escalation / Execution"
                    report.riskScore >= 40 -> "Suspicious Permission Binding"
                    else -> "Application Sandbox Verification"
                }
                val recommendedAction = when {
                    report.riskScore >= 60 -> "Immediately revoke overlay/accessibility permissions or uninstall suspicious package"
                    report.riskScore >= 40 -> "Review requested permissions and restrict background data/battery access"
                    else -> "Verified application install - standard sandbox monitoring active"
                }

                val incident = IncidentEntity(
                    incidentId = "INC-APP-${UUID.randomUUID().toString().take(6).uppercase()}",
                    title = if (isHighRisk) "Suspicious Application Installed: ${report.appName}" else "New Application Verified: ${report.appName}",
                    status = if (isHighRisk) "NEW" else "MONITORED",
                    severity = severity,
                    risk = (report.riskScore / 100f).coerceIn(0.05f, 0.95f),
                    confidence = 0.95f,
                    etaSeconds = if (isHighRisk) 30 else 0,
                    predictedStage = predictedStage,
                    affectedAssetsJson = Gson().toJson(listOf(report.appName, packageName, if (report.isSideloaded) "Sideloaded APK" else "Play Store Package")),
                    evidenceJson = Gson().toJson(if (report.riskReasons.isNotEmpty()) report.riskReasons else listOf("Verified Clean Signature", "${report.requestedPermissions.size} permissions audited", "Dalvik bytecode verified clean")),
                    recommendedAction = recommendedAction,
                    acknowledged = false,
                    createdAt = nowStr
                )
                db.dao().insertIncidents(listOf(incident))

                val popupIntent = Intent(context, com.vajraworld.defender.ui.screens.popup.ThreatInspectionPopupActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(com.vajraworld.defender.ui.screens.popup.ThreatInspectionPopupActivity.EXTRA_THREAT_TYPE, "NEW_APP_INSTALL")
                    putExtra(com.vajraworld.defender.ui.screens.popup.ThreatInspectionPopupActivity.EXTRA_TARGET_NAME, report.appName)
                    putExtra(com.vajraworld.defender.ui.screens.popup.ThreatInspectionPopupActivity.EXTRA_TARGET_ID, packageName)
                    putExtra(com.vajraworld.defender.ui.screens.popup.ThreatInspectionPopupActivity.EXTRA_RISK_SCORE, report.riskScore)
                    putStringArrayListExtra(
                        com.vajraworld.defender.ui.screens.popup.ThreatInspectionPopupActivity.EXTRA_DETAILS,
                        ArrayList(if (report.riskReasons.isNotEmpty()) report.riskReasons else listOf("Verified Clean Signature", "Audited ${report.requestedPermissions.size} permissions & Dalvik bytecode", "Zero malicious loops or trojan patterns detected"))
                    )
                    putExtra(com.vajraworld.defender.ui.screens.popup.ThreatInspectionPopupActivity.EXTRA_MAGIC_HEADER, if (report.isSideloaded) "SIDELOADED_APK" else "PLAY_STORE_VERIFIED")
                }

                if (isHighRisk) {
                    val reason = report.riskReasons.firstOrNull() ?: "High privilege permissions requested"
                    VajraNotificationManager.sendThreatAlert(
                        context = context,
                        title = "Suspicious App Installed: ${report.appName}",
                        message = "'${report.appName}' ($packageName) flagged: $reason.",
                        targetId = packageName,
                        targetType = "PACKAGE",
                        riskScore = report.riskScore
                    )
                } else {
                    VajraNotificationManager.sendScanCompleteNotification(
                        context = context,
                        title = " App Verified Clean: ${report.appName}",
                        message = "Audited $packageName. Zero toxic permissions or bytecode threats detected.",
                        popupIntent = popupIntent
                    )
                }

                // Launch zero-latency WindowManager floating overlay card directly over home screen / installer
                if (android.provider.Settings.canDrawOverlays(context)) {
                    com.vajraworld.defender.ui.screens.popup.VajraFloatingInspector.showInspectionOverlay(
                        context = context,
                        threatType = "NEW_APP_INSTALL",
                        targetName = report.appName,
                        targetId = packageName,
                        riskScore = report.riskScore,
                        details = if (report.riskReasons.isNotEmpty()) report.riskReasons else listOf("Verified Clean Signature", "Audited ${report.requestedPermissions.size} permissions & Dalvik bytecode", "Zero malicious loops or trojan patterns detected"),
                        magicHeader = if (report.isSideloaded) "SIDELOADED_APK" else "PLAY_STORE_VERIFIED"
                    )
                } else {
                    // Secondary fallback if overlay permission not granted
                    try {
                        context.startActivity(popupIntent)
                    } catch (e: Exception) {
                        Log.w("AppInstallReceiver", "Direct activity start restricted by OS: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("AppInstallReceiver", "Error auditing installed package $packageName: ${e.message}", e)
            } finally {
                try {
                    pendingResult.finish()
                } catch (_: Exception) {}
            }
        }
    }
}
