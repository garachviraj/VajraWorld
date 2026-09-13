package com.vajraworld.defender.ui.screens.popup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.data.local.SecurityEventEntity
import com.vajraworld.defender.data.local.VajraDatabase
import com.vajraworld.defender.ui.components.SecurityStatusPill
import com.vajraworld.defender.ui.components.TechnicalMetadataRow
import com.vajraworld.defender.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class ThreatInspectionPopupActivity : ComponentActivity() {

    companion object {
        const val EXTRA_THREAT_TYPE = "EXTRA_THREAT_TYPE" // "DOWNLOAD_FILE" or "NEW_APP_INSTALL"
        const val EXTRA_TARGET_NAME = "EXTRA_TARGET_NAME"
        const val EXTRA_TARGET_ID = "EXTRA_TARGET_ID"     // file path or package name
        const val EXTRA_RISK_SCORE = "EXTRA_RISK_SCORE"   // int
        const val EXTRA_DETAILS = "EXTRA_DETAILS"         // ArrayList<String>
        const val EXTRA_MAGIC_HEADER = "EXTRA_MAGIC_HEADER"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val threatType = intent.getStringExtra(EXTRA_THREAT_TYPE) ?: "DOWNLOAD_FILE"
        val targetName = intent.getStringExtra(EXTRA_TARGET_NAME) ?: "Unknown Item"
        val targetId = intent.getStringExtra(EXTRA_TARGET_ID) ?: ""
        val riskScore = intent.getIntExtra(EXTRA_RISK_SCORE, 75)
        val details = intent.getStringArrayListExtra(EXTRA_DETAILS) ?: arrayListOf("Suspicious activity detected")
        val magicHeader = intent.getStringExtra(EXTRA_MAGIC_HEADER) ?: "UNVERIFIED"

        setContent {
            VajraWorldTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.82f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ThreatPopupCard(
                        threatType = threatType,
                        targetName = targetName,
                        targetId = targetId,
                        riskScore = riskScore,
                        details = details,
                        magicHeader = magicHeader,
                        onAllow = {
                            handleAllow(threatType, targetName, targetId, riskScore)
                            finish()
                        },
                        onBlockAndDelete = {
                            handleBlockAndDelete(threatType, targetName, targetId, riskScore)
                            finish()
                        },
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }

    private fun handleAllow(threatType: String, name: String, targetId: String, riskScore: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = VajraDatabase.getInstance(applicationContext)
                val event = SecurityEventEntity(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    eventType = "USER_ALLOWED_THREAT",
                    source = targetId,
                    risk = riskScore.toFloat(),
                    confidence = 0.95f,
                    explanation = "User manually whitelisted & allowed $threatType: $name ($targetId)",
                    rawContentHash = "allowed_${System.currentTimeMillis()}",
                    isSynthetic = false
                )
                db.dao().insertSecurityEvent(event)
            } catch (_: Exception) {}
        }
        Toast.makeText(this, "Allowed '$name' - marked safe by user.", Toast.LENGTH_LONG).show()
    }

    private fun handleBlockAndDelete(threatType: String, name: String, targetId: String, riskScore: Int) {
        if (threatType == "DOWNLOAD_FILE") {
            try {
                val file = File(targetId)
                val deleted = if (file.exists()) file.delete() else false
                if (deleted) {
                    Toast.makeText(this, " Threat Blocked & Deleted: $name", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "File quarantined or already removed.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Deletion failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // NEW_APP_INSTALL -> launch system uninstaller prompt
            try {
                val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.parse("package:$targetId")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(uninstallIntent)
                Toast.makeText(this, "Prompting uninstallation for '$name'...", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = VajraDatabase.getInstance(applicationContext)
                val event = SecurityEventEntity(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    eventType = "USER_BLOCKED_AND_DELETED",
                    source = targetId,
                    risk = riskScore.toFloat(),
                    confidence = 0.99f,
                    explanation = "User blocked & requested removal of $threatType: $name ($targetId)",
                    rawContentHash = "blocked_${System.currentTimeMillis()}",
                    isSynthetic = false
                )
                db.dao().insertSecurityEvent(event)
            } catch (_: Exception) {}
        }
    }
}

@Composable
fun ThreatPopupCard(
    threatType: String,
    targetName: String,
    targetId: String,
    riskScore: Int,
    details: List<String>,
    magicHeader: String,
    onAllow: () -> Unit,
    onBlockAndDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, if (riskScore >= 40) CriticalBorder else HealthyBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Surface0)
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Alert badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isThreat = riskScore >= 40
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isThreat) CriticalBg else HealthyBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isThreat) Icons.Default.Warning else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isThreat) Critical else Healthy,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isThreat) "THREAT INTERCEPTED" else "APPLICATION VERIFIED CLEAN",
                            style = TechnicalValue.copy(fontSize = 12.sp, color = if (isThreat) Critical else Healthy, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isThreat) {
                                if (threatType == "DOWNLOAD_FILE") "MALICIOUS DOWNLOAD DETECTED" else "SUSPICIOUS APP INSTALLED"
                            } else {
                                "ZERO BYTECODE LOOPS OR THREATS FOUND"
                            },
                            style = MetadataText.copy(fontSize = 9.sp, color = TextSecondary)
                        )
                    }
                }
                SecurityStatusPill(riskScore = riskScore)
            }

            HorizontalDivider(color = BorderSubtle)

            // Target Name & ID
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = targetName,
                    style = Typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                )
                Text(
                    text = targetId,
                    style = MetadataText.copy(fontSize = 9.sp, color = TextSecondary),
                    maxLines = 2
                )
            }

            // Forensic Details Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Surface1)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TechnicalMetadataRow(label = "Classification", value = if (threatType == "DOWNLOAD_FILE") "File Payload" else "Android Package")
                TechnicalMetadataRow(label = "Signature / Header", value = magicHeader)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "FORENSIC ATTRIBUTION REASONS:",
                    style = TechnicalValue.copy(fontSize = 9.5.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                )
                details.forEach { reason ->
                    Text(
                        text = "• $reason",
                        style = MetadataText.copy(fontSize = 9.sp, color = if (riskScore >= 60) Critical else TextPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons: Allow vs Block & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ALLOW Button
                OutlinedButton(
                    onClick = onAllow,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Healthy)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Healthy, modifier = Modifier.size(16.dp))
                        Text(
                            text = "ALLOW",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Healthy, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // BLOCK & DELETE Button
                Button(
                    onClick = onBlockAndDelete,
                    modifier = Modifier.weight(1.3f).height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Critical)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Bg0, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (threatType == "DOWNLOAD_FILE") "BLOCK & DELETE" else "BLOCK & UNINSTALL",
                            style = TechnicalValue.copy(fontSize = 10.5.sp, color = Bg0, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
