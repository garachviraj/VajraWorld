package com.vajraworld.defender.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightCockpitColorScheme = lightColorScheme(
    primary = Info,
    onPrimary = TextWhite,
    primaryContainer = Surface1,
    onPrimaryContainer = TextPrimary,
    secondary = Healthy,
    onSecondary = TextWhite,
    secondaryContainer = HealthyBg,
    onSecondaryContainer = Healthy,
    tertiary = AccentCyan,
    onTertiary = TextWhite,
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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Bg0.toArgb()
                window.navigationBarColor = Bg0.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = LightCockpitColorScheme,
        typography = Typography,
        content = content
    )
}

