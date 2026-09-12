package com.vajraworld.defender.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.R
import com.vajraworld.defender.ui.theme.*

@Composable
fun VajraAppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Image(
        painter = painterResource(id = R.drawable.vajra_logo),
        contentDescription = "VajraWorld Guardian Logo",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape((size.value * 0.22f).dp))
            .border(1.dp, BorderColor, RoundedCornerShape((size.value * 0.22f).dp))
    )
}

@Composable
fun VajraBrandHeader(
    modifier: Modifier = Modifier,
    isLive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "HeaderLivePulse")
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
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VajraAppLogo(size = 42.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "VAJRAWORLD",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = "GUARDIAN DEFENDER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = BrandBlue,
                    maxLines = 1
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
                    if (isLive) SafeGreenBorder else WarningAmberBorder,
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .alpha(if (isLive) pulseAlpha else 1f)
                    .background(if (isLive) SafeGreen else WarningAmber)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isLive) "LIVE RADAR" else "CONNECTING",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isLive) SafeGreen else WarningAmber,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
