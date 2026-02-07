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
import com.example.reminder.ui.activity.CallActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val id = intent.getLongExtra(EXTRA_ID, -1L)
            val message = intent.getStringExtra(EXTRA_MSG) ?: Constants.DEFAULT_REMINDER_MESSAGE
            val useCallApi = TtsPreferences.getUseCallApi(context)

            // Показываем входящий звонок, если аккаунт включён: можно ответить с гарнитуры.
            // При включённом API — TTS в интерфейсе звонка (разговорный динамик).
            // При выключенном API — по ответу открываем экран напоминалки с TTS через основной динамик.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val app = context.applicationContext as? ReminderApp
                val handle = app?.phoneAccountHandle
                if (handle != null) {
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                    val extras = Bundle().apply {
                        putLong(EXTRA_ID, id)
                        putString(EXTRA_MSG, message)
                        putBoolean(EXTRA_OPEN_CALL_ACTIVITY_ON_ANSWER, !useCallApi)
                    }
                    try {
                        telecomManager.addNewIncomingCall(handle, extras)
                        return
                    } catch (_: Exception) { }
                }
            }

            // API звонков выключен: показываем уведомление с fullScreenIntent.
            // Старт активности из фона (ReminderService -> startActivity) на Android 10+ блокируется,
            // поэтому используем fullScreenIntent — система сама покажет экран при получении уведомления.
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
        } catch (_: Throwable) { }
    }

    companion object {
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_MSG = "MSG"
        /** При ответе открыть CallActivity и воспроизвести TTS через основной динамик (используется при выключенном API звонков). */
        const val EXTRA_OPEN_CALL_ACTIVITY_ON_ANSWER = "open_call_activity_on_answer"
    }
}