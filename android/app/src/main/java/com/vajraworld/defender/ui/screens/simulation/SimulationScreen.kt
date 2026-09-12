package com.vajraworld.defender.ui.screens.simulation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    text = "Zero Device Disruption",
                    style = MetadataText
                )
            }

            // Notification / Status Message if rule applied
            state.statusMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = HealthyBg),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Healthy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = msg,
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Healthy),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section 1: Threat Scenario Selector
            Text(
                text = "SELECT CYBER ATTACK SCENARIO",
                style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.availableScenarios.forEach { scenario ->
                    val isSelected = scenario.id == state.selectedScenarioId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectScenario(scenario) }
                            .border(
                                1.dp,
                                if (isSelected) Info else BorderColor,
                                RoundedCornerShape(10.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) InfoBg else Surface0
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = scenario.title,
                                    style = TechnicalValue.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Info else TextPrimary
                                    )
                                )
                                Text(
                                    text = scenario.mitreTactic.split(" ").firstOrNull() ?: "",
                                    style = TechnicalValue.copy(fontSize = 9.sp, color = Warning)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = scenario.description,
                                style = MetadataText.copy(color = TextSecondary, fontSize = 11.sp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Vector: ${scenario.attackVector}",
                                    style = MetadataText.copy(fontSize = 10.sp, color = TextMuted)
                                )
                                Text(
                                    text = "Base: ${(scenario.baselineRisk * 100).toInt()}% -> ${(scenario.mitigatedRisk * 100).toInt()}%",
                                    style = TechnicalValue.copy(fontSize = 10.sp, color = if (isSelected) Healthy else TextSecondary)
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Target Device Entity
            Text(
                text = "CONTAINMENT TARGET (DEVICE LAYER)",
                style = TechnicalValue.copy(fontSize = 11.sp, color = TextSecondary)
            )

            val assetScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(assetScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.availableAssets.forEach { asset ->
                    val isSelected = asset == state.targetAsset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) InfoBg else Surface0)
                            .border(1.dp, if (isSelected) Info else BorderColor, RoundedCornerShape(8.dp))
                            .clickable { viewModel.selectTargetAsset(asset) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = asset,
                            style = TechnicalValue.copy(
                                fontSize = 11.sp,
                                color = if (isSelected) Info else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }

            // Section 3: Mitigation Action Picker
            Text(
                text = "COUNTERMEASURE STRATEGY",
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

            // Section 4: Multi-Step Rollout Progression (when running)
            if (state.isRunning) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, InfoBorder, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = InfoBg)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "REPLAYING ATTACK TRAJECTORY IN WORLD MODEL...",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Info, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StepIndicator(stepNum = 1, label = "Ingress Probe", active = state.simulationStep >= 1, done = state.simulationStep > 1)
                            StepIndicator(stepNum = 2, label = "Exploit Hook", active = state.simulationStep >= 2, done = state.simulationStep > 2)
                            StepIndicator(stepNum = 3, label = "Containment", active = state.simulationStep >= 3, done = state.simulationStep >= 3)
                        }
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
                    text = if (state.isRunning) "SIMULATING IN LATENT SPACE..." else "RUN COUNTERFACTUAL SIMULATION",
                    color = Bg0,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Replay Output Card
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
                                    text = "LATENT WORLD MODEL FORECAST",
                                    style = TechnicalValue.copy(fontSize = 10.sp, color = Healthy)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(HealthyBg, RoundedCornerShape(4.dp))
                                        .border(1.dp, HealthyBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "COUNTERMEASURE VERIFIED",
                                        style = TechnicalValue.copy(fontSize = 9.sp, color = Healthy)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Dual Risk Curve Canvas Chart
                            DualRiskComparisonCanvas(
                                baselineRisk = res.baselineRisk,
                                residualRisk = res.residualRisk,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                            )

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
                                        style = TechnicalValue.copy(fontSize = 22.sp, color = Critical, fontWeight = FontWeight.Black)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "RISK REDUCTION", style = MetadataText)
                                    Text(
                                        text = "-${res.riskReductionPct}%",
                                        style = TechnicalValue.copy(fontSize = 22.sp, color = Healthy, fontWeight = FontWeight.Black)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "AFTER (RESIDUAL)", style = MetadataText)
                                    Text(
                                        text = "${(res.residualRisk * 100).toInt()}%",
                                        style = TechnicalValue.copy(fontSize = 22.sp, color = Healthy, fontWeight = FontWeight.Black)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = BorderSubtle)
                            Spacer(modifier = Modifier.height(10.dp))

                            TechnicalMetadataRow(label = "Mitigated State", value = res.newLikelyStage)
                            TechnicalMetadataRow(label = "Operational Disruption", value = res.disruptionRating)
                            TechnicalMetadataRow(label = "Defence Utility Score", value = String.format(java.util.Locale.US, "%.2f", res.utilityScore))
                            TechnicalMetadataRow(label = "Simulation ID", value = res.simulationId, allowCopy = true)

                            Spacer(modifier = Modifier.height(14.dp))

                            // CTA: Apply Mitigation Rule to Active Defender Engine
                            Button(
                                onClick = { viewModel.applyMitigationRule() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Healthy)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Bg0,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "APPLY MITIGATION RULE TO DEFENDER",
                                    color = Bg0,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(stepNum: Int, label: String, active: Boolean, done: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (done) Healthy else if (active) Info else Surface2)
                .border(1.dp, if (done) Healthy else if (active) Info else BorderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$stepNum",
                style = TechnicalValue.copy(
                    fontSize = 11.sp,
                    color = if (active || done) Bg0 else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MetadataText.copy(fontSize = 9.sp, color = if (active || done) TextPrimary else TextMuted)
        )
    }
}

@Composable
private fun DualRiskComparisonCanvas(
    baselineRisk: Float,
    residualRisk: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .background(Bg0, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        val w = size.width
        val h = size.height

        // Draw baseline trajectory (red curve escalating)
        val baselinePath = Path().apply {
            moveTo(0f, h * (1f - 0.3f))
            cubicTo(
                w * 0.35f, h * (1f - 0.45f),
                w * 0.7f, h * (1f - baselineRisk * 0.85f),
                w, h * (1f - baselineRisk)
            )
        }
        drawPath(
            path = baselinePath,
            color = Critical,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw mitigated trajectory (green curve dropping after intervention)
        val mitigatedPath = Path().apply {
            moveTo(0f, h * (1f - 0.3f))
            cubicTo(
                w * 0.35f, h * (1f - 0.45f),
                w * 0.55f, h * (1f - 0.5f),
                w, h * (1f - residualRisk)
            )
        }
        drawPath(
            path = mitigatedPath,
            color = Healthy,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Intervention point marker at x = 0.45w
        val markerX = w * 0.45f
        drawLine(
            color = Info,
            start = Offset(markerX, 0f),
            end = Offset(markerX, h),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}
