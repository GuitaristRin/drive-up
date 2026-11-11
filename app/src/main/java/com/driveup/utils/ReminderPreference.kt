package com.driveup.utils

import android.content.Context

object ReminderPreference {
    private const val PREFS = "driveup_prefs"
    private const val KEY_ENABLED = "reminder_enabled"
    private const val KEY_MINUTES = "reminder_minutes"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()

    fun getAdvanceMinutes(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_MINUTES, 10)

    fun setAdvanceMinutes(context: Context, minutes: Int) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_MINUTES, minutes).apply()
}
