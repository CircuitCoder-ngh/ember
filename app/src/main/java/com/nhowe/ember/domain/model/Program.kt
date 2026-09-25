package com.nhowe.ember.domain.model

import java.time.LocalDate

/** A stretch of days a program was paused for. [to] is null while the pause is still open. */
data class PausePeriod(val from: LocalDate, val to: LocalDate?) {
    fun contains(date: LocalDate): Boolean = date >= from && (to == null || date <= to)
    /** Days covered so far; an open pause includes [today]. */
    fun days(today: LocalDate): Int {
        val end = to ?: today
        return if (end < from) 0 else (end.toEpochDay() - from.toEpochDay()).toInt() + 1
    }
}

/** An enrolment in a multi-week plan. Goals are ordinary goals whose versions were pre-dated per phase. */
data class Program(
    val id: String,
    val templateId: String,
    val title: String,
    val emoji: String,
    val startDate: LocalDate,
    val lengthDays: Int,
    val goalIds: List<String>,
    val strict: Boolean,
    val graduationTitle: String,
    val graduationIcon: String,
    val graduationXp: Int,
    val nextTemplateId: String?,
    val abandonedOn: LocalDate?,
    val pauses: List<PausePeriod> = emptyList(),
) {
    val openPause: PausePeriod? get() = pauses.firstOrNull { it.to == null }
    val isPaused: Boolean get() = openPause != null
    fun pausedDays(today: LocalDate): Int = pauses.sumOf { it.days(today) }
    fun isPausedOn(date: LocalDate): Boolean = pauses.any { it.contains(date) }
    /** The calendar day the program ends, pushed back by every paused day. */
    fun endDate(today: LocalDate): LocalDate = startDate.plusDays((lengthDays - 1 + pausedDays(today)).toLong())
    val endDate: LocalDate get() = startDate.plusDays((lengthDays - 1).toLong())
    val totalWeeks: Int get() = (lengthDays + 6) / 7
    val isAbandoned: Boolean get() = abandonedOn != null
}

/** Where a program stands as of today. */
data class ProgramProgress(
    val program: Program,
    val dayIndex: Int,          // 1-based; 0 before start
    val week: Int,              // 1-based
    val fraction: Float,
    val adherence: Double?,     // mean day score over program goals on elapsed days
    val strictDay: Int,         // strict programs: consecutive days with every daily goal done, ending today or yesterday
    val strictBrokenOn: LocalDate?,
    val graduatedOn: LocalDate?,
    val endDate: LocalDate,
) {
    val isActive: Boolean get() = graduatedOn == null && !program.isAbandoned
    val isPaused: Boolean get() = program.isPaused
    val daysLeft: Int get() = (program.lengthDays - dayIndex).coerceAtLeast(0)
}

/** A badge earned from a program rather than the fixed catalogue. */
data class EarnedBadge(val title: String, val icon: String, val earnedOn: LocalDate, val blurb: String)
