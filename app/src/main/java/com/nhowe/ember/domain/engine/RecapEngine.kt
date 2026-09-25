package com.nhowe.ember.domain.engine

import com.nhowe.ember.domain.model.DayPlan
import com.nhowe.ember.domain.model.EngineSnapshot
import com.nhowe.ember.domain.model.WeeklyRecap
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Builds the Sunday-evening summary for any week from a snapshot. */
object RecapEngine {

    fun recap(weekStart: LocalDate, snap: EngineSnapshot): WeeklyRecap? {
        val weekEnd = weekStart.plusDays(6)
        val plans = (0..6L).mapNotNull { snap.plans[weekStart.plusDays(it)] }.filter { it.date <= snap.today }
        val scored = plans.filter { it.score != null }
        if (scored.isEmpty()) return null
        val stats = Scoring.periodStats(snap.plans, weekStart, weekEnd, snap.today, snap.periods)
        val best = scored.maxByOrNull { it.score!! }
        val xp = (0..6L).sumOf { snap.xp.xpByDay[weekStart.plusDays(it)] ?: 0 }
        val quests = snap.allQuests.count { it.weekStart == weekStart && it.isComplete }
        val perfect = scored.count { it.isPerfect }
        val onTarget = scored.count { (it.score ?: 0.0) + 1e-9 >= snap.settings.streakThreshold }
        return WeeklyRecap(
            weekStart = weekStart,
            weekEnd = weekEnd,
            score = stats.score,
            bestDay = best?.date,
            bestDayScore = best?.score,
            goalsDone = stats.goalsDone,
            goalsTotal = stats.goalsTotal,
            perfectDays = perfect,
            daysOnTarget = onTarget,
            xpEarned = xp,
            questsCompleted = quests,
            observation = observation(weekStart, scored, perfect, best),
        )
    }

    private data class GoalStat(val emoji: String, val title: String, val done: Int, val scheduled: Int) {
        val rate get() = done.toDouble() / scheduled
    }

    private fun observation(weekStart: LocalDate, plans: List<DayPlan>, perfect: Int, best: DayPlan?): String {
        if (perfect >= 7) return "Seven perfect days. A flawless week."
        val byGoal = LinkedHashMap<String, GoalStat>()
        plans.forEach { p ->
            p.goals.forEach { g ->
                val s = byGoal[g.id] ?: GoalStat(g.version.emoji, g.version.title, 0, 0)
                byGoal[g.id] = s.copy(done = s.done + if (g.isDone) 1 else 0, scheduled = s.scheduled + 1)
            }
        }
        val eligible = byGoal.values.filter { it.scheduled >= 3 }
        val top = eligible.maxByOrNull { it.rate }
        val weak = eligible.filter { it.rate < 0.5 }.minByOrNull { it.rate }
        val alternate = weekStart.toEpochDay() % 2 == 1L
        return when {
            top != null && eligible.all { it.rate >= 0.999 } -> "Every goal, every day it was scheduled."
            alternate && weak != null && top != null && weak !== top ->
                "${weak.emoji} ${weak.title} slipped: ${weak.done} of ${weak.scheduled} days. Worth a look next week."
            top != null && top.rate >= 0.5 ->
                "${top.emoji} ${top.title} was your most consistent goal, ${top.done} of ${top.scheduled} days."
            best != null && best.score != null ->
                "${best.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())} was your best day at ${(best.score!! * 100).roundToInt()}%."
            else -> "A week on the board. That's the whole game."
        }
    }
}
