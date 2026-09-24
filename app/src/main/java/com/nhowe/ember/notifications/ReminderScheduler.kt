package com.nhowe.ember.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.nhowe.ember.domain.model.Settings
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Schedules the daily nudge with an inexact alarm (no special permission needed). */
class ReminderScheduler(private val context: Context) {
    private val alarmManager get() = context.getSystemService(AlarmManager::class.java)

    fun sync(settings: Settings) {
        if (settings.reminderEnabled) scheduleNext(settings.reminderTime) else cancel()
        syncHourly(settings)
    }

    /** Inexact repeating alarm at the top of every hour; the receiver decides whether to show anything. */
    fun syncHourly(settings: Settings) {
        val pi = hourlyIntent()
        if (!settings.hourlyEnabled) { alarmManager.cancel(pi); return }
        val next = ZonedDateTime.now(ZoneId.systemDefault()).plusHours(1).withMinute(0).withSecond(0).withNano(0)
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), AlarmManager.INTERVAL_HOUR, pi)
    }

    private fun hourlyIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, HOURLY_REQUEST_CODE, Intent(context, HourlyReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun scheduleNext(time: LocalTime, now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())) {
        var next = now.with(time).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(),
            WINDOW_MS,
            pendingIntent(),
        )
    }

    fun cancel() = alarmManager.cancel(pendingIntent())

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val REQUEST_CODE = 42
        const val HOURLY_REQUEST_CODE = 43
        const val WINDOW_MS = 10L * 60L * 1000L
    }
}
