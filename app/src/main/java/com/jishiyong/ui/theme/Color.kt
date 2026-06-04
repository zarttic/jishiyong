package com.jishiyong.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==================== 主色调 - 渐变绿 ====================
val Primary = Color(0xFF00C853)
val PrimaryVariant = Color(0xFF00E676)
val PrimaryDark = Color(0xFF00A844)
val OnPrimary = Color(0xFFFFFFFF)

// 深色主题主色
val PrimaryDarkTheme = Color(0xFF69F0AE)
val PrimaryVariantDark = Color(0xFFB9F6CA)

// ==================== 表面和背景 ====================
val SurfaceLight = Color(0xFFFAFAFA)
val SurfaceDark = Color(0xFF121212)
val SurfaceVariantLight = Color(0xFFF5F5F5)
val SurfaceVariantDark = Color(0xFF1E1E1E)

val BackgroundLight = Color(0xFFFFFFFF)
val BackgroundDark = Color(0xFF0A0A0A)

val CardLight = Color(0xFFFFFFFF)
val CardDark = Color(0xFF1A1A1A)

// ==================== 状态颜色 - 更鲜艳 ====================
val StatusFresh = Color(0xFF4CAF50)
val StatusFreshLight = Color(0xFFE8F5E9)
val StatusWarning = Color(0xFFFF9800)
val StatusWarningLight = Color(0xFFFFF3E0)
val StatusUrgent = Color(0xFFFF5722)
val StatusUrgentLight = Color(0xFFFBE9E7)
val StatusCritical = Color(0xFFF44336)
val StatusCriticalLight = Color(0xFFFFEBEE)
val StatusExpired = Color(0xFF9E9E9E)
val StatusExpiredLight = Color(0xFFF5F5F5)

// ==================== 渐变色 ====================
val GradientPrimary = Brush.linearGradient(
    colors = listOf(Color(0xFF00C853), Color(0xFF00E676))
)

val GradientPrimaryDark = Brush.linearGradient(
    colors = listOf(Color(0xFF00A844), Color(0xFF00C853))
)

val GradientSunset = Brush.linearGradient(
    colors = listOf(Color(0xFFFF6B6B), Color(0xFFFFE66D))
)

val GradientOcean = Brush.linearGradient(
    colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
)

val GradientForest = Brush.linearGradient(
    colors = listOf(Color(0xFF11998e), Color(0xFF38ef7d))
)

val GradientCard = Brush.linearGradient(
    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF8FFF8))
)

val GradientCardDark = Brush.linearGradient(
    colors = listOf(Color(0xFF1A1A1A), Color(0xFF2A2A2A))
)

// ==================== 分类颜色 - 更现代 ====================
val CategoryFood = Color(0xFFFF6B6B)
val CategoryDrink = Color(0xFF4ECDC4)
val CategoryDaily = Color(0xFF95E1D3)
val CategoryMedicine = Color(0xFFF38181)
val CategoryCosmetics = Color(0xFFAA96DA)
val CategoryElectronics = Color(0xFF6C5CE7)
val CategoryClothing = Color(0xFFFFA07A)
val CategoryOther = Color(0xFF95A5A6)

// ==================== 文字颜色 ====================
val TextPrimary = Color(0xFF1A1A1A)
val TextSecondary = Color(0xFF666666)
val TextTertiary = Color(0xFF999999)
val TextPrimaryDark = Color(0xFFEEEEEE)
val TextSecondaryDark = Color(0xFFAAAAAA)
val TextTertiaryDark = Color(0xFF777777)
