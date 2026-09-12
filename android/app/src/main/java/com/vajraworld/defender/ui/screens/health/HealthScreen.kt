package com.vajraworld.defender.ui.screens.health

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.components.TechnicalMetadataRow
import com.vajraworld.defender.ui.components.VajraTopBar
import com.vajraworld.defender.ui.theme.*
import java.util.Locale

@Composable
fun HealthScreen(viewModel: HealthViewModel) {
    val state by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scrollState = rememberScrollState()

    val profiles = listOf("Enterprise IT", "Data Center", "Cloud / Hybrid", "OT / ICS")

    val isTrained = state.status in listOf("TRAINED", "ACTIVE_BENCHMARKED", "HEALTHY")
    val statusColor = if (isTrained) Healthy else Warning
    val statusBg = if (isTrained) HealthyBg else WarningBg
    val statusBorder = if (isTrained) HealthyBorder else WarningBorder

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1)
            .verticalScroll(scrollState)
    ) {
        VajraTopBar(
            title = "MODEL HEALTH",
            subtitle = "SCIENTIFIC INSTRUMENT PANEL"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Model Status Card (Section 28)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, statusBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                            Text(
                                text = "STATUS: ${state.status}",
                                style = TechnicalValue.copy(fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.Bold)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Health",
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { viewModel.fetchModelHealth() }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "VajraWorld Temporal World Model & Baseline Suite",
                        style = Typography.titleMedium.copy(color = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Version: ${state.modelVersion} • Active: ${state.activeModelId}",
                        style = MetadataText
                    )
                }
            }

            // Benchmark Performance Metrics Grid
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DYNAMIC BENCHMARK VALIDATION",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = Info)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    TechnicalMetadataRow(label = "F1 Score (Campaign Test)", value = String.format(Locale.US, "%.3f", state.accuracy))
                    TechnicalMetadataRow(label = "Brier Calibration Score", value = String.format(Locale.US, "%.3f", state.brierScore))
                    TechnicalMetadataRow(label = "Mean Advance Lead Time", value = "${String.format(Locale.US, "%.1f", state.leadTimeSec)}s")
                    TechnicalMetadataRow(label = "Calibration Method", value = state.calibration)
                    TechnicalMetadataRow(label = "Inference Latency", value = "${String.format(Locale.US, "%.1f", state.inferenceLatencyMs)} ms")
                }
            }

            // Sensor Telemetry & Drift Monitoring
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SENSOR DRIFT & TELEMETRY QUALITY",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = Info)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    TechnicalMetadataRow(label = "Sensor Telemetry Coverage", value = "${(state.sensorCoverage * 100).toInt()}%")
                    TechnicalMetadataRow(label = "Telemetry Age / Freshness", value = "${String.format(Locale.US, "%.1f", state.telemetryFreshnessSec)}s")
                    TechnicalMetadataRow(label = "Out-Of-Distribution (OOD) Rate", value = String.format(Locale.US, "%.3f", state.oodRate))
                    TechnicalMetadataRow(label = "Distribution Drift Score", value = String.format(Locale.US, "%.3f", state.driftScore))
                }
            }

            // Environment Profile Selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ACTIVE ENVIRONMENT PROFILE",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        profiles.take(2).forEach { prof ->
                            val isSel = state.environmentProfile == prof
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) InfoBg else Surface1)
                                    .border(1.dp, if (isSel) InfoBorder else BorderColor, RoundedCornerShape(6.dp))
                                    .clickable { viewModel.setEnvironmentProfile(prof) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = prof,
                                    style = TechnicalValue.copy(
                                        fontSize = 10.sp,
                                        color = if (isSel) Info else TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
