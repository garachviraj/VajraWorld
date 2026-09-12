package com.vajraworld.defender.ui.screens.incidents

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.domain.model.Incident
import com.vajraworld.defender.ui.components.ConfidenceBadge
import com.vajraworld.defender.ui.components.SecurityStatusPill
import com.vajraworld.defender.ui.components.TechnicalMetadataRow
import com.vajraworld.defender.ui.components.VajraTopBar
import com.vajraworld.defender.ui.theme.*

@Composable
fun IncidentDetailScreen(
    incident: Incident,
    onBack: () -> Unit,
    onTestDefence: () -> Unit,
    onAcknowledge: (String) -> Unit,
    onResolve: (String) -> Unit = {},
    onContain: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1)
            .verticalScroll(scrollState)
    ) {
        VajraTopBar(
            title = incident.incidentId,
            subtitle = "ATT&CK THREAT FORENSICS",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Incident Hero Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        when (incident.status) {
                            "RESOLVED" -> HealthyBorder
                            "CONTAINED" -> InfoBorder
                            else -> CriticalBorder
                        },
                        RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        when (incident.status) {
                                            "RESOLVED" -> HealthyBg
                                            "CONTAINED" -> InfoBg
                                            else -> CriticalBg
                                        },
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = incident.status,
                                    style = TechnicalValue.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (incident.status) {
                                            "RESOLVED" -> Healthy
                                            "CONTAINED" -> Info
                                            else -> Critical
                                        }
                                    )
                                )
                            }
                            Text(
                                text = incident.severity,
                                style = TechnicalValue.copy(fontSize = 11.sp, color = Warning)
                            )
                        }
                        ConfidenceBadge(confidence = incident.confidence)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = incident.title,
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "MITRE ATT&CK: ${incident.mitreTactic}",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = Warning)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = BorderSubtle)
                    Spacer(modifier = Modifier.height(8.dp))

                    TechnicalMetadataRow(label = "Risk Evaluation", value = "${(incident.risk * 100).toInt()}%")
                    TechnicalMetadataRow(label = "Estimated Progression ETA", value = "~${incident.etaSeconds}s")
                    TechnicalMetadataRow(label = "Forecast Stage", value = incident.predictedStage)
                    TechnicalMetadataRow(label = "Process UID", value = "${incident.processUid}")
                }
            }

            // Affected Assets Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TARGETED ASSETS & INTERFACES",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = Info)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        incident.affectedAssets.forEach { asset ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Surface1)
                                    .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = asset,
                                    style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary)
                                )
                            }
                        }
                    }
                }
            }

            // Correlated Socket & Network Packets Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CORRELATED SOCKET & PACKET LOGS",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = Info)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Active Socket: ${incident.correlatedSocket}",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Bg0)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        incident.correlatedPackets.forEach { pkt ->
                            Text(
                                text = pkt,
                                style = TechnicalValue.copy(
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (pkt.contains("su") || pkt.contains("7.82") || pkt.contains("OVERLAY")) Critical else TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // Evidence Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "OBSERVED EVIDENCE & TELEMETRY SIGNALS",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = Info)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    incident.evidence.forEach { evMap ->
                        val desc = evMap["description"] as? String ?: evMap["type"] as? String ?: evMap.toString()
                        Text(
                            text = "• $desc",
                            style = Typography.bodySmall.copy(color = TextPrimary),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            // Recommended Action Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, InfoBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = InfoBg)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "RECOMMENDED ACTION",
                        style = TechnicalValue.copy(fontSize = 10.sp, color = Info, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = incident.recommendedAction,
                        style = Typography.bodySmall.copy(color = TextPrimary)
                    )
                }
            }

            // Workable Containment & Resolution Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onContain(incident.incidentId) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Warning)
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Bg0, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "CONTAIN",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = Bg0, fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = { onResolve(incident.incidentId) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Healthy)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Bg0, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RESOLVE",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = Bg0, fontWeight = FontWeight.Bold)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTestDefence,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Info)
                ) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Bg0, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SIMULATE DEFENCE",
                        style = TechnicalValue.copy(fontSize = 10.sp, color = Bg0, fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "VajraWorld Incident Forensics: ${incident.incidentId}")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                """
                                === VAJRA FORENSIC INCIDENT DUMP ===
                                ID: ${incident.incidentId}
                                Title: ${incident.title}
                                Severity: ${incident.severity} | Risk: ${(incident.risk * 100).toInt()}%
                                MITRE ATT&CK: ${incident.mitreTactic}
                                Socket: ${incident.correlatedSocket}
                                Packets:
                                ${incident.correlatedPackets.joinToString("\n")}
                                Affected: ${incident.affectedAssets.joinToString(", ")}
                                Recommendation: ${incident.recommendedAction}
                                """.trimIndent()
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Export Incident Forensics"))
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SHARE FORENSICS",
                        style = TechnicalValue.copy(fontSize = 10.sp, color = TextPrimary)
                    )
                }
            }
        }
    }
}
