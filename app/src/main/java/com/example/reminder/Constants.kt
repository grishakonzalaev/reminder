package com.example.reminder

object Constants {
    const val DEFAULT_REMINDER_MESSAGE = "Пора!"
    const val CHANNEL_ID_REMINDER_CALL = "reminder_call_channel"
    const val CHANNEL_ID_REMINDER_FG = "reminder_fg_channel"
    const val CHANNEL_NAME_REMINDERS = "Напоминания"
    const val CHANNEL_NAME_FOREGROUND = "Напоминание"
    const val CALENDAR_SYNC_INTERVAL_MS = 30_000L
    const val SNOOZE_REQUEST_CODE_BASE = 0x800000
    const val CALENDAR_SYNC_REQUEST_CODE = 19001
    const val CALENDAR_SYNC_NOTIF_ID = 19002
    const val CHANNEL_ID_CALENDAR_SYNC = "calendar_sync_channel"
}
