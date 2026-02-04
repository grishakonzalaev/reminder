package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/** Запуск/остановка фоновой синхронизации календаря каждые 30 секунд (чтение событий → напоминания). */
object CalendarSyncScheduler {
    private const val REQUEST_CODE = 19001
    private const val INTERVAL_MS = 30_000L // 30 секунд

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
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + INTERVAL_MS
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(context: Context) {
        val intent = Intent(context, CalendarSyncReceiver::class.java).apply {
            action = CalendarSyncReceiver.ACTION_SYNC_CALENDAR
        }
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
    }
}
