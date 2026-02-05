package com.example.reminder.scheduler

import android.content.Context
import com.example.reminder.data.preferences.ReminderPreferences
import com.example.reminder.data.repository.ReminderRepository
import com.example.reminder.helper.CalendarHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Общая логика синхронизации «календарь → напоминания».
 * Вызывается из CalendarSyncReceiver и CalendarSyncWorker.
 */
object CalendarSyncRunner {

    suspend fun runSync(context: Context) = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        if (!ReminderPreferences.getSyncFromCalendar(app)) return@withContext
        val repo = ReminderRepository(app)
        val scheduler = AlarmScheduler(app)
        val now = System.currentTimeMillis()
        val toMillis = now + 30L * 24 * 60 * 60 * 1000
        val readCalendarId = ReminderPreferences.getReadCalendarId(app)
        val instances = CalendarHelper.queryFutureInstances(app, now, toMillis, readCalendarId)
        for (inst in instances) {
            if (repo.isCalendarEventMapped(inst.eventId)) continue
            if (repo.isCalendarInstanceImported(inst.eventId, inst.beginMillis)) continue
            val message = inst.title.ifBlank { "Событие календаря" }
            val reminder = repo.addReminder(message, inst.beginMillis)
            scheduler.schedule(reminder)
            repo.setCalendarEventId(reminder.id, inst.eventId)
            repo.addImportedCalendarInstance(inst.eventId, inst.beginMillis)
        }
    }
}
