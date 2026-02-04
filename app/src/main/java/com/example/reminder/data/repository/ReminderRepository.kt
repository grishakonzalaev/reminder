package com.example.reminder.data.repository

import android.content.Context
import com.example.reminder.data.model.Reminder
import com.example.reminder.data.preferences.ReminderPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "reminders"
private const val KEY_LIST = "list"
private const val KEY_CALENDAR_EVENT_IDS = "calendar_event_ids"
private const val KEY_IMPORTED_CALENDAR_INSTANCES = "imported_calendar_instances"

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

    fun updateReminder(id: Long, message: String, timeMillis: Long) {
        val list = _reminders.value
        val updated = Reminder(id = id, message = message, timeMillis = timeMillis)
        val newList = list.map { if (it.id == id) updated else it }.sortedBy { it.timeMillis }
        _reminders.value = newList
        saveToPrefs(newList)
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

    fun getCalendarEventId(reminderId: Long): Long? {
        val json = prefs.getString(KEY_CALENDAR_EVENT_IDS, "{}") ?: return null
        return try {
            val obj = org.json.JSONObject(json)
            if (obj.has(reminderId.toString())) obj.getLong(reminderId.toString()) else null
        } catch (_: Exception) { null }
    }

    fun setCalendarEventId(reminderId: Long, eventId: Long) {
        val json = prefs.getString(KEY_CALENDAR_EVENT_IDS, "{}") ?: "{}"
        val obj = try { org.json.JSONObject(json) } catch (_: Exception) { org.json.JSONObject() }
        obj.put(reminderId.toString(), eventId)
        prefs.edit().putString(KEY_CALENDAR_EVENT_IDS, obj.toString()).apply()
    }

    fun removeCalendarEventId(reminderId: Long) {
        val json = prefs.getString(KEY_CALENDAR_EVENT_IDS, "{}") ?: return
        try {
            val obj = org.json.JSONObject(json)
            obj.remove(reminderId.toString())
            prefs.edit().putString(KEY_CALENDAR_EVENT_IDS, obj.toString()).apply()
        } catch (_: Exception) { }
    }

    fun removeCalendarEventIds(reminderIds: List<Long>) {
        val json = prefs.getString(KEY_CALENDAR_EVENT_IDS, "{}") ?: return
        try {
            val obj = org.json.JSONObject(json)
            reminderIds.forEach { obj.remove(it.toString()) }
            prefs.edit().putString(KEY_CALENDAR_EVENT_IDS, obj.toString()).apply()
        } catch (_: Exception) { }
    }

    /** Ключ экземпляра события календаря (eventId_beginMillis) для обратной синхронизации. */
    fun isCalendarInstanceImported(eventId: Long, beginMillis: Long): Boolean {
        val key = "${eventId}_$beginMillis"
        val json = prefs.getString(KEY_IMPORTED_CALENDAR_INSTANCES, "[]") ?: return false
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).any { arr.getString(it) == key }
        } catch (_: Exception) { false }
    }

    fun addImportedCalendarInstance(eventId: Long, beginMillis: Long) {
        val key = "${eventId}_$beginMillis"
        val json = prefs.getString(KEY_IMPORTED_CALENDAR_INSTANCES, "[]") ?: "[]"
        val arr = try { JSONArray(json) } catch (_: Exception) { JSONArray() }
        arr.put(key)
        prefs.edit().putString(KEY_IMPORTED_CALENDAR_INSTANCES, arr.toString()).apply()
    }
}
