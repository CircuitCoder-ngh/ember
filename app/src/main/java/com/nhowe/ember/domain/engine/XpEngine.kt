package com.nhowe.ember.domain.engine

import com.nhowe.ember.domain.model.DayPlan
import com.nhowe.ember.domain.model.XpState
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

object XpEngine {

    const val XP_PER_GOAL = 10
    const val PERFECT_BONUS = 50

    private val TITLES = listOf(
        1 to "Spark", 2 to "Ember", 4 to "Flame", 7 to "Blaze",
        10 to "Inferno", 15 to "Wildfire", 20 to "Supernova", 30 to "Eternal Flame",
    )

    /** XP earned on a day. Streak multiplier: +10% per full week of streak, capped at +50%. */
    fun dayXp(plan: DayPlan, streakThatDay: Int): Int {
        if (plan.goals.isEmpty()) return 0
        val base = plan.goals.sumOf { (XP_PER_GOAL * it.credit * it.version.weight.coerceAtLeast(1)).roundToInt() }
        val bonus = if (plan.isPerfect) PERFECT_BONUS else 0
        val multiplier = 1.0 + 0.1 * min(streakThatDay / 7, 5)
        return ((base + bonus) * multiplier).roundToInt()
    }

    fun levelFor(xp: Int): Int = floor(sqrt(xp.coerceAtLeast(0) / 100.0)).toInt() + 1

    fun xpAtLevelStart(level: Int): Int = 100 * (level - 1) * (level - 1)

    fun titleFor(level: Int): String = TITLES.last { level >= it.first }.second

    fun compute(plans: Map<LocalDate, DayPlan>, streakByDay: Map<LocalDate, Int>, today: LocalDate, extraXpByDay: Map<LocalDate, Int> = emptyMap()): XpState {
        val xpByDay = LinkedHashMap<LocalDate, Int>()
        var total = 0
        for (plan in plans.values.sortedBy { it.date }) {
            if (plan.date > today) continue
            val xp = dayXp(plan, streakByDay[plan.date] ?: 0) + (extraXpByDay[plan.date] ?: 0)
            if (xp > 0) xpByDay[plan.date] = xp
            total += xp
        }
        val level = levelFor(total)
        val start = xpAtLevelStart(level)
        val next = xpAtLevelStart(level + 1)
        return XpState(
            totalXp = total,
            level = level,
            levelTitle = titleFor(level),
            xpIntoLevel = total - start,
            xpForNextLevel = next - start,
            todayXp = xpByDay[today] ?: 0,
            xpByDay = xpByDay,
        )
    }
}
