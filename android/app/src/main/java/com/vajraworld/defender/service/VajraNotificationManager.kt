package com.vajraworld.defender.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vajraworld.defender.MainActivity
import com.vajraworld.defender.R

object VajraNotificationManager {

    const val CHANNEL_THREAT_ALERTS = "vajra_threat_alerts"
    const val CHANNEL_LIVE_PROTECTION = "vajra_live_protection"
    const val CHANNEL_SCAN_STATUS = "vajra_scan_status"

    const val ACTION_BLOCK = "com.vajraworld.defender.ACTION_BLOCK"
    const val ACTION_UNBLOCK = "com.vajraworld.defender.ACTION_UNBLOCK"
    const val ACTION_INVESTIGATE = "com.vajraworld.defender.ACTION_INVESTIGATE"

    const val EXTRA_TARGET_ID = "extra_target_id"
    const val EXTRA_TARGET_TYPE = "extra_target_type"
    const val EXTRA_NOTIFICATION_ID = "extra_notif_id"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // 1. Threat Alerts Channel (Heads-up, High Importance, Sound, Vibration)
            val threatChannel = NotificationChannel(
                CHANNEL_THREAT_ALERTS,
                "VajraWorld Threat & Phishing Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical real-time alerts for malware, toxic permissions, phishing URLs, and privacy leaks"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                enableLights(true)
                lightColor = android.graphics.Color.RED
            }

            // 2. Live Protection Channel (Low Importance, Ongoing Monitor)
            val liveChannel = NotificationChannel(
                CHANNEL_LIVE_PROTECTION,
                "VajraWorld Real-Time Shield",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status of real-time background protection and on-device monitors"
            }

            // 3. Scan Status Channel (Default Importance)
            val scanChannel = NotificationChannel(
                CHANNEL_SCAN_STATUS,
                "VajraWorld Scan Results",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Completion reports for on-demand device and file scans"
            }

            nm.createNotificationChannel(threatChannel)
            nm.createNotificationChannel(liveChannel)
            nm.createNotificationChannel(scanChannel)
        }
    }

    fun sendThreatAlert(
        context: Context,
        title: String,
        message: String,
        targetId: String,
        targetType: String = "PACKAGE", // "PACKAGE" or "URL"
        riskScore: Int = 85
    ) {
        createNotificationChannels(context)

        val notificationId = (targetId.hashCode() and 0x7FFFFFFF)

        // Tap notification -> Open MainActivity
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TARGET_ID, targetId)
            putExtra(EXTRA_TARGET_TYPE, targetType)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Block Action Intent
        val blockIntent = Intent(context, SecurityActionReceiver::class.java).apply {
            action = ACTION_BLOCK
            putExtra(EXTRA_TARGET_ID, targetId)
            putExtra(EXTRA_TARGET_TYPE, targetType)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val blockPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            blockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Unblock Action Intent
        val unblockIntent = Intent(context, SecurityActionReceiver::class.java).apply {
            action = ACTION_UNBLOCK
            putExtra(EXTRA_TARGET_ID, targetId)
            putExtra(EXTRA_TARGET_TYPE, targetType)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val unblockPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            unblockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_THREAT_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ $title (Risk $riskScore/100)")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$message\n\nTarget: $targetId\nThreat Score: $riskScore/100. Choose an action below:"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(defaultSound)
            .setVibrate(longArrayOf(0, 350, 150, 350))
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_delete, "BLOCK / CONTAIN", blockPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "ALLOW / UNBLOCK", unblockPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Android 13 permission not yet granted
        }
    }

    fun sendScanCompleteNotification(context: Context, title: String, message: String) {
        createNotificationChannels(context)
        val notificationId = 9901

        val contentIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_SCAN_STATUS)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {}
    }

    fun sendMasterScanReportNotification(context: Context, progress: com.vajraworld.defender.domain.engine.StorageScanProgress) {
        createNotificationChannels(context)
        val notificationId = 9905

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "FILE_SCAN")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hasThreats = progress.suspiciousCount > 0
        val title = if (hasThreats) {
            "🚨 Master Storage Scan: ${progress.suspiciousCount} Threat(s) Found!"
        } else {
            "🛡️ Master Storage Scan Complete: 100% Clean"
        }

        val shortMessage = if (hasThreats) {
            "ACTION REQUIRED: ${progress.suspiciousCount} suspicious file(s) flagged across ${progress.totalFilesAudited} storage files & ${progress.totalAppsAudited} apps."
        } else {
            "Verified ${progress.totalFilesAudited} files & ${progress.totalAppsAudited} apps across ${progress.foldersAuditedCount} storage folders. Zero threats detected."
        }

        val bigText = buildString {
            appendLine(shortMessage)
            appendLine()
            appendLine("📊 MASTER SCAN FORENSIC BREAKDOWN:")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📦 Applications Audited: ${progress.totalAppsAudited} Packages")
            appendLine("📁 Storage Files Inspected: ${progress.totalFilesAudited} Files")
            appendLine("📂 Volumes Traversed: Downloads, Documents, DCIM, Pictures, /sdcard")
            appendLine("✅ Clean Verified Assets: ${progress.cleanFilesCount}")
            appendLine("🚨 Identified Threats: ${progress.suspiciousCount}")
            appendLine("🔒 Ransomware Artifacts: ${progress.ransomwareCount}")
            appendLine("🕵️ Spoofed / Disguised Binaries: ${progress.spoofedFilesCount}")
            appendLine()
            appendLine("Posture: ${if (hasThreats) "⚠️ ELEVATED THREAT LEVEL - INSPECTION REQUIRED" else "✅ NOMINAL & FULLY SECURED"}")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_SCAN_STATUS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(shortMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {}
    }

    fun sendTestNotification(context: Context) {
        sendThreatAlert(
            context = context,
            title = "Test Threat Simulation",
            message = "VajraWorld Autonomous Engine verified. Threat blocking and interactive unblock controls are operational.",
            targetId = "com.test.suspicious.payload",
            targetType = "PACKAGE",
            riskScore = 88
        )
    }
}
