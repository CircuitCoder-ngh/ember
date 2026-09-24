package com.nhowe.ember.domain.engine

import com.nhowe.ember.core.time.endOfMonth
import com.nhowe.ember.domain.model.Badge
import com.nhowe.ember.domain.model.DayPlan
import com.nhowe.ember.domain.model.DayState
import com.nhowe.ember.domain.model.XpState
import java.time.LocalDate
import java.time.YearMonth

object BadgeEngine {

    private const val PERFECT_MONTH_MIN_DAYS = 15

    /** Returns each earned badge with the date it was first earned. */
    fun compute(
        plans: Map<LocalDate, DayPlan>,
        streak: StreakEngine.Result,
        xp: XpState,
        today: LocalDate,
    ): Map<Badge, LocalDate> {
        val earned = LinkedHashMap<Badge, LocalDate>()
        fun award(badge: Badge, date: LocalDate) { earned.putIfAbsent(badge, date) }

        var perfectCount = 0
        var consecutivePerfect = 0
        var goalsDone = 0
        var cumulativeXp = 0
        var seenMiss = false
        var streakSinceMiss = 0

        for (plan in plans.values.sortedBy { it.date }) {
            val date = plan.date
            if (date > today) break
            val state = streak.dayStates[date] ?: continue
            val streakLen = streak.streakByDay[date] ?: 0

            // Streak badges
            streakBadge(streakLen)?.let { award(it, date) }

            // Perfection
            if (state == DayState.PERFECT) {
                perfectCount++
                consecutivePerfect++
                award(Badge.FIRST_PERFECT, date)
                if (perfectCount >= 10) award(Badge.PERFECT_10, date)
                if (perfectCount >= 50) award(Badge.PERFECT_50, date)
                if (consecutivePerfect >= 7) award(Badge.PERFECT_WEEK, date)
            } else if (state != DayState.REST && state != DayState.PENDING) {
                consecutivePerfect = 0
            }

            // Resilience
            if (state == DayState.MISS) {
                seenMiss = true
                streakSinceMiss = 0
            } else if (state == DayState.HIT || state == DayState.PERFECT) {
                streakSinceMiss++
                if (seenMiss && streakSinceMiss >= 7) award(Badge.COMEBACK, date)
            }
            if (state == DayState.FROZEN) award(Badge.FREEZE_SAVED, date)

            // Volume
            goalsDone += plan.doneCount
            if (goalsDone >= 100) award(Badge.GOALS_100, date)
            if (goalsDone >= 1000) award(Badge.GOALS_1000, date)
            cumulativeXp += xp.xpByDay[date] ?: 0
            val level = XpEngine.levelFor(cumulativeXp)
            if (level >= 5) award(Badge.LEVEL_5, date)
            if (level >= 10) award(Badge.LEVEL_10, date)
        }

        // Perfect month: a fully elapsed month where every scored day was perfect.
        val thisMonth = YearMonth.from(today)
        plans.values.groupBy { YearMonth.from(it.date) }
            .filterKeys { it < thisMonth }
            .forEach { (month, monthPlans) ->
                val scored = monthPlans.filter { it.score != null }
                if (scored.size >= PERFECT_MONTH_MIN_DAYS && scored.all { it.isPerfect }) {
                    award(Badge.PERFECT_MONTH, month.atDay(1).endOfMonth())
                }
            }

        return earned
    }

    private fun streakBadge(len: Int): Badge? = when {
        len >= 365 -> Badge.STREAK_365
        len >= 100 -> Badge.STREAK_100
        len >= 50 -> Badge.STREAK_50
        len >= 30 -> Badge.STREAK_30
        len >= 14 -> Badge.STREAK_14
        len >= 7 -> Badge.STREAK_7
        len >= 3 -> Badge.STREAK_3
        else -> null
    }
}
