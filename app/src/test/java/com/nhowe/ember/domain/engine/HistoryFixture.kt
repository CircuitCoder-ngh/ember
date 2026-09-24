package com.nhowe.ember.domain.engine

import com.nhowe.ember.core.time.ALL_WEEKDAYS
import com.nhowe.ember.domain.model.Completion
import com.nhowe.ember.domain.model.DayOverride
import com.nhowe.ember.domain.model.Goal
import com.nhowe.ember.domain.model.GoalKind
import com.nhowe.ember.domain.model.GoalType
import com.nhowe.ember.domain.model.GoalVersion
import com.nhowe.ember.domain.model.History
import com.nhowe.ember.domain.model.OverrideKind
import java.time.LocalDate
import java.util.SortedMap

val START: LocalDate = LocalDate.of(2026, 9, 1)   // a Tuesday
val TODAY: LocalDate = LocalDate.of(2026, 9, 24)  // a Thursday

fun d(day: Int): LocalDate = LocalDate.of(2026, 9, day)

class HistoryBuilder {
    private val goals = ArrayList<Goal>()
    private val versions = ArrayList<GoalVersion>()
    private val completions = ArrayList<Completion>()
    private val overrides = ArrayList<DayOverride>()

    fun goal(
        id: String,
        type: GoalType = GoalType.CHECK,
        target: Int? = null,
        weekdays: Int = ALL_WEEKDAYS,
        from: LocalDate = START,
        to: LocalDate? = null,
        weight: Int = 1,
        kind: GoalKind = GoalKind.RECURRING,
        oneOffDate: LocalDate? = null,
        title: String = id,
    ): String {
        if (goals.none { it.id == id }) {
            goals += Goal(id, kind, oneOffDate, createdAt = goals.size.toLong(), archivedAt = null, sortOrder = goals.size)
        }
        versions += GoalVersion(
            id = "$id-v${versions.size}", goalId = id, validFrom = from, validTo = to, title = title,
            emoji = "✅", colorIndex = 0, type = type, targetCount = target, unit = null,
            weekdayMask = weekdays, weight = weight,
        )
        return id
    }

    fun done(goalId: String, date: LocalDate) { completions += Completion(goalId, date, checked = true, count = 0, updatedAt = 0) }
    fun count(goalId: String, date: LocalDate, n: Int) { completions += Completion(goalId, date, checked = false, count = n, updatedAt = 0) }
    fun skip(goalId: String, date: LocalDate) { overrides += DayOverride(goalId, date, OverrideKind.SKIP) }

    fun build() = History(goals, versions, completions, overrides)
}

fun history(block: HistoryBuilder.() -> Unit): History = HistoryBuilder().apply(block).build()

/** Day scores starting at [start]; the last value is "today". */
fun scores(start: LocalDate, vararg values: Double?): Pair<SortedMap<LocalDate, Double?>, LocalDate> {
    val map = sortedMapOf<LocalDate, Double?>()
    values.forEachIndexed { i, v -> map[start.plusDays(i.toLong())] = v }
    return map to start.plusDays((values.size - 1).toLong())
}
