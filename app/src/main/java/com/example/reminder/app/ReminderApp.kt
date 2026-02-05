package com.example.reminder.app

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi
import com.example.reminder.R
import com.example.reminder.data.preferences.ReminderPreferences
import com.example.reminder.data.repository.ReminderRepository
import com.example.reminder.scheduler.AlarmScheduler
import com.example.reminder.scheduler.CalendarSyncScheduler
import com.example.reminder.service.ReminderConnectionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderApp : android.app.Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        ReminderPreferences.applyThemeMode(this)
        rescheduleAllFutureReminders()
        CalendarSyncScheduler.scheduleIfEnabled(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            registerPhoneAccount()
        }
    }

    /** Восстанавливает будильники для всех будущих напоминаний (после перезагрузки и при любом старте процесса). */
    private fun rescheduleAllFutureReminders() {
        val repo = ReminderRepository(this)
        val scheduler = AlarmScheduler(this)
        val now = System.currentTimeMillis()
        CoroutineScope(Dispatchers.IO).launch {
            repo.getAll()
                .filter { it.timeMillis > now }
                .forEach { scheduler.schedule(it) }
        }
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

