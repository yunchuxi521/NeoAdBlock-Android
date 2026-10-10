package com.adblock.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = LaserGreen,
    secondary = LaserGreenDim,
    tertiary = WarningAmber,
    background = DarkBgStart,
    surface = DarkBgEnd,
    surfaceVariant = SurfaceDark,
    surfaceTint = LaserGreen.copy(alpha = 0.05f),
    onPrimary = Color.Black,
    onSecondary = TextPrimary,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    outlineVariant = TileBorder,
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun AdBlockTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBgStart.toArgb()
            window.navigationBarColor = DarkBgEnd.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AdBlockTypography,
        content = content
    )
}
