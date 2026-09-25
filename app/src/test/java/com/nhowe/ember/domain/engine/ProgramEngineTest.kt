package com.nhowe.ember.domain.engine

import com.nhowe.ember.data.repo.ProgramRepository
import com.nhowe.ember.data.templates.PhaseSpec
import com.nhowe.ember.data.templates.ProgramGoalSpec
import com.nhowe.ember.data.templates.ProgramLibrary
import com.nhowe.ember.domain.model.Cadence
import com.nhowe.ember.domain.model.History
import com.nhowe.ember.domain.model.Program
import com.nhowe.ember.domain.model.Settings
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProgramEngineTest {

    private fun program(start: java.time.LocalDate, length: Int, goalIds: List<String>, strict: Boolean = false) = Program(
        id = "p1", templateId = "t", title = "Test plan", emoji = "🏃", startDate = start, lengthDays = length, goalIds = goalIds,
        strict = strict, graduationTitle = "Finisher", graduationIcon = "🏅", graduationXp = 500, nextTemplateId = null, abandonedOn = null,
    )

    @Test
    fun `phase chain builds contiguous pre-dated versions`() {
        val spec = ProgramGoalSpec(
            key = "k", title = "Plank", emoji = "🧘", type = "CHECK",
            phases = listOf(PhaseSpec(1, title = "Plank 20s"), PhaseSpec(4, title = "Plank 30s"), PhaseSpec(7, title = "Plank 40s")),
        )
        val v = ProgramRepository.buildVersions("g", spec, d(7), lengthDays = 9)
        assertEquals(3, v.size)
        assertEquals(d(7) to d(9), v[0].validFrom to v[0].validTo)
        assertEquals(d(10) to d(12), v[1].validFrom to v[1].validTo)
        assertEquals(d(13) to d(15), v[2].validFrom to v[2].validTo)
        assertEquals("Plank 30s", v[1].title)
    }

    @Test
    fun `phases past the program end are dropped and the last phase is clipped`() {
        val spec = ProgramGoalSpec(key = "k", title = "X", phases = listOf(PhaseSpec(1), PhaseSpec(8), PhaseSpec(15)))
        val v = ProgramRepository.buildVersions("g", spec, d(1), lengthDays = 10)
        assertEquals(2, v.size)
        assertEquals(d(10), v[1].validTo)
    }

    @Test
    fun `week label, adherence and graduation`() {
        val h0 = history { goal("run", from = d(7), to = d(20)); (7..12).forEach { done("run", d(it)) } }
        val h = h0.copy(programs = listOf(program(d(7), 14, listOf("run"))))
        val mid = Engine.compute(h, Settings(), today = d(16)).programs.single()
        assertEquals(10, mid.dayIndex)
        assertEquals(2, mid.week)
        assertEquals(6.0 / 10.0, mid.adherence!!, 1e-9)
        assertNull(mid.graduatedOn)
        assertTrue(mid.isActive)

        val after = Engine.compute(h, Settings(), today = d(21))
        val done = after.programs.single()
        assertEquals(d(20), done.graduatedOn)
        assertEquals(1, after.programBadges.size)
        assertEquals("Finisher", after.programBadges.single().title)
        assertTrue(after.pendingCelebrations.any { it.key == "program:p1" })
        assertTrue(after.xp.xpByDay[d(21)]!! >= 500)
    }

    @Test
    fun `strict count resets on a missed day but today pending does not break it`() {
        val h0 = history { goal("a", from = d(7), to = d(30)); goal("b", from = d(7), to = d(30))
            (7..9).forEach { done("a", d(it)); done("b", d(it)) }
            done("a", d(10))             // b missed on day 4
            (11..13).forEach { done("a", d(it)); done("b", d(it)) }
        }
        val h = h0.copy(programs = listOf(program(d(7), 24, listOf("a", "b"), strict = true)))
        val p = Engine.compute(h, Settings(), today = d(14)).programs.single()   // today nothing logged yet
        assertEquals(3, p.strictDay)
        assertEquals(d(10), p.strictBrokenOn)
    }

    @Test
    fun `program library parses and phases are ordered within length`() {
        val file = listOf("src/main/assets/programs.json", "app/src/main/assets/programs.json").map(::File).first { it.exists() }
        val lib = Json { ignoreUnknownKeys = true }.decodeFromString(ProgramLibrary.serializer(), file.readText())
        assertTrue(lib.programs.size >= 10)
        lib.programs.forEach { p ->
            p.goals.forEach { g ->
                val days = g.phases.map { it.fromDay }
                assertEquals(days.sorted(), days, "${p.id}/${g.key} phases out of order")
                assertTrue(days.first() >= 1 && days.last() <= p.lengthDays, "${p.id}/${g.key} phase outside program")
                if (g.cadence == "WEEKLY") assertTrue(days.all { (it - 1) % 7 == 0 }, "${p.id}/${g.key} weekly phases must start on week boundaries")
                val versions = ProgramRepository.buildVersions("g", g, d(7), p.lengthDays)
                assertEquals(g.phases.size, versions.size, "${p.id}/${g.key}")
                versions.zipWithNext { a, b -> assertEquals(a.validTo!!.plusDays(1), b.validFrom, "${p.id}/${g.key} gap between phases") }
            }
            assertNotNull(p.graduation.title)
        }
    }
}
