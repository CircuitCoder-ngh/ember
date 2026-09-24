package com.nhowe.ember.ui.navigation

import androidx.compose.runtime.saveable.Saver
import com.nhowe.ember.domain.model.Badge
import com.nhowe.ember.domain.model.CelebrationEvent
import java.time.LocalDate

/** Survives configuration changes; a celebration in progress should not vanish on rotate. */
val CelebrationSaver: Saver<CelebrationEvent?, String> = Saver(
    save = { e ->
        when (e) {
            null -> ""
            is CelebrationEvent.PerfectDay -> "perfect|${e.date}"
            is CelebrationEvent.StreakMilestone -> "milestone|${e.days}|${e.freezeGranted}"
            is CelebrationEvent.LevelUp -> "level|${e.level}|${e.title}"
            is CelebrationEvent.BadgeEarned -> "badge|${e.badge.name}"
        }
    },
    restore = { s ->
        val parts = s.split("|")
        when (parts.firstOrNull()) {
            "perfect" -> CelebrationEvent.PerfectDay(LocalDate.parse(parts[1]))
            "milestone" -> CelebrationEvent.StreakMilestone(parts[1].toInt(), parts[2].toBoolean())
            "level" -> CelebrationEvent.LevelUp(parts[1].toInt(), parts[2])
            "badge" -> CelebrationEvent.BadgeEarned(Badge.valueOf(parts[1]))
            else -> null
        }
    },
)
