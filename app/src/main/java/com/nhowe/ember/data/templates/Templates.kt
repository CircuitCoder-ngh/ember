package com.nhowe.ember.data.templates

import android.content.Context
import com.nhowe.ember.core.time.ALL_WEEKDAYS
import com.nhowe.ember.data.repo.GoalDraft
import com.nhowe.ember.domain.model.Cadence
import com.nhowe.ember.domain.model.GoalType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TemplateCategory(val id: String, val title: String, val emoji: String, val blurb: String)

@Serializable
data class TemplateGoal(
    val title: String,
    val emoji: String = "✅",
    val colorIndex: Int = 0,
    val type: String = "CHECK",
    val cadence: String = "DAILY",
    val target: Int? = null,
    val unit: String? = null,
    val weekdays: Int = ALL_WEEKDAYS,
    val weight: Int = 1,
    val note: String? = null,
) {
    fun toDraft() = GoalDraft(
        title = title, emoji = emoji, colorIndex = colorIndex,
        type = runCatching { GoalType.valueOf(type) }.getOrDefault(GoalType.CHECK),
        targetCount = target, unit = unit, weekdayMask = weekdays, weight = weight, note = note,
        cadence = runCatching { Cadence.valueOf(cadence) }.getOrDefault(Cadence.DAILY),
    )

    val isDaily: Boolean get() = cadence == "DAILY"
}

@Serializable
data class GoalTemplate(
    val id: String,
    val category: String,
    val emoji: String,
    val title: String,
    val blurb: String,
    val goals: List<TemplateGoal>,
    val season: SeasonWindow? = null,
) {
    val dailyCount: Int get() = goals.count { it.isDaily }
}

@Serializable
data class TemplateLibrary(val version: Int = 1, val categories: List<TemplateCategory>, val templates: List<GoalTemplate>)

/** Goal bundles shipped as a data file so the library can grow without touching code. */
class TemplateRepository(context: Context) {
    val library: TemplateLibrary by lazy {
        val text = context.assets.open("templates.json").bufferedReader().use { it.readText() }
        Json { ignoreUnknownKeys = true }.decodeFromString(TemplateLibrary.serializer(), text)
    }

    val programLibrary: ProgramLibrary by lazy {
        val text = context.assets.open("programs.json").bufferedReader().use { it.readText() }
        Json { ignoreUnknownKeys = true }.decodeFromString(ProgramLibrary.serializer(), text)
    }
    private val customFile = java.io.File(context.filesDir, "custom_programs.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
    private val _custom = kotlinx.coroutines.flow.MutableStateFlow(loadCustom())
    /** Programs the user imported from files. */
    val customPrograms: kotlinx.coroutines.flow.StateFlow<List<ProgramTemplate>> get() = _custom

    val programs: List<ProgramTemplate> get() = programLibrary.programs + _custom.value
    fun programById(id: String): ProgramTemplate? = programs.firstOrNull { it.id == id }

    private fun loadCustom(): List<ProgramTemplate> = runCatching {
        if (!customFile.exists()) emptyList()
        else json.decodeFromString(ProgramLibrary.serializer(), customFile.readText()).programs.map { it.copy(custom = true) }
    }.getOrDefault(emptyList())

    /** Parses a shared file (a library or a single program), validates it, and stores it. Returns the stored programs. */
    fun importPrograms(text: String): Result<List<ProgramTemplate>> = runCatching {
        val parsed: List<ProgramTemplate> = runCatching { json.decodeFromString(ProgramLibrary.serializer(), text).programs }
            .getOrElse { listOf(json.decodeFromString(ProgramTemplate.serializer(), text)) }
        require(parsed.isNotEmpty()) { "No programs in this file" }
        val builtIn = programLibrary.programs.map { it.id }.toSet()
        val stored = parsed.map { p ->
            validate(p)
            val id = if (p.id in builtIn) "${p.id}-custom" else p.id
            p.copy(id = id, custom = true)
        }
        val merged = (_custom.value.filter { c -> stored.none { it.id == c.id } } + stored)
        customFile.writeText(json.encodeToString(ProgramLibrary.serializer(), ProgramLibrary(programs = merged.map { it.copy(custom = false) })))
        _custom.value = merged
        stored
    }

    fun removeCustom(id: String) {
        val merged = _custom.value.filter { it.id != id }
        customFile.writeText(json.encodeToString(ProgramLibrary.serializer(), ProgramLibrary(programs = merged.map { it.copy(custom = false) })))
        _custom.value = merged
    }

    /** A program as a shareable file. */
    fun exportProgram(p: ProgramTemplate): String =
        json.encodeToString(ProgramLibrary.serializer(), ProgramLibrary(programs = listOf(p.copy(custom = false))))

    companion object {
        fun validate(p: ProgramTemplate) {
            require(p.id.matches(Regex("[a-z0-9-]{1,40}"))) { "Program id must be lowercase letters, digits and dashes" }
            require(p.title.isNotBlank() && p.title.length <= 60) { "Title missing or too long" }
            require(p.lengthDays in 1..730) { "Length must be 1-730 days" }
            require(p.goals.isNotEmpty() && p.goals.size <= 8) { "A program needs 1-8 goals" }
            p.goals.forEach { g ->
                require(g.title.isNotBlank() && g.title.length <= 60) { "Goal title missing or too long" }
                require(g.type in setOf("CHECK", "QUANTITY")) { "Goal type must be CHECK or QUANTITY" }
                require(g.cadence in setOf("DAILY", "WEEKLY", "MONTHLY")) { "Goal cadence must be DAILY, WEEKLY or MONTHLY" }
                require(g.weekdays in 1..127) { "Bad weekday mask on ${g.title}" }
                require(g.phases.isNotEmpty()) { "${g.title} has no phases" }
                val days = g.phases.map { it.fromDay }
                require(days == days.sorted() && days.first() >= 1 && days.last() <= p.lengthDays) { "${g.title} phases out of order or outside the program" }
                if (g.cadence == "WEEKLY") require(days.all { (it - 1) % 7 == 0 }) { "${g.title}: weekly phases must start on day 1, 8, 15…" }
            }
            require(p.graduation.xp in 0..5000) { "Graduation XP must be 0-5000" }
        }
    }

    val categories: List<TemplateCategory> get() = library.categories
    val templates: List<GoalTemplate> get() = library.templates
    fun byId(id: String): GoalTemplate? = templates.firstOrNull { it.id == id }
    fun forCategories(ids: Collection<String>): List<GoalTemplate> =
        if (ids.isEmpty()) templates else templates.filter { it.category in ids }
}

/** The "don't start ten things at once" guard. */
object Stacking {
    const val COMFORTABLE_DAILY = 5
    const val TOO_MANY_DAILY = 8

    fun warning(dailyGoals: Int, bundles: Int = 0): String? = when {
        dailyGoals > TOO_MANY_DAILY -> "That's $dailyGoals daily goals. Almost nobody keeps more than $TOO_MANY_DAILY going; days will feel like failing. Start smaller and add later."
        bundles >= 3 -> "Three bundles at once is a lot to change in one week. One bundle plus a couple of habits sticks better."
        dailyGoals > COMFORTABLE_DAILY -> "$dailyGoals daily goals is doable but ambitious. Most people settle around $COMFORTABLE_DAILY."
        else -> null
    }
}
