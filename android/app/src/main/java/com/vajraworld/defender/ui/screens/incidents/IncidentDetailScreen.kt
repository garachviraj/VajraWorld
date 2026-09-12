package com.vajraworld.defender.ui.screens.incidents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onAcknowledge: (String) -> Unit
) {
    val scrollState = rememberScrollState()

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
                    .border(1.dp, CriticalBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PREDICTED ATTACK PROGRESSION",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Critical)
                        )
                        ConfidenceBadge(confidence = incident.confidence)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = incident.title,
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = BorderSubtle)
                    Spacer(modifier = Modifier.height(8.dp))

                    TechnicalMetadataRow(label = "Risk Evaluation", value = "${(incident.risk * 100).toInt()}%")
                    TechnicalMetadataRow(label = "Estimated Progression ETA", value = "~${incident.etaSeconds}s")
                    TechnicalMetadataRow(label = "Forecast Stage", value = incident.predictedStage)
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
                    if (incident.evidence.isEmpty()) {
                        Text(
                            text = "• Telemetry evidence recorded in World Model state buffer",
                            style = Typography.bodySmall.copy(color = TextPrimary)
                        )
                    } else {
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
            }

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onTestDefence,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Info)
                ) {
                    Text(
                        text = "SIMULATE DEFENCE",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = Bg0, fontWeight = FontWeight.Bold)
                    )
                }

                Button(
                    onClick = { onAcknowledge(incident.incidentId) },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Surface2)
                ) {
                    Text(
                        text = "ACKNOWLEDGE",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = TextPrimary)
                    )
                }
            }
        }
    }
}
