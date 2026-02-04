package com.example.reminder.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "imported_calendar_instances")
data class ImportedCalendarInstance(
    @PrimaryKey
    val instanceKey: String,
    val eventId: Long,
    val beginMillis: Long
)
