package com.example.reminder.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.core.app.NotificationCompat
import com.example.reminder.Constants
import com.example.reminder.app.ReminderApp
import com.example.reminder.data.preferences.TtsPreferences
import com.example.reminder.helper.NotificationHelper
import com.example.reminder.helper.PendingIntentHelper
import com.example.reminder.service.ReminderService
import com.example.reminder.ui.activity.CallActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val id = intent.getLongExtra(EXTRA_ID, -1L)
            val message = intent.getStringExtra(EXTRA_MSG) ?: Constants.DEFAULT_REMINDER_MESSAGE
            val useCallApi = TtsPreferences.getUseCallApi(context)

            // При включённой опции «Использовать API звонков» — доставляем через Telecom (интерфейс звонков, звук через звонок)
            if (useCallApi && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val app = context.applicationContext as? ReminderApp
                val handle = app?.phoneAccountHandle
                if (handle != null) {
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                    val extras = Bundle().apply {
                        putLong(EXTRA_ID, id)
                        putString(EXTRA_MSG, message)
                    }
                    try {
                        telecomManager.addNewIncomingCall(handle, extras)
                        return // Успех — система покажет входящий звонок, TTS пойдёт через звонок
                    } catch (_: Exception) { }
                }
            }

            // Fallback: запуск через foreground-сервис, чтобы обойти ограничения
            // на старт активности из фона (Honor, Xiaomi, Android 10+)
            val serviceIntent = Intent(context, ReminderService::class.java).apply {
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_MSG, message)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (_: Exception) {
                // Если сервис не запустился — показываем уведомление с fullScreenIntent
                val callIntent = Intent(context, CallActivity::class.java).apply {
                    putExtra(CallActivity.EXTRA_MSG, message)
                    putExtra(CallActivity.EXTRA_ID, id)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        @Suppress("WrongConstant")
                        addFlags(0x00080000 or 0x00040000)
                    }
                }
                val channelId = NotificationHelper.createReminderCallChannel(context)
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val fullScreenPending = PendingIntentHelper.createActivity(
                    context,
                    (id and 0x7FFFFFFF).toInt(),
                    callIntent
                )
                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("Напоминание")
                    .setContentText(message)
                    .setContentIntent(fullScreenPending)
                    .setFullScreenIntent(fullScreenPending, true)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .build()
                manager.notify((id and 0x7FFFFFFF).toInt(), notification)
            }
        } catch (_: Throwable) { }
    }

    companion object {
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_MSG = "MSG"
    }
}