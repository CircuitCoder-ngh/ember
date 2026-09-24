package com.nhowe.ember.data.backup

import com.nhowe.ember.domain.model.Completion
import com.nhowe.ember.domain.model.DayOverride
import com.nhowe.ember.domain.model.Goal
import com.nhowe.ember.domain.model.GoalKind
import com.nhowe.ember.domain.model.GoalType
import com.nhowe.ember.domain.model.GoalVersion
import com.nhowe.ember.domain.model.History
import com.nhowe.ember.domain.model.OverrideKind
import com.nhowe.ember.domain.model.Settings
import com.nhowe.ember.domain.model.ThemeMode
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupFile(
    val format: Int = FORMAT,
    val app: String = "ember",
    val exportedAt: String,
    val goals: List<GoalJson>,
    val versions: List<VersionJson>,
    val completions: List<CompletionJson>,
    val overrides: List<OverrideJson>,
    val settings: SettingsJson,
) {
    companion object { const val FORMAT = 1 }
}

@Serializable
data class GoalJson(val id: String, val kind: String, val oneOffDate: String?, val createdAt: Long, val archivedAt: Long?, val sortOrder: Int)

@Serializable
data class VersionJson(
    val id: String, val goalId: String, val validFrom: String, val validTo: String?, val title: String, val emoji: String,
    val colorIndex: Int, val type: String, val targetCount: Int?, val unit: String?, val weekdayMask: Int, val weight: Int, val note: String?,
)

@Serializable
data class CompletionJson(val goalId: String, val date: String, val checked: Boolean, val count: Int, val updatedAt: Long)

@Serializable
data class OverrideJson(val goalId: String, val date: String, val kind: String)

@Serializable
data class SettingsJson(
    val streakThreshold: Double, val dayBoundaryHour: Int, val reminderEnabled: Boolean, val reminderTime: String,
    val hapticsEnabled: Boolean, val soundEnabled: Boolean, val themeMode: String, val dynamicColor: Boolean,
    val completedSinkToBottom: Boolean, val userName: String,
)

object BackupCodec {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(history: History, settings: Settings, exportedAt: String): String = json.encodeToString(
        BackupFile.serializer(),
        BackupFile(
            exportedAt = exportedAt,
            goals = history.goals.map { GoalJson(it.id, it.kind.name, it.oneOffDate?.toString(), it.createdAt, it.archivedAt, it.sortOrder) },
            versions = history.versions.map {
                VersionJson(it.id, it.goalId, it.validFrom.toString(), it.validTo?.toString(), it.title, it.emoji, it.colorIndex,
                    it.type.name, it.targetCount, it.unit, it.weekdayMask, it.weight, it.note)
            },
            completions = history.completions.map { CompletionJson(it.goalId, it.date.toString(), it.checked, it.count, it.updatedAt) },
            overrides = history.overrides.map { OverrideJson(it.goalId, it.date.toString(), it.kind.name) },
            settings = SettingsJson(
                settings.streakThreshold, settings.dayBoundaryHour, settings.reminderEnabled, settings.reminderTime.toString(),
                settings.hapticsEnabled, settings.soundEnabled, settings.themeMode.name, settings.dynamicColor,
                settings.completedSinkToBottom, settings.userName,
            ),
        ),
    )

    fun decode(text: String): Pair<History, Settings> {
        val file = json.decodeFromString(BackupFile.serializer(), text)
        require(file.app == "ember") { "Not an Ember backup" }
        val history = History(
            goals = file.goals.map { Goal(it.id, GoalKind.valueOf(it.kind), it.oneOffDate?.let(LocalDate::parse), it.createdAt, it.archivedAt, it.sortOrder) },
            versions = file.versions.map {
                GoalVersion(it.id, it.goalId, LocalDate.parse(it.validFrom), it.validTo?.let(LocalDate::parse), it.title, it.emoji, it.colorIndex,
                    GoalType.valueOf(it.type), it.targetCount, it.unit, it.weekdayMask, it.weight, it.note)
            },
            completions = file.completions.map { Completion(it.goalId, LocalDate.parse(it.date), it.checked, it.count, it.updatedAt) },
            overrides = file.overrides.map { DayOverride(it.goalId, LocalDate.parse(it.date), OverrideKind.valueOf(it.kind)) },
        )
        val s = file.settings
        val settings = Settings(
            streakThreshold = s.streakThreshold, dayBoundaryHour = s.dayBoundaryHour, reminderEnabled = s.reminderEnabled,
            reminderTime = LocalTime.parse(s.reminderTime), hapticsEnabled = s.hapticsEnabled, soundEnabled = s.soundEnabled,
            themeMode = runCatching { ThemeMode.valueOf(s.themeMode) }.getOrDefault(ThemeMode.DARK), dynamicColor = s.dynamicColor,
            onboardingDone = true, completedSinkToBottom = s.completedSinkToBottom, userName = s.userName,
        )
        return history to settings
    }
}
