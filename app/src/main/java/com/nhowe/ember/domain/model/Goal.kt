package com.nhowe.ember.domain.model

import com.nhowe.ember.core.time.hasWeekday
import java.time.LocalDate

enum class GoalKind { RECURRING, ONE_OFF }

enum class GoalType { CHECK, QUANTITY }

/** How often the goal resets. DAILY goals score each day; WEEKLY/MONTHLY goals have a target per period. */
enum class Cadence { DAILY, WEEKLY, MONTHLY }

/** Stable identity of a goal. Everything the user can edit lives on [GoalVersion]. */
data class Goal(
    val id: String,
    val kind: GoalKind,
    val oneOffDate: LocalDate?,
    val createdAt: Long,
    val archivedAt: Long?,
    val sortOrder: Int,
) {
    val isArchived: Boolean get() = archivedAt != null
}

/**
 * A snapshot of a goal's definition valid for the inclusive date range [validFrom, validTo].
 * Editing a goal closes the current version and opens a new one, so history never changes.
 */
data class GoalVersion(
    val id: String,
    val goalId: String,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val title: String,
    val emoji: String,
    val colorIndex: Int,
    val type: GoalType,
    val targetCount: Int?,
    val unit: String?,
    val weekdayMask: Int,
    val weight: Int = 1,
    val note: String? = null,
    val cadence: Cadence = Cadence.DAILY,
) {
    val isPeriodic: Boolean get() = cadence != Cadence.DAILY

    fun covers(date: LocalDate): Boolean =
        date >= validFrom && (validTo == null || date <= validTo)

    fun scheduledOn(date: LocalDate, goal: Goal): Boolean = when (goal.kind) {
        GoalKind.ONE_OFF -> goal.oneOffDate == date
        GoalKind.RECURRING -> isPeriodic || weekdayMask.hasWeekday(date.dayOfWeek)
    }

    val target: Int get() = (targetCount ?: 1).coerceAtLeast(1)
}

data class Completion(
    val goalId: String,
    val date: LocalDate,
    val checked: Boolean,
    val count: Int,
    val updatedAt: Long,
)

enum class OverrideKind { SKIP }

data class DayOverride(
    val goalId: String,
    val date: LocalDate,
    val kind: OverrideKind,
)

/** Everything the engine needs. Loaded once and kept in memory; a personal app has few rows. */
data class History(
    val goals: List<Goal> = emptyList(),
    val versions: List<GoalVersion> = emptyList(),
    val completions: List<Completion> = emptyList(),
    val overrides: List<DayOverride> = emptyList(),
    val programs: List<Program> = emptyList(),
) {
    val firstDate: LocalDate? get() = versions.minOfOrNull { it.validFrom }
}
