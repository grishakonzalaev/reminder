package com.example.reminder

import android.content.Context

object ReminderPreferences {
    private const val PREFS = "reminder_prefs"
    private const val KEY_AUTO_DELETE_PAST = "auto_delete_past"

    fun getAutoDeletePast(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_DELETE_PAST, false)
    }

    fun setAutoDeletePast(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_AUTO_DELETE_PAST, enabled).apply()
    }
}
