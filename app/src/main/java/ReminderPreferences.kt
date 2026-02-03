package com.example.reminder

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ReminderPreferences {
    private const val PREFS = "reminder_prefs"
    private const val KEY_AUTO_DELETE_PAST = "auto_delete_past"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_ADD_TO_CALENDAR = "add_to_calendar"
    private const val KEY_SYNC_FROM_CALENDAR = "sync_from_calendar"

    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val THEME_SYSTEM = "system"

    fun getThemeMode(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    fun setThemeMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_THEME_MODE, mode).apply()
        applyThemeMode(mode)
    }

    fun applyThemeMode(context: Context) {
        applyThemeMode(getThemeMode(context))
    }

    private fun applyThemeMode(mode: String) {
        val nightMode = when (mode) {
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun getAutoDeletePast(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_DELETE_PAST, false)
    }

    fun setAutoDeletePast(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_AUTO_DELETE_PAST, enabled).apply()
    }

    fun getAddToCalendar(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ADD_TO_CALENDAR, true)
    }

    fun setAddToCalendar(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ADD_TO_CALENDAR, enabled).apply()
    }

    /** Включено: приложение читает календарь и добавляет будущие события как напоминания. */
    fun getSyncFromCalendar(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SYNC_FROM_CALENDAR, true)
    }

    fun setSyncFromCalendar(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_SYNC_FROM_CALENDAR, enabled).apply()
    }
}
