package com.vajraworld.defender.ui.screens.simulation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.components.SecurityStatusPill
import com.vajraworld.defender.ui.components.TechnicalMetadataRow
import com.vajraworld.defender.ui.components.VajraTopBar
import com.vajraworld.defender.ui.theme.*

@Composable
fun SimulationScreen(
    viewModel: SimulationViewModel,
    onBack: (() -> Unit)? = null,
    onHubClick: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1)
            .verticalScroll(scrollState)
    ) {
        VajraTopBar(
            title = "DEFENCE SIMULATOR",
            subtitle = "COUNTERFACTUAL WORLD MODEL REPLAY",
            onBack = onBack,
            onHubClick = onHubClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Read-Only Safety Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(HealthyBg)
                    .border(1.dp, HealthyBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "READ-ONLY LATENT SIMULATION",
                    style = TechnicalValue.copy(fontSize = 11.sp, color = Healthy)
                )
                Text(
                    text = "Zero Production Disruption",
                    style = MetadataText
                )
            }

            // Target Asset Selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "TARGET ASSET FOR CONTAINMENT",
                        style = TechnicalValue.copy(fontSize = 10.sp, color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.targetAsset,
                        style = TechnicalValue.copy(fontSize = 15.sp, color = Info)
                    )
                }
            }

            // Action Picker (Section 34)
            Text(
                text = "WHAT WOULD YOU LIKE TO CHANGE?",
                style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.availableActions.forEach { action ->
                    val isSelected = state.selectedAction == action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) InfoBg else Surface0)
                            .border(1.dp, if (isSelected) InfoBorder else BorderColor, RoundedCornerShape(8.dp))
                            .clickable { viewModel.selectAction(action) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = action.replace("_", " "),
                            style = TechnicalValue.copy(
                                fontSize = 12.sp,
                                color = if (isSelected) Info else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, if (isSelected) Info else TextMuted, CircleShape)
                                .background(if (isSelected) Info else Color.Transparent)
                        )
                    }
                }
            }

            // Run Simulation CTA Button
            Button(
                onClick = { viewModel.runSimulation() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Info),
                enabled = !state.isRunning
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Simulate",
                    tint = Bg0,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.isRunning) "REPLAYING TRAJECTORY IN WORLD MODEL..." else "RUN COUNTERFACTUAL SIMULATION",
                    color = Bg0,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Prompt when no simulation run yet
            if (state.lastResult == null && !state.isRunning) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = Surface0)
                ) {
                    Text(
                        text = "Select an intervention above and tap 'RUN COUNTERFACTUAL SIMULATION' to forecast the post-containment attack trajectory and evaluate risk reduction.",
                        modifier = Modifier.padding(14.dp),
                        style = MetadataText.copy(color = TextSecondary, lineHeight = 18.sp)
                    )
                }
            }

            // Replay Output Card (Section 35)
            AnimatedVisibility(
                visible = state.lastResult != null && !state.isRunning,
                enter = fadeIn()
            ) {
                state.lastResult?.let { res ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, HealthyBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Surface0)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LATENT WORLD MODEL SIMULATION",
                                    style = TechnicalValue.copy(fontSize = 10.sp, color = Healthy)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(HealthyBg, RoundedCornerShape(4.dp))
                                        .border(1.dp, HealthyBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "RECOMMENDED ACTION",
                                        style = TechnicalValue.copy(fontSize = 9.sp, color = Healthy)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Before vs After Risk Comparison
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "BEFORE (BASELINE)", style = MetadataText)
                                    Text(
                                        text = "${(res.baselineRisk * 100).toInt()}%",
                                        style = TechnicalValue.copy(fontSize = 24.sp, color = Critical, fontWeight = FontWeight.Black)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "RISK REDUCTION", style = MetadataText)
                                    Text(
                                        text = "-${res.riskReductionPct}%",
                                        style = TechnicalValue.copy(fontSize = 24.sp, color = Healthy, fontWeight = FontWeight.Black)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "AFTER (RESIDUAL)", style = MetadataText)
                                    Text(
                                        text = "${(res.residualRisk * 100).toInt()}%",
                                        style = TechnicalValue.copy(fontSize = 24.sp, color = Healthy, fontWeight = FontWeight.Black)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = BorderSubtle)
                            Spacer(modifier = Modifier.height(10.dp))

                            TechnicalMetadataRow(label = "New Likely Stage", value = res.newLikelyStage)
                            TechnicalMetadataRow(label = "Operational Disruption", value = res.disruptionRating)
                            TechnicalMetadataRow(label = "Defence Utility Score", value = String.format(java.util.Locale.US, "%.2f", res.utilityScore))
                            TechnicalMetadataRow(label = "Simulation ID", value = res.simulationId, allowCopy = true)
                        }
                    }
                }
            }
        }
    }
}
