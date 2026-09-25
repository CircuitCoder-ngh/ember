package com.nhowe.ember.domain.engine

import com.nhowe.ember.data.templates.GraduationSpec
import com.nhowe.ember.data.templates.PhaseSpec
import com.nhowe.ember.data.templates.ProgramGoalSpec
import com.nhowe.ember.data.templates.ProgramTemplate
import com.nhowe.ember.data.templates.SeasonWindow
import com.nhowe.ember.data.templates.TemplateRepository
import com.nhowe.ember.domain.model.PausePeriod
import com.nhowe.ember.domain.model.Program
import com.nhowe.ember.domain.model.Settings
import java.time.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PauseAndSeasonTest {

    private fun program(pauses: List<PausePeriod>) = Program(
        id = "p1", templateId = "t", title = "Plan", emoji = "🏃", startDate = d(7), lengthDays = 10, goalIds = listOf("a"),
        strict = false, graduationTitle = "Done", graduationIcon = "🏅", graduationXp = 100, nextTemplateId = null, abandonedOn = null, pauses = pauses,
    )

    @Test
    fun `paused days drop the goal from the plan and push the end date back`() {
        val base = history { goal("a", from = d(7), to = d(16)); (7..9).forEach { done("a", d(it)) } }
        val paused = base.copy(programs = listOf(program(listOf(PausePeriod(d(10), d(12))))))
        val snap = Engine.compute(paused, Settings(), today = d(14))
        // days 10-12 are rest days for the program goal
        assertNull(snap.plans[d(11)]!!.score)
        assertEquals(0, snap.plans[d(11)]!!.goals.size)
        val p = snap.programs.single()
        assertEquals(3, p.program.pausedDays(d(14)))
        assertEquals(d(19), p.endDate)          // 7 + 10 - 1 + 3
        assertEquals(5, p.dayIndex)             // 8 elapsed calendar days minus 3 paused
        assertNull(p.graduatedOn)
    }

    @Test
    fun `an open pause counts up to yesterday and blocks graduation`() {
        val base = history { goal("a", from = d(7), to = d(16)) }
        val h = base.copy(programs = listOf(program(listOf(PausePeriod(d(10), null)))))
        val p = Engine.compute(h, Settings(), today = d(20)).programs.single()
        assertTrue(p.isPaused)
        assertEquals(11, p.program.pausedDays(d(20)))
        assertNull(p.graduatedOn)
        assertEquals(3, p.dayIndex)
    }

    @Test
    fun `season windows handle the year wrap`() {
        val w = SeasonWindow("12-20", "01-31")
        assertTrue(w.isOpen(LocalDate.of(2026, 12, 25)))
        assertTrue(w.isOpen(LocalDate.of(2027, 1, 10)))
        assertFalse(w.isOpen(LocalDate.of(2026, 6, 1)))
        assertEquals(LocalDate.of(2026, 12, 20), w.nextOpening(LocalDate.of(2026, 9, 24)))
        assertEquals("2026", w.seasonKey(LocalDate.of(2027, 1, 10)))
        val plain = SeasonWindow("10-18", "11-07")
        assertTrue(plain.isOpen(LocalDate.of(2026, 11, 1)))
        assertFalse(plain.isOpen(LocalDate.of(2026, 11, 8)))
    }

    @Test
    fun `import validation rejects broken programs`() {
        fun tpl(vararg goals: ProgramGoalSpec, length: Int = 14, id: String = "ok-id") =
            ProgramTemplate(id = id, category = "x", emoji = "x", title = "T", blurb = "", lengthDays = length, goals = goals.toList(), graduation = GraduationSpec("G"))
        TemplateRepository.validate(tpl(ProgramGoalSpec(key = "a", title = "A", phases = listOf(PhaseSpec(1), PhaseSpec(8)))))
        assertFailsWith<IllegalArgumentException> { TemplateRepository.validate(tpl(ProgramGoalSpec(key = "a", title = "A", phases = listOf(PhaseSpec(1), PhaseSpec(20))))) }
        assertFailsWith<IllegalArgumentException> { TemplateRepository.validate(tpl(ProgramGoalSpec(key = "a", title = "A", cadence = "WEEKLY", target = 2, phases = listOf(PhaseSpec(1), PhaseSpec(5))))) }
        assertFailsWith<IllegalArgumentException> { TemplateRepository.validate(tpl(length = 0)) }
        assertFailsWith<IllegalArgumentException> { TemplateRepository.validate(tpl(ProgramGoalSpec(key = "a", title = "A"), id = "Bad Id!")) }
    }
}
