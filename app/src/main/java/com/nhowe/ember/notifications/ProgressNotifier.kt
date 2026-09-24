package com.nhowe.ember.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.nhowe.ember.MainActivity
import com.nhowe.ember.R
import com.nhowe.ember.domain.model.Cadence
import com.nhowe.ember.domain.model.EngineSnapshot
import com.nhowe.ember.domain.model.GoalType
import com.nhowe.ember.domain.model.Settings
import java.time.LocalTime
import kotlin.math.roundToInt

/** The hourly "what's left today" card. Silent, replaced in place, gone once the day is done. */
object ProgressNotifier {
    const val CHANNEL = "hourly_progress"
    const val CHANNEL_ALERT = "hourly_progress_alert"
    private const val ID = 2001

    fun isWithinWindow(settings: Settings, now: LocalTime = LocalTime.now()): Boolean {
        val start = settings.hourlyStart
        val end = settings.hourlyEnd
        return if (start <= end) now >= start && now < end else now >= start || now < end
    }

    /**
     * Posts or refreshes the card, or clears it when nothing is left / outside the window / disabled.
     * [alert] is true only on the hourly tick when the user chose the buzzing variant; live refreshes
     * after a check-off are always quiet.
     */
    fun sync(context: Context, snapshot: EngineSnapshot, settings: Settings, alert: Boolean = false) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!settings.hourlyEnabled || !isWithinWindow(settings) || !Notifications.canPost(context)) {
            manager.cancel(ID)
            return
        }
        val plan = snapshot.todayPlan
        val left = plan.goals.filter { !it.isDone }
        val periodicLeft = plan.periodic.filter { g ->
            val p = snapshot.periodFor(g.id, snapshot.today)
            (p?.progress ?: 0) < g.version.target
        }
        if (left.isEmpty() && periodicLeft.isEmpty()) {
            manager.cancel(ID)
            return
        }
        val pct = ((plan.score ?: 0.0) * 100).roundToInt()
        val title = when {
            !plan.hasGoals -> "Chip away at the week"
            left.isEmpty() -> "$pct% today · the week still needs you"
            left.size == 1 -> "$pct% · 1 goal left today"
            else -> "$pct% · ${left.size} goals left today"
        }
        val lines = ArrayList<String>()
        left.forEach { g ->
            lines += if (g.version.type == GoalType.QUANTITY) "○ ${g.version.emoji} ${g.version.title}  ${g.count}/${g.version.target}${g.version.unit?.let { " $it" } ?: ""}"
            else "○ ${g.version.emoji} ${g.version.title}"
        }
        periodicLeft.forEach { g ->
            val p = snapshot.periodFor(g.id, snapshot.today)
            val span = if (g.version.cadence == Cadence.WEEKLY) "this week" else "this month"
            lines += "◌ ${g.version.emoji} ${g.version.title}  ${p?.progress ?: 0}/${g.version.target} $span"
        }
        if (snapshot.streak.current > 0) lines += "🔥 ${snapshot.streak.current}-day streak"
        lines += ""
        lines += Quotes.random()

        val open = PendingIntent.getActivity(
            context, 1, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val buzz = alert && settings.hourlyAlert
        val notification = NotificationCompat.Builder(context, if (settings.hourlyAlert) CHANNEL_ALERT else CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(left.joinToString(" · ") { it.version.title }.ifEmpty { periodicLeft.joinToString(" · ") { it.version.title } })
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setContentIntent(open)
            .setOnlyAlertOnce(!buzz)
            .setSilent(!buzz)
            .setPriority(if (settings.hourlyAlert) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        manager.notify(ID, notification)
    }

    fun clear(context: Context) = context.getSystemService(NotificationManager::class.java).cancel(ID)

    /** Re-post on the right channel after the silent/buzz choice changes. */
    fun repost(context: Context, snapshot: EngineSnapshot, settings: Settings) {
        clear(context)
        sync(context, snapshot, settings)
    }
}
