package com.example.reminder.helper

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.TimeZone

/** Экземпляр события календаря (для обратной синхронизации). */
data class CalendarInstance(
    val eventId: Long,
    val beginMillis: Long,
    val title: String
)

/**
 * Интеграция с API календаря Android: создание, обновление, удаление событий и чтение будущих.
 */
object CalendarHelper {

    /**
     * Список доступных календарей: (id, displayName). Только видимые.
     */
    fun getAvailableCalendars(context: Context): List<Pair<Long, String>> {
        if (!hasReadCalendarPermission(context)) return emptyList()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )
        val list = mutableListOf<Pair<Long, String>>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1",
            null,
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
            val nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            if (idIdx >= 0 && nameIdx >= 0) {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx)?.takeIf { it.isNotBlank() } ?: "Календарь $id"
                    list.add(id to name)
                }
            }
        }
        return list
    }

    /**
     * Возвращает ID календаря для записи: из настроек (если задан и существует) или первый доступный.
     */
    fun getDefaultCalendarId(context: Context): Long? {
        if (!hasCalendarPermission(context)) return null
        val preferred = ReminderPreferences.getWriteCalendarId(context)
        if (preferred != 0L) {
            val available = getAvailableCalendars(context).map { it.first }
            if (preferred in available) return preferred
        }
        val projection = arrayOf(CalendarContract.Calendars._ID)
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1",
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                if (idIdx >= 0) return cursor.getLong(idIdx)
            }
        }
        return null
    }

    /**
     * Создаёт событие в календаре по напоминанию. Возвращает eventId или null.
     */
    fun insertEvent(context: Context, reminder: Reminder): Long? {
        if (!hasCalendarPermission(context)) return null
        val calendarId = getDefaultCalendarId(context) ?: return null
        val timeZone = TimeZone.getDefault().id
        val startMillis = reminder.timeMillis
        val endMillis = startMillis + 60_000L // +1 минута

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.TITLE, reminder.message)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, timeZone)
            put(CalendarContract.Events.ALL_DAY, 0)
        }
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            ?: return null
        return ContentUris.parseId(uri)
    }

    /**
     * Обновляет событие в календаре (время и заголовок).
     */
    fun updateEvent(context: Context, eventId: Long, reminder: Reminder): Boolean {
        if (!hasCalendarPermission(context)) return false
        val timeZone = TimeZone.getDefault().id
        val startMillis = reminder.timeMillis
        val endMillis = startMillis + 60_000L

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.TITLE, reminder.message)
            put(CalendarContract.Events.EVENT_TIMEZONE, timeZone)
        }
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        return context.contentResolver.update(uri, values, null, null) > 0
    }

    /**
     * Удаляет событие из календаря.
     */
    fun deleteEvent(context: Context, eventId: Long): Boolean {
        if (!hasCalendarPermission(context)) return false
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        return context.contentResolver.delete(uri, null, null) > 0
    }

    /**
     * Читает будущие экземпляры событий календаря в заданном диапазоне.
     * @param readCalendarId 0 = все календари, иначе только указанный.
     */
    fun queryFutureInstances(
        context: Context,
        fromMillis: Long,
        toMillis: Long,
        readCalendarId: Long = 0L
    ): List<CalendarInstance> {
        if (!hasReadCalendarPermission(context)) return emptyList()
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, fromMillis)
        ContentUris.appendId(builder, toMillis)
        val uri = builder.build()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.TITLE
        )
        val selection = if (readCalendarId != 0L) "${CalendarContract.Instances.CALENDAR_ID} = ?" else null
        val selectionArgs = if (readCalendarId != 0L) arrayOf(readCalendarId.toString()) else null
        val list = mutableListOf<CalendarInstance>()
        context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Instances.BEGIN} ASC"
        )?.use { cursor ->
            val colEventId = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
            val colBegin = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
            val colTitle = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
            if (colEventId < 0 || colBegin < 0) return@use
            while (cursor.moveToNext()) {
                val eventId = cursor.getLong(colEventId)
                val begin = cursor.getLong(colBegin)
                val title = if (colTitle >= 0) cursor.getString(colTitle).orEmpty() else ""
                list.add(CalendarInstance(eventId = eventId, beginMillis = begin, title = title))
            }
        }
        return list
    }

    private fun hasCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun hasReadCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
