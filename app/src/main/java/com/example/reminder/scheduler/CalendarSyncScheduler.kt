package com.example.reminder.scheduler

import android.content.Context
import android.content.Intent
import com.example.reminder.Constants
import com.example.reminder.data.preferences.ReminderPreferences
import com.example.reminder.helper.AlarmHelper
import com.example.reminder.helper.PendingIntentHelper
import com.example.reminder.receiver.CalendarSyncReceiver

/** Запуск/остановка фоновой синхронизации календаря каждые 30 секунд (чтение событий → напоминания). */
object CalendarSyncScheduler {

    fun scheduleIfEnabled(context: Context) {
        if (!ReminderPreferences.getSyncFromCalendar(context)) return
        scheduleNext(context.applicationContext)
    }

    /** Планирует следующий запуск через 30 секунд. Вызывается из ресивера и при включении опции. */
    fun scheduleNext(context: Context) {
        if (!ReminderPreferences.getSyncFromCalendar(context)) return
        val intent = Intent(context, CalendarSyncReceiver::class.java).apply {
            action = CalendarSyncReceiver.ACTION_SYNC_CALENDAR
        }
        val pending = PendingIntentHelper.createBroadcast(
            context,
            Constants.CALENDAR_SYNC_REQUEST_CODE,
            intent
        )
        val triggerAt = System.currentTimeMillis() + Constants.CALENDAR_SYNC_INTERVAL_MS
        AlarmHelper.scheduleExactAlarm(context, triggerAt, pending)
    }

    fun cancel(context: Context) {
        val intent = Intent(context, CalendarSyncReceiver::class.java).apply {
            action = CalendarSyncReceiver.ACTION_SYNC_CALENDAR
        }
        val pending = PendingIntentHelper.createBroadcast(
            context,
            Constants.CALENDAR_SYNC_REQUEST_CODE,
            intent
        )
        AlarmHelper.cancelAlarm(context, pending)
    }
}
