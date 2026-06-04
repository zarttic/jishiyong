package com.jishiyong.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = LightSurface,
    primaryContainer = Green90,
    onPrimaryContainer = Green40,
    secondary = Teal40,
    onSecondary = LightSurface,
    secondaryContainer = Teal80,
    onSecondaryContainer = Teal40,
    background = LightBackground,
    onBackground = Color(0xFF1A1C18),
    surface = LightSurface,
    onSurface = Color(0xFF1A1C18),
    surfaceVariant = Color(0xFFE2E5D8),
    onSurfaceVariant = Color(0xFF44483D),
    error = StatusCritical,
    onError = LightSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = Green40,
    primaryContainer = Green40,
    onPrimaryContainer = Green90,
    secondary = Teal80,
    onSecondary = Teal40,
    secondaryContainer = Teal40,
    onSecondaryContainer = Teal80,
    background = DarkBackground,
    onBackground = Color(0xFFE2E3D8),
    surface = DarkSurface,
    onSurface = Color(0xFFE2E3D8),
    surfaceVariant = Color(0xFF44483D),
    onSurfaceVariant = Color(0xFFC4C8BA),
    error = StatusCritical,
    onError = DarkSurface
)

@Composable
fun JiShiYongTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
