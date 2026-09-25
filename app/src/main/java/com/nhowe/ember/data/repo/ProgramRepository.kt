package com.nhowe.ember.data.repo

import com.nhowe.ember.core.time.toEpochDayInt
import com.nhowe.ember.data.db.EmberDatabase
import com.nhowe.ember.data.db.entity.GoalEntity
import com.nhowe.ember.data.db.entity.GoalVersionEntity
import com.nhowe.ember.data.db.entity.ProgramEntity
import com.nhowe.ember.data.templates.ProgramGoalSpec
import com.nhowe.ember.data.templates.ProgramTemplate
import com.nhowe.ember.domain.model.GoalKind
import com.nhowe.ember.domain.model.GoalVersion
import com.nhowe.ember.domain.model.Program
import java.time.LocalDate
import java.util.UUID

/** Enrolment: a program becomes ordinary goals whose versions are pre-dated one per phase. */
class ProgramRepository(private val db: EmberDatabase) {

    suspend fun enrol(template: ProgramTemplate, startDate: LocalDate): String {
        val programId = UUID.randomUUID().toString()
        val goalIds = ArrayList<String>()
        var order = db.goalDao().nextSortOrder()
        for (spec in template.goals) {
            val goalId = UUID.randomUUID().toString()
            db.goalDao().upsert(GoalEntity(goalId, GoalKind.RECURRING.name, null, System.currentTimeMillis(), null, order++))
            db.goalVersionDao().upsertAll(buildVersions(goalId, spec, startDate, template.lengthDays).map { GoalVersionEntity.from(it) })
            goalIds += goalId
        }
        db.programDao().upsert(
            ProgramEntity(
                id = programId, templateId = template.id, title = template.title, emoji = template.emoji,
                startDate = startDate.toEpochDayInt(), lengthDays = template.lengthDays, goalIds = goalIds.joinToString(","),
                strict = template.strict, graduationTitle = template.graduation.title, graduationIcon = template.graduation.icon,
                graduationXp = template.graduation.xp, nextTemplateId = template.next, abandonedOn = null,
            )
        )
        return programId
    }

    /** Leave a program: its goals stop from tomorrow and are archived; history stays. */
    suspend fun leave(program: Program, today: LocalDate) {
        val lastInt = today.toEpochDayInt()
        for (goalId in program.goalIds) {
            db.goalVersionDao().versionOn(goalId, lastInt)?.let { db.goalVersionDao().setValidTo(it.id, lastInt) }
            db.goalVersionDao().deleteVersionsAfter(goalId, lastInt)
            db.goalDao().setArchived(goalId, System.currentTimeMillis())
        }
        db.programDao().setAbandoned(program.id, lastInt)
    }

    /** Strict programs: start over from today. Past completions remain in history. */
    suspend fun restart(program: Program, template: ProgramTemplate, today: LocalDate) {
        template.goals.zip(program.goalIds).forEach { (spec, goalId) ->
            val yesterday = today.toEpochDayInt() - 1
            db.goalVersionDao().versionOn(goalId, yesterday)?.let { db.goalVersionDao().setValidTo(it.id, yesterday) }
            db.goalVersionDao().deleteVersionsAfter(goalId, yesterday)
            db.goalVersionDao().upsertAll(buildVersions(goalId, spec, today, template.lengthDays).map { GoalVersionEntity.from(it) })
        }
        db.programDao().setStart(program.id, today.toEpochDayInt())
    }

    companion object {
        /** One version per phase, each ending the day before the next phase, the last ending with the program. */
        fun buildVersions(goalId: String, spec: ProgramGoalSpec, startDate: LocalDate, lengthDays: Int): List<GoalVersion> {
            val end = startDate.plusDays((lengthDays - 1).toLong())
            val phases = spec.phases.sortedBy { it.fromDay }
            return phases.mapIndexedNotNull { i, phase ->
                val from = startDate.plusDays((phase.fromDay - 1).toLong())
                val to = phases.getOrNull(i + 1)?.let { startDate.plusDays((it.fromDay - 2).toLong()) } ?: end
                if (from > end || to < from) return@mapIndexedNotNull null
                val d = spec.draftFor(phase)
                GoalVersion(
                    id = UUID.randomUUID().toString(), goalId = goalId, validFrom = from, validTo = minOf(to, end),
                    title = d.title, emoji = d.emoji, colorIndex = d.colorIndex, type = d.type,
                    targetCount = if (d.type == com.nhowe.ember.domain.model.GoalType.QUANTITY || d.cadence != com.nhowe.ember.domain.model.Cadence.DAILY) (d.targetCount ?: 1).coerceAtLeast(1) else null,
                    unit = d.unit, weekdayMask = d.weekdayMask, weight = d.weight, note = d.note, cadence = d.cadence,
                )
            }
        }
    }
}
