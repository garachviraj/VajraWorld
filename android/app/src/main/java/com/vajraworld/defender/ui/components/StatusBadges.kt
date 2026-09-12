package com.vajraworld.defender.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.theme.*

/**
 * Explicit Mode Badge as mandated by Sections 9, 40, and 41.
 * Never hides the data source: either "LIVE DEVICE TELEMETRY" or "SYNTHETIC DEMO".
 */
@Composable
fun ModeBadge(
    isSynthetic: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    if (isSynthetic) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .background(WarningBg)
                .border(1.dp, WarningBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 3.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Warning)
            )
            Text(
                text = if (compact) "SYNTHETIC" else "SYNTHETIC DEMO",
                color = Warning,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    } else {
        // Living pulse effect for genuine device telemetry
        val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )

        Row(
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .background(HealthyBg)
                .border(1.dp, HealthyBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 3.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .alpha(pulseAlpha)
                    .background(Healthy)
            )
            Text(
                text = if (compact) "LIVE TELEMETRY" else "LIVE DEVICE TELEMETRY",
                color = Healthy,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Security status pill (PROTECTED, ELEVATED, HIGH RISK, CRITICAL).
 */
@Composable
fun SecurityStatusPill(
    riskScore: Int,
    modifier: Modifier = Modifier
) {
    val (label, bg, border, textColor) = when {
        riskScore >= 75 -> Quad("CRITICAL", CriticalBg, CriticalBorder, Critical)
        riskScore >= 50 -> Quad("HIGH RISK", HighBg, HighBorder, High)
        riskScore >= 25 -> Quad("ELEVATED", WarningBg, WarningBorder, Warning)
        else -> Quad("PROTECTED", HealthyBg, HealthyBorder, Healthy)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(textColor)
        )
        Text(
            text = label,
            color = textColor,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Geometric confidence badge.
 */
@Composable
fun ConfidenceBadge(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val pct = (confidence * 100).toInt().coerceIn(0, 100)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Surface2)
            .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CONFIDENCE $pct%",
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}
