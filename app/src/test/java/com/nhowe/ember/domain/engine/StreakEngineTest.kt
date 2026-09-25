package com.nhowe.ember.domain.engine

import com.nhowe.ember.domain.model.DayState
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreakEngineTest {

    private fun run(vararg values: Double?, threshold: Double = 0.8): StreakEngine.Result {
        val (map, today) = scores(START, *values)
        return StreakEngine.compute(map, today, threshold)
    }

    @Test
    fun `threshold boundary`() {
        assertEquals(0, run(0.79).state.current)
        assertEquals(1, run(0.80).state.current)
        assertEquals(1, run(0.8, threshold = 0.8).state.current)
    }

    @Test
    fun `rest day is neutral`() {
        assertEquals(2, run(1.0, null, 1.0).state.current)
    }

    @Test
    fun `today is pending, not a miss`() {
        val r = run(1.0, 1.0, 0.0)
        assertEquals(2, r.state.current)
        assertFalse(r.state.todaySecured)
        assertEquals(DayState.PENDING, r.dayStates[START.plusDays(2)])
    }

    @Test
    fun `today secured counts and reaches milestone`() {
        val r = run(1.0, 1.0, 1.0)
        assertEquals(3, r.state.current)
        assertTrue(r.state.todaySecured)
        assertEquals(listOf(3), r.state.milestones.map { it.days })
        assertEquals(0, r.state.freezesHeld) // no freeze below 7
    }

    @Test
    fun `freeze granted at 7 and consumed on a miss`() {
        val seven = Array<Double?>(7) { 1.0 }
        val r = run(*seven, 0.0, 1.0)
        assertEquals(8, r.state.current)
        assertEquals(0, r.state.freezesHeld)
        assertEquals(DayState.FROZEN, r.dayStates[START.plusDays(7)])
        assertEquals(setOf(START.plusDays(7)), r.state.frozenDays)
    }

    @Test
    fun `miss without freeze resets`() {
        val r = run(1.0, 1.0, 1.0, 0.0, 1.0)
        assertEquals(1, r.state.current)
        assertEquals(3, r.state.best)
        assertEquals(DayState.MISS, r.dayStates[START.plusDays(3)])
    }

    @Test
    fun `freezes are capped at two`() {
        val thirty = Array<Double?>(30) { 1.0 }
        val r = run(*thirty)
        assertEquals(listOf(3, 7, 14, 30), r.state.milestones.map { it.days })
        assertEquals(StreakEngine.MAX_FREEZES, r.state.freezesHeld)
    }

    @Test
    fun `best streak is remembered`() {
        val r = run(1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0)
        assertEquals(2, r.state.current)
        assertEquals(5, r.state.best)
        assertEquals(START.plusDays(4), r.state.bestEndedOn)
    }

    @Test
    fun `perfect days are marked distinctly`() {
        val r = run(1.0, 0.85)
        assertEquals(DayState.PERFECT, r.dayStates[START])
        assertEquals(DayState.HIT, r.dayStates[START.plusDays(1)])
    }

    @Test
    fun `streak by day records the running value`() {
        val r = run(1.0, 1.0, 0.0, 1.0)
        assertEquals(listOf(1, 2, 0, 1), r.streakByDay.values.toList())
    }
}

class ComebackQuestTest {
    private fun run(vararg values: Double?): StreakEngine.Result {
        val (map, today) = scores(START, *values)
        return StreakEngine.compute(map, today, 0.8)
    }

    @Test
    fun `breaking a 3-day streak opens a quest that earns a freeze after 3 hits`() {
        val r = run(1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0)
        assertEquals(1, r.state.freezesHeld)
        assertEquals(null, r.state.quest)
        val q = r.state.completedQuests.single()
        assertEquals(3, q.brokenStreak)
        assertEquals(START.plusDays(6), q.completedOn)
        assertTrue(q.freezeGranted)
    }

    @Test
    fun `quest progress resets on a miss but the quest stays open`() {
        val r = run(1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0)
        val q = r.state.quest!!
        assertEquals(2, q.progress)
        assertEquals(0, r.state.freezesHeld)
    }

    @Test
    fun `a short streak breaking does not start a quest`() {
        val r = run(1.0, 1.0, 0.0, 1.0)
        assertEquals(null, r.state.quest)
    }

    @Test
    fun `today pending does not count toward the quest yet`() {
        val r = run(1.0, 1.0, 1.0, 0.0, 1.0, 0.5)
        assertEquals(1, r.state.quest!!.progress)
        assertFalse(r.state.todaySecured)
    }

    @Test
    fun `rest days are neutral during a quest`() {
        val r = run(1.0, 1.0, 1.0, 0.0, 1.0, null, 1.0, 1.0)
        assertEquals(1, r.state.freezesHeld)
        assertTrue(r.state.completedQuests.single().freezeGranted)
    }
}
