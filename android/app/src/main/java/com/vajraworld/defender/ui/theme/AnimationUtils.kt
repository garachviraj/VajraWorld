package com.vajraworld.defender.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Animated Shimmer Brush for CTAs, cards, and scanning state.
 * Emits a dynamic diagonal light sweep.
 */
@Composable
fun shimmerBrush(
    targetValue: Float = 1200f,
    colors: List<Color> = listOf(
        Color.White.copy(alpha = 0.0f),
        Color.White.copy(alpha = 0.35f),
        Color.White.copy(alpha = 0.0f)
    )
): Brush {
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -350f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )

    return Brush.linearGradient(
        colors = colors,
        start = Offset(translateAnim, translateAnim),
        end = Offset(translateAnim + 260f, translateAnim + 260f)
    )
}

/**
 * Interactive spring scale on click/press.
 */
fun Modifier.tactileClick(
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "TactileScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/**
 * Choreographed Staggered Entrance wrapper.
 * Fades and glides up with spring physics when visible becomes true.
 */
@Composable
fun StaggeredCard(
    visible: Boolean = true,
    delayMs: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var itemVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            if (delayMs > 0) {
                kotlinx.coroutines.delay(delayMs.toLong())
            }
            itemVisible = true
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (itemVisible) 1f else 0f,
        animationSpec = tween(450, easing = LinearOutSlowInEasing),
        label = "staggeredAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (itemVisible) 0f else 28f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "staggeredOffset"
    )

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = offsetY
            }
    ) {
        content()
    }
}

