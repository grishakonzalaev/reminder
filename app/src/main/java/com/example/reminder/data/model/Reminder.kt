package com.example.reminder.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val message: String,
    val timeMillis: Long,
    /** none | daily | monthly | yearly; null при чтении из старых БД трактуется как "none" */
    val repeatType: String? = "none"
)
