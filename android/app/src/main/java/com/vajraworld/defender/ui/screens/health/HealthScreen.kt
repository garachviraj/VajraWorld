package com.vajraworld.defender.ui.screens.health

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
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
import java.util.Locale

@Composable
fun HealthScreen(
    viewModel: HealthViewModel,
    onBack: (() -> Unit)? = null,
    onHubClick: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scrollState = rememberScrollState()

    val profiles = listOf("Enterprise IT", "Data Center", "Cloud / Hybrid", "OT / ICS")

    val isTrained = state.status in listOf("TRAINED", "ACTIVE_BENCHMARKED", "HEALTHY", "HARDWARE VALIDATED")
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
            subtitle = "SCIENTIFIC INSTRUMENT PANEL",
            onBack = onBack,
            onHubClick = onHubClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Model Status Card
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Engine RAM: ${state.ramFootprintMb} MB",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Info)
                        )
                        Text(
                            text = "${state.evalsPerSec} evals/sec",
                            style = TechnicalValue.copy(fontSize = 11.sp, color = Healthy)
                        )
                    }
                }
            }

            // Real-Time Latency Sparkline Canvas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Surface0)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INFERENCE LATENCY STREAM (LAST 20 EVALS)",
                            style = TechnicalValue.copy(fontSize = 10.sp, color = Info)
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", state.inferenceLatencyMs)} ms",
                            style = TechnicalValue.copy(fontSize = 12.sp, color = Healthy, fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LatencySparklineCanvas(
                        latencies = state.latencyHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "T-40s", style = MetadataText.copy(fontSize = 9.sp))
                        Text(text = "Target SLA: <25ms", style = MetadataText.copy(fontSize = 9.sp, color = Healthy))
                        Text(text = "NOW", style = MetadataText.copy(fontSize = 9.sp))
                    }
                }
            }

            // Interactive Hardware Benchmark Button
            Button(
                onClick = { viewModel.runHardwareInferenceBenchmark() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Info),
                enabled = !state.isBenchmarking
            ) {
                if (state.isBenchmarking) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Bg0, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EXECUTING 100 INFERENCE CYCLES ON CPU...",
                        color = Bg0,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = Bg0, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RUN HARDWARE INFERENCE BENCHMARK",
                        color = Bg0,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
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
                    TechnicalMetadataRow(label = "P95 Tail Latency", value = "${String.format(Locale.US, "%.1f", state.lastBenchmarkP95Ms)} ms")
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

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(profiles[0], profiles[1]).forEach { prof ->
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(profiles[2], profiles[3]).forEach { prof ->
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
}

@Composable
private fun LatencySparklineCanvas(
    latencies: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .background(Bg0, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        if (latencies.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height
        val maxVal = 25f // 25ms max scale
        val minVal = 0f

        val stepX = if (latencies.size > 1) w / (latencies.size - 1) else w

        val path = Path()
        latencies.forEachIndexed { index, lat ->
            val x = index * stepX
            val normalizedY = ((lat - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
            val y = h * (1f - normalizedY)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = Info,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw last point dot
        if (latencies.isNotEmpty()) {
            val lastX = (latencies.size - 1) * stepX
            val lastY = h * (1f - ((latencies.last() - minVal) / (maxVal - minVal)).coerceIn(0f, 1f))
            drawCircle(
                color = Healthy,
                radius = 4.dp.toPx(),
                center = Offset(lastX, lastY)
            )
        }
    }
}
