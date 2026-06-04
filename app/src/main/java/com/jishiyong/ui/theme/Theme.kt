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
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = StatusFreshLight,
    onPrimaryContainer = PrimaryDark,
    secondary = CategoryDrink,
    onSecondary = OnPrimary,
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF00695C),
    tertiary = CategoryCosmetics,
    onTertiary = OnPrimary,
    tertiaryContainer = Color(0xFFF3E5F5),
    onTertiaryContainer = Color(0xFF4A148C),
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFF5F5F5),
    error = StatusCritical,
    onError = OnPrimary,
    errorContainer = StatusCriticalLight,
    onErrorContainer = Color(0xFFB71C1C),
    inverseSurface = TextPrimary,
    inverseOnSurface = SurfaceLight,
    inversePrimary = PrimaryVariantDark,
    surfaceTint = Primary
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDarkTheme,
    onPrimary = PrimaryDark,
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = PrimaryVariantDark,
    secondary = CategoryDrink,
    onSecondary = Color(0xFF003D33),
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = Color(0xFF80CBC4),
    tertiary = CategoryCosmetics,
    onTertiary = Color(0xFF311B92),
    tertiaryContainer = Color(0xFF4A148C),
    onTertiaryContainer = Color(0xFFD1C4E9),
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF424242),
    outlineVariant = Color(0xFF2A2A2A),
    error = StatusCritical,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = StatusCriticalLight,
    inverseSurface = TextPrimaryDark,
    inverseOnSurface = SurfaceDark,
    inversePrimary = PrimaryDark,
    surfaceTint = PrimaryDarkTheme
)

@Composable
fun JiShiYongTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
