package com.nhowe.ember.domain.engine

import com.nhowe.ember.core.time.bit
import com.nhowe.ember.domain.model.GoalKind
import com.nhowe.ember.domain.model.GoalType
import java.time.DayOfWeek
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScoringTest {

    @Test
    fun `check goal gives full or no credit`() {
        val h = history { goal("a"); done("a", d(2)) }
        assertEquals(1.0, DayPlanResolver.resolve(d(2), h).score)
        assertEquals(0.0, DayPlanResolver.resolve(d(3), h).score)
    }

    @Test
    fun `quantity goal gives partial credit and clamps at target`() {
        val h = history {
            goal("read", GoalType.QUANTITY, target = 20)
            count("read", d(2), 12); count("read", d(3), 30)
        }
        assertEquals(0.6, DayPlanResolver.resolve(d(2), h).score!!, 1e-9)
        assertEquals(1.0, DayPlanResolver.resolve(d(3), h).score!!, 1e-9)
    }

    @Test
    fun `weights produce a weighted mean`() {
        val h = history { goal("a", weight = 2); goal("b", weight = 1); done("a", d(2)) }
        assertEquals(2.0 / 3.0, DayPlanResolver.resolve(d(2), h).score!!, 1e-9)
    }

    @Test
    fun `day with no goals is a rest day with null score`() {
        val h = history { goal("a", from = d(10)) }
        val plan = DayPlanResolver.resolve(d(2), h)
        assertNull(plan.score)
        assertTrue(plan.goals.isEmpty())
    }

    @Test
    fun `skipped goal is excluded from the score`() {
        val h = history { goal("a"); goal("b"); done("a", d(2)); skip("b", d(2)) }
        val plan = DayPlanResolver.resolve(d(2), h)
        assertEquals(1.0, plan.score)
        assertEquals(1, plan.skipped.size)
    }

    @Test
    fun `editing a goal keeps the old definition for old days`() {
        val h = history {
            goal("read", GoalType.QUANTITY, target = 20, to = d(10), title = "Read 20")
            goal("read", GoalType.QUANTITY, target = 40, from = d(11), title = "Read 40")
            count("read", d(5), 20); count("read", d(12), 20)
        }
        val before = DayPlanResolver.resolve(d(5), h)
        val after = DayPlanResolver.resolve(d(12), h)
        assertEquals("Read 20", before.goals.single().version.title)
        assertEquals(1.0, before.score)
        assertEquals("Read 40", after.goals.single().version.title)
        assertEquals(0.5, after.score)
    }

    @Test
    fun `one-off goal only appears on its date`() {
        val h = history { goal("dentist", kind = GoalKind.ONE_OFF, oneOffDate = d(9), from = d(9), to = d(9)) }
        assertEquals(1, DayPlanResolver.resolve(d(9), h).goals.size)
        assertEquals(0, DayPlanResolver.resolve(d(10), h).goals.size)
        assertEquals(0, DayPlanResolver.resolve(d(8), h).goals.size)
    }

    @Test
    fun `weekday schedule limits the days a goal applies`() {
        val h = history { goal("gym", weekdays = DayOfWeek.MONDAY.bit() or DayOfWeek.WEDNESDAY.bit()) }
        assertEquals(1, DayPlanResolver.resolve(d(7), h).goals.size)   // Monday
        assertEquals(0, DayPlanResolver.resolve(d(8), h).goals.size)   // Tuesday
        assertEquals(1, DayPlanResolver.resolve(d(9), h).goals.size)   // Wednesday
    }

    @Test
    fun `period score averages scored days only and ignores the future`() {
        val h = history {
            goal("a", from = d(1))
            done("a", d(1)); done("a", d(3))
            skip("a", d(2))
        }
        val plans = DayPlanResolver.resolveRange(d(1), d(5), h)
        // d1 = 1.0, d2 = rest (skipped), d3 = 1.0, d4 = 0.0; d5 is in the future relative to "today" d4
        assertEquals(2.0 / 3.0, Scoring.periodScore(plans, d(1), d(5), today = d(4))!!, 1e-9)
    }
}
