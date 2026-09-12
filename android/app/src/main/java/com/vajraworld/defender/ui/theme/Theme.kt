package com.vajraworld.defender.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkCockpitColorScheme = darkColorScheme(
    primary = Info,
    onPrimary = Bg0,
    primaryContainer = Surface2,
    onPrimaryContainer = TextPrimary,
    secondary = Healthy,
    onSecondary = Bg0,
    secondaryContainer = Surface1,
    onSecondaryContainer = Healthy,
    tertiary = AccentCyan,
    onTertiary = Bg0,
    background = Bg1,
    onBackground = TextPrimary,
    surface = Surface0,
    onSurface = TextPrimary,
    surfaceVariant = Surface1,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    outlineVariant = BorderSubtle,
    error = Critical,
    onError = TextWhite
)

@Composable
fun VajraWorldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkCockpitColorScheme,
        typography = Typography,
        content = content
    )
}

