package com.example.reminder.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build

object AlarmHelper {
    /**
     * Точный будильник для периодических задач (например синхронизация календаря).
     * На агрессивных OEM (Honor, Xiaomi) может задерживаться при оптимизации батареи.
     */
    fun scheduleExactAlarm(
        context: Context,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    /**
     * Будильник через setAlarmClock — максимально надёжен в Doze и на Honor/Xiaomi:
     * система не откладывает доставку и показывает иконку будильника в статус-баре.
     * Используется для срабатывания напоминаний (звонок/экран).
     */
    fun scheduleAlarmClock(
        context: Context,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val info = AlarmManager.AlarmClockInfo(triggerAtMillis, null)
            alarmManager.setAlarmClock(info, pendingIntent)
        } else {
            scheduleExactAlarm(context, triggerAtMillis, pendingIntent)
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun cancelAlarm(context: Context, pendingIntent: PendingIntent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}
