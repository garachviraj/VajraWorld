package com.vajraworld.defender.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Security State Orb (Section 10).
 * Multi-layer live animated ring:
 * - Continuous rotating outer telemetry ticks
 * - Continuous breathing core pulse
 * - Orbiting beacon dot along current risk arc
 * - Dynamic uncertainty halo
 */
@Composable
fun SecurityStatusOrb(
    currentRisk: Float,           // 0.0 to 1.0 (e.g., 0.18 for 18% risk, or 82% protected)
    forecastRisk: Float = currentRisk + 0.10f,
    uncertainty: Float = 0.08f,    // 0.0 to 1.0 (halo width)
    size: Dp = 190.dp,
    modifier: Modifier = Modifier
) {
    val animatedCurrentRisk by animateFloatAsState(
        targetValue = currentRisk.coerceIn(0f, 1f),
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "currentRisk"
    )

    val animatedForecastRisk by animateFloatAsState(
        targetValue = forecastRisk.coerceIn(0f, 1f),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "forecastRisk"
    )

    val animatedUncertainty by animateFloatAsState(
        targetValue = uncertainty.coerceIn(0.02f, 0.40f),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "uncertainty"
    )

    // Continuous Live Ambient Animations
    val infiniteTransition = rememberInfiniteTransition(label = "OrbLiveMotion")

    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloRotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val beaconProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beaconProgress"
    )

    val currentRiskScore = (animatedCurrentRisk * 100).toInt()
    val securityIndex = 100 - currentRiskScore

    val animatedSecurityIndex by animateIntAsState(
        targetValue = securityIndex,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "securityIndexInt"
    )

    val statusColor = when {
        currentRiskScore >= 65 -> Critical
        currentRiskScore >= 35 -> Warning
        else -> Healthy
    }

    val statusLabel = when {
        currentRiskScore >= 65 -> "HIGH RISK"
        currentRiskScore >= 35 -> "ELEVATED"
        else -> "PROTECTED"
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f

            // Outer Rotating Dashed Telemetry Ring
            val tickPathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)
            rotate(degrees = haloRotation, pivot = center) {
                drawCircle(
                    color = BorderColor.copy(alpha = 0.7f),
                    radius = radius * 0.95f,
                    center = center,
                    style = Stroke(width = 1.5f, pathEffect = tickPathEffect)
                )
            }

            // Layer 3: Uncertainty Halo (translucent band)
            val haloWidth = (radius * 0.10f + animatedUncertainty * radius * 0.30f)
            drawCircle(
                color = UncertaintyHaloColor,
                radius = radius * 0.85f,
                center = center,
                style = Stroke(width = haloWidth)
            )

            // Layer 2: Baseline Track Ring
            val arcTrackRadius = radius * 0.82f
            drawCircle(
                color = BorderColor.copy(alpha = 0.35f),
                radius = arcTrackRadius,
                center = center,
                style = Stroke(width = 8f)
            )

            // Layer 1: Animated Arc Fill (Current Risk)
            val startAngle = -90f
            val sweepAngle = animatedCurrentRisk * 360f
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to statusColor.copy(alpha = 0.5f),
                    0.5f to statusColor,
                    1.0f to statusColor.copy(alpha = 0.8f),
                    center = center
                ),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 9f, cap = StrokeCap.Round)
            )

            // Moving Orbiting Beacon Dot
            val beaconAngle = startAngle + (beaconProgress * 360f)
            val sweepRad = Math.toRadians(beaconAngle.toDouble())
            val beaconX = center.x + (arcTrackRadius * cos(sweepRad)).toFloat()
            val beaconY = center.y + (arcTrackRadius * sin(sweepRad)).toFloat()

            drawCircle(
                color = statusColor.copy(alpha = 0.25f),
                radius = 8f,
                center = Offset(beaconX, beaconY)
            )
            drawCircle(
                color = TextWhite,
                radius = 3.5f,
                center = Offset(beaconX, beaconY)
            )

            // Outer Soft Ambient Glow for core
            val coreGlowRadius = radius * 0.72f * pulseScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(statusColor.copy(alpha = 0.14f), Color.Transparent),
                    center = center,
                    radius = coreGlowRadius
                ),
                radius = coreGlowRadius,
                center = center
            )

            // Inner breathing core background (Executive Light Theme)
            val coreRadius = radius * 0.65f * pulseScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Surface0, Surface1),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            // Subtle inner core border
            drawCircle(
                color = BorderColor.copy(alpha = 0.6f),
                radius = coreRadius,
                center = center,
                style = Stroke(width = 1.2f)
            )
        }

        // Center Data: Score & Status
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$animatedSecurityIndex",
                fontSize = 42.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = statusLabel,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${currentRiskScore}% RISK",
                fontSize = 9.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )
        }
    }
}
