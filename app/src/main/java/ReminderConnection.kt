package com.example.reminder

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi

/**
 * Представляет «звонок» напоминания в системе Telecom.
 * Позволяет гарнитуре воспринимать напоминание как звонок и отвечать кнопкой.
 */
@RequiresApi(Build.VERSION_CODES.M)
class ReminderConnection : Connection() {

    override fun onAnswer() {
        setActive()
        val ctx = ReminderApp.instance ?: return
        val extras = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> getExtras()
            else -> null
        } ?: ReminderConnectionService.lastRequestExtras
        val msg = extras?.getString(ReminderReceiver.EXTRA_MSG) ?: "Пора!"
        val id = extras?.getLong(ReminderReceiver.EXTRA_ID, -1L) ?: -1L
        val intent = Intent(ctx, CallActivity::class.java).apply {
            putExtra(CallActivity.EXTRA_MSG, msg)
            putExtra(CallActivity.EXTRA_ID, id)
            putExtra(CallActivity.EXTRA_ANSWERED_BY_HEADSET, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        ctx.startActivity(intent)
        destroy()
    }

    override fun onReject() {
        destroy()
    }

    override fun onAbort() {
        destroy()
    }

    override fun onDisconnect() {
        destroy()
    }
}
