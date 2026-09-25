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
            is CelebrationEvent.ComebackComplete -> "comeback|${e.date}|${e.freezeGranted}|${e.brokenStreak}"
            is CelebrationEvent.QuestComplete -> "quest|${e.quest.weekStart}|${e.quest.id}|${e.quest.title}|${e.quest.xpReward}"
        }
    },
    restore = { s ->
        val parts = s.split("|")
        when (parts.firstOrNull()) {
            "perfect" -> CelebrationEvent.PerfectDay(LocalDate.parse(parts[1]))
            "milestone" -> CelebrationEvent.StreakMilestone(parts[1].toInt(), parts[2].toBoolean())
            "level" -> CelebrationEvent.LevelUp(parts[1].toInt(), parts[2])
            "badge" -> CelebrationEvent.BadgeEarned(Badge.valueOf(parts[1]))
            "comeback" -> CelebrationEvent.ComebackComplete(LocalDate.parse(parts[1]), parts[2].toBoolean(), parts[3].toInt())
            "quest" -> CelebrationEvent.QuestComplete(com.nhowe.ember.domain.model.WeeklyQuest(parts[2], com.nhowe.ember.domain.model.QuestKind.XP, LocalDate.parse(parts[1]), parts[3], 1, 1, parts[4].toInt(), LocalDate.parse(parts[1])))
            else -> null
        }
    },
)
