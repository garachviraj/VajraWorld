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
            .background(BgDark)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(text = "MODEL HEALTH & SETTINGS", style = MaterialTheme.typography.titleLarge, color = AccentCyan)
        Text(text = "Operational Telemetry & Safety Profiles", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

        Spacer(modifier = Modifier.height(16.dp))

        // Health Status Badge Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SafeGreen, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "MODEL STATUS: ${state.status}", style = MaterialTheme.typography.titleMedium, color = SafeGreen)
                    Text(text = state.modelVersion, style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Active ID: ${state.activeModelId}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid Metrics
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "TELEMETRY & INFERENCE METRICS", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(12.dp))

                MetricRow("Model Accuracy", "${(state.accuracy * 100).toInt()}%")
                MetricRow("Brier Calibration Score", "${state.brierScore} (Calibrated)")
                MetricRow("Mean Warning Lead Time", "${state.leadTimeSec}s")
                MetricRow("Sensor Coverage", "${(state.sensorCoverage * 100).toInt()}%")
                MetricRow("Telemetry Freshness", "${state.telemetryFreshnessSec}s ago")
                MetricRow("Inference Latency", "${state.inferenceLatencyMs} ms")
                MetricRow("Out-Of-Distribution Rate", "${(state.oodRate * 100).toInt()}%")
                MetricRow("Concept Drift Score", "${state.driftScore}")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Environment Profile Selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "ENVIRONMENT SAFETY PROFILE", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(8.dp))
                profiles.forEach { profile ->
                    val isSelected = state.environmentProfile == profile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(if (isSelected) AccentCyan.copy(alpha = 0.15f) else CardDark, RoundedCornerShape(4.dp))
                            .clickable { viewModel.setEnvironmentProfile(profile) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = profile,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) AccentCyan else TextPrimary
                        )
                        if (isSelected) {
                            Text(text = "ACTIVE", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
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
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}
