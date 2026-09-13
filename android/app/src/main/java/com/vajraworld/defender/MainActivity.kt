package com.vajraworld.defender

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.vajraworld.defender.domain.engine.FileInspector
import com.vajraworld.defender.service.VajraGuardianService
import com.vajraworld.defender.service.VajraNotificationManager
import com.vajraworld.defender.ui.components.VajraSplashScreen
import com.vajraworld.defender.ui.navigation.VajraNavGraph
import com.vajraworld.defender.ui.theme.VajraWorldTheme
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var lastAuditedClipboardText: String = ""

    override fun onResume() {
        super.onResume()
        inspectClipboardOnFocus()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            inspectClipboardOnFocus()
        }
    }

    private fun inspectClipboardOnFocus() {
        try {
            val cm = getSystemService(CLIPBOARD_SERVICE) as? android.content.ClipboardManager ?: return
            val clip = cm.primaryClip ?: return
            if (clip.itemCount == 0) return
            val text = clip.getItemAt(0).text?.toString() ?: return
            if (text.isBlank() || text == lastAuditedClipboardText) return
            lastAuditedClipboardText = text

            val result = com.vajraworld.defender.domain.engine.ClipboardSecretEngine.analyze(text)

            val app = application as? VajraApplication
            app?.let { vajraApp ->
                lifecycleScope.launch(Dispatchers.IO) {
                    vajraApp.repository.recordClipboardScan(result, text)
                }
            }

            if (result.isSensitive) {
                VajraNotificationManager.sendSensitiveClipboardAlert(
                    context = this,
                    types = result.detectedTypes,
                    timerSec = result.suggestedClearTimerSec
                )
            }
        } catch (_: Exception) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as VajraApplication

        VajraNotificationManager.createNotificationChannels(this)
        val prefs = getSharedPreferences("vajra_security_settings", MODE_PRIVATE)
        if (prefs.getBoolean("background_sentinel", true)) {
            try {
                VajraGuardianService.start(this)
            } catch (_: Exception) {}
        }

        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
            perms.add(Manifest.permission.READ_MEDIA_IMAGES)
            perms.add(Manifest.permission.READ_MEDIA_VIDEO)
            perms.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val ungranted = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (ungranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, ungranted.toTypedArray(), 101)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            app.repository.seedInitialTelemetry()
        }

        handleSendIntent(intent, app)

        setContent {
            VajraWorldTheme {
                var isAppReady by remember { mutableStateOf(false) }

                Crossfade(
                    targetState = isAppReady,
                    animationSpec = tween(durationMillis = 450),
                    label = "AppStartupTransition"
                ) { ready ->
                    if (ready) {
                        VajraNavGraph(repository = app.repository)
                    } else {
                        VajraSplashScreen(onLoadingComplete = { isAppReady = true })
                    }
                }
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

