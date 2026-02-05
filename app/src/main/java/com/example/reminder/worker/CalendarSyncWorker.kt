package com.example.reminder.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.reminder.data.preferences.ReminderPreferences
import com.example.reminder.scheduler.CalendarSyncRunner
import com.example.reminder.scheduler.CalendarSyncScheduler
/**
 * Периодическая синхронизация календаря через WorkManager.
 * На устройствах с жёсткой оптимизацией батареи (Honor, Xiaomi) AlarmManager может не срабатывать —
 * WorkManager выполняется надёжнее (интервал не менее 15 минут).
 */
class CalendarSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext
        if (!ReminderPreferences.getSyncFromCalendar(app)) {
            return Result.success()
        }
        return try {
            CalendarSyncRunner.runSync(app)
            CalendarSyncScheduler.scheduleIfEnabled(app)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "calendar_sync"
    }
}
