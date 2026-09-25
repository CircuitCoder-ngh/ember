package com.nhowe.ember.domain.engine

import com.nhowe.ember.core.time.datesBetween
import com.nhowe.ember.core.time.startOfWeek
import com.nhowe.ember.domain.model.DayPlan
import com.nhowe.ember.domain.model.GoalKind
import com.nhowe.ember.domain.model.GoalType
import com.nhowe.ember.domain.model.History
import com.nhowe.ember.domain.model.PeriodGoalProgress
import com.nhowe.ember.domain.model.QuestKind
import com.nhowe.ember.domain.model.WeeklyQuest
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

/**
 * Two fresh targets every Monday-Sunday week, picked deterministically from the week's start date so
 * they never change under the user, and scored purely from history.
 */
object QuestEngine {

    const val QUESTS_PER_WEEK = 2
    const val XP_REWARD = 75

    private data class Candidate(val id: String, val kind: QuestKind, val title: String, val target: Int, val goalId: String? = null, val emoji: String = "")

    fun allWeeks(
        history: History,
        plans: Map<LocalDate, DayPlan>,
        periods: List<PeriodGoalProgress>,
        baseXpByDay: Map<LocalDate, Int>,
        threshold: Double,
        today: LocalDate,
    ): List<WeeklyQuest> {
        val first = history.firstDate ?: return emptyList()
        val out = ArrayList<WeeklyQuest>()
        var ws = first.startOfWeek()
        val lastWeek = today.startOfWeek()
        while (ws <= lastWeek) {
            out += forWeek(ws, history, plans, periods, baseXpByDay, threshold, today)
            ws = ws.plusWeeks(1)
        }
        return out
    }

    fun forWeek(
        weekStart: LocalDate,
        history: History,
        plans: Map<LocalDate, DayPlan>,
        periods: List<PeriodGoalProgress>,
        baseXpByDay: Map<LocalDate, Int>,
        threshold: Double,
        today: LocalDate,
    ): List<WeeklyQuest> {
        val weekEnd = weekStart.plusDays(6)
        val weekPlans = datesBetween(weekStart, weekEnd).mapNotNull { plans[it] ?: if (it <= today) DayPlanResolver.resolve(it, history) else null }.toList()
        val candidates = candidates(weekStart, history, weekPlans)
        if (candidates.isEmpty()) return emptyList()
        val rnd = Random(weekStart.toEpochDay())
        val picked = ArrayList<Candidate>()
        for (c in candidates.shuffled(rnd)) {
            if (picked.size >= QUESTS_PER_WEEK) break
            if (picked.any { it.goalId != null && it.goalId == c.goalId }) continue
            picked += c
        }
        return picked.map { c -> evaluate(c, weekStart, weekEnd, weekPlans, history, periods, baseXpByDay, threshold, today) }
    }

    /** XP granted on quest completion days, to feed the XP engine. */
    fun xpByDay(quests: List<WeeklyQuest>): Map<LocalDate, Int> {
        val out = HashMap<LocalDate, Int>()
        quests.forEach { q -> q.completedOn?.let { out[it] = (out[it] ?: 0) + q.xpReward } }
        return out
    }

    private fun candidates(weekStart: LocalDate, history: History, weekPlans: List<DayPlan>): List<Candidate> {
        val active = history.goals.filter { g ->
            g.kind == GoalKind.RECURRING && history.versions.any { it.goalId == g.id && it.covers(weekStart) }
        }
        val versionAt = { goalId: String -> history.versions.firstOrNull { it.goalId == goalId && it.covers(weekStart) } }
        val daily = active.mapNotNull { g -> versionAt(g.id)?.takeIf { !it.isPeriodic }?.let { g to it } }
        val periodic = active.mapNotNull { g -> versionAt(g.id)?.takeIf { it.isPeriodic }?.let { g to it } }
        if (daily.isEmpty() && periodic.isEmpty()) return emptyList()

        val out = ArrayList<Candidate>()
        if (daily.isNotEmpty()) {
            out += Candidate("perfect3", QuestKind.PERFECT_DAYS, "Hit 100% on 3 days this week", 3)
            out += Candidate("hit5", QuestKind.HIT_DAYS, "Reach your bar on 5 days this week", 5)
            out += Candidate("weekend", QuestKind.WEEKEND, "Hit your bar both weekend days", 2)
            out += Candidate("streak4", QuestKind.STREAK_RUN, "Build a 4-day run this week", 4)
            out += Candidate("xp250", QuestKind.XP, "Earn 250 XP this week", 250)
            // The daily goal that has been hardest lately gets a focused quest.
            val weakest = daily.minByOrNull { (g, _) -> recentRate(g.id, weekStart, history) ?: 1.0 }
            weakest?.let { (g, v) ->
                val scheduled = (0..6).count { v.weekdayMask and (1 shl it) != 0 }
                val n = minOf(5, scheduled).coerceAtLeast(2)
                out += Candidate("goal-${g.id}", QuestKind.GOAL_TIMES, "Complete ${v.title} $n times this week", n, g.id, v.emoji)
            }
            daily.firstOrNull { (_, v) -> v.type == GoalType.QUANTITY }?.let { (g, v) ->
                val n = v.target * 5
                out += Candidate("qty-${g.id}", QuestKind.QUANTITY_TOTAL, "Total $n${v.unit?.let { " $it" } ?: ""} of ${v.title} this week", n, g.id, v.emoji)
            }
        }
        if (periodic.isNotEmpty()) {
            out += Candidate("periodic2", QuestKind.LOG_PERIODIC, "Log a weekly or monthly goal 3 times", 3)
        }
        return out
    }

    private fun recentRate(goalId: String, weekStart: LocalDate, history: History): Double? {
        val from = weekStart.minusWeeks(4)
        val to = weekStart.minusDays(1)
        if (to < (history.firstDate ?: return null)) return null
        val plans = DayPlanResolver.resolveRange(from, to, history)
        return Scoring.goalRate(plans, goalId, from, to, to)
    }

    private fun evaluate(
        c: Candidate,
        weekStart: LocalDate,
        weekEnd: LocalDate,
        weekPlans: List<DayPlan>,
        history: History,
        periods: List<PeriodGoalProgress>,
        baseXpByDay: Map<LocalDate, Int>,
        threshold: Double,
        today: LocalDate,
    ): WeeklyQuest {
        val days = weekPlans.filter { it.date <= today }.sortedBy { it.date }
        fun hit(p: DayPlan) = (p.score ?: -1.0) + 1e-9 >= threshold
        // progress as of each day, in order, so the completion day is the first day the target is met
        val series: List<Pair<LocalDate, Int>> = when (c.kind) {
            QuestKind.PERFECT_DAYS -> cumulative(days) { if (it.isPerfect) 1 else 0 }
            QuestKind.HIT_DAYS -> cumulative(days) { if (hit(it)) 1 else 0 }
            QuestKind.WEEKEND -> cumulative(days) { if ((it.date.dayOfWeek == DayOfWeek.SATURDAY || it.date.dayOfWeek == DayOfWeek.SUNDAY) && hit(it)) 1 else 0 }
            QuestKind.GOAL_TIMES -> cumulative(days) { p -> if (p.goals.any { it.id == c.goalId && it.isDone }) 1 else 0 }
            QuestKind.QUANTITY_TOTAL -> cumulative(days) { p -> p.goals.firstOrNull { it.id == c.goalId }?.count ?: 0 }
            QuestKind.XP -> cumulative(days) { baseXpByDay[it.date] ?: 0 }
            QuestKind.STREAK_RUN -> {
                var run = 0; var best = 0
                days.map { p ->
                    when {
                        p.score == null -> Unit
                        hit(p) -> { run++; if (run > best) best = run }
                        else -> run = 0
                    }
                    p.date to best
                }
            }
            QuestKind.LOG_PERIODIC -> {
                val logged = periods.flatMap { p -> p.days.keys.filter { it >= weekStart && it <= weekEnd && it <= today }.map { d -> p.goal.id to d } }.toSet()
                cumulative(days) { p -> logged.count { it.second == p.date } }
            }
        }
        val progress = series.lastOrNull()?.second ?: 0
        val completedOn = series.firstOrNull { it.second >= c.target }?.first
        return WeeklyQuest(c.id, c.kind, weekStart, c.title, c.target, progress, XP_REWARD, completedOn, c.goalId, c.emoji)
    }

    private fun cumulative(days: List<DayPlan>, value: (DayPlan) -> Int): List<Pair<LocalDate, Int>> {
        var acc = 0
        return days.map { p -> acc += value(p); p.date to acc }
    }
}
