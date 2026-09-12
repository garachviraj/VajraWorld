package com.vajraworld.defender.ui.screens.trajectory

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.domain.model.FutureBranch
import com.vajraworld.defender.ui.components.SecurityStatusPill
import com.vajraworld.defender.ui.components.TechnicalMetadataRow
import com.vajraworld.defender.ui.components.VajraTopBar
import com.vajraworld.defender.ui.theme.*

@Composable
fun TrajectoryScreen(
    viewModel: TrajectoryViewModel,
    onNavigateToSimulation: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onHubClick: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Pulse animation for active forecast line
    val infiniteTransition = rememberInfiniteTransition(label = "TrajectoryPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val activeNode = state.timelineNodes.find { it.secondsOffset == state.scrubTimeOffsetSec }
        ?: state.timelineNodes.find { it.secondsOffset == 0 }
        ?: state.timelineNodes.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1)
            .verticalScroll(scrollState)
    ) {
        VajraTopBar(
            title = "ATT&CK TRAJECTORY",
            subtitle = "LATENT RECURRENT MULTI-HORIZON ROLLOUT",
            onBack = onBack,
            onHubClick = onHubClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Threat Velocity & Lead Time Telemetry Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = Info, modifier = Modifier.size(16.dp))
                            Text(
                                text = "TEMPORAL KINEMATICS & LEAD TIME",
                                style = TechnicalValue.copy(fontSize = 10.5.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(InfoBg, RoundedCornerShape(4.dp))
                                .border(1.dp, InfoBorder, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "K-STEP WORLD MODEL",
                                style = TechnicalValue.copy(fontSize = 9.sp, color = Info, fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Surface1)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(text = "THREAT VELOCITY", style = MetadataText.copy(fontSize = 8.5.sp))
                                Text(
                                    text = "+${String.format("%.3f", state.threatVelocity)} /s",
                                    style = TechnicalValue.copy(fontSize = 12.sp, color = Warning, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Surface1)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(text = "PREDICTIVE LEAD TIME", style = MetadataText.copy(fontSize = 8.5.sp))
                                Text(
                                    text = "~${state.leadTimeSec}s Ahead",
                                    style = TechnicalValue.copy(fontSize = 12.sp, color = Info, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Surface1)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(text = "CURRENT STAGE", style = MetadataText.copy(fontSize = 8.5.sp))
                                Text(
                                    text = state.predictedStage.take(12),
                                    style = TechnicalValue.copy(fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // Dynamic Continuous Trajectory Waveform Canvas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PROJECTION TRAJECTORY CURVE (T-60s → T+120s)",
                            style = TechnicalValue.copy(fontSize = 10.5.sp, color = TextSecondary)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp, 2.dp).background(Info))
                                Text(text = "Observed", style = MetadataText.copy(fontSize = 9.sp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp, 2.dp).background(Critical))
                                Text(text = "Projected", style = MetadataText.copy(fontSize = 9.sp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Surface1)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                        val totalSec = 180f
                                        val targetSec = (-60 + (fraction * totalSec)).toInt()
                                        val closest = state.timelineNodes.minByOrNull { kotlin.math.abs(it.secondsOffset - targetSec) }
                                        if (closest != null) {
                                            viewModel.setScrubTime(closest.secondsOffset)
                                        }
                                    }
                                }
                        ) {
                            val w = size.width
                            val h = size.height
                            val midX = w * (60f / 180f) // T-0s point

                            // Draw subtle grid lines
                            drawLine(BorderSubtle, Offset(midX, 0f), Offset(midX, h), strokeWidth = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))

                            // Observed history curve (0 to midX)
                            val obsPath = Path().apply {
                                moveTo(0f, h * 0.85f)
                                lineTo(w * 0.15f, h * 0.80f)
                                lineTo(midX, h * 0.65f)
                            }
                            drawPath(obsPath, Info, style = Stroke(width = 3.dp.toPx()))

                            // Uncertainty shaded band for projection
                            val bandPath = Path().apply {
                                moveTo(midX, h * 0.65f)
                                lineTo(w * 0.5f, h * 0.45f)
                                lineTo(w * 0.75f, h * 0.25f)
                                lineTo(w, h * 0.10f)
                                lineTo(w, h * 0.35f)
                                lineTo(w * 0.75f, h * 0.55f)
                                lineTo(w * 0.5f, h * 0.70f)
                                lineTo(midX, h * 0.65f)
                                close()
                            }
                            drawPath(bandPath, Critical.copy(alpha = 0.12f))

                            // Forecast projected curve (midX to w)
                            val predPath = Path().apply {
                                moveTo(midX, h * 0.65f)
                                lineTo(w * 0.55f, h * 0.50f)
                                lineTo(w * 0.78f, h * 0.32f)
                                lineTo(w, h * 0.18f)
                            }
                            drawPath(
                                predPath,
                                Critical.copy(alpha = pulseAlpha),
                                style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                            )

                            // Scrubber reticle line
                            val scrubFraction = ((activeNode.secondsOffset + 60f) / 180f).coerceIn(0f, 1f)
                            val scrubX = w * scrubFraction
                            drawLine(Color.White, Offset(scrubX, 0f), Offset(scrubX, h), strokeWidth = 2f)
                            drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(scrubX, h * 0.5f))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Timeline Milestones Row (Tap to scrub)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.timelineNodes) { node ->
                            val isSel = node.secondsOffset == activeNode.secondsOffset
                            val nodeColor = if (node.riskPct > 70) Critical else if (node.riskPct > 40) Warning else Info

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Surface2 else Surface1)
                                    .border(1.dp, if (isSel) Info else BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setScrubTime(node.secondsOffset) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${node.riskPct}%",
                                        style = TechnicalValue.copy(fontSize = 10.sp, color = nodeColor, fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = node.offset, style = TechnicalValue.copy(fontSize = 9.sp, color = TextPrimary))
                                    Text(text = node.stage.take(9), style = MetadataText.copy(fontSize = 8.sp))
                                }
                            }
                        }
                    }
                }
            }

            // Inspected Scrub Timeline Details Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, InfoBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "INSPECTED HORIZON STEP (${activeNode.offset})",
                                style = TechnicalValue.copy(fontSize = 11.sp, color = Info, fontWeight = FontWeight.Bold)
                            )
                            Text(text = activeNode.mitreTactic, style = MetadataText.copy(color = Warning))
                        }
                        SecurityStatusPill(riskScore = activeNode.riskPct)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = activeNode.stage, style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = activeNode.description, style = MetadataText.copy(fontSize = 10.sp, color = TextSecondary, lineHeight = 15.sp))

                    if (onNavigateToSimulation != null && activeNode.isPredicted) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onNavigateToSimulation,
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Info)
                        ) {
                            Text(text = "SIMULATE MITIGATION FOR ${activeNode.offset}", style = TechnicalValue.copy(fontSize = 10.5.sp, color = Bg0, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // Multi-Future Probability Branches
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "MULTI-FUTURE PROBABILITY BRANCHES",
                        style = TechnicalValue.copy(fontSize = 11.sp, color = Info, fontWeight = FontWeight.Bold)
                    )

                    state.branches.forEach { branch ->
                        val isSel = state.selectedBranch?.name == branch.name
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Surface2 else Surface1)
                                .border(1.dp, if (isSel) Info else BorderColor, RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectBranch(branch) }
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = branch.name,
                                    style = Typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${(branch.probability * 100).toInt()}%",
                                    style = TechnicalValue.copy(fontSize = 11.sp, color = Info, fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Surface0)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = branch.probability.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .background(if (branch.probability > 0.5f) Healthy else Critical)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Terminal Stage: ${branch.terminalStage} • Trend: ${branch.trajectoryTrend}",
                                style = MetadataText.copy(fontSize = 9.5.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}
