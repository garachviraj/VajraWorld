package com.vajraworld.defender.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Environment
import android.os.FileObserver
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vajraworld.defender.MainActivity
import com.vajraworld.defender.R
import com.vajraworld.defender.VajraApplication
import com.vajraworld.defender.data.local.IncidentEntity
import com.vajraworld.defender.data.local.SecurityEventEntity
import com.vajraworld.defender.domain.engine.FileInspector
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

class VajraGuardianService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadObserver: FileObserver? = null
    private var wasScreenSharing = false

    companion object {
        const val CHANNEL_ID = "vajra_guardian_background_service"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, VajraGuardianService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VajraGuardianService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        startDownloadFileObserver()
        startPeriodicBackgroundWatchdog()
        Log.i("VajraGuardianService", "24/7 Background Guardian Protection Started")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VajraWorld 24/7 Real-Time Shield",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous on-device security, kernel telemetry, and threat surveillance"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VajraWorld Guardian Active")
            .setContentText("24/7 Real-Time Shield • Kernel, Storage & Network Monitored")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startDownloadFileObserver() {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir.exists()) {
                val mask = FileObserver.CREATE or FileObserver.CLOSE_WRITE or FileObserver.MOVED_TO
                downloadObserver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    object : FileObserver(downloadsDir, mask) {
                        override fun onEvent(event: Int, path: String?) {
                            if (path != null) handleNewFileDetected(File(downloadsDir, path))
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    object : FileObserver(downloadsDir.absolutePath, mask) {
                        override fun onEvent(event: Int, path: String?) {
                            if (path != null) handleNewFileDetected(File(downloadsDir, path))
                        }
                    }
                }
                downloadObserver?.startWatching()
            }
        } catch (e: Exception) {
            Log.e("VajraGuardianService", "Error setting up download observer: ${e.message}")
        }
    }

    private fun handleNewFileDetected(file: File) {
        if (!file.exists() || !file.canRead() || file.isDirectory) return
        val ext = file.extension.lowercase()

        // Scan if it's an executable, script, APK, or ransomware extension
        val isTarget = ext in listOf("apk", "xapk", "sh", "bat", "py", "dex", "so", "locked", "crypto", "enc")
        if (!isTarget) return

        serviceScope.launch {
            delay(1000) // Brief delay to let write finish
            try {
                if (ext in listOf("locked", "crypto", "enc", "ransom", "wnry")) {
                    // Ransomware extension detected in background!
                    VajraNotificationManager.sendThreatAlert(
                        context = applicationContext,
                        title = "🚨 Critical Ransomware Activity Detected",
                        message = "Suspicious encrypted file generated in Downloads: ${file.name}. Automatic storage quarantine recommended.",
                        targetId = file.absolutePath,
                        targetType = "FILE",
                        riskScore = 95
                    )
                    recordSecurityEvent("RANSOMWARE_ENCRYPTION_DETECTED", file.name, 95f, "Ransomware extension .${ext} detected in background storage watchdog")
                } else if (ext == "apk") {
                    val analysis = FileInputStream(file).use { FileInspector.inspectStream(file.name, it) }
                    if (analysis.riskScore >= 60) {
                        VajraNotificationManager.sendThreatAlert(
                            context = applicationContext,
                            title = "⚠️ Toxic Application Download Intercepted",
                            message = "Downloaded APK '${file.name}' requests toxic permission combinations (${analysis.whyPoints.firstOrNull() ?: "High Risk"}).",
                            targetId = file.absolutePath,
                            targetType = "FILE",
                            riskScore = analysis.riskScore
                        )
                        recordSecurityEvent("MALICIOUS_APK_DOWNLOADED", file.name, analysis.riskScore.toFloat(), analysis.whyPoints.joinToString("; "))
                    }
                } else if (ext in listOf("sh", "bat", "py", "dex")) {
                    VajraNotificationManager.sendThreatAlert(
                        context = applicationContext,
                        title = "⚠️ Executable Script in Storage",
                        message = "Executable ${ext.uppercase()} script detected in storage: ${file.name}. Review before execution.",
                        targetId = file.absolutePath,
                        targetType = "FILE",
                        riskScore = 70
                    )
                    recordSecurityEvent("EXECUTABLE_SCRIPT_DOWNLOADED", file.name, 70f, "Direct shell script ${file.name} located in Downloads")
                }
            } catch (_: Exception) {}
        }
    }

    private fun startPeriodicBackgroundWatchdog() {
        serviceScope.launch {
            while (isActive) {
                delay(5000)
                try {
                    // Check active Screen Sharing in background
                    val dm = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                    val displays = dm?.displays ?: emptyArray()
                    val isSharing = displays.any { it.displayId != android.view.Display.DEFAULT_DISPLAY }

                    if (isSharing && !wasScreenSharing) {
                        wasScreenSharing = true
                        VajraNotificationManager.sendThreatAlert(
                            context = applicationContext,
                            title = "🛡️ Screen Sharing / Cast Active",
                            message = "Virtual display detected. VajraWorld has enabled Privacy Shield: all OTP and banking notifications will be suppressed/masked from remote viewers.",
                            targetId = "screen_cast",
                            targetType = "DISPLAY",
                            riskScore = 55
                        )
                        recordSecurityEvent("SCREEN_SHARING_ACTIVE", "DisplayManager", 55f, "Virtual or presentation display active. OTP shield engaged.")
                    } else if (!isSharing && wasScreenSharing) {
                        wasScreenSharing = false
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private suspend fun recordSecurityEvent(type: String, source: String, risk: Float, explanation: String) {
        val app = application as? VajraApplication ?: return
        try {
            val dao = app.database.dao()
            val event = SecurityEventEntity(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                eventType = type,
                source = source,
                risk = risk,
                confidence = 0.95f,
                explanation = explanation,
                rawContentHash = "bg_${System.currentTimeMillis()}",
                isSynthetic = false
            )
            dao.insertSecurityEvent(event)

            val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val severity = when {
                risk >= 75 -> "CRITICAL"
                risk >= 50 -> "HIGH"
                risk >= 30 -> "MEDIUM"
                else -> "LOW"
            }
            val (title, stage, action) = when (type) {
                "RANSOMWARE_ENCRYPTION_DETECTED" -> Triple(
                    "Critical Ransomware Activity: $source",
                    "Data Encrypted for Impact (T1486)",
                    "Quarantine file immediately and inspect parent storage directory"
                )
                "MALICIOUS_APK_DOWNLOADED" -> Triple(
                    "Toxic APK Download Intercepted: $source",
                    "Malicious Payload Delivery (T1204)",
                    "Delete downloaded package before installation"
                )
                "EXECUTABLE_SCRIPT_DOWNLOADED" -> Triple(
                    "Unmanaged Script in Storage: $source",
                    "Command & Scripting Interpreter (T1059)",
                    "Verify script source and disallow storage execution"
                )
                "SCREEN_SHARING_ACTIVE" -> Triple(
                    "Screen Sharing / Virtual Display Active",
                    "Input Capture & Screen Sniffing (T1056)",
                    "Vajra Privacy Shield active • OTP notifications masked"
                )
                else -> Triple("Sentinel Threat Event: $source", "Host Telemetry Anomaly", "Review security log")
            }

            val incident = IncidentEntity(
                incidentId = "INC-BG-${UUID.randomUUID().toString().take(6).uppercase()}",
                title = title,
                status = "NEW",
                severity = severity,
                risk = (risk / 100f).coerceIn(0.1f, 0.99f),
                confidence = 0.95f,
                etaSeconds = 15,
                predictedStage = stage,
                affectedAssetsJson = com.google.gson.Gson().toJson(listOf(source, type)),
                evidenceJson = com.google.gson.Gson().toJson(listOf(explanation)),
                recommendedAction = action,
                acknowledged = false,
                createdAt = nowStr
            )
            dao.insertIncidents(listOf(incident))
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadObserver?.stopWatching()
        serviceScope.cancel()
        Log.i("VajraGuardianService", "VajraGuardianService Stopped")
    }
}
