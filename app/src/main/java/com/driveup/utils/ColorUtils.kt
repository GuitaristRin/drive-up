package com.driveup.utils

import androidx.compose.ui.graphics.Color

object ColorUtils {
    // Metro Design tile palette — higher saturation, still readable with dark text
    private val palette = listOf(
        Color(0xFF4E9FD4),  // metro blue
        Color(0xFF2EBD6B),  // metro green
        Color(0xFFE04F5F),  // metro red
        Color(0xFFF4C430),  // metro yellow
        Color(0xFF8B5CF6),  // metro purple
        Color(0xFFE8720C),  // metro orange
        Color(0xFF00A8CC),  // metro cyan
        Color(0xFFD63384),  // metro pink
        Color(0xFF3DAA70),  // metro teal
        Color(0xFFF97316),  // metro amber
        Color(0xFF6366F1),  // metro indigo
        Color(0xFF84CC16),  // metro lime
    )

    // Text color on top of a given tile — white for dark tiles, dark for light tiles
    fun textColorFor(bg: Color): Color {
        val luminance = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
        return if (luminance > 0.55f) Color(0xFF1A1A1A) else Color.White
    }

    fun indexForName(name: String): Int {
        val hash = name.fold(0) { acc, c -> acc * 31 + c.code }
        return ((hash and 0x7FFFFFFF) % palette.size)
    }

    fun forIndex(index: Int): Color = palette[index % palette.size]

    fun forName(name: String): Color = forIndex(indexForName(name))
}
