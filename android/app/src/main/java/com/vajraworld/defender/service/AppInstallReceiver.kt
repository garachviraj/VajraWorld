package com.vajraworld.defender.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

        CoroutineScope(Dispatchers.IO).launch {
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
                    else "Verified clean - no toxic permission signatures"
                }",
                rawContentHash = "pkg_${packageName}_${System.currentTimeMillis()}",
                isSynthetic = false
            )
            db.dao().insertSecurityEvent(event)

            // Construct and persist authentic Incident in Room Database
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
                evidenceJson = Gson().toJson(if (report.riskReasons.isNotEmpty()) report.riskReasons else listOf("Verified Clean Signature", "${report.requestedPermissions.size} permissions audited")),
                recommendedAction = recommendedAction,
                acknowledged = false,
                createdAt = nowStr
            )
            db.dao().insertIncidents(listOf(incident))

            if (isHighRisk) {
                val reason = report.riskReasons.firstOrNull() ?: "High privilege permissions requested"
                VajraNotificationManager.sendThreatAlert(
                    context = context,
                    title = "High-Risk App Installed",
                    message = "'${report.appName}' ($packageName) requested toxic permissions: $reason.",
                    targetId = packageName,
                    targetType = "PACKAGE",
                    riskScore = report.riskScore
                )
            }
        }
    }
}
