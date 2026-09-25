package com.nhowe.ember.data.templates

import com.nhowe.ember.core.time.ALL_WEEKDAYS
import com.nhowe.ember.data.repo.GoalDraft
import com.nhowe.ember.domain.model.Cadence
import com.nhowe.ember.domain.model.GoalType
import kotlinx.serialization.Serializable

@Serializable
data class PhaseSpec(
    val fromDay: Int,
    val title: String? = null,
    val target: Int? = null,
    val note: String? = null,
    val weekdays: Int? = null,
)

@Serializable
data class ProgramGoalSpec(
    val key: String,
    val title: String,
    val emoji: String = "✅",
    val colorIndex: Int = 0,
    val type: String = "CHECK",
    val cadence: String = "DAILY",
    val target: Int? = null,
    val unit: String? = null,
    val weekdays: Int = ALL_WEEKDAYS,
    val weight: Int = 1,
    val note: String? = null,
    val phases: List<PhaseSpec> = listOf(PhaseSpec(1)),
) {
    val isPeriodic: Boolean get() = cadence != "DAILY"

    /** The goal as it should look during [phase]. */
    fun draftFor(phase: PhaseSpec) = GoalDraft(
        title = phase.title ?: title, emoji = emoji, colorIndex = colorIndex,
        type = runCatching { GoalType.valueOf(type) }.getOrDefault(GoalType.CHECK),
        targetCount = phase.target ?: target, unit = unit, weekdayMask = phase.weekdays ?: weekdays, weight = weight,
        note = phase.note ?: note,
        cadence = runCatching { Cadence.valueOf(cadence) }.getOrDefault(Cadence.DAILY),
    )
}

@Serializable
data class GraduationSpec(val title: String, val icon: String = "🏅", val xp: Int = 300)

@Serializable
data class ProgramTemplate(
    val id: String,
    val category: String,
    val emoji: String,
    val title: String,
    val blurb: String,
    val lengthDays: Int,
    val goals: List<ProgramGoalSpec>,
    val graduation: GraduationSpec,
    val strict: Boolean = false,
    val next: String? = null,
) {
    val weeks: Int get() = (lengthDays + 6) / 7
    val hasPeriodic: Boolean get() = goals.any { it.isPeriodic }
    val dailyCount: Int get() = goals.count { !it.isPeriodic }
}

@Serializable
data class ProgramLibrary(val version: Int = 1, val programs: List<ProgramTemplate>)
