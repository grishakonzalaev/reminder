package com.example.reminder.scheduler

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.reminder.Constants
import com.example.reminder.data.preferences.ReminderPreferences
import com.example.reminder.helper.AlarmHelper
import com.example.reminder.helper.PendingIntentHelper
import com.example.reminder.receiver.CalendarSyncReceiver
import com.example.reminder.service.CalendarSyncForegroundService
import com.example.reminder.worker.CalendarSyncWorker
import java.util.concurrent.TimeUnit

/**
 * Запуск/остановка фоновой синхронизации календаря.
 * Два механизма: AlarmManager каждые 30 сек (точнее, но на Honor/Xiaomi может не срабатывать)
 * и WorkManager каждые 15 мин (надёжнее на устройствах с жёсткой оптимизацией батареи).
 */
object CalendarSyncScheduler {

    fun scheduleIfEnabled(context: Context) {
        if (!ReminderPreferences.getSyncFromCalendar(context)) return
        val app = context.applicationContext
        scheduleNext(app)
        scheduleWorkManager(app)
        if (ReminderPreferences.getCalendarSyncForeground(app)) {
            val intent = Intent(app, CalendarSyncForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                app.startForegroundService(intent)
            } else {
                app.startService(intent)
            }
        }
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
        val app = context.applicationContext
        val intent = Intent(app, CalendarSyncReceiver::class.java).apply {
            action = CalendarSyncReceiver.ACTION_SYNC_CALENDAR
        }
        val pending = PendingIntentHelper.createBroadcast(
            app,
            Constants.CALENDAR_SYNC_REQUEST_CODE,
            intent
        )
        AlarmHelper.cancelAlarm(app, pending)
        WorkManager.getInstance(app).cancelUniqueWork(CalendarSyncWorker.WORK_NAME)
        app.stopService(Intent(app, CalendarSyncForegroundService::class.java))
    }

    private fun scheduleWorkManager(context: Context) {
        val request = PeriodicWorkRequestBuilder<CalendarSyncWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                CalendarSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }
}
