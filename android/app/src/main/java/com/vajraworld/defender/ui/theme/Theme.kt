package com.vajraworld.defender.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = BrandBlueLight,
    onPrimaryContainer = BrandBlue,
    secondary = PurpleAccent,
    onSecondary = SurfaceWhite,
    secondaryContainer = PurpleAccentLight,
    onSecondaryContainer = PurpleAccent,
    tertiary = AccentCyan,
    onTertiary = SurfaceWhite,
    background = BgLight,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSecondary,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    error = ThreatRed,
    onError = SurfaceWhite
)

@Composable
fun VajraWorldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
