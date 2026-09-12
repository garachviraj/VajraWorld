package com.vajraworld.defender

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.vajraworld.defender.domain.engine.FileInspector
import com.vajraworld.defender.ui.navigation.VajraNavGraph
import com.vajraworld.defender.ui.theme.VajraWorldTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as VajraApplication

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
                    app.repository.analyzeUrlLocally(sharedText)
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
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}

