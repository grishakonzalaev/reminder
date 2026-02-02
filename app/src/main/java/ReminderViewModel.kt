package com.example.reminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
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
    }

    fun addReminder(message: String, timeMillis: Long) {
        viewModelScope.launch {
            val reminder = repo.addReminder(message, timeMillis)
            alarmScheduler.schedule(reminder)
        }
    }

    fun updateReminder(reminder: Reminder, message: String, timeMillis: Long) {
        viewModelScope.launch {
            alarmScheduler.cancel(reminder.id)
            repo.updateReminder(reminder.id, message, timeMillis)
            val updated = Reminder(id = reminder.id, message = message, timeMillis = timeMillis)
            alarmScheduler.schedule(updated)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            alarmScheduler.cancel(reminder.id)
            repo.deleteReminder(reminder)
        }
    }

    fun deleteReminders(reminders: List<Reminder>) {
        viewModelScope.launch {
            reminders.forEach { alarmScheduler.cancel(it.id) }
            repo.deleteReminders(reminders)
        }
    }
}
