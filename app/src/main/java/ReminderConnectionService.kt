package com.example.reminder

import android.net.Uri
import android.os.Build
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi

/**
 * Сервис Telecom для напоминаний: система воспринимает напоминание как входящий звонок,
 * гарнитура звонит и кнопка ответа открывает экран звонка (TTS).
 */
@RequiresApi(Build.VERSION_CODES.M)
class ReminderConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        lastRequestExtras = request.extras
        val connection = ReminderConnection()
        val extras = request.extras
        if (extras != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            connection.setExtras(extras)
        }
        connection.setAddress(Uri.parse("tel:reminder"), TelecomManager.PRESENTATION_ALLOWED)
        connection.setCallerDisplayName("Напоминание", TelecomManager.PRESENTATION_ALLOWED)
        connection.setRinging()
        return connection
    }

    companion object {
        @Volatile
        var lastRequestExtras: android.os.Bundle? = null
    }
}
