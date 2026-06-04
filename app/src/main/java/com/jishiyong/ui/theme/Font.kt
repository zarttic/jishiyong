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

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = emptyList() // 使用系统默认证书
)

// ==================== 英文字体 - Nunito ====================
// 圆润、现代、友好，适合生活类 App
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
// 思源黑体，清晰、现代、支持简繁中文
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
 * 优先使用 Nunito（英文/数字），回退到 Noto Sans SC（中文）
 */
val AppFontFamily = FontFamily(
    // Nunito 字体文件（英文/数字）
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

/**
 * 中文专用字体族
 * 用于标题等需要突出中文的场景
 */
val ChineseFontFamily = NotoSansSCFamily

/**
 * 数字专用字体族
 * 用于统计数据等需要突出数字的场景
 */
val NumberFontFamily = NunitoFamily
