package com.nhowe.ember.domain.engine

import com.nhowe.ember.core.time.datesBetween
import com.nhowe.ember.domain.model.DayPlan
import com.nhowe.ember.domain.model.EarnedBadge
import com.nhowe.ember.domain.model.History
import com.nhowe.ember.domain.model.Program
import com.nhowe.ember.domain.model.ProgramProgress
import java.time.LocalDate

/** Progress, strict-day counting and graduation for enrolled programs, all from history. */
object ProgramEngine {

    fun progressAll(history: History, plans: Map<LocalDate, DayPlan>, today: LocalDate): List<ProgramProgress> =
        history.programs.map { progress(it, plans, today) }

    fun progress(program: Program, plans: Map<LocalDate, DayPlan>, today: LocalDate): ProgramProgress {
        val ids = program.goalIds.toSet()
        val dayIndex = if (today < program.startDate) 0 else minOf((today.toEpochDay() - program.startDate.toEpochDay()).toInt() + 1, program.lengthDays)
        val week = if (dayIndex == 0) 0 else (dayIndex - 1) / 7 + 1
        val elapsedEnd = minOf(today, program.endDate)

        // Adherence: mean credit of the program's daily goals on elapsed days that had any.
        val credits = ArrayList<Double>()
        var strict = 0
        var brokenOn: LocalDate? = null
        if (today >= program.startDate) {
            for (d in datesBetween(program.startDate, elapsedEnd)) {
                val goals = plans[d]?.goals?.filter { it.id in ids } ?: emptyList()
                if (goals.isEmpty()) continue
                credits += goals.sumOf { it.credit } / goals.size
                val allDone = goals.all { it.isDone }
                if (allDone) strict++
                else if (d < today) { strict = 0; brokenOn = d }
                // today unfinished is pending, not a break
            }
        }
        val graduatedOn = if (!program.isAbandoned && today > program.endDate) program.endDate else null
        return ProgramProgress(
            program = program,
            dayIndex = dayIndex,
            week = week,
            fraction = (dayIndex.toFloat() / program.lengthDays).coerceIn(0f, 1f),
            adherence = if (credits.isEmpty()) null else credits.average(),
            strictDay = strict,
            strictBrokenOn = brokenOn,
            graduatedOn = graduatedOn,
        )
    }

    fun badges(progress: List<ProgramProgress>): List<EarnedBadge> =
        progress.mapNotNull { p ->
            p.graduatedOn?.let { EarnedBadge(p.program.graduationTitle, p.program.graduationIcon, it, "Finished ${p.program.title}") }
        }.sortedBy { it.earnedOn }

    /** Graduation XP lands on the day after the program ends (the day it is recognised). */
    fun xpByDay(progress: List<ProgramProgress>): Map<LocalDate, Int> =
        progress.filter { it.graduatedOn != null }.groupBy { it.graduatedOn!!.plusDays(1) }
            .mapValues { (_, list) -> list.sumOf { it.program.graduationXp } }
}
