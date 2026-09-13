package com.vajraworld.defender.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajraworld.defender.ui.theme.*
import kotlinx.coroutines.delay

/**
 * World-Class Executive Light Theme Startup Loading Screen for VajraWorld Guardian.
 * Features rotating telemetry ring, breathing brand emblem, stepped cyber defense
 * bootstrap phases, and a precision technical progress bar.
 */
@Composable
fun VajraSplashScreen(
    onLoadingComplete: () -> Unit
) {
    val progressAnim = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "SplashLoop")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingRotation"
    )
    val ringRotationReverse by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingRotationReverse"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    LaunchedEffect(Unit) {
        // Smoothly animate boot sequence from 0 to 100%
        progressAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 1900, easing = FastOutSlowInEasing)
        )
        delay(250)
        onLoadingComplete()
    }

    val currentProgress = progressAnim.value
    val phaseText = when {
        currentProgress < 0.28f -> "INITIALIZING SECURE HARDWARE ENCLAVE..."
        currentProgress < 0.58f -> "LOADING ON-DEVICE LATENT WORLD MODEL..."
        currentProgress < 0.86f -> "CALIBRATING CROSS-SURFACE DEFENSE MATRIX..."
        else -> "VAJRAWORLD GUARDIAN ACTIVE • NOMINAL"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg1),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background radar sweep grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = size.minDimension * 0.46f

            // Concentric technical rings
            for (i in 1..4) {
                drawCircle(
                    color = BorderColor.copy(alpha = 0.35f),
                    radius = maxR * (i / 4f),
                    center = Offset(cx, cy),
                    style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Hero Emblem with Multi-Ring Telemetry
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // Outer Counter-Rotating Dashed Halo
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(ringRotationReverse)
                ) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                Info.copy(alpha = 0.1f),
                                Info.copy(alpha = 0.8f),
                                Healthy.copy(alpha = 0.8f),
                                Info.copy(alpha = 0.1f)
                            )
                        ),
                        style = Stroke(
                            width = 2.5f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                        )
                    )
                }

                // Inner Fast Rotating Cyan Indicator Arc
                Canvas(
                    modifier = Modifier
                        .size(118.dp)
                        .rotate(ringRotation)
                ) {
                    drawArc(
                        color = Info,
                        startAngle = 0f,
                        sweepAngle = 110f,
                        useCenter = false,
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Healthy,
                        startAngle = 180f,
                        sweepAngle = 90f,
                        useCenter = false,
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )
                }

                // Core Jewel Shield Container
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Bg0)
                        .border(1.5.dp, BorderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    VajraAppLogo(size = 54.dp)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Brand Titles
            Text(
                text = "VAJRAWORLD",
                style = TechnicalValue.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.5.sp,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "AUTONOMOUS MOBILE CYBER DEFENSE",
                style = TechnicalValue.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = Info
                )
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Stepped Boot Status & Percent
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = phaseText,
                    style = TechnicalValue.copy(
                        fontSize = 9.5.sp,
                        color = if (currentProgress >= 0.86f) Healthy else TextSecondary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(currentProgress * 100).toInt()}%",
                    style = TechnicalValue.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Info
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Precision Hairline Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Surface2)
                    .border(0.5.dp, BorderSubtle, RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(currentProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Info, Healthy)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Technical Architecture Attestation Tags
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Surface1, RoundedCornerShape(4.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "KERNEL VERIFIED",
                        style = MetadataText.copy(fontSize = 8.5.sp, color = Healthy, fontWeight = FontWeight.Bold)
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Surface1, RoundedCornerShape(4.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ARM64 LATENT CORE",
                        style = MetadataText.copy(fontSize = 8.5.sp, color = Info, fontWeight = FontWeight.Bold)
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Surface1, RoundedCornerShape(4.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ZERO CLOUD DEPENDENCY",
                        style = MetadataText.copy(fontSize = 8.5.sp, color = TextMuted)
                    )
                }
            }
        }

        // Bottom Footer
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "VAJRA DEFENSE SYSTEM • ACTIVE SENSOR ENCLAVE",
                style = MetadataText.copy(fontSize = 9.sp, color = TextMuted)
            )
        }
    }
}
