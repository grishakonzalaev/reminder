package com.example.reminder

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi

class ReminderApp : android.app.Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        rescheduleAllFutureReminders()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            registerPhoneAccount()
        }
    }

    /** Восстанавливает будильники для всех будущих напоминаний (после перезагрузки и при любом старте процесса). */
    private fun rescheduleAllFutureReminders() {
        val repo = ReminderRepository(this)
        val scheduler = AlarmScheduler(this)
        val now = System.currentTimeMillis()
        repo.getAll()
            .filter { it.timeMillis > now }
            .forEach { scheduler.schedule(it) }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun registerPhoneAccount() {
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val handle = phoneAccountHandle ?: return
        val account = PhoneAccount.builder(handle, getString(R.string.app_name))
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            .setShortDescription(getString(R.string.app_name))
            .build()
        telecomManager.registerPhoneAccount(account)
    }

    val phoneAccountHandle: PhoneAccountHandle?
        get() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
            val componentName = ComponentName(this, ReminderConnectionService::class.java)
            return PhoneAccountHandle(componentName, PHONE_ACCOUNT_ID)
        }

    companion object {
        const val PHONE_ACCOUNT_ID = "reminder_call"

        @Volatile
        var instance: ReminderApp? = null
            private set
    }
}
