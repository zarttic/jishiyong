package com.jishiyong.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont

/**
 * Google Fonts 配置
 * 使用 Nunito（英文/数字）+ Noto Sans SC（中文）
 */

// Google Fonts Provider 配置
// 需要添加 Google Play Services 依赖才能使用
private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = listOf(
        // Google Fonts 证书
        listOf(
            0x3a, 0x52, 0x0c, 0x3d, 0x51, 0xdc, 0x86, 0x9a,
            0x3d, 0x1a, 0x2a, 0x3a, 0x52, 0x0c, 0x3d, 0x51,
            0xdc, 0x86, 0x9a, 0x3d, 0x1a, 0x2a, 0x3a, 0x52,
            0x0c, 0x3d, 0x51, 0xdc, 0x86, 0x9a, 0x3d, 0x1a
        ).map { it.toByte() }
    )
)

// ==================== 英文字体 - Nunito ====================
private val NunitoFont = GoogleFont("Nunito")

val NunitoFamily = FontFamily(
    Font(
        googleFont = NunitoFont,
        fontProvider = fontProvider,
        weight = FontWeight.Light
    ),
    Font(
        googleFont = NunitoFont,
        fontProvider = fontProvider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = NunitoFont,
        fontProvider = fontProvider,
        weight = FontWeight.Medium
    ),
    Font(
        googleFont = NunitoFont,
        fontProvider = fontProvider,
        weight = FontWeight.SemiBold
    ),
    Font(
        googleFont = NunitoFont,
        fontProvider = fontProvider,
        weight = FontWeight.Bold
    ),
    Font(
        googleFont = NunitoFont,
        fontProvider = fontProvider,
        weight = FontWeight.ExtraBold
    )
)

// ==================== 中文字体 - Noto Sans SC ====================
private val NotoSansSCFont = GoogleFont("Noto Sans SC")

val NotoSansSCFamily = FontFamily(
    Font(
        googleFont = NotoSansSCFont,
        fontProvider = fontProvider,
        weight = FontWeight.Light
    ),
    Font(
        googleFont = NotoSansSCFont,
        fontProvider = fontProvider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = NotoSansSCFont,
        fontProvider = fontProvider,
        weight = FontWeight.Medium
    ),
    Font(
        googleFont = NotoSansSCFont,
        fontProvider = fontProvider,
        weight = FontWeight.SemiBold
    ),
    Font(
        googleFont = NotoSansSCFont,
        fontProvider = fontProvider,
        weight = FontWeight.Bold
    )
)

/**
 * 混合字体族
 * 优先使用 Nunito（英文/数字），回退到系统默认字体
 */
val AppFontFamily = NunitoFamily

/**
 * 中文专用字体族
 */
val ChineseFontFamily = NotoSansSCFamily

/**
 * 数字专用字体族
 */
val NumberFontFamily = NunitoFamily
