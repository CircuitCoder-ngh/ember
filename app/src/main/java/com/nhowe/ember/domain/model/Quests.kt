package com.nhowe.ember.domain.model

import java.time.LocalDate

enum class QuestKind { PERFECT_DAYS, HIT_DAYS, LOG_PERIODIC, GOAL_TIMES, WEEKEND, QUANTITY_TOTAL, STREAK_RUN, XP }

/** A short-term target for one Monday-Sunday week, chosen per week and scored from history. */
data class WeeklyQuest(
    val id: String,
    val kind: QuestKind,
    val weekStart: LocalDate,
    val title: String,
    val target: Int,
    val progress: Int,
    val xpReward: Int,
    val completedOn: LocalDate?,
    val goalId: String? = null,
    val emoji: String = "",
) {
    val isComplete: Boolean get() = completedOn != null
    val fraction: Float get() = (progress.toFloat() / target).coerceIn(0f, 1f)
}

/** Sunday-evening summary of a week. */
data class WeeklyRecap(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val score: Double?,
    val bestDay: LocalDate?,
    val bestDayScore: Double?,
    val goalsDone: Int,
    val goalsTotal: Int,
    val perfectDays: Int,
    val daysOnTarget: Int,
    val xpEarned: Int,
    val questsCompleted: Int,
    val observation: String,
) {
    val key: String get() = "recap:$weekStart"
}
