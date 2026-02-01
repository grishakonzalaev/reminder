package com.example.reminder

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "reminders"
private const val KEY_LIST = "list"

class ReminderRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _reminders = MutableStateFlow(loadFromPrefs())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    private fun loadFromPrefs(): List<Reminder> {
        val json = prefs.getString(KEY_LIST, "[]") ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val message = o.optString("message", o.optString("title", ""))
                Reminder(
                    id = o.getLong("id"),
                    message = message,
                    timeMillis = o.getLong("timeMillis")
                )
            }.sortedBy { it.timeMillis }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveToPrefs(list: List<Reminder>) {
        val arr = JSONArray()
        list.forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id)
                put("message", r.message)
                put("timeMillis", r.timeMillis)
            })
        }
        prefs.edit().putString(KEY_LIST, arr.toString()).apply()
    }

    fun addReminder(message: String, timeMillis: Long): Reminder {
        val list = _reminders.value
        val nextId = (list.maxOfOrNull { it.id } ?: 0L) + 1L
        val reminder = Reminder(id = nextId, message = message, timeMillis = timeMillis)
        val newList = (list + reminder).sortedBy { it.timeMillis }
        _reminders.value = newList
        saveToPrefs(newList)
        return reminder
    }

    fun deleteReminder(reminder: Reminder) {
        val newList = _reminders.value.filter { it.id != reminder.id }
        _reminders.value = newList
        saveToPrefs(newList)
    }

    fun deleteReminders(reminders: List<Reminder>) {
        val ids = reminders.map { it.id }.toSet()
        val newList = _reminders.value.filter { it.id !in ids }
        _reminders.value = newList
        saveToPrefs(newList)
    }

    /**
     * Удаляет напоминания с прошедшей датой. Возвращает список удалённых (для отмены будильников).
     */
    fun removePastReminders(context: Context): List<Reminder> {
        if (!ReminderPreferences.getAutoDeletePast(context)) return emptyList()
        val now = System.currentTimeMillis()
        val list = _reminders.value
        val (past, future) = list.partition { it.timeMillis < now }
        if (past.isEmpty()) return emptyList()
        _reminders.value = future.sortedBy { it.timeMillis }
        saveToPrefs(_reminders.value)
        return past
    }

    fun getAll(): List<Reminder> = _reminders.value
}
