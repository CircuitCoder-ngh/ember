package com.nhowe.ember.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nhowe.ember.appContainer
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val container = context.appContainer
        container.appScope.launch {
            try {
                val settings = container.settingsRepository.settings.first()
                if (!settings.reminderEnabled) return@launch
                val snapshot = withTimeoutOrNull(8_000) { container.engineStore.snapshot.filterNotNull().first() }
                if (snapshot != null) {
                    val plan = snapshot.todayPlan
                    val score = plan.score ?: 0.0
                    if (plan.hasGoals && score < settings.streakThreshold) {
                        Notifications.showReminder(context, plan, snapshot.streak.current)
                    }
                }
                container.reminderScheduler.scheduleNext(settings.reminderTime)
            } finally {
                pending.finish()
            }
        }
    }
}
