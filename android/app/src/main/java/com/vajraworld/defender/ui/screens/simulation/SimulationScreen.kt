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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
            .background(BgLight)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DEFENCE SIMULATOR",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Test Counterfactual Interventions in Latent Model",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .background(SafeGreenBg, RoundedCornerShape(8.dp))
                    .border(1.dp, SafeGreenBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "READ-ONLY SAFETY",
                    style = MaterialTheme.typography.labelSmall,
                    color = SafeGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Target Asset Selection
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .border(1.dp, BorderLight, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "TARGET ASSET FOR CONTAINMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.targetAsset,
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Picker
        Text(
            text = "SELECT HYPOTHETICAL INTERVENTION",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
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
                        .background(if (isSelected) BrandBlueLight else SurfaceWhite, RoundedCornerShape(8.dp))
                        .border(1.dp, if (isSelected) BrandBlue else BorderLight, RoundedCornerShape(8.dp))
                        .clickable { viewModel.selectAction(action) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = action.replace("_", " "),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) BrandBlue else TextPrimary
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
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            enabled = !state.isRunning
        ) {
            Text(
                text = if (state.isRunning) "COMPUTING ROLLOUT..." else "RUN SIMULATION",
                color = SurfaceWhite,
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Simulation Results Card
        if (state.lastResult != null) {
            val res = state.lastResult!!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .border(1.dp, BrandBlue, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SIMULATION OUTCOME REPORT",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandBlue
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Before vs After Risk Comparison
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "BASELINE RISK", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(res.baselineRisk * 100).toInt()}%",
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                                color = ThreatRed
                            )
                        }

                        Text(text = "->", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "SIMULATED RISK", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(res.postActionRisk * 100).toInt()}%",
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
                                color = SafeGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = BorderLight)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Risk Reduction:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            text = "-${res.riskReductionPct}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = SafeGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Business Disruption Rating:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            text = res.disruptionRating,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = WarningAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Action Utility Score:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            text = "${res.utilityScore} (Recommended: ${res.isRecommended})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = BrandBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Expected New State:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            text = res.newLikelyStage,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}
