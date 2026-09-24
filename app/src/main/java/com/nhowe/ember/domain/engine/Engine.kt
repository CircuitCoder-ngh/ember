package com.nhowe.ember.domain.engine

import com.nhowe.ember.domain.model.Badge
import com.nhowe.ember.domain.model.CelebrationEvent
import com.nhowe.ember.domain.model.EngineSnapshot
import com.nhowe.ember.domain.model.History
import com.nhowe.ember.domain.model.Settings
import java.time.LocalDate

/** Pure entry point: history + settings + today -> everything the UI shows. */
object Engine {

    fun compute(
        history: History,
        settings: Settings,
        today: LocalDate,
        shownCelebrationKeys: Set<String> = emptySet(),
    ): EngineSnapshot {
        val start = history.firstDate?.let { minOf(it, today) } ?: today
        val plans = DayPlanResolver.resolveRange(start, today, history)
        val scores = plans.mapValues { it.value.score }.toSortedMap()
        val streak = StreakEngine.compute(scores, today, settings.streakThreshold)
        val periods = PeriodResolver.resolveRange(start, today, history)
        val xp = XpEngine.compute(plans, streak.streakByDay, today, PeriodResolver.xpByDay(periods))
        val badges = BadgeEngine.compute(plans, streak, xp, today)

        val events = ArrayList<CelebrationEvent>()
        val todayPlan = plans[today]
        if (todayPlan != null && todayPlan.isPerfect) events += CelebrationEvent.PerfectDay(today)
        streak.state.milestones.lastOrNull()
            ?.takeIf { it.reachedOn == today && streak.state.todaySecured }
            ?.let { events += CelebrationEvent.StreakMilestone(it.days, it.days >= StreakEngine.FREEZE_MIN_MILESTONE) }
        val levelBefore = XpEngine.levelFor(xp.totalXp - xp.todayXp)
        if (xp.level > levelBefore) events += CelebrationEvent.LevelUp(xp.level, xp.levelTitle)
        badges.filterValues { it == today }.keys.forEach { badge: Badge -> events += CelebrationEvent.BadgeEarned(badge) }

        return EngineSnapshot(
            today = today,
            history = history,
            settings = settings,
            plans = plans,
            dayStates = streak.dayStates,
            streak = streak.state,
            xp = xp,
            badges = badges,
            pendingCelebrations = events.filter { it.key !in shownCelebrationKeys },
            periods = periods,
        )
    }
}
