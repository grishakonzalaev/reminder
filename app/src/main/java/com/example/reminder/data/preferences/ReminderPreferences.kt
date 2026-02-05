package com.example.reminder.data.preferences

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

object ReminderPreferences {
    private const val PREFS = "reminder_prefs"
    private const val KEY_AUTO_DELETE_PAST = "auto_delete_past"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_ADD_TO_CALENDAR = "add_to_calendar"
    private const val KEY_SYNC_FROM_CALENDAR = "sync_from_calendar"
    private const val KEY_WRITE_CALENDAR_ID = "write_calendar_id"
    private const val KEY_READ_CALENDAR_ID = "read_calendar_id"
    private const val KEY_SNOOZE_ENABLED = "snooze_enabled"
    private const val KEY_SNOOZE_REPEATS = "snooze_repeats"
    private const val KEY_SNOOZE_DELAY_MINUTES = "snooze_delay_minutes"
    private const val KEY_SNOOZE_REMAINING_PREFIX = "snooze_remaining_"

    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val THEME_SYSTEM = "system"

    const val LANG_SYSTEM = "system"
    const val LANG_ENGLISH = "en"
    const val LANG_RUSSIAN = "ru"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getThemeMode(context: Context): String {
        return prefs(context).getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    fun setThemeMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_THEME_MODE, mode).apply()
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

    fun getLanguage(context: Context): String {
        return prefs(context).getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
    }

    fun setLanguage(context: Context, language: String) {
        prefs(context).edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun wrapContext(context: Context): Context {
        val language = getLanguage(context)

        val locale = when (language) {
            LANG_ENGLISH -> Locale("en")
            LANG_RUSSIAN -> Locale("ru")
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            }
        }

        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    fun syncLanguageFromSystem(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val systemLocales = AppCompatDelegate.getApplicationLocales()
            if (!systemLocales.isEmpty) {
                val systemLanguage = systemLocales[0]?.language
                val currentLanguage = getLanguage(context)

                val expectedLanguage = when (systemLanguage) {
                    "en" -> LANG_ENGLISH
                    "ru" -> LANG_RUSSIAN
                    else -> LANG_SYSTEM
                }

                if (currentLanguage != expectedLanguage) {
                    setLanguage(context, expectedLanguage)
                }
            } else {
                if (getLanguage(context) != LANG_SYSTEM) {
                    setLanguage(context, LANG_SYSTEM)
                }
            }
        }
    }

    fun applyLanguageForSystemSync(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val language = getLanguage(context)
            if (language == LANG_SYSTEM) {
                AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.getEmptyLocaleList())
            } else {
                val locale = when (language) {
                    LANG_ENGLISH -> Locale("en")
                    LANG_RUSSIAN -> Locale("ru")
                    else -> Locale.getDefault()
                }
                AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(locale.toLanguageTag()))
            }
        }
    }

    fun applyLanguage(context: Context) {
        val language = getLanguage(context)

        val locale = when (language) {
            LANG_ENGLISH -> Locale("en")
            LANG_RUSSIAN -> Locale("ru")
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            }
        }

        Locale.setDefault(locale)
    }

    fun getAutoDeletePast(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_AUTO_DELETE_PAST, false)
    }

    fun setAutoDeletePast(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_DELETE_PAST, enabled).apply()
    }

    fun getAddToCalendar(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ADD_TO_CALENDAR, true)
    }

    fun setAddToCalendar(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ADD_TO_CALENDAR, enabled).apply()
    }

    /** Включено: приложение читает календарь и добавляет будущие события как напоминания. */
    fun getSyncFromCalendar(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_SYNC_FROM_CALENDAR, true)
    }

    fun setSyncFromCalendar(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SYNC_FROM_CALENDAR, enabled).apply()
    }

    /** ID календаря для записи напоминаний; 0 = первый доступный. */
    fun getWriteCalendarId(context: Context): Long {
        return prefs(context).getLong(KEY_WRITE_CALENDAR_ID, 0L)
    }

    fun setWriteCalendarId(context: Context, calendarId: Long) {
        prefs(context).edit().putLong(KEY_WRITE_CALENDAR_ID, calendarId).apply()
    }

    /** ID календаря для чтения событий; 0 = все календари. */
    fun getReadCalendarId(context: Context): Long {
        return prefs(context).getLong(KEY_READ_CALENDAR_ID, 0L)
    }

    fun setReadCalendarId(context: Context, calendarId: Long) {
        prefs(context).edit().putLong(KEY_READ_CALENDAR_ID, calendarId).apply()
    }

    // ——— Отложенные напоминания ———

    fun getSnoozeEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_SNOOZE_ENABLED, false)
    }

    fun setSnoozeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SNOOZE_ENABLED, enabled).apply()
    }

    /** Сколько раз повторить напоминание после отложения (0 = только один повтор, 1 = два раза всего и т.д.). */
    fun getSnoozeRepeats(context: Context): Int {
        return prefs(context).getInt(KEY_SNOOZE_REPEATS, 2).coerceIn(0, 10)
    }

    fun setSnoozeRepeats(context: Context, count: Int) {
        prefs(context).edit().putInt(KEY_SNOOZE_REPEATS, count.coerceIn(0, 10)).apply()
    }

    /** На сколько минут откладывать напоминание при отклонении звонка. */
    fun getSnoozeDelayMinutes(context: Context): Int {
        return prefs(context).getInt(KEY_SNOOZE_DELAY_MINUTES, 5).coerceIn(1, 60)
    }

    fun setSnoozeDelayMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_SNOOZE_DELAY_MINUTES, minutes.coerceIn(1, 60)).apply()
    }

    fun getRemainingSnoozes(context: Context, reminderId: Long): Int {
        val key = KEY_SNOOZE_REMAINING_PREFIX + reminderId
        val p = prefs(context)
        return if (p.contains(key)) p.getInt(key, 0) else getSnoozeRepeats(context)
    }

    fun setRemainingSnoozes(context: Context, reminderId: Long, remaining: Int) {
        val key = KEY_SNOOZE_REMAINING_PREFIX + reminderId
        prefs(context).edit().putInt(key, remaining.coerceAtLeast(0)).apply()
    }

    fun clearRemainingSnoozes(context: Context, reminderId: Long) {
        prefs(context).edit().remove(KEY_SNOOZE_REMAINING_PREFIX + reminderId).apply()
    }
}
