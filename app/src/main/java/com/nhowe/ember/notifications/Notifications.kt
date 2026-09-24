package com.nhowe.ember.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.nhowe.ember.MainActivity
import com.nhowe.ember.R
import com.nhowe.ember.domain.model.DayPlan
import kotlin.math.roundToInt

object Notifications {
    const val CHANNEL_REMINDER = "daily_reminder"
    private const val ID_REMINDER = 1001

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_REMINDER, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "A nudge when today's goals are still open"
        }
        manager.createNotificationChannel(channel)
        val hourly = NotificationChannel(ProgressNotifier.CHANNEL, "Hourly progress card", NotificationManager.IMPORTANCE_LOW).apply {
            description = "A silent card with what's left today, refreshed every hour"
        }
        manager.createNotificationChannel(hourly)
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun showReminder(context: Context, plan: DayPlan, streak: Int) {
        if (!canPost(context)) return
        val left = plan.goals.size - plan.doneCount
        val pct = ((plan.score ?: 0.0) * 100).roundToInt()
        val title = if (left == 1) "1 goal left today" else "$left goals left today"
        val text = buildString {
            append("You're at $pct%")
            if (streak > 0) append(" · $streak-day streak on the line")
        }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(ID_REMINDER, notification)
    }
}
