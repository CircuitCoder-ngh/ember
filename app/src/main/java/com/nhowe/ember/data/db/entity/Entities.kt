package com.nhowe.ember.data.db.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.nhowe.ember.core.time.toEpochDayInt
import com.nhowe.ember.core.time.toLocalDate
import com.nhowe.ember.domain.model.Completion
import com.nhowe.ember.domain.model.DayOverride
import com.nhowe.ember.domain.model.Goal
import com.nhowe.ember.domain.model.GoalKind
import com.nhowe.ember.domain.model.GoalType
import com.nhowe.ember.domain.model.GoalVersion
import com.nhowe.ember.domain.model.OverrideKind

@Entity(tableName = "goal")
data class GoalEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val oneOffDate: Int?,
    val createdAt: Long,
    val archivedAt: Long?,
    val sortOrder: Int,
) {
    fun toDomain() = Goal(id, GoalKind.valueOf(kind), oneOffDate?.toLocalDate(), createdAt, archivedAt, sortOrder)

    companion object {
        fun from(g: Goal) = GoalEntity(g.id, g.kind.name, g.oneOffDate?.toEpochDayInt(), g.createdAt, g.archivedAt, g.sortOrder)
    }
}

@Entity(
    tableName = "goal_version",
    indices = [Index("goalId"), Index(value = ["goalId", "validFrom"])],
    foreignKeys = [ForeignKey(entity = GoalEntity::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.CASCADE)],
)
data class GoalVersionEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val validFrom: Int,
    val validTo: Int?,
    val title: String,
    val emoji: String,
    val colorIndex: Int,
    val type: String,
    val targetCount: Int?,
    val unit: String?,
    val weekdayMask: Int,
    val weight: Int,
    val note: String?,
) {
    fun toDomain() = GoalVersion(
        id, goalId, validFrom.toLocalDate(), validTo?.toLocalDate(), title, emoji, colorIndex,
        GoalType.valueOf(type), targetCount, unit, weekdayMask, weight, note,
    )

    companion object {
        fun from(v: GoalVersion) = GoalVersionEntity(
            v.id, v.goalId, v.validFrom.toEpochDayInt(), v.validTo?.toEpochDayInt(), v.title, v.emoji, v.colorIndex,
            v.type.name, v.targetCount, v.unit, v.weekdayMask, v.weight, v.note,
        )
    }
}

@Entity(tableName = "completion", primaryKeys = ["goalId", "date"])
data class CompletionEntity(
    val goalId: String,
    val date: Int,
    val checked: Boolean,
    val count: Int,
    val updatedAt: Long,
) {
    fun toDomain() = Completion(goalId, date.toLocalDate(), checked, count, updatedAt)

    companion object {
        fun from(c: Completion) = CompletionEntity(c.goalId, c.date.toEpochDayInt(), c.checked, c.count, c.updatedAt)
    }
}

@Entity(tableName = "day_override", primaryKeys = ["goalId", "date"])
data class DayOverrideEntity(
    val goalId: String,
    val date: Int,
    val kind: String,
) {
    fun toDomain() = DayOverride(goalId, date.toLocalDate(), OverrideKind.valueOf(kind))

    companion object {
        fun from(o: DayOverride) = DayOverrideEntity(o.goalId, o.date.toEpochDayInt(), o.kind.name)
    }
}

@Entity(tableName = "celebration")
data class CelebrationEntity(
    @PrimaryKey val key: String,
    val shownAt: Long,
)
