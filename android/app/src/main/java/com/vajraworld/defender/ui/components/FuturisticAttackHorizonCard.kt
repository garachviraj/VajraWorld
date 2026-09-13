package com.vajraworld.defender.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.theme.*
import kotlin.math.sin

@Composable
fun FuturisticAttackHorizonCard(
    threatVelocity: String,
    etaSeconds: Int,
    horizonBars: List<Float>, // 4 values from 0f to 1f
    predictedStage: String,
    criticalAsset: String,
    targetedService: String,
    primaryRecommendation: String,
    onSimulate: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Infinite Animation for subtle cyber wave ripple and glowing node pulses
    val infiniteTransition = rememberInfiniteTransition(label = "HorizonCyberMotion")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Surface0)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Row: Title on Left, Badges on Right (Guaranteed Single-Line No Wrapping)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(InfoBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = Info,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "ATTACK HORIZON PROJECTION",
                            style = TechnicalValue.copy(fontSize = 10.5.sp, color = TextPrimary, fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Latent Neural World Model • 120s Forecast",
                            style = MetadataText.copy(fontSize = 8.sp, color = TextMuted),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status Badges (Never wrap, strictly single line)
                Row(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(InfoBg, RoundedCornerShape(4.dp))
                            .border(1.dp, InfoBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = threatVelocity,
                            style = TechnicalValue.copy(fontSize = 8.5.sp, color = Info, fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(CriticalBg, RoundedCornerShape(4.dp))
                            .border(1.dp, CriticalBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = "ETA ~${etaSeconds}s",
                            style = TechnicalValue.copy(fontSize = 8.5.sp, color = Critical, fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // High-Tech Cyber Waveform Horizon Canvas (Compact 52dp height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Surface1)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                val bars = horizonBars.take(4).ifEmpty { listOf(0.2f, 0.4f, 0.65f, 0.85f) }
                val stages = listOf(
                    "+30s Access",
                    "+60s Probe",
                    "+90s Sniff",
                    "+120s Exfil"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val stepX = w / (bars.size.coerceAtLeast(1) + 1)
                    val points = mutableListOf<Offset>()

                    // Calculate point offsets with subtle harmonic wave
                    bars.forEachIndexed { idx, barVal ->
                        val px = stepX * (idx + 1)
                        val baselineY = h - (barVal * (h * 0.55f)).coerceIn(10f, h - 14f)
                        val waveY = baselineY + sin(wavePhase + idx * 1.4f) * 2f
                        points.add(Offset(px, waveY))
                    }

                    // 1. Draw smooth gradient area under curve
                    if (points.size >= 2) {
                        val fillPath = Path().apply {
                            moveTo(points.first().x, h)
                            lineTo(points.first().x, points.first().y)
                            for (i in 0 until points.size - 1) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val cx = (p0.x + p1.x) / 2f
                                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            }
                            lineTo(points.last().x, h)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Critical.copy(alpha = 0.22f),
                                    Info.copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = h
                            )
                        )

                        // 2. Draw glowing spline line
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 0 until points.size - 1) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val cx = (p0.x + p1.x) / 2f
                                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            }
                        }
                        // Outer glow
                        drawPath(
                            path = linePath,
                            color = Info.copy(alpha = 0.3f * glowPulse),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Core neon line
                        drawPath(
                            path = linePath,
                            brush = Brush.horizontalGradient(
                                listOf(Healthy, Warning, Critical)
                            ),
                            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // 3. Draw pulsing horizon node beacons
                    points.forEachIndexed { idx, pt ->
                        val barVal = bars.getOrElse(idx) { 0.5f }
                        val nodeColor = if (barVal > 0.6f) Critical else if (barVal > 0.35f) Warning else Healthy

                        // Glowing outer ring
                        drawCircle(
                            color = nodeColor.copy(alpha = 0.25f * glowPulse),
                            radius = 6.dp.toPx(),
                            center = pt
                        )
                        // Core dot
                        drawCircle(
                            color = nodeColor,
                            radius = 3.dp.toPx(),
                            center = pt
                        )
                        // Center pip
                        drawCircle(
                            color = Bg0,
                            radius = 1.2.dp.toPx(),
                            center = pt
                        )
                    }
                }

                // Stage percentage pills positioned along the horizon bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    bars.forEachIndexed { idx, barVal ->
                        val barColor = if (barVal > 0.6f) Critical else if (barVal > 0.35f) Warning else Healthy
                        val label = stages.getOrElse(idx) { "+${(idx + 1) * 30}s" }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "${(barVal * 100).toInt()}%",
                                style = TechnicalValue.copy(fontSize = 7.5.sp, color = barColor, fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = label,
                                style = MetadataText.copy(fontSize = 7.sp, color = TextMuted),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Compact Bottom Telemetry Strip: Predicted Stage, Target Asset & Simulate Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(5.dp))
                    .background(Surface1)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "STAGE: $predictedStage",
                        style = TechnicalValue.copy(fontSize = 8.5.sp, color = Warning, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "•",
                        style = TechnicalValue.copy(fontSize = 8.5.sp, color = TextMuted)
                    )
                    Text(
                        text = criticalAsset,
                        style = TechnicalValue.copy(fontSize = 8.sp, color = Info),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onSimulate,
                    modifier = Modifier.height(24.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Info),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = Bg0, modifier = Modifier.size(11.dp))
                        Text(text = "SIMULATE", style = TechnicalValue.copy(fontSize = 8.5.sp, color = Bg0, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}
