package com.nhowe.ember.domain.engine

import com.nhowe.ember.core.time.datesBetween
import com.nhowe.ember.core.time.endOfMonth
import com.nhowe.ember.core.time.endOfWeek
import com.nhowe.ember.core.time.startOfMonth
import com.nhowe.ember.core.time.startOfWeek
import com.nhowe.ember.domain.model.Cadence
import com.nhowe.ember.domain.model.Completion
import com.nhowe.ember.domain.model.Goal
import com.nhowe.ember.domain.model.GoalKind
import com.nhowe.ember.domain.model.GoalType
import com.nhowe.ember.domain.model.GoalVersion
import com.nhowe.ember.domain.model.History
import com.nhowe.ember.domain.model.PeriodGoalProgress
import java.time.LocalDate

/** Turns per-day completions into per-period progress for weekly and monthly goals. */
object PeriodResolver {

    fun bounds(cadence: Cadence, date: LocalDate): Pair<LocalDate, LocalDate> = when (cadence) {
        Cadence.WEEKLY -> date.startOfWeek() to date.endOfWeek()
        Cadence.MONTHLY -> date.startOfMonth() to date.endOfMonth()
        Cadence.DAILY -> date to date
    }

    /** Every period of every periodic goal version that overlaps [from, to]. */
    fun resolveRange(from: LocalDate, to: LocalDate, history: History): List<PeriodGoalProgress> {
        val completions = history.completions.associateBy { it.goalId to it.date }
        val result = ArrayList<PeriodGoalProgress>()
        for (goal in history.goals) {
            if (goal.kind != GoalKind.RECURRING) continue
            for (version in history.versions) {
                if (version.goalId != goal.id || !version.isPeriodic) continue
                val start = maxOf(version.validFrom, from)
                val end = minOf(version.validTo ?: to, to)
                if (start > end) continue
                var (ps, pe) = bounds(version.cadence, start)
                while (ps <= end) {
                    result += progress(goal, version, ps, pe, completions)
                    ps = pe.plusDays(1)
                    pe = bounds(version.cadence, ps).second
                }
            }
        }
        return result
    }

    /** Progress of the goal's period containing [date], or null if no periodic version covers it. */
    fun progressFor(goalId: String, date: LocalDate, history: History): PeriodGoalProgress? {
        val goal = history.goals.firstOrNull { it.id == goalId } ?: return null
        val version = history.versions.firstOrNull { it.goalId == goalId && it.isPeriodic && it.covers(date) } ?: return null
        val (ps, pe) = bounds(version.cadence, date)
        return progress(goal, version, ps, pe, history.completions.associateBy { it.goalId to it.date })
    }

    private fun progress(
        goal: Goal,
        version: GoalVersion,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        completions: Map<Pair<String, LocalDate>, Completion>,
    ): PeriodGoalProgress {
        val days = LinkedHashMap<LocalDate, Int>()
        var running = 0
        var doneOn: LocalDate? = null
        val first = maxOf(periodStart, version.validFrom)
        val last = version.validTo?.let { minOf(periodEnd, it) } ?: periodEnd
        for (d in datesBetween(first, last)) {
            val c = completions[goal.id to d] ?: continue
            val units = if (version.type == GoalType.CHECK) (if (c.checked) 1 else 0) else c.count
            if (units <= 0) continue
            days[d] = units
            running += units
            if (doneOn == null && running >= version.target) doneOn = d
        }
        return PeriodGoalProgress(goal, version, periodStart, periodEnd, days, doneOn)
    }

    /** XP per day from periodic goals: per unit logged (capped at the target) plus a bonus on completion. */
    fun xpByDay(periods: List<PeriodGoalProgress>): Map<LocalDate, Int> {
        val xp = HashMap<LocalDate, Int>()
        for (p in periods) {
            val w = p.version.weight.coerceAtLeast(1)
            var counted = 0
            for ((date, units) in p.days) {
                val usable = (minOf(counted + units, p.target) - counted).coerceAtLeast(0)
                counted += units
                val earned = if (p.version.type == GoalType.CHECK) XpEngine.XP_PER_GOAL * w * usable
                else Math.round(XpEngine.XP_PER_GOAL.toDouble() * w * usable / p.target).toInt()
                if (earned > 0) xp[date] = (xp[date] ?: 0) + earned
            }
            p.doneOn?.let { xp[it] = (xp[it] ?: 0) + PERIOD_BONUS * w }
        }
        return xp
    }

    const val PERIOD_BONUS = 25
}
