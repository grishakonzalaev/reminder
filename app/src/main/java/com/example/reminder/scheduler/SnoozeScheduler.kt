package com.example.reminder.scheduler

import android.content.Context
import android.content.Intent
import com.example.reminder.Constants
import com.example.reminder.helper.AlarmHelper
import com.example.reminder.helper.PendingIntentHelper
import com.example.reminder.receiver.ReminderReceiver

/**
 * Планирует повторное срабатывание напоминания через заданное время (отложение при отклонении звонка).
 */
object SnoozeScheduler {

    fun schedule(context: Context, reminderId: Long, message: String, triggerAtMillis: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, reminderId)
            putExtra(ReminderReceiver.EXTRA_MSG, message)
        }
        val requestCode = Constants.SNOOZE_REQUEST_CODE_BASE + (reminderId and 0x7FFF).toInt()
        val pending = PendingIntentHelper.createBroadcast(context, requestCode, intent)
        AlarmHelper.scheduleExactAlarm(context, triggerAtMillis, pending)
    }

    fun cancel(context: Context, reminderId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, reminderId)
        }
        val requestCode = Constants.SNOOZE_REQUEST_CODE_BASE + (reminderId and 0x7FFF).toInt()
        val pending = PendingIntentHelper.createBroadcast(context, requestCode, intent)
        AlarmHelper.cancelAlarm(context, pending)
    }
}
