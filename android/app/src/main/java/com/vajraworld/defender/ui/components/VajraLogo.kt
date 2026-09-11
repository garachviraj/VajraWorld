package com.vajraworld.defender.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.theme.*

@Composable
fun VajraAppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Outer Shield
        val shieldPath = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            lineTo(w * 0.88f, h * 0.22f)
            cubicTo(w * 0.88f, h * 0.55f, w * 0.72f, h * 0.82f, w * 0.5f, h * 0.94f)
            cubicTo(w * 0.28f, h * 0.82f, w * 0.12f, h * 0.55f, w * 0.12f, h * 0.22f)
            close()
        }
        drawPath(shieldPath, color = BrandBlue)

        // Inner Shield
        val innerPath = Path().apply {
            moveTo(w * 0.5f, h * 0.14f)
            lineTo(w * 0.82f, h * 0.26f)
            cubicTo(w * 0.82f, h * 0.53f, w * 0.68f, h * 0.77f, w * 0.5f, h * 0.88f)
            cubicTo(w * 0.32f, h * 0.77f, w * 0.18f, h * 0.53f, w * 0.18f, h * 0.26f)
            close()
        }
        drawPath(innerPath, color = Color(0xFF0F172A))

        // Center Diamond (Vajra core)
        val diamond = Path().apply {
            moveTo(w * 0.5f, h * 0.28f)
            lineTo(w * 0.66f, h * 0.44f)
            lineTo(w * 0.5f, h * 0.60f)
            lineTo(w * 0.34f, h * 0.44f)
            close()
        }
        drawPath(diamond, color = Color(0xFF38BDF8))

        // Vajra Thunderbolt Spikes (Golden)
        val vajraSpike = Path().apply {
            moveTo(w * 0.5f, h * 0.58f)
            lineTo(w * 0.60f, h * 0.70f)
            lineTo(w * 0.5f, h * 0.80f)
            lineTo(w * 0.40f, h * 0.70f)
            close()
        }
        drawPath(vajraSpike, color = Color(0xFFF59E0B))

        // Emerald Core Eye
        drawCircle(
            color = Color(0xFF10B981),
            radius = w * 0.06f,
            center = Offset(w * 0.5f, h * 0.44f)
        )
    }
}

@Composable
fun VajraBrandHeader(
    modifier: Modifier = Modifier,
    isLive: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VajraAppLogo(size = 46.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "VAJRAWORLD",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = TextPrimary
                    )
                }
                Text(
                    text = "GUARDIAN DEFENDER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = BrandBlue
                )
            }
        }

        // Live Real-Time Pulse Indicator
        Row(
            modifier = Modifier
                .background(
                    if (isLive) SafeGreenBg else WarningAmberBg,
                    RoundedCornerShape(20.dp)
                )
                .border(
                    1.dp,
                    if (isLive) SafeGreen.copy(alpha = 0.3f) else WarningAmber.copy(alpha = 0.3f),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (isLive) SafeGreen else WarningAmber, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isLive) "LIVE RADAR" else "CONNECTING",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isLive) SafeGreen else WarningAmber
            )
        }
    }
}
