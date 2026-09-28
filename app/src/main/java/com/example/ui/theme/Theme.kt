package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ZxCyberCyan,
    onPrimary = ZxDarkBackground,
    primaryContainer = ZxDarkCyan,
    onPrimaryContainer = ZxCyberCyan,
    secondary = ZxEmeraldGreen,
    onSecondary = ZxDarkBackground,
    secondaryContainer = ZxDarkGreen,
    onSecondaryContainer = ZxEmeraldGreen,
    tertiary = ZxNeonAmber,
    onTertiary = ZxDarkBackground,
    tertiaryContainer = ZxDarkAmber,
    onTertiaryContainer = ZxNeonAmber,
    error = ZxCrimsonRed,
    onError = ZxDarkBackground,
    errorContainer = ZxDarkCrimson,
    onErrorContainer = ZxCrimsonRed,
    background = ZxDarkBackground,
    onBackground = ZxTextPrimary,
    surface = ZxDarkSurface,
    onSurface = ZxTextPrimary,
    surfaceVariant = ZxDarkSurfaceVariant,
    onSurfaceVariant = ZxTextSecondary,
    outline = ZxSurfaceBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force high contrast tactical dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ZxDarkBackground.toArgb()
            window.navigationBarColor = ZxDarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
