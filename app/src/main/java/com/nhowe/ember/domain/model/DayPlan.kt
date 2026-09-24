package com.nhowe.ember.domain.model

import java.time.LocalDate

/** A goal as it applied on a given day, with that day's progress. */
data class ResolvedGoal(
    val goal: Goal,
    val version: GoalVersion,
    val completion: Completion?,
) {
    val id: String get() = goal.id
    val count: Int get() = completion?.count ?: 0
    val checked: Boolean get() = completion?.checked ?: false

    /** 0.0..1.0 credit toward the day's score. */
    val credit: Double
        get() = when (version.type) {
            GoalType.CHECK -> if (checked) 1.0 else 0.0
            GoalType.QUANTITY -> (count.toDouble() / version.target).coerceIn(0.0, 1.0)
        }

    val isDone: Boolean get() = credit >= 1.0
}

data class DayPlan(
    val date: LocalDate,
    val goals: List<ResolvedGoal>,
    val skipped: List<ResolvedGoal> = emptyList(),
) {
    /** Weighted mean credit, or null when nothing was scheduled (a rest day). */
    val score: Double?
        get() {
            if (goals.isEmpty()) return null
            val totalWeight = goals.sumOf { it.version.weight.coerceAtLeast(1) }
            val earned = goals.sumOf { it.credit * it.version.weight.coerceAtLeast(1) }
            return earned / totalWeight
        }

    val isPerfect: Boolean get() = (score ?: 0.0) >= PERFECT_SCORE
    val doneCount: Int get() = goals.count { it.isDone }
    val hasGoals: Boolean get() = goals.isNotEmpty()

    companion object {
        const val PERFECT_SCORE = 0.999
    }
}

/** How a day is displayed on the calendar and counted by the streak engine. */
enum class DayState { PERFECT, HIT, MISS, FROZEN, REST, PENDING, FUTURE }
