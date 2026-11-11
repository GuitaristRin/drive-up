package com.driveup.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import io.github.takahashirinta.kanesumi.core.theme.MetroColors
import io.github.takahashirinta.kanesumi.core.theme.MetroTheme

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private fun appColorsFor(dark: Boolean) = if (dark) DarkAppColors else LightAppColors

// AppColors → MetroColors 桥接,与 Ncrust 的"NcrustColors 派生 MetroTheme"同构。
// 读 LocalAppColors 的既有 UI 继续工作;读 LocalMetroColors 的 Kanesumi 组件
// (MetroText/MetroIcon/MetroButton/…) 自动拿到同一套色值。
// pressTint 决定 MetroIndication 直角闪切的底色:浅色主题用低透明度黑,深色用白。
private fun metroColorsFor(colors: AppColors, dark: Boolean) = MetroColors(
    background = colors.background,
    surface = colors.surface,
    surfaceVariant = colors.surfaceVariant,
    divider = colors.gridLine,
    primary = colors.todayAccent,
    onPrimary = Color.White,
    onBackground = colors.textPrimary,
    onSurface = colors.textPrimary,
    onSurfaceVariant = colors.textSecondary,
    pressTint = if (dark) Color(0x22FFFFFF) else Color(0x14000000),
)

@Composable
fun DriveUpTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }
    val appColors = appColorsFor(dark)
    val metroColors = remember(appColors, dark) { metroColorsFor(appColors, dark) }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MetroTheme(
            colors = metroColors,
            content = content
        )
    }
}
