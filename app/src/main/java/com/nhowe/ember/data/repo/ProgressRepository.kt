package com.nhowe.ember.data.repo

import com.nhowe.ember.core.time.toEpochDayInt
import com.nhowe.ember.data.db.dao.CompletionDao
import com.nhowe.ember.data.db.dao.DayOverrideDao
import com.nhowe.ember.data.db.entity.CompletionEntity
import com.nhowe.ember.data.db.entity.DayOverrideEntity
import com.nhowe.ember.domain.model.OverrideKind
import java.time.LocalDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Write side for daily progress. Future dates are refused. */
class ProgressRepository(
    private val completionDao: CompletionDao,
    private val overrideDao: DayOverrideDao,
) {
    private val writes = Mutex()

    suspend fun setChecked(goalId: String, date: LocalDate, checked: Boolean, today: LocalDate) = writes.withLock {
        if (date > today) return@withLock
        val existing = completionDao.get(goalId, date.toEpochDayInt())
        completionDao.upsert(
            CompletionEntity(goalId, date.toEpochDayInt(), checked, existing?.count ?: 0, System.currentTimeMillis())
        )
    }

    suspend fun setCount(goalId: String, date: LocalDate, count: Int, today: LocalDate) = writes.withLock {
        if (date > today) return@withLock
        val existing = completionDao.get(goalId, date.toEpochDayInt())
        completionDao.upsert(
            CompletionEntity(goalId, date.toEpochDayInt(), existing?.checked ?: false, count.coerceAtLeast(0), System.currentTimeMillis())
        )
    }

    suspend fun setSkipped(goalId: String, date: LocalDate, skipped: Boolean) = writes.withLock {
        if (skipped) overrideDao.upsert(DayOverrideEntity(goalId, date.toEpochDayInt(), OverrideKind.SKIP.name))
        else overrideDao.delete(goalId, date.toEpochDayInt())
    }
}
