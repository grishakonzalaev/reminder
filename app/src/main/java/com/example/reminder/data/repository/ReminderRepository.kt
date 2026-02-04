package com.example.reminder.data.repository

import android.content.Context
import com.example.reminder.data.dao.CalendarEventMappingDao
import com.example.reminder.data.dao.ImportedCalendarInstanceDao
import com.example.reminder.data.dao.ReminderDao
import com.example.reminder.data.database.ReminderDatabase
import com.example.reminder.data.model.CalendarEventMapping
import com.example.reminder.data.model.ImportedCalendarInstance
import com.example.reminder.data.model.Reminder
import com.example.reminder.data.preferences.ReminderPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ReminderRepository(private val context: Context) {

    private val database = ReminderDatabase.getDatabase(context)
    private val reminderDao: ReminderDao = database.reminderDao()
    private val calendarMappingDao: CalendarEventMappingDao = database.calendarEventMappingDao()
    private val importedInstanceDao: ImportedCalendarInstanceDao = database.importedCalendarInstanceDao()

    val reminders: Flow<List<Reminder>> = reminderDao.getAllFlow()

    init {
        migrateFromSharedPreferences()
    }

    suspend fun addReminder(message: String, timeMillis: Long): Reminder = withContext(Dispatchers.IO) {
        val reminder = Reminder(id = 0, message = message, timeMillis = timeMillis)
        val id = reminderDao.insert(reminder)
        reminder.copy(id = id)
    }

    suspend fun updateReminder(id: Long, message: String, timeMillis: Long) = withContext(Dispatchers.IO) {
        val reminder = Reminder(id = id, message = message, timeMillis = timeMillis)
        reminderDao.update(reminder)
    }

    suspend fun deleteReminder(reminder: Reminder) = withContext(Dispatchers.IO) {
        reminderDao.delete(reminder)
    }

    suspend fun deleteReminders(reminders: List<Reminder>) = withContext(Dispatchers.IO) {
        val ids = reminders.map { it.id }
        reminderDao.deleteByIds(ids)
    }

    suspend fun removePastReminders(context: Context): List<Reminder> = withContext(Dispatchers.IO) {
        if (!ReminderPreferences.getAutoDeletePast(context)) return@withContext emptyList()

        val now = System.currentTimeMillis()
        val pastReminders = reminderDao.getPastReminders(now)
        if (pastReminders.isNotEmpty()) {
            reminderDao.deletePastReminders(now)
        }
        pastReminders
    }

    suspend fun getAll(): List<Reminder> = withContext(Dispatchers.IO) {
        reminderDao.getAll()
    }

    suspend fun getCalendarEventId(reminderId: Long): Long? = withContext(Dispatchers.IO) {
        calendarMappingDao.getCalendarEventId(reminderId)
    }

    suspend fun isCalendarEventMapped(calendarEventId: Long): Boolean = withContext(Dispatchers.IO) {
        calendarMappingDao.existsByCalendarEventId(calendarEventId)
    }

    suspend fun setCalendarEventId(reminderId: Long, eventId: Long) = withContext(Dispatchers.IO) {
        val mapping = CalendarEventMapping(reminderId = reminderId, calendarEventId = eventId)
        calendarMappingDao.insert(mapping)
    }

    suspend fun removeCalendarEventId(reminderId: Long) = withContext(Dispatchers.IO) {
        calendarMappingDao.deleteByReminderId(reminderId)
    }

    suspend fun removeCalendarEventIds(reminderIds: List<Long>) = withContext(Dispatchers.IO) {
        calendarMappingDao.deleteByReminderIds(reminderIds)
    }

    suspend fun isCalendarInstanceImported(eventId: Long, beginMillis: Long): Boolean = withContext(Dispatchers.IO) {
        val key = "${eventId}_$beginMillis"
        importedInstanceDao.exists(key)
    }

    suspend fun addImportedCalendarInstance(eventId: Long, beginMillis: Long) = withContext(Dispatchers.IO) {
        val key = "${eventId}_$beginMillis"
        val instance = ImportedCalendarInstance(
            instanceKey = key,
            eventId = eventId,
            beginMillis = beginMillis
        )
        importedInstanceDao.insert(instance)
    }

    private fun migrateFromSharedPreferences() {
        val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
        val migrationDone = prefs.getBoolean("migration_to_room_done", false)

        if (!migrationDone) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val remindersJson = prefs.getString("list", null)
                    if (!remindersJson.isNullOrEmpty() && remindersJson != "[]") {
                        val arr = JSONArray(remindersJson)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val reminder = Reminder(
                                id = obj.getLong("id"),
                                message = obj.optString("message", obj.optString("title", "")),
                                timeMillis = obj.getLong("timeMillis")
                            )
                            reminderDao.insert(reminder)
                        }
                    }

                    val mappingsJson = prefs.getString("calendar_event_ids", null)
                    if (!mappingsJson.isNullOrEmpty() && mappingsJson != "{}") {
                        val obj = JSONObject(mappingsJson)
                        val keys = obj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val reminderId = key.toLongOrNull() ?: continue
                            val eventId = obj.getLong(key)
                            val mapping = CalendarEventMapping(reminderId, eventId)
                            calendarMappingDao.insert(mapping)
                        }
                    }

                    val instancesJson = prefs.getString("imported_calendar_instances", null)
                    if (!instancesJson.isNullOrEmpty() && instancesJson != "[]") {
                        val arr = JSONArray(instancesJson)
                        for (i in 0 until arr.length()) {
                            val instanceKey = arr.getString(i)
                            val parts = instanceKey.split("_")
                            if (parts.size == 2) {
                                val eventId = parts[0].toLongOrNull() ?: continue
                                val beginMillis = parts[1].toLongOrNull() ?: continue
                                val instance = ImportedCalendarInstance(instanceKey, eventId, beginMillis)
                                importedInstanceDao.insert(instance)
                            }
                        }
                    }

                    prefs.edit().putBoolean("migration_to_room_done", true).apply()

                    prefs.edit()
                        .remove("list")
                        .remove("calendar_event_ids")
                        .remove("imported_calendar_instances")
                        .apply()

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
