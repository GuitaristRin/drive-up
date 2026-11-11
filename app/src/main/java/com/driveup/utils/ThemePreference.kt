package com.driveup.utils

import android.content.Context
import com.driveup.ui.theme.ThemeMode

object ThemePreference {
    private const val PREFS = "driveup_prefs"
    private const val KEY   = "theme_mode"

    fun get(context: Context): ThemeMode =
        ThemeMode.valueOf(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        )

    fun set(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, mode.name).apply()
    }
}
