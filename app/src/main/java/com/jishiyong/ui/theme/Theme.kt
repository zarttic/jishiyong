package com.jishiyong.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandSoft,
    onPrimaryContainer = BrandPrimaryDark,
    secondary = BrandAccent,
    onSecondary = Ink,
    secondaryContainer = BrandAccentSoft,
    onSecondaryContainer = Ink,
    tertiary = CategoryDrink,
    onTertiary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = SurfaceClean,
    onSurface = Ink,
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = InkMuted,
    outline = OutlineSoft,
    outlineVariant = OutlineSoft,
    error = StatusCritical,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandSoft,
    onPrimary = DarkPaper,
    primaryContainer = DarkSurfaceRaised,
    onPrimaryContainer = BrandSoft,
    secondary = BrandAccent,
    onSecondary = DarkPaper,
    secondaryContainer = Color(0xFF3B2F16),
    onSecondaryContainer = BrandAccentSoft,
    tertiary = Color(0xFF9BBDF2),
    onTertiary = DarkPaper,
    background = DarkPaper,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceRaised,
    onSurfaceVariant = DarkMuted,
    outline = Color(0xFF34433E),
    outlineVariant = Color(0xFF34433E),
    error = Color(0xFFFFB4A9),
    onError = DarkPaper
)

@Composable
fun JiShiYongTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
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
