package com.nhowe.ember.data.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.nhowe.ember.data.db.dao.CelebrationDao
import com.nhowe.ember.data.db.dao.CompletionDao
import com.nhowe.ember.data.db.dao.DayOverrideDao
import com.nhowe.ember.data.db.dao.GoalDao
import com.nhowe.ember.data.db.dao.GoalVersionDao
import com.nhowe.ember.data.db.entity.CelebrationEntity
import com.nhowe.ember.data.db.entity.CompletionEntity
import com.nhowe.ember.data.db.entity.DayOverrideEntity
import com.nhowe.ember.data.db.entity.GoalEntity
import com.nhowe.ember.data.db.entity.GoalVersionEntity
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [GoalEntity::class, GoalVersionEntity::class, CompletionEntity::class, DayOverrideEntity::class, CelebrationEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class EmberDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun goalVersionDao(): GoalVersionDao
    abstract fun completionDao(): CompletionDao
    abstract fun dayOverrideDao(): DayOverrideDao
    abstract fun celebrationDao(): CelebrationDao

    companion object {
        /** v2: weekly/monthly cadence on goal versions. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE goal_version ADD COLUMN cadence TEXT NOT NULL DEFAULT 'DAILY'")
            }
        }

        fun build(context: Context, name: String = "ember.db"): EmberDatabase =
            Room.databaseBuilder<EmberDatabase>(context.applicationContext, name)
                .setDriver(BundledSQLiteDriver())
                .addMigrations(MIGRATION_1_2)
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }
}
