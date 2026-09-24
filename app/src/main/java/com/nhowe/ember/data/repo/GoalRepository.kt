package com.nhowe.ember.data.repo

import com.nhowe.ember.core.time.ALL_WEEKDAYS
import com.nhowe.ember.core.time.toEpochDayInt
import com.nhowe.ember.data.db.dao.GoalDao
import com.nhowe.ember.data.db.dao.GoalVersionDao
import com.nhowe.ember.data.db.entity.GoalEntity
import com.nhowe.ember.data.db.entity.GoalVersionEntity
import com.nhowe.ember.domain.model.GoalKind
import com.nhowe.ember.domain.model.GoalType
import java.time.LocalDate
import java.util.UUID

/** Everything the user can edit about a goal. */
data class GoalDraft(
    val title: String,
    val emoji: String = "✅",
    val colorIndex: Int = 0,
    val type: GoalType = GoalType.CHECK,
    val targetCount: Int? = null,
    val unit: String? = null,
    val weekdayMask: Int = ALL_WEEKDAYS,
    val weight: Int = 1,
    val note: String? = null,
)

/**
 * Write side for goals. The key rule: editing never rewrites a version that already applied
 * to a past day; it closes that version yesterday and opens a new one today.
 */
class GoalRepository(
    private val goalDao: GoalDao,
    private val versionDao: GoalVersionDao,
) {
    suspend fun createRecurring(draft: GoalDraft, today: LocalDate): String {
        val id = UUID.randomUUID().toString()
        goalDao.upsert(GoalEntity(id, GoalKind.RECURRING.name, null, System.currentTimeMillis(), null, goalDao.nextSortOrder()))
        versionDao.upsert(draft.toEntity(id, validFrom = today, validTo = null))
        return id
    }

    suspend fun createOneOff(draft: GoalDraft, date: LocalDate): String {
        val id = UUID.randomUUID().toString()
        goalDao.upsert(GoalEntity(id, GoalKind.ONE_OFF.name, date.toEpochDayInt(), System.currentTimeMillis(), null, goalDao.nextSortOrder()))
        versionDao.upsert(draft.toEntity(id, validFrom = date, validTo = date))
        return id
    }

    suspend fun edit(goalId: String, draft: GoalDraft, today: LocalDate, oneOffDate: LocalDate? = null) {
        val goal = goalDao.get(goalId) ?: return
        if (goal.kind == GoalKind.ONE_OFF.name) {
            val date = oneOffDate ?: goal.oneOffDate?.let { LocalDate.ofEpochDay(it.toLong()) } ?: today
            val latest = versionDao.latestVersion(goalId)
            goalDao.setOneOffDate(goalId, date.toEpochDayInt())
            versionDao.upsert(draft.toEntity(goalId, validFrom = date, validTo = date, id = latest?.id))
            return
        }
        val open = versionDao.openVersion(goalId)
        when {
            open == null -> versionDao.upsert(draft.toEntity(goalId, validFrom = today, validTo = null))
            open.validFrom >= today.toEpochDayInt() ->
                versionDao.upsert(draft.toEntity(goalId, validFrom = today, validTo = null, id = open.id))
            else -> {
                versionDao.setValidTo(open.id, today.minusDays(1).toEpochDayInt())
                versionDao.upsert(draft.toEntity(goalId, validFrom = today, validTo = null))
            }
        }
    }

    /** Stops the goal from applying from tomorrow on (or from today if it has no progress today). */
    suspend fun archive(goalId: String, today: LocalDate, hasProgressToday: Boolean) {
        val open = versionDao.openVersion(goalId)
        if (open != null) {
            val lastDay = if (hasProgressToday) today else today.minusDays(1)
            if (open.validFrom > lastDay.toEpochDayInt()) versionDao.delete(open.id)
            else versionDao.setValidTo(open.id, lastDay.toEpochDayInt())
        }
        goalDao.setArchived(goalId, System.currentTimeMillis())
    }

    suspend fun unarchive(goalId: String, today: LocalDate) {
        val latest = versionDao.latestVersion(goalId) ?: return
        val todayInt = today.toEpochDayInt()
        if (latest.validTo != null && latest.validTo >= todayInt) {
            versionDao.setValidTo(latest.id, null)
        } else {
            versionDao.upsert(latest.copy(id = UUID.randomUUID().toString(), validFrom = todayInt, validTo = null))
        }
        goalDao.setArchived(goalId, null)
    }

    suspend fun reorder(idsInOrder: List<String>) {
        idsInOrder.forEachIndexed { index, id -> goalDao.setOrder(id, index) }
    }

    /** Permanently removes the goal and all its history. */
    suspend fun delete(goalId: String) = goalDao.delete(goalId)

    private fun GoalDraft.toEntity(goalId: String, validFrom: LocalDate, validTo: LocalDate?, id: String? = null) =
        GoalVersionEntity(
            id = id ?: UUID.randomUUID().toString(),
            goalId = goalId,
            validFrom = validFrom.toEpochDayInt(),
            validTo = validTo?.toEpochDayInt(),
            title = title.trim(),
            emoji = emoji,
            colorIndex = colorIndex,
            type = type.name,
            targetCount = if (type == GoalType.QUANTITY) (targetCount ?: 1).coerceAtLeast(1) else null,
            unit = unit?.trim()?.takeIf { it.isNotEmpty() },
            weekdayMask = if (weekdayMask == 0) ALL_WEEKDAYS else weekdayMask,
            weight = weight.coerceIn(1, 3),
            note = note?.trim()?.takeIf { it.isNotEmpty() },
        )
}
