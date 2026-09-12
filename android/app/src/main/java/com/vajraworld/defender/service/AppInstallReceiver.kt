package com.vajraworld.defender.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.vajraworld.defender.data.local.SecurityEventEntity
import com.vajraworld.defender.data.local.VajraDatabase
import com.vajraworld.defender.domain.engine.InstalledAppScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

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
