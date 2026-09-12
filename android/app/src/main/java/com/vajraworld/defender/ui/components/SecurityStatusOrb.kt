package com.vajraworld.defender.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.theme.*

/**
 * Security State Orb (Section 10).
 * Multi-layer animated ring:
 * Layer 1: Current risk arc
 * Layer 2: Forecast risk arc
 * Layer 3: Geometric uncertainty halo that widens geometrically with uncertainty
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

    val currentRiskScore = (animatedCurrentRisk * 100).toInt()
    val securityIndex = 100 - currentRiskScore

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

            // Inner dark cockpit gradient background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Surface1, Surface0, Bg0),
                    center = center,
                    radius = radius * 0.75f
                ),
                radius = radius * 0.75f,
                center = center
            )

            // Inner subtle border
            drawCircle(
                color = BorderColor,
                radius = radius * 0.75f,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // Layer 3: Uncertainty Halo (translucent band positioned outside the rings)
            val haloWidth = (radius * 0.12f + animatedUncertainty * radius * 0.35f)
            drawCircle(
                color = UncertaintyHaloColor.copy(alpha = 0.15f + animatedUncertainty * 0.4f),
                radius = radius * 0.88f,
                center = center,
                style = Stroke(width = haloWidth)
            )

            // Track background ring
            drawArc(
                color = BorderSubtle,
                startAngle = -220f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(radius * 0.22f, radius * 0.22f),
                size = Size(radius * 1.56f, radius * 1.56f),
                style = Stroke(width = 10f, cap = StrokeCap.Round)
            )

            // Layer 2: Forecast Risk Arc (thin dotted/accent arc)
            val forecastSweep = (animatedForecastRisk * 260f).coerceIn(4f, 260f)
            drawArc(
                color = Info.copy(alpha = 0.55f),
                startAngle = -220f,
                sweepAngle = forecastSweep,
                useCenter = false,
                topLeft = Offset(radius * 0.16f, radius * 0.16f),
                size = Size(radius * 1.68f, radius * 1.68f),
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )

            // Layer 1: Current Risk Arc
            val currentSweep = (animatedCurrentRisk * 260f).coerceIn(4f, 260f)
            drawArc(
                color = statusColor,
                startAngle = -220f,
                sweepAngle = currentSweep,
                useCenter = false,
                topLeft = Offset(radius * 0.22f, radius * 0.22f),
                size = Size(radius * 1.56f, radius * 1.56f),
                style = Stroke(width = 10f, cap = StrokeCap.Round)
            )
        }

        // Center Data: Score & Status
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$securityIndex",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = (-1).sp
            )
            Text(
                text = statusLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "${currentRiskScore}% RISK",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )
        }
    }
}
