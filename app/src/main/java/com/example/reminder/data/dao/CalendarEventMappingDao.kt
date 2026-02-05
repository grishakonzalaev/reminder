package com.example.reminder.data.dao

import androidx.room.*
import com.example.reminder.data.model.CalendarEventMapping

@Dao
interface CalendarEventMappingDao {

    @Query("SELECT * FROM calendar_event_mappings WHERE reminderId = :reminderId")
    suspend fun getByReminderId(reminderId: Long): CalendarEventMapping?

    @Query("SELECT calendarEventId FROM calendar_event_mappings WHERE reminderId = :reminderId")
    suspend fun getCalendarEventId(reminderId: Long): Long?

    @Query("SELECT EXISTS(SELECT 1 FROM calendar_event_mappings WHERE calendarEventId = :calendarEventId)")
    suspend fun existsByCalendarEventId(calendarEventId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mapping: CalendarEventMapping)

    @Query("DELETE FROM calendar_event_mappings WHERE reminderId = :reminderId")
    suspend fun deleteByReminderId(reminderId: Long)

    @Query("DELETE FROM calendar_event_mappings WHERE reminderId IN (:reminderIds)")
    suspend fun deleteByReminderIds(reminderIds: List<Long>)

    @Query("SELECT * FROM calendar_event_mappings")
    suspend fun getAll(): List<CalendarEventMapping>
}
