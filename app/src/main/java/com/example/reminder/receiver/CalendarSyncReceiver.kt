package com.example.reminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.reminder.data.preferences.ReminderPreferences
import com.example.reminder.scheduler.CalendarSyncRunner
import com.example.reminder.scheduler.CalendarSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * По срабатыванию будильника выполняет синхронизацию «календарь → напоминания»
 * и планирует следующий запуск через 30 секунд (если опция включена).
 */
class CalendarSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_SYNC_CALENDAR) return
        val app = context.applicationContext
        if (!ReminderPreferences.getSyncFromCalendar(app)) {
            CalendarSyncScheduler.cancel(app)
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                CalendarSyncRunner.runSync(app)
                CalendarSyncScheduler.scheduleNext(app)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SYNC_CALENDAR = "com.example.reminder.SYNC_CALENDAR"
    }
}
