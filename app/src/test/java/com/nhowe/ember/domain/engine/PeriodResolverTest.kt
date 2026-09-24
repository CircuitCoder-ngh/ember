package com.nhowe.ember.domain.engine

import com.nhowe.ember.domain.model.Cadence
import com.nhowe.ember.domain.model.GoalType
import com.nhowe.ember.domain.model.Settings
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PeriodResolverTest {

    @Test
    fun `weekly check goal counts logged days toward its target`() {
        // Sep 1 2026 is a Tuesday; its ISO week runs Aug 31 - Sep 6.
        val h = history {
            goal("gym", target = 3, cadence = Cadence.WEEKLY)
            done("gym", d(2)); done("gym", d(4))
        }
        val p = PeriodResolver.progressFor("gym", d(3), h)!!
        assertEquals(d(1).minusDays(1), p.start)
        assertEquals(d(6), p.end)
        assertEquals(2, p.progress)
        assertEquals(2.0 / 3.0, p.credit, 1e-9)
        assertFalse(p.isDone)
        assertNull(p.doneOn)
        assertEquals(1, p.contributionOn(d(2)))
        assertEquals(0, p.contributionOn(d(3)))
    }

    @Test
    fun `weekly goal completes on the day the target is reached and resets next week`() {
        val h = history {
            goal("gym", target = 3, cadence = Cadence.WEEKLY)
            done("gym", d(2)); done("gym", d(4)); done("gym", d(5)); done("gym", d(8))
        }
        val week1 = PeriodResolver.progressFor("gym", d(6), h)!!
        assertTrue(week1.isDone)
        assertEquals(d(5), week1.doneOn)
        val week2 = PeriodResolver.progressFor("gym", d(8), h)!!
        assertEquals(1, week2.progress)
        assertEquals(d(7), week2.start)
    }

    @Test
    fun `monthly quantity goal sums counts and caps credit`() {
        val h = history {
            goal("pages", GoalType.QUANTITY, target = 100, cadence = Cadence.MONTHLY)
            count("pages", d(3), 40); count("pages", d(10), 70)
        }
        val p = PeriodResolver.progressFor("pages", d(20), h)!!
        assertEquals(d(1), p.start)
        assertEquals(d(30), p.end)
        assertEquals(110, p.progress)
        assertEquals(1.0, p.credit)
        assertEquals(d(10), p.doneOn)
    }

    @Test
    fun `periodic goals are excluded from the day plan score but listed`() {
        val h = history { goal("a"); goal("gym", target = 3, cadence = Cadence.WEEKLY); done("a", d(2)); done("gym", d(2)) }
        val plan = DayPlanResolver.resolve(d(2), h)
        assertEquals(1, plan.goals.size)
        assertEquals(1, plan.periodic.size)
        assertEquals(1.0, plan.score)
    }

    @Test
    fun `period stats include a weekly goal only when its whole week is in range`() {
        val h = history {
            goal("a"); goal("gym", target = 2, cadence = Cadence.WEEKLY)
            (7..13).forEach { done("a", d(it)) }   // a perfect Mon-Sun week
            done("gym", d(8))                        // gym at 1 of 2
        }
        val snap = Engine.compute(h, Settings(), today = d(13))
        // full week: 7 perfect days (7 items at 1.0) + gym (1 item at 0.5) = 7.5 / 8
        assertEquals(7.5 / 8.0, Scoring.periodScore(snap.plans, d(7), d(13), d(13), snap.periods)!!, 1e-9)
        // partial range: gym's week is not fully inside, so only the days count
        assertEquals(1.0, Scoring.periodScore(snap.plans, d(7), d(10), d(13), snap.periods)!!, 1e-9)
    }

    @Test
    fun `periodic goals earn xp per unit and a bonus on completion`() {
        val h = history {
            goal("gym", target = 2, cadence = Cadence.WEEKLY, weight = 2)
            done("gym", d(2)); done("gym", d(4)); done("gym", d(5))   // third log is over target: no xp
        }
        val xp = PeriodResolver.xpByDay(PeriodResolver.resolveRange(d(1), d(6), h))
        assertEquals(20, xp[d(2)])
        assertEquals(20 + 50, xp[d(4)])
        assertNull(xp[d(5)])
        val snap = Engine.compute(h, Settings(), today = d(6))
        assertEquals(90, snap.xp.totalXp)
    }
}
