package com.example.reminder.helper

import android.content.Context
import com.example.reminder.data.preferences.ReminderPreferences
import com.example.reminder.scheduler.SnoozeScheduler

/**
 * При отклонении звонка: при включённой опции планирует повтор через N минут (до M повторов).
 */
object SnoozeHelper {

    /**
     * Вызывать при отклонении звонка напоминания. Если опция включена и остались повторы —
     * планирует повтор через настроенный интервал и уменьшает счётчик.
     */
    fun tryScheduleSnooze(context: Context, reminderId: Long, message: String) {
        val app = context.applicationContext
        if (!ReminderPreferences.getSnoozeEnabled(app)) return
        val remaining = ReminderPreferences.getRemainingSnoozes(app, reminderId)
        if (remaining <= 0) return
        val delayMinutes = ReminderPreferences.getSnoozeDelayMinutes(app).coerceIn(1, 60)
        val triggerAt = System.currentTimeMillis() + delayMinutes * 60_000L
        SnoozeScheduler.schedule(app, reminderId, message, triggerAt)
        ReminderPreferences.setRemainingSnoozes(app, reminderId, remaining - 1)
    }

    fun cancelSnooze(context: Context, reminderId: Long) {
        SnoozeScheduler.cancel(context.applicationContext, reminderId)
        ReminderPreferences.clearRemainingSnoozes(context.applicationContext, reminderId)
    }
}
