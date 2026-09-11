package com.vajraworld.defender.ui.screens.explainability

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
fun ExplainabilityScreen(viewModel: ExplainabilityViewModel) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(text = "EXPLAINABILITY & EVIDENCE", style = MaterialTheme.typography.titleLarge, color = AccentCyan)
        Text(text = "Layered Forensics for Forecast ${state.forecastId}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

        Spacer(modifier = Modifier.height(16.dp))

        // Level 1: Human Narrative
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "LEVEL 1 — EXECUTIVE SUMMARY", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = state.narrative, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Level 2: Feature Attribution
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "LEVEL 2 — SHAP FEATURE ATTRIBUTION", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                Spacer(modifier = Modifier.height(8.dp))
                state.attributions.forEach { attr ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = attr.description, style = MaterialTheme.typography.bodySmall)
                        val isPos = attr.impact > 0
                        Text(
                            text = "${if (isPos) "+" else ""}${(attr.impact * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isPos) ThreatRed else SafeGreen
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Level 3: Temporal Evidence Timeline
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "LEVEL 3 — TEMPORAL EVIDENCE TIMELINE", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                Spacer(modifier = Modifier.height(8.dp))
                state.temporalEvents.forEach { ev ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = ev.timeOffset, style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                        Text(text = ev.description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                        Text(text = "${(ev.risk * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = if (ev.risk > 0.5f) ThreatRed else SafeGreen)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Level 5: Uncertainty & Coverage Warning
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "LEVEL 5 — CALIBRATION & SENSOR COVERAGE", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = state.uncertaintyWarning, style = MaterialTheme.typography.bodySmall, color = SafeGreen)
            }
        }
    }
}
