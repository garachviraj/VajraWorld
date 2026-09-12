package com.vajraworld.defender

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.vajraworld.defender.domain.engine.FileInspector
import com.vajraworld.defender.service.VajraNotificationManager
import com.vajraworld.defender.ui.navigation.VajraNavGraph
import com.vajraworld.defender.ui.theme.VajraWorldTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as VajraApplication

        VajraNotificationManager.createNotificationChannels(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            app.repository.seedInitialTelemetry()
        }

        handleSendIntent(intent, app)

        setContent {
            VajraWorldTheme {
                VajraNavGraph(repository = app.repository)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val app = application as VajraApplication
        handleSendIntent(intent, app)
    }

    private fun handleSendIntent(intent: Intent?, app: VajraApplication) {
        if (intent?.action != Intent.ACTION_SEND) return

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        @Suppress("DEPRECATION")
        val sharedStreamUri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (!sharedText.isNullOrBlank()) {
                    val analysis = app.repository.analyzeUrlLocally(sharedText)
                    if (analysis.riskScore >= 40 || analysis.brandDeception != null) {
                        VajraNotificationManager.sendThreatAlert(
                            context = this@MainActivity,
                            title = "Phishing Link Shared to VajraWorld",
                            message = if (analysis.brandDeception != null) analysis.brandDeception!! else "High-risk deceptive URL: $sharedText",
                            targetId = sharedText,
                            targetType = "URL",
                            riskScore = analysis.riskScore
                        )
                    }
                } else if (sharedStreamUri != null) {
                    contentResolver.openInputStream(sharedStreamUri)?.use { inputStream ->
                        val filename = sharedStreamUri.lastPathSegment ?: "shared_file"
                        val result = FileInspector.inspectStream(filename, inputStream)
                        val entity = com.vajraworld.defender.data.local.ScanResultEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            target = filename,
                            scanType = "FILE",
                            riskScore = result.riskScore,
                            confidence = result.confidence,
                            signalsJson = com.google.gson.Gson().toJson(result.whyPoints),
                            sha256 = result.sha256,
                            createdAt = System.currentTimeMillis()
                        )
                        app.database.dao().insertScanResult(entity)

                        if (result.riskScore >= 50) {
                            VajraNotificationManager.sendThreatAlert(
                                context = this@MainActivity,
                                title = "High-Risk File Analyzed",
                                message = "File '$filename' flagged with ${result.whyPoints.firstOrNull() ?: "Dangerous content"}.",
                                targetId = filename,
                                targetType = "FILE",
                                riskScore = result.riskScore
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}

