package com.driveup.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Static color constants (accent stays the same in both themes)
val TodayAccent = Color(0xFF0078D7)

// Fluent 风格错误红 —— Kanesumi 的 MetroColors 没有 error 语义色,导入失败提示
// 需要一个与 Metro 一致的错误色,不再读 MaterialTheme.colorScheme.error。
val ErrorRed = Color(0xFFD13438)

// AppColors holds all theme-sensitive colors
data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val gridLine: Color,
    val weekBarBg: Color,
    val weekBarText: Color,
    val todayAccent: Color = TodayAccent,
)

val LightAppColors = AppColors(
    background    = Color(0xFFF3F3F3),
    surface       = Color(0xFFFFFFFF),
    surfaceVariant= Color(0xFFF0F0F0),
    textPrimary   = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF6B6B6B),
    gridLine      = Color(0xFFE0E0E0),
    weekBarBg     = Color(0xFF2D2D2D),
    weekBarText   = Color(0xFFFFFFFF),
)

val DarkAppColors = AppColors(
    background    = Color(0xFF121212),
    surface       = Color(0xFF1E1E1E),
    surfaceVariant= Color(0xFF2A2A2A),
    textPrimary   = Color(0xFFE8E8E8),
    textSecondary = Color(0xFF9E9E9E),
    gridLine      = Color(0xFF333333),
    weekBarBg     = Color(0xFF0A0A0A),
    weekBarText   = Color(0xFFE8E8E8),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
