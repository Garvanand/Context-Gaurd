package com.contextguard.app.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Spectral Signal: Cinematic Primary Dark Theme
private val DarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = SoftWhite,
    primaryContainer = DeepSurface,
    onPrimaryContainer = SoftWhite,
    secondary = IonCyan,
    onSecondary = Ink,
    tertiary = SignalLime,
    background = Ink,
    onBackground = SoftWhite,
    surface = DeepSurface,
    onSurface = SoftWhite,
    surfaceVariant = ElevatedSurface,
    onSurfaceVariant = MutedText,
    outline = ContourBorder,
    error = StopCoral,
    onError = SoftWhite
)

// Spectral Signal: Secondary Light Theme (Cool Porcelain & Violet)
private val LightColorScheme = lightColorScheme(
    primary = ElectricVioletMuted,
    onPrimary = SoftWhite,
    primaryContainer = PorcelainSurface,
    onPrimaryContainer = PorcelainTextPrimary,
    secondary = IonCyanMuted,
    onSecondary = PorcelainElevated,
    tertiary = SignalLime,
    background = PorcelainBg,
    onBackground = PorcelainTextPrimary,
    surface = PorcelainElevated,
    onSurface = PorcelainTextPrimary,
    surfaceVariant = PorcelainSurface,
    onSurfaceVariant = PorcelainTextSecondary,
    outline = PorcelainBorder,
    error = StopCoral,
    onError = SoftWhite
)

@Composable
fun ContextGuardTheme(
    darkTheme: Boolean = true, // Default to cinematic Spectral Signal dark mode
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val statusBarColor = if (darkTheme) Ink.toArgb() else PorcelainBg.toArgb()
            val navBarColor = if (darkTheme) Ink.toArgb() else PorcelainBg.toArgb()
            window.statusBarColor = statusBarColor
            window.navigationBarColor = navBarColor
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
