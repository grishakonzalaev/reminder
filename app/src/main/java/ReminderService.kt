package com.example.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class ReminderService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val id = intent?.getLongExtra(ReminderReceiver.EXTRA_ID, -1L) ?: -1L
            val message = intent?.getStringExtra(ReminderReceiver.EXTRA_MSG) ?: "Пора!"

            val channelId = "reminder_fg_channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "Напоминание", NotificationManager.IMPORTANCE_LOW)
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Напоминание")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            if (Build.VERSION.SDK_INT >= 34) {
                (this as android.app.Service).startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                @Suppress("DEPRECATION")
                (this as android.app.Service).startForeground(NOTIF_ID, notification)
            }

            val callIntent = Intent(this, CallActivity::class.java).apply {
                putExtra(CallActivity.EXTRA_MSG, message)
                putExtra(CallActivity.EXTRA_ID, id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_USER_ACTION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    @Suppress("WrongConstant")
                    addFlags(0x00080000 or 0x00040000)
                }
            }
            applicationContext.startActivity(callIntent)

            // Останавливаем сервис с задержкой, чтобы экран звонка успел открыться
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    (this as android.app.Service).stopForeground(android.app.Service.STOP_FOREGROUND_REMOVE)
                } catch (_: Exception) { }
                stopSelf()
            }, 3000)
        } catch (e: Exception) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    companion object {
        private const val NOTIF_ID = 9001
    }
}
