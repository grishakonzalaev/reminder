package com.example.reminder.scheduler

import android.content.Context
import android.content.Intent
import com.example.reminder.data.model.Reminder
import com.example.reminder.helper.AlarmHelper
import com.example.reminder.helper.PendingIntentHelper
import com.example.reminder.receiver.ReminderReceiver

class AlarmScheduler(private val context: Context) {

    fun schedule(reminder: Reminder) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, reminder.id)
            putExtra(ReminderReceiver.EXTRA_MSG, reminder.message)
        }
        val pending = PendingIntentHelper.createBroadcast(
            context,
            reminder.id.toInt(),
            intent
        )
        AlarmHelper.scheduleAlarmClock(context, reminder.timeMillis, pending)
    }

    fun cancel(reminderId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, reminderId)
        }
        val pending = PendingIntentHelper.createBroadcast(
            context,
            reminderId.toInt(),
            intent
        )
        AlarmHelper.cancelAlarm(context, pending)
    }
}
