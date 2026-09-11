package com.vajraworld.defender.ui.screens.simulation

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
fun SimulationScreen(viewModel: SimulationViewModel) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "COUNTERFACTUAL SIMULATOR", style = MaterialTheme.typography.titleLarge, color = AccentCyan)
                Text(text = "Test Defence Interventions in World Model", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SafeGreen)
            ) {
                Text(
                    text = "READ-ONLY SAFETY ACTIVE",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = SafeGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Target Asset Selection
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "TARGET ASSET", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = state.targetAsset, style = MaterialTheme.typography.titleMedium, color = PurpleAccent)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Picker
        Text(text = "SELECT HYPOTHETICAL INTERVENTION", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.availableActions.take(3).forEach { action ->
                val isSelected = state.selectedAction == action
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isSelected) AccentCyan else CardDark, RoundedCornerShape(6.dp))
                        .border(1.dp, if (isSelected) AccentCyan else BorderDark, RoundedCornerShape(6.dp))
                        .clickable { viewModel.selectAction(action) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = action.replace("_", " "),
                        fontSize = 11.sp,
                        color = if (isSelected) BgDark else TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Run Button
        Button(
            onClick = { viewModel.runSimulation() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
            enabled = !state.isRunning
        ) {
            Text(
                text = if (state.isRunning) "COMPUTING ROLLOUT..." else "RUN SIMULATION",
                color = BgDark,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Simulation Results Card
        if (state.lastResult != null) {
            val res = state.lastResult!!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AccentCyan, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "SIMULATION OUTCOME REPORT", style = MaterialTheme.typography.labelSmall, color = AccentCyan)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Before vs After Risk Comparison
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "BASELINE RISK", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "${(res.baselineRisk * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, color = ThreatRed)
                        }

                        Text(text = "➔", fontSize = 24.sp, color = TextSecondary)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "SIMULATED RISK", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "${(res.postActionRisk * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, color = SafeGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = BorderDark)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Risk Reduction:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(text = "-${res.riskReductionPct}%", style = MaterialTheme.typography.titleMedium, color = SafeGreen)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Business Interruption:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(text = res.disruptionRating, style = MaterialTheme.typography.bodyMedium, color = WarningAmber)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Action Utility Score:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(text = "${res.utilityScore} (Recommended)", style = MaterialTheme.typography.bodyMedium, color = AccentCyan)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Expected New State:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(text = res.newLikelyStage, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                }
            }
        }
    }
}
