package com.vajraworld.defender.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.theme.*

/**
 * Live Risk Graph (Section 11).
 * Real time-series canvas graph:
 * - Solid line = observed history
 * - Dotted line = forecast
 * - Translucent area = uncertainty band
 * - Continuous pulsing live beacon on current telemetry
 * High-performance zero-allocation rendering cached across animation frames.
 */
@Composable
fun LiveRiskGraph(
    observedPoints: List<Float>, // Values 0.0 to 1.0
    forecastPoints: List<Float> = emptyList(), // Values 0.0 to 1.0
    uncertainty: Float = 0.08f,
    modifier: Modifier = Modifier,
    height: Dp = 130.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "GraphLiveIndicator")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 11f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    // Pre-allocated cached paths & effects to eliminate GC stutter during 60fps animations
    val forecastDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }
    val nowMarkerDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f) }
    val obsPath = remember { Path() }
    val fcPath = remember { Path() }
    val bandArea = remember { Path() }
    val upperPath = remember { Path() }
    val lowerPath = remember { Path() }
    val obsCoords = remember { mutableListOf<Offset>() }
    val fcCoords = remember { mutableListOf<Offset>() }

    var cachedW by remember { mutableStateOf(-1f) }
    var cachedH by remember { mutableStateOf(-1f) }
    var cachedObsHash by remember { mutableStateOf(0) }
    var cachedFcHash by remember { mutableStateOf(0) }
    var cachedUncertainty by remember { mutableStateOf(-1f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(Surface0)
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        if (observedPoints.isEmpty() && forecastPoints.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO RECENT TELEMETRY RECORDED",
                    style = MaterialThemeTypography.labelSmall,
                    color = TextMuted
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val paddingBottom = 16f
                val chartH = h - paddingBottom

                val obsHash = observedPoints.hashCode()
                val fcHash = forecastPoints.hashCode()

                // Recompute geometry ONLY if canvas size or inputs change (Zero allocations during pulse animation)
                if (w != cachedW || h != cachedH || obsHash != cachedObsHash || fcHash != cachedFcHash || uncertainty != cachedUncertainty) {
                    cachedW = w
                    cachedH = h
                    cachedObsHash = obsHash
                    cachedFcHash = fcHash
                    cachedUncertainty = uncertainty

                    val allObs = if (observedPoints.isEmpty()) listOf(0.1f) else observedPoints
                    val obsCount = allObs.size
                    val fcCount = forecastPoints.size
                    val totalSteps = (obsCount + fcCount - 1).coerceAtLeast(1)
                    val stepX = w / totalSteps.toFloat()

                    obsCoords.clear()
                    for (i in allObs.indices) {
                        val x = i * stepX
                        val y = chartH - (allObs[i].coerceIn(0f, 1f) * chartH)
                        obsCoords.add(Offset(x, y))
                    }

                    fcCoords.clear()
                    if (forecastPoints.isNotEmpty() && obsCoords.isNotEmpty()) {
                        fcCoords.add(obsCoords.last())
                        for (i in forecastPoints.indices) {
                            val x = (obsCount + i) * stepX
                            val y = chartH - (forecastPoints[i].coerceIn(0f, 1f) * chartH)
                            fcCoords.add(Offset(x, y))
                        }

                        upperPath.reset()
                        lowerPath.reset()
                        for (i in fcCoords.indices) {
                            val pt = fcCoords[i]
                            val bandPx = uncertainty * chartH
                            val upY = (pt.y - bandPx).coerceAtLeast(0f)
                            val downY = (pt.y + bandPx).coerceAtMost(chartH)
                            if (i == 0) {
                                upperPath.moveTo(pt.x, upY)
                                lowerPath.moveTo(pt.x, downY)
                            } else {
                                upperPath.lineTo(pt.x, upY)
                                lowerPath.lineTo(pt.x, downY)
                            }
                        }

                        bandArea.reset()
                        bandArea.addPath(upperPath)
                        for (i in fcCoords.indices.reversed()) {
                            val pt = fcCoords[i]
                            val bandPx = uncertainty * chartH
                            val downY = (pt.y + bandPx).coerceAtMost(chartH)
                            bandArea.lineTo(pt.x, downY)
                        }
                        bandArea.close()

                        fcPath.reset()
                        fcPath.moveTo(fcCoords.first().x, fcCoords.first().y)
                        for (i in 1 until fcCoords.size) {
                            fcPath.lineTo(fcCoords[i].x, fcCoords[i].y)
                        }
                    }

                    obsPath.reset()
                    if (obsCoords.isNotEmpty()) {
                        obsPath.moveTo(obsCoords.first().x, obsCoords.first().y)
                        for (i in 1 until obsCoords.size) {
                            obsPath.lineTo(obsCoords[i].x, obsCoords[i].y)
                        }
                    }
                }

                // Draw horizontal grid lines (0, 50, 100)
                val gridY0 = chartH
                val gridY50 = chartH * 0.5f
                val gridY100 = 0f

                drawLine(color = BorderSubtle, start = Offset(0f, gridY0), end = Offset(w, gridY0), strokeWidth = 1f)
                drawLine(color = BorderSubtle, start = Offset(0f, gridY50), end = Offset(w, gridY50), strokeWidth = 1f)
                drawLine(color = BorderSubtle, start = Offset(0f, gridY100), end = Offset(w, gridY100), strokeWidth = 1f)

                // Uncertainty Band around forecast
                if (forecastPoints.isNotEmpty() && fcCoords.isNotEmpty()) {
                    drawPath(bandArea, color = Warning.copy(alpha = 0.12f))
                    drawPath(
                        path = fcPath,
                        color = Info,
                        style = Stroke(
                            width = 2.5f,
                            cap = StrokeCap.Round,
                            pathEffect = forecastDashEffect
                        )
                    )
                }

                // Solid Observed Line
                if (obsCoords.isNotEmpty()) {
                    drawPath(
                        path = obsPath,
                        color = Info,
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )

                    // Draw observation points
                    obsCoords.forEach { pt ->
                        drawCircle(color = Surface0, radius = 4.5f, center = pt)
                        drawCircle(color = Info, radius = 2.5f, center = pt)
                    }

                    // "NOW" vertical divider marker with live pulse
                    val nowPt = obsCoords.last()
                    drawLine(
                        color = Warning.copy(alpha = 0.6f),
                        start = Offset(nowPt.x, 0f),
                        end = Offset(nowPt.x, chartH),
                        strokeWidth = 1.5f,
                        pathEffect = nowMarkerDashEffect
                    )
                    // Animated live beacon ripple
                    drawCircle(
                        color = Warning.copy(alpha = pulseAlpha),
                        radius = pulseRadius,
                        center = nowPt
                    )
                    drawCircle(color = Surface0, radius = 5f, center = nowPt)
                    drawCircle(color = Warning, radius = 3.5f, center = nowPt)
                }
            }
        }

        // Axis label row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "T-30m", style = MetadataText)
            Text(text = "T-15m", style = MetadataText)
            Text(text = "NOW", style = MetadataText.copy(color = Warning, fontWeight = FontWeight.Bold))
            Text(text = "+15m (FC)", style = MetadataText.copy(color = Info))
            Text(text = "+30m (FC)", style = MetadataText.copy(color = Info))
        }
    }
}

private val MaterialThemeTypography = Typography
