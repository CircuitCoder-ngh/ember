package com.nhowe.ember.data.repo

import com.nhowe.ember.data.db.EmberDatabase
import com.nhowe.ember.data.db.entity.CelebrationEntity
import com.nhowe.ember.data.db.entity.CompletionEntity
import com.nhowe.ember.data.db.entity.DayOverrideEntity
import com.nhowe.ember.data.db.entity.GoalEntity
import com.nhowe.ember.data.db.entity.GoalVersionEntity
import com.nhowe.ember.domain.model.History
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Read side: the whole history as one flow, plus bulk replace for backup import. */
class HistoryRepository(private val db: EmberDatabase) {

    val history: Flow<History> = combine(
        db.goalDao().observeAll(),
        db.goalVersionDao().observeAll(),
        db.completionDao().observeAll(),
        db.dayOverrideDao().observeAll(),
    ) { goals, versions, completions, overrides ->
        History(
            goals = goals.map { it.toDomain() },
            versions = versions.map { it.toDomain() },
            completions = completions.map { it.toDomain() },
            overrides = overrides.map { it.toDomain() },
        )
    }

    val shownCelebrationKeys: Flow<Set<String>> = db.celebrationDao().observeKeys().map { it.toSet() }

    suspend fun markCelebrationShown(key: String) =
        db.celebrationDao().upsert(CelebrationEntity(key, System.currentTimeMillis()))

    suspend fun replaceAll(history: History) {
        db.celebrationDao().deleteAll()
        db.dayOverrideDao().deleteAll()
        db.completionDao().deleteAll()
        db.goalVersionDao().deleteAll()
        db.goalDao().deleteAll()
        db.goalDao().upsertAll(history.goals.map { GoalEntity.from(it) })
        db.goalVersionDao().upsertAll(history.versions.map { GoalVersionEntity.from(it) })
        db.completionDao().upsertAll(history.completions.map { CompletionEntity.from(it) })
        db.dayOverrideDao().upsertAll(history.overrides.map { DayOverrideEntity.from(it) })
    }

    suspend fun clearAll() = replaceAll(History())
}
