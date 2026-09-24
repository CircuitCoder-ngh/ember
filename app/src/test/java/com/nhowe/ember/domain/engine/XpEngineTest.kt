package com.nhowe.ember.domain.engine

import org.junit.Test
import kotlin.test.assertEquals

class XpEngineTest {

    @Test
    fun `level boundaries`() {
        assertEquals(1, XpEngine.levelFor(0))
        assertEquals(1, XpEngine.levelFor(99))
        assertEquals(2, XpEngine.levelFor(100))
        assertEquals(2, XpEngine.levelFor(399))
        assertEquals(3, XpEngine.levelFor(400))
        assertEquals(10, XpEngine.levelFor(8100))
    }

    @Test
    fun `day xp with partial completion has no bonus`() {
        val h = history { goal("a"); goal("b"); goal("c"); done("a", d(2)); done("b", d(2)) }
        val plan = DayPlanResolver.resolve(d(2), h)
        assertEquals(20, XpEngine.dayXp(plan, streakThatDay = 0))
    }

    @Test
    fun `perfect day gets bonus and streak multiplier`() {
        val h = history { goal("a"); goal("b"); done("a", d(2)); done("b", d(2)) }
        val plan = DayPlanResolver.resolve(d(2), h)
        assertEquals(70, XpEngine.dayXp(plan, streakThatDay = 0))
        assertEquals(77, XpEngine.dayXp(plan, streakThatDay = 7))
        assertEquals(105, XpEngine.dayXp(plan, streakThatDay = 70)) // capped at 1.5x
    }

    @Test
    fun `compute sums xp and tracks level progress`() {
        val h = history { goal("a"); done("a", d(1)); done("a", d(2)) }
        val plans = DayPlanResolver.resolveRange(d(1), d(2), h)
        val streak = StreakEngine.compute(plans.mapValues { it.value.score }.toSortedMap(), d(2), 0.8)
        val xp = XpEngine.compute(plans, streak.streakByDay, d(2))
        assertEquals(120, xp.totalXp)
        assertEquals(2, xp.level)
        assertEquals(20, xp.xpIntoLevel)
        assertEquals(300, xp.xpForNextLevel)
        assertEquals(60, xp.todayXp)
    }
}
