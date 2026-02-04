package com.example.reminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.reminder.data.preferences.ReminderPreferences
import com.example.reminder.data.repository.ReminderRepository
import com.example.reminder.helper.CalendarHelper
import com.example.reminder.scheduler.AlarmScheduler
import com.example.reminder.scheduler.CalendarSyncScheduler

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
        Thread {
            try {
                runSync(app)
                CalendarSyncScheduler.scheduleNext(app)
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    private fun runSync(app: Context) {
        val repo = ReminderRepository(app)
        val scheduler = AlarmScheduler(app)
        val now = System.currentTimeMillis()
        val toMillis = now + 30L * 24 * 60 * 60 * 1000
        val readCalendarId = ReminderPreferences.getReadCalendarId(app)
        val instances = CalendarHelper.queryFutureInstances(app, now, toMillis, readCalendarId)
        for (inst in instances) {
            if (repo.isCalendarInstanceImported(inst.eventId, inst.beginMillis)) continue
            val message = inst.title.ifBlank { "Событие календаря" }
            val reminder = repo.addReminder(message, inst.beginMillis)
            scheduler.schedule(reminder)
            repo.addImportedCalendarInstance(inst.eventId, inst.beginMillis)
        }
    }

    companion object {
        const val ACTION_SYNC_CALENDAR = "com.example.reminder.SYNC_CALENDAR"
    }
}
