package com.example.reminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.reminder.data.repository.ReminderRepository
import com.example.reminder.scheduler.AlarmScheduler
import com.example.reminder.scheduler.CalendarSyncScheduler

/**
 * После перезагрузки устройства AlarmManager сбрасывает все будильники.
 * Этот приёмник по BOOT_COMPLETED заново ставит в очередь все будущие напоминания.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val repo = ReminderRepository(context)
        val scheduler = AlarmScheduler(context)
        val now = System.currentTimeMillis()
        repo.getAll()
            .filter { it.timeMillis > now }
            .forEach { scheduler.schedule(it) }
        CalendarSyncScheduler.scheduleIfEnabled(context)
    }
}
