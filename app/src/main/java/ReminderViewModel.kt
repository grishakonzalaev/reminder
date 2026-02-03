package com.example.reminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = ReminderRepository(application)
    private val alarmScheduler = AlarmScheduler(application)
    private val app = application

    val reminders: StateFlow<List<Reminder>> = repo.reminders

    init {
        viewModelScope.launch {
            val removed = repo.removePastReminders(app)
            removed.forEach { alarmScheduler.cancel(it.id) }
        }
        viewModelScope.launch {
            delay(1_000L) // первая синхронизация через 1 сек после старта
            while (isActive) {
                if (ReminderPreferences.getSyncFromCalendar(app)) {
                    syncFromCalendar()
                }
                delay(30_000L) // далее раз в пол минуты
            }
        }
    }

    /** Читает календарь и добавляет будущие события как напоминания (если ещё не добавлены). */
    private fun syncFromCalendar() {
        val now = System.currentTimeMillis()
        val toMillis = now + 30L * 24 * 60 * 60 * 1000 // +30 дней
        val readCalendarId = ReminderPreferences.getReadCalendarId(app)
        val instances = CalendarHelper.queryFutureInstances(app, now, toMillis, readCalendarId)
        for (inst in instances) {
            if (repo.isCalendarInstanceImported(inst.eventId, inst.beginMillis)) continue
            val message = inst.title.ifBlank { "Событие календаря" }
            val reminder = repo.addReminder(message, inst.beginMillis)
            alarmScheduler.schedule(reminder)
            repo.addImportedCalendarInstance(inst.eventId, inst.beginMillis)
        }
    }

    fun addReminder(message: String, timeMillis: Long) {
        viewModelScope.launch {
            val reminder = repo.addReminder(message, timeMillis)
            alarmScheduler.schedule(reminder)
            if (ReminderPreferences.getAddToCalendar(app)) {
                CalendarHelper.insertEvent(app, reminder)?.let { eventId ->
                    repo.setCalendarEventId(reminder.id, eventId)
                }
            }
        }
    }

    fun updateReminder(reminder: Reminder, message: String, timeMillis: Long) {
        viewModelScope.launch {
            alarmScheduler.cancel(reminder.id)
            repo.updateReminder(reminder.id, message, timeMillis)
            val updated = Reminder(id = reminder.id, message = message, timeMillis = timeMillis)
            alarmScheduler.schedule(updated)
            if (ReminderPreferences.getAddToCalendar(app)) {
                repo.getCalendarEventId(reminder.id)?.let { eventId ->
                    CalendarHelper.updateEvent(app, eventId, updated)
                } ?: run {
                    CalendarHelper.insertEvent(app, updated)?.let { eventId ->
                        repo.setCalendarEventId(reminder.id, eventId)
                    }
                }
            } else {
                repo.getCalendarEventId(reminder.id)?.let { eventId ->
                    CalendarHelper.deleteEvent(app, eventId)
                    repo.removeCalendarEventId(reminder.id)
                }
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            alarmScheduler.cancel(reminder.id)
            repo.getCalendarEventId(reminder.id)?.let { eventId ->
                CalendarHelper.deleteEvent(app, eventId)
                repo.removeCalendarEventId(reminder.id)
            }
            repo.deleteReminder(reminder)
        }
    }

    fun deleteReminders(reminders: List<Reminder>) {
        viewModelScope.launch {
            reminders.forEach { alarmScheduler.cancel(it.id) }
            reminders.forEach { r ->
                repo.getCalendarEventId(r.id)?.let { eventId ->
                    CalendarHelper.deleteEvent(app, eventId)
                }
            }
            repo.removeCalendarEventIds(reminders.map { it.id })
            repo.deleteReminders(reminders)
        }
    }
}
