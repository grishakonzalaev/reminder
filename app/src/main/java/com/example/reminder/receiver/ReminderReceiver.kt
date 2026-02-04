package com.example.reminder.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.core.app.NotificationCompat
import com.example.reminder.app.ReminderApp
import com.example.reminder.data.preferences.TtsPreferences
import com.example.reminder.ui.activity.CallActivity

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val id = intent.getLongExtra(EXTRA_ID, -1L)
            val message = intent.getStringExtra(EXTRA_MSG) ?: "Пора!"
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

            // Fallback: уведомление и fullScreenIntent (без API звонков)
            val callIntent = Intent(context, CallActivity::class.java).apply {
                putExtra(CallActivity.EXTRA_MSG, message)
                putExtra(CallActivity.EXTRA_ID, id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    @Suppress("WrongConstant")
                    addFlags(0x00080000 or 0x00040000)
                }
            }
            try {
                context.startActivity(callIntent)
            } catch (_: Exception) { }

            val channelId = "reminder_call_channel"
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "Напоминания", NotificationManager.IMPORTANCE_HIGH).apply {
                    setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC)
                    setBypassDnd(true)
                }
                manager.createNotificationChannel(channel)
            }

            val fullScreenPending = PendingIntent.getActivity(
                context,
                (id and 0x7FFFFFFF).toInt(),
                callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
    }
}