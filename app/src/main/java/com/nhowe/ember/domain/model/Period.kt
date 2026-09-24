package com.nhowe.ember.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Progress of a weekly or monthly goal within one of its periods. */
data class PeriodGoalProgress(
    val goal: Goal,
    val version: GoalVersion,
    val start: LocalDate,
    val end: LocalDate,
    /** Logged days -> units that day (1 for a check, the count for a quantity). */
    val days: Map<LocalDate, Int>,
    val doneOn: LocalDate?,
) {
    val target: Int get() = version.target
    val progress: Int get() = days.values.sum()
    val credit: Double get() = (progress.toDouble() / target).coerceIn(0.0, 1.0)
    val isDone: Boolean get() = progress >= target
    fun contains(date: LocalDate): Boolean = date >= start && date <= end
    fun contributionOn(date: LocalDate): Int = days[date] ?: 0
    fun daysLeft(today: LocalDate): Int = ChronoUnit.DAYS.between(today, end).toInt().coerceAtLeast(0)
}
