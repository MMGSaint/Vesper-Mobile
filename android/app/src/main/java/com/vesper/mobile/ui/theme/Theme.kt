package com.vesper.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Scheme = darkColorScheme(
    primary = Steel,
    onPrimary = NearBlack,
    secondary = Muted,
    onSecondary = NearBlack,
    background = NearBlack,
    onBackground = Parchment,
    surface = Elevated,
    onSurface = Parchment,
    surfaceVariant = Panel,
    onSurfaceVariant = Muted,
    error = Crimson,
    onError = Parchment,
    outline = Hairline,
    outlineVariant = Hairline,
    tertiary = Steel,
    onTertiary = NearBlack,
    scrim = Color(0xCC09090B),
)

@Composable
fun VesperTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(
        colorScheme = Scheme,
        typography = VesperTypography,
        content = content,
    )
}
