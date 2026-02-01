package com.example.reminder

data class Reminder(
    val id: Long,
    val message: String,
    val timeMillis: Long
)
