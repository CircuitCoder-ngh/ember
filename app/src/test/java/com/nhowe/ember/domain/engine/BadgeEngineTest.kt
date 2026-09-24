package com.nhowe.ember.domain.engine

import com.nhowe.ember.domain.model.Badge
import com.nhowe.ember.domain.model.Settings
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BadgeEngineTest {

    @Test
    fun `perfect week and first perfect`() {
        val h = history { goal("a"); (1..7).forEach { done("a", d(it)) } }
        val snap = Engine.compute(h, Settings(), today = d(7))
        assertEquals(d(1), snap.badges[Badge.FIRST_PERFECT])
        assertEquals(d(7), snap.badges[Badge.PERFECT_WEEK])
        assertEquals(d(7), snap.badges[Badge.STREAK_7])
        assertEquals(d(3), snap.badges[Badge.STREAK_3])
    }

    @Test
    fun `comeback after a miss`() {
        val h = history {
            goal("a")
            (1..3).forEach { done("a", d(it)) }
            // d4 missed
            (5..11).forEach { done("a", d(it)) }
        }
        val snap = Engine.compute(h, Settings(), today = d(11))
        assertEquals(d(11), snap.badges[Badge.COMEBACK])
        assertFalse(Badge.FREEZE_SAVED in snap.badges)
    }

    @Test
    fun `pending celebrations fire once`() {
        val h = history { goal("a"); done("a", d(2)) }
        val first = Engine.compute(h, Settings(), today = d(2))
        assertTrue(first.pendingCelebrations.any { it.key == "perfect:2026-09-02" })
        val shown = first.pendingCelebrations.map { it.key }.toSet()
        val second = Engine.compute(h, Settings(), today = d(2), shownCelebrationKeys = shown)
        assertTrue(second.pendingCelebrations.isEmpty())
    }
}
