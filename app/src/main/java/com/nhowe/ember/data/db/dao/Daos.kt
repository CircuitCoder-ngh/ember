package com.nhowe.ember.data.db.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.nhowe.ember.data.db.entity.CelebrationEntity
import com.nhowe.ember.data.db.entity.CompletionEntity
import com.nhowe.ember.data.db.entity.DayOverrideEntity
import com.nhowe.ember.data.db.entity.GoalEntity
import com.nhowe.ember.data.db.entity.GoalVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goal ORDER BY sortOrder, createdAt")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goal WHERE id = :id")
    suspend fun get(id: String): GoalEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM goal")
    suspend fun nextSortOrder(): Int

    @Upsert
    suspend fun upsert(goal: GoalEntity)

    @Upsert
    suspend fun upsertAll(goals: List<GoalEntity>)

    @Query("UPDATE goal SET sortOrder = :order WHERE id = :id")
    suspend fun setOrder(id: String, order: Int)

    @Query("UPDATE goal SET archivedAt = :at WHERE id = :id")
    suspend fun setArchived(id: String, at: Long?)

    @Query("UPDATE goal SET oneOffDate = :date WHERE id = :id")
    suspend fun setOneOffDate(id: String, date: Int)

    @Query("DELETE FROM goal WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM goal")
    suspend fun deleteAll()
}

@Dao
interface GoalVersionDao {
    @Query("SELECT * FROM goal_version ORDER BY validFrom")
    fun observeAll(): Flow<List<GoalVersionEntity>>

    @Query("SELECT * FROM goal_version WHERE goalId = :goalId AND validTo IS NULL ORDER BY validFrom DESC LIMIT 1")
    suspend fun openVersion(goalId: String): GoalVersionEntity?

    @Query("SELECT * FROM goal_version WHERE goalId = :goalId ORDER BY validFrom DESC LIMIT 1")
    suspend fun latestVersion(goalId: String): GoalVersionEntity?

    @Upsert
    suspend fun upsert(version: GoalVersionEntity)

    @Upsert
    suspend fun upsertAll(versions: List<GoalVersionEntity>)

    @Query("UPDATE goal_version SET validTo = :validTo WHERE id = :id")
    suspend fun setValidTo(id: String, validTo: Int?)

    @Query("DELETE FROM goal_version WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM goal_version")
    suspend fun deleteAll()
}

@Dao
interface CompletionDao {
    @Query("SELECT * FROM completion")
    fun observeAll(): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completion WHERE goalId = :goalId AND date = :date")
    suspend fun get(goalId: String, date: Int): CompletionEntity?

    @Upsert
    suspend fun upsert(completion: CompletionEntity)

    @Upsert
    suspend fun upsertAll(completions: List<CompletionEntity>)

    @Query("DELETE FROM completion WHERE goalId = :goalId AND date = :date")
    suspend fun delete(goalId: String, date: Int)

    @Query("DELETE FROM completion")
    suspend fun deleteAll()
}

@Dao
interface DayOverrideDao {
    @Query("SELECT * FROM day_override")
    fun observeAll(): Flow<List<DayOverrideEntity>>

    @Upsert
    suspend fun upsert(override: DayOverrideEntity)

    @Upsert
    suspend fun upsertAll(overrides: List<DayOverrideEntity>)

    @Query("DELETE FROM day_override WHERE goalId = :goalId AND date = :date")
    suspend fun delete(goalId: String, date: Int)

    @Query("DELETE FROM day_override")
    suspend fun deleteAll()
}

@Dao
interface CelebrationDao {
    @Query("SELECT `key` FROM celebration")
    fun observeKeys(): Flow<List<String>>

    @Upsert
    suspend fun upsert(celebration: CelebrationEntity)

    @Query("DELETE FROM celebration")
    suspend fun deleteAll()
}
