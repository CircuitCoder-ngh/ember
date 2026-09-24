package com.nhowe.ember.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nhowe.ember.appContainer
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Fires roughly every hour; refreshes the progress card from the latest snapshot. */
class HourlyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val container = context.appContainer
        container.appScope.launch {
            try {
                val settings = container.settingsRepository.settings.first()
                val snapshot = withTimeoutOrNull(8_000) { container.engineStore.snapshot.filterNotNull().first() }
                if (snapshot != null) ProgressNotifier.sync(context, snapshot, settings) else ProgressNotifier.clear(context)
            } finally {
                pending.finish()
            }
        }
    }
}
