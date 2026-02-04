package com.example.reminder.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_event_mappings")
data class CalendarEventMapping(
    @PrimaryKey
    val reminderId: Long,
    val calendarEventId: Long
)
