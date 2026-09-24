package com.nhowe.ember.domain.engine

import com.nhowe.ember.core.time.datesBetween
import com.nhowe.ember.domain.model.DayPlan
import com.nhowe.ember.domain.model.History
import com.nhowe.ember.domain.model.OverrideKind
import com.nhowe.ember.domain.model.ResolvedGoal
import java.time.LocalDate
import java.util.SortedMap

/**
 * Answers "which goals applied on this date, and how did they go?" purely from history.
 * Because goal definitions are versioned, past days always resolve to the definition that
 * was in force at the time.
 */
object DayPlanResolver {

    fun resolve(date: LocalDate, history: History): DayPlan =
        resolveRange(date, date, history)[date] ?: DayPlan(date, emptyList())

    fun resolveRange(from: LocalDate, to: LocalDate, history: History): SortedMap<LocalDate, DayPlan> {
        val versionsByGoal = history.versions.groupBy { it.goalId }
        val completions = history.completions.associateBy { it.goalId to it.date }
        val skips = history.overrides
            .filter { it.kind == OverrideKind.SKIP }
            .mapTo(HashSet()) { it.goalId to it.date }
        val orderedGoals = history.goals.sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))

        val result = sortedMapOf<LocalDate, DayPlan>()
        for (date in datesBetween(from, to)) {
            val active = ArrayList<ResolvedGoal>()
            val skipped = ArrayList<ResolvedGoal>()
            for (goal in orderedGoals) {
                val version = versionsByGoal[goal.id]?.firstOrNull { it.covers(date) } ?: continue
                if (!version.scheduledOn(date, goal)) continue
                val resolved = ResolvedGoal(goal, version, completions[goal.id to date])
                if ((goal.id to date) in skips) skipped += resolved else active += resolved
            }
            result[date] = DayPlan(date, active, skipped)
        }
        return result
    }
}
