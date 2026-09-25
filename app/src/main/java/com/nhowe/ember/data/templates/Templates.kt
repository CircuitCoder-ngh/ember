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
    val programs: List<ProgramTemplate> get() = programLibrary.programs
    fun programById(id: String): ProgramTemplate? = programs.firstOrNull { it.id == id }

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
