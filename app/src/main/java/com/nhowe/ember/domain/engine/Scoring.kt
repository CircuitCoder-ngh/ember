package com.nhowe.ember.domain.engine

import com.nhowe.ember.domain.model.DayPlan
import com.nhowe.ember.domain.model.PeriodGoalProgress
import java.time.LocalDate

object Scoring {

    data class PeriodStats(
        val score: Double?,
        val scoredDays: Int,
        val perfectDays: Int,
        val goalsDone: Int,
        val goalsTotal: Int,
        val periodicDone: Int = 0,
        val periodicTotal: Int = 0,
    )

    /** Mean of day scores in [from, to] up to [today]. Rest days are excluded, not counted as zero. */
    fun periodScore(plans: Map<LocalDate, DayPlan>, from: LocalDate, to: LocalDate, today: LocalDate, periods: List<PeriodGoalProgress> = emptyList()): Double? =
        periodStats(plans, from, to, today, periods).score

    /**
     * Day scores count as one item each; a weekly/monthly goal counts as one item (times its weight)
     * whenever its whole period sits inside [from, to] and has started.
     */
    fun periodStats(plans: Map<LocalDate, DayPlan>, from: LocalDate, to: LocalDate, today: LocalDate, periods: List<PeriodGoalProgress> = emptyList()): PeriodStats {
        val inRange = plans.values.filter { it.date >= from && it.date <= to && it.date <= today }
        val scores = inRange.mapNotNull { it.score }
        val periodic = periods.filter { it.start >= from && it.end <= to && it.start <= today }
        var weight = scores.size.toDouble()
        var credit = scores.sum()
        for (p in periodic) {
            val w = p.version.weight.coerceAtLeast(1).toDouble()
            weight += w
            credit += p.credit * w
        }
        return PeriodStats(
            score = if (weight == 0.0) null else credit / weight,
            periodicDone = periodic.count { it.isDone },
            periodicTotal = periodic.size,
            scoredDays = scores.size,
            perfectDays = inRange.count { it.isPerfect },
            goalsDone = inRange.sumOf { it.doneCount },
            goalsTotal = inRange.sumOf { it.goals.size },
        )
    }

    /** Completion rate of a single goal over the days it was scheduled in the range. */
    fun goalRate(plans: Map<LocalDate, DayPlan>, goalId: String, from: LocalDate, to: LocalDate, today: LocalDate): Double? {
        val credits = plans.values
            .filter { it.date >= from && it.date <= to && it.date <= today }
            .mapNotNull { plan -> plan.goals.firstOrNull { it.id == goalId }?.credit }
        return if (credits.isEmpty()) null else credits.average()
    }
}
