package com.nhowe.ember.domain.engine

import com.nhowe.ember.data.templates.Stacking
import com.nhowe.ember.data.templates.TemplateLibrary
import com.nhowe.ember.domain.model.Cadence
import com.nhowe.ember.domain.model.GoalType
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TemplateLibraryTest {
    private val library: TemplateLibrary by lazy {
        val file = listOf("src/main/assets/templates.json", "app/src/main/assets/templates.json").map(::File).first { it.exists() }
        Json { ignoreUnknownKeys = true }.decodeFromString(TemplateLibrary.serializer(), file.readText())
    }

    @Test
    fun `library parses and every template is well formed`() {
        assertTrue(library.templates.size >= 15)
        val categoryIds = library.categories.map { it.id }.toSet()
        library.templates.forEach { t ->
            assertTrue(t.category in categoryIds, "${t.id} has unknown category ${t.category}")
            assertTrue(t.goals.isNotEmpty(), "${t.id} has no goals")
            assertTrue(t.dailyCount <= Stacking.COMFORTABLE_DAILY, "${t.id} alone has too many daily goals")
            t.goals.forEach { g ->
                val draft = g.toDraft()
                assertTrue(draft.title.isNotBlank())
                if (draft.type == GoalType.QUANTITY || draft.cadence != Cadence.DAILY) assertTrue((draft.targetCount ?: 0) > 0, "${t.id}/${g.title} needs a target")
                assertTrue(g.weekdays in 1..127, "${t.id}/${g.title} bad weekday mask")
            }
        }
        assertEquals(library.templates.size, library.templates.map { it.id }.toSet().size, "duplicate template ids")
    }

    @Test
    fun `no trademarked program names ship in the library`() {
        val banned = listOf("75 hard", "couch to 5k", "stronglifts", "miracle morning", "artist's way", "nanowrimo", "100daysofcode")
        val text = library.templates.joinToString(" ") { "${it.title} ${it.blurb} " + it.goals.joinToString(" ") { g -> g.title + " " + (g.note ?: "") } }.lowercase()
        banned.forEach { assertTrue(it !in text, "found '$it'") }
    }

    @Test
    fun `stacking guard thresholds`() {
        assertNull(Stacking.warning(4, 1))
        assertNotNull(Stacking.warning(6, 1))
        assertNotNull(Stacking.warning(4, 3))
        assertTrue(Stacking.warning(9, 1)!!.contains("Start smaller"))
    }
}
