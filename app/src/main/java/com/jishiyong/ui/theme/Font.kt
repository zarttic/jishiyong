package com.jishiyong.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont

/**
 * Google Fonts 配置
 */

// Google Fonts Provider - 使用系统默认证书
private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = listOf(listOf())
)

// ==================== 英文字体 - Nunito ====================
private val NunitoFont = GoogleFont("Nunito")

val NunitoFamily = FontFamily(
    Font(googleFont = NunitoFont, fontProvider = fontProvider, weight = FontWeight.Light),
    Font(googleFont = NunitoFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = NunitoFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = NunitoFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = NunitoFont, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = NunitoFont, fontProvider = fontProvider, weight = FontWeight.ExtraBold)
)

// ==================== 中文字体 - Noto Sans SC ====================
private val NotoSansSCFont = GoogleFont("Noto Sans SC")

val NotoSansSCFamily = FontFamily(
    Font(googleFont = NotoSansSCFont, fontProvider = fontProvider, weight = FontWeight.Light),
    Font(googleFont = NotoSansSCFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = NotoSansSCFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = NotoSansSCFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = NotoSansSCFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

// 混合字体族
val AppFontFamily = NunitoFamily
val ChineseFontFamily = NotoSansSCFamily
val NumberFontFamily = NunitoFamily
