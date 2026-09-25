package com.nhowe.ember.domain.model

import java.time.LocalDate

data class Milestone(val days: Int, val reachedOn: LocalDate)

/**
 * Softens a broken streak: hit the bar [target] days in a row after a break and earn a freeze back.
 * A miss during the quest restarts its progress; rest days are neutral.
 */
data class ComebackQuest(
    val startedOn: LocalDate,
    val brokenStreak: Int,
    val progress: Int = 0,
    val target: Int = 3,
    val completedOn: LocalDate? = null,
    val freezeGranted: Boolean = false,
) {
    val isComplete: Boolean get() = completedOn != null
}

data class StreakState(
    val current: Int = 0,
    val best: Int = 0,
    val bestEndedOn: LocalDate? = null,
    val freezesHeld: Int = 0,
    val milestones: List<Milestone> = emptyList(),
    val todaySecured: Boolean = false,
    val frozenDays: Set<LocalDate> = emptySet(),
    /** The quest in progress, if a streak recently broke. */
    val quest: ComebackQuest? = null,
    /** Quests finished, newest last. */
    val completedQuests: List<ComebackQuest> = emptyList(),
) {
    /** The next milestone above the current streak, if any. */
    fun nextMilestone(all: List<Int>): Int? = all.firstOrNull { it > current }
}

data class XpState(
    val totalXp: Int = 0,
    val level: Int = 1,
    val levelTitle: String = "",
    val xpIntoLevel: Int = 0,
    val xpForNextLevel: Int = 100,
    val todayXp: Int = 0,
    val xpByDay: Map<LocalDate, Int> = emptyMap(),
) {
    val levelProgress: Float
        get() = if (xpForNextLevel <= 0) 1f else (xpIntoLevel.toFloat() / xpForNextLevel).coerceIn(0f, 1f)
}

enum class BadgeGroup { STREAK, PERFECTION, RESILIENCE, VOLUME }

enum class Badge(val group: BadgeGroup, val title: String, val blurb: String, val icon: String) {
    STREAK_3(BadgeGroup.STREAK, "Kindling", "3-day streak", "🔥"),
    STREAK_7(BadgeGroup.STREAK, "One Week Strong", "7-day streak", "🔥"),
    STREAK_14(BadgeGroup.STREAK, "Fortnight", "14-day streak", "🔥"),
    STREAK_30(BadgeGroup.STREAK, "Monthly Blaze", "30-day streak", "🔥"),
    STREAK_50(BadgeGroup.STREAK, "Half Century", "50-day streak", "🔥"),
    STREAK_100(BadgeGroup.STREAK, "Centurion", "100-day streak", "🏆"),
    STREAK_365(BadgeGroup.STREAK, "Full Orbit", "365-day streak", "🪐"),
    FIRST_PERFECT(BadgeGroup.PERFECTION, "Flawless", "Your first perfect day", "✨"),
    PERFECT_10(BadgeGroup.PERFECTION, "Ten Out of Ten", "10 perfect days", "✨"),
    PERFECT_50(BadgeGroup.PERFECTION, "Gold Standard", "50 perfect days", "🌟"),
    PERFECT_WEEK(BadgeGroup.PERFECTION, "Perfect Week", "7 perfect days in a row", "🥇"),
    PERFECT_MONTH(BadgeGroup.PERFECTION, "Perfect Month", "Every day of a month perfect", "👑"),
    COMEBACK(BadgeGroup.RESILIENCE, "Comeback", "A new 7-day streak after a miss", "💪"),
    FREEZE_SAVED(BadgeGroup.RESILIENCE, "Saved by Ice", "A streak freeze protected you", "🧊"),
    GOALS_100(BadgeGroup.VOLUME, "Century of Wins", "100 goals completed", "💯"),
    GOALS_1000(BadgeGroup.VOLUME, "Thousand Wins", "1,000 goals completed", "🎯"),
    LEVEL_5(BadgeGroup.VOLUME, "Level 5", "Reached level 5", "⭐"),
    LEVEL_10(BadgeGroup.VOLUME, "Level 10", "Reached level 10", "🌠"),
}

/** Something worth celebrating that happened today. */
sealed class CelebrationEvent(val key: String) {
    data class PerfectDay(val date: LocalDate) : CelebrationEvent("perfect:$date")
    data class StreakMilestone(val days: Int, val freezeGranted: Boolean) : CelebrationEvent("milestone:$days")
    data class LevelUp(val level: Int, val title: String) : CelebrationEvent("level:$level")
    data class BadgeEarned(val badge: Badge) : CelebrationEvent("badge:${badge.name}")
    data class ComebackComplete(val date: LocalDate, val freezeGranted: Boolean, val brokenStreak: Int) : CelebrationEvent("comeback:$date")
    data class QuestComplete(val quest: WeeklyQuest) : CelebrationEvent("quest:${quest.weekStart}:${quest.id}")
    data class ProgramGraduated(val programId: String, val title: String, val badgeTitle: String, val badgeIcon: String, val xp: Int, val nextTemplateId: String?) : CelebrationEvent("program:$programId")
}

/** Everything derived from history + settings for a given "today". */
data class EngineSnapshot(
    val today: LocalDate,
    val history: History,
    val settings: Settings,
    val plans: Map<LocalDate, DayPlan>,
    val dayStates: Map<LocalDate, DayState>,
    val streak: StreakState,
    val xp: XpState,
    val badges: Map<Badge, LocalDate>,
    val pendingCelebrations: List<CelebrationEvent>,
    val periods: List<PeriodGoalProgress> = emptyList(),
    /** This week's quests. */
    val quests: List<WeeklyQuest> = emptyList(),
    /** Quests from every week so far, for stats and recaps. */
    val allQuests: List<WeeklyQuest> = emptyList(),
    val programs: List<ProgramProgress> = emptyList(),
    val programBadges: List<EarnedBadge> = emptyList(),
) {
    val activePrograms: List<ProgramProgress> get() = programs.filter { it.isActive }
    fun periodFor(goalId: String, date: LocalDate): PeriodGoalProgress? =
        periods.firstOrNull { it.goal.id == goalId && it.contains(date) }

    val todayPlan: DayPlan get() = plans[today] ?: DayPlan(today, emptyList())
}
