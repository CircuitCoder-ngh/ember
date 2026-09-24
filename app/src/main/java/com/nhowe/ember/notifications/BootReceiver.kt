package com.nhowe.ember.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nhowe.ember.appContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Alarms do not survive a reboot; re-arm the reminder. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val container = context.appContainer
        container.appScope.launch {
            try {
                container.reminderScheduler.sync(container.settingsRepository.settings.first())
            } finally {
                pending.finish()
            }
        }
    }
}
