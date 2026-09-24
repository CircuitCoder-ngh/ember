package com.nhowe.ember.domain.engine

import com.nhowe.ember.domain.model.DayPlan
import java.time.LocalDate

object Scoring {

    data class PeriodStats(
        val score: Double?,
        val scoredDays: Int,
        val perfectDays: Int,
        val goalsDone: Int,
        val goalsTotal: Int,
    )

    /** Mean of day scores in [from, to] up to [today]. Rest days are excluded, not counted as zero. */
    fun periodScore(plans: Map<LocalDate, DayPlan>, from: LocalDate, to: LocalDate, today: LocalDate): Double? =
        periodStats(plans, from, to, today).score

    fun periodStats(plans: Map<LocalDate, DayPlan>, from: LocalDate, to: LocalDate, today: LocalDate): PeriodStats {
        val inRange = plans.values.filter { it.date >= from && it.date <= to && it.date <= today }
        val scores = inRange.mapNotNull { it.score }
        return PeriodStats(
            score = if (scores.isEmpty()) null else scores.average(),
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
