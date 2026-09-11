package com.vajraworld.defender.ui.screens.health

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.theme.*

@Composable
fun HealthScreen(viewModel: HealthViewModel) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val profiles = listOf("Enterprise IT", "Data Center", "Cloud / Hybrid", "OT / ICS", "Critical Infrastructure")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "MODEL HEALTH & SETTINGS",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Text(
            text = "Operational Telemetry, Calibration & Safety Profiles",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Health Status Badge Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .border(1.dp, SafeGreenBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SafeGreenBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MODEL STATUS: ${state.status}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SafeGreen
                    )
                    Box(
                        modifier = Modifier
                            .background(SurfaceWhite, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = state.modelVersion,
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandBlue
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Active Model ID: ${state.activeModelId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Grid Metrics
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .border(1.dp, BorderLight, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TELEMETRY & INFERENCE METRICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandBlue
                )
                Spacer(modifier = Modifier.height(12.dp))

                MetricRow("Model Accuracy", "${(state.accuracy * 100).toInt()}%")
                MetricRow("Brier Calibration Score", "${state.brierScore} (Well Calibrated)")
                MetricRow("Mean Warning Lead Time", "${state.leadTimeSec}s ahead")
                MetricRow("Sensor Coverage", "${(state.sensorCoverage * 100).toInt()}% nominal")
                MetricRow("Telemetry Freshness", "${state.telemetryFreshnessSec}s ago")
                MetricRow("Inference Latency", "${state.inferenceLatencyMs} ms")
                MetricRow("Out-Of-Distribution Rate", "${(state.oodRate * 100).toInt()}%")
                MetricRow("Concept Drift Score", "${state.driftScore} (Stable)")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Environment Profile Selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .border(1.dp, BorderLight, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ENVIRONMENT SAFETY PROFILE",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandBlue
                )
                Spacer(modifier = Modifier.height(10.dp))
                profiles.forEach { profile ->
                    val isSelected = state.environmentProfile == profile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                if (isSelected) BrandBlueLight else SurfaceWhite,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) BrandBlue else BorderLight,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setEnvironmentProfile(profile) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = profile,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) BrandBlue else TextPrimary
                        )
                        if (isSelected) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
    }
}
