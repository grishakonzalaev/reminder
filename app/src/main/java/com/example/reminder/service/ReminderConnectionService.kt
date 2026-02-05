package com.example.reminder.service

import android.net.Uri
import android.os.Build
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi
import com.example.reminder.receiver.ReminderReceiver

/**
 * Сервис Telecom для напоминаний: система воспринимает напоминание как входящий звонок,
 * гарнитура звонит и кнопка ответа открывает экран звонка (TTS).
 * В журнале: адрес = «reminder:» + текст напоминания (часть диалеров покажет текст), имя = тот же текст.
 * Длинный текст обрезается.
 */
@RequiresApi(Build.VERSION_CODES.M)
class ReminderConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        lastRequestExtras = request.extras
        val connection = ReminderConnection()
        connection.setAudioModeIsVoip(true)
        val extras = request.extras
        if (extras != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            connection.setExtras(extras)
        }
        val rawMessage = extras?.getString(ReminderReceiver.EXTRA_MSG)?.takeIf { it.isNotBlank() }
            ?: "Напоминание"
        val forCallLog = if (rawMessage.length > MAX_CALL_LOG_DISPLAY_LENGTH) {
            rawMessage.take(MAX_CALL_LOG_DISPLAY_LENGTH - 1) + "…"
        } else {
            rawMessage
        }
        val safeForUri = forCallLog.replace(":", " ").replace("\n", " ").trim()
        connection.setAddress(Uri.parse("reminder:$safeForUri"), TelecomManager.PRESENTATION_ALLOWED)
        connection.setCallerDisplayName(forCallLog, TelecomManager.PRESENTATION_ALLOWED)
        connection.setRinging()
        return connection
    }

    companion object {
        /** Ограничение длины текста в журнале звонков. */
        private const val MAX_CALL_LOG_DISPLAY_LENGTH = 50

        @Volatile
        var lastRequestExtras: android.os.Bundle? = null
    }
}
