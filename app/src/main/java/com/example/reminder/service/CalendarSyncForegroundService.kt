package com.example.reminder.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.reminder.Constants
import com.example.reminder.data.preferences.ReminderPreferences
import com.example.reminder.helper.NotificationHelper
import com.example.reminder.scheduler.CalendarSyncRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Foreground-сервис с уведомлением в шторке: периодически выполняет синхронизацию календаря.
 * На Honor/Xiaomi приложение в фоне часто убивают — в foreground синхронизация идёт стабильно.
 * Включение опционально в настройках.
 */
class CalendarSyncForegroundService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var tickRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!ReminderPreferences.getSyncFromCalendar(this) || !ReminderPreferences.getCalendarSyncForeground(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        val channelId = NotificationHelper.createCalendarSyncChannel(this)
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Напоминалка")
            .setContentText("Синхронизация календаря")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(Constants.CALENDAR_SYNC_NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            @Suppress("DEPRECATION")
            startForeground(Constants.CALENDAR_SYNC_NOTIF_ID, notification)
        }
        scheduleNextSync()
        return START_STICKY
    }

    private fun scheduleNextSync() {
        tickRunnable?.let { handler.removeCallbacks(it) }
        tickRunnable = Runnable {
            if (!ReminderPreferences.getSyncFromCalendar(this) || !ReminderPreferences.getCalendarSyncForeground(this)) {
                stopSelf()
                return@Runnable
            }
            scope.launch {
                CalendarSyncRunner.runSync(this@CalendarSyncForegroundService)
                handler.post { scheduleNextSync() }
            }
        }
        handler.postDelayed(tickRunnable!!, Constants.CALENDAR_SYNC_INTERVAL_MS)
    }

    override fun onDestroy() {
        tickRunnable?.let { handler.removeCallbacks(it) }
        tickRunnable = null
        super.onDestroy()
    }
}
