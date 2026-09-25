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
import com.nhowe.ember.data.db.dao.ProgramDao
import com.nhowe.ember.data.db.entity.CelebrationEntity
import com.nhowe.ember.data.db.entity.CompletionEntity
import com.nhowe.ember.data.db.entity.DayOverrideEntity
import com.nhowe.ember.data.db.entity.GoalEntity
import com.nhowe.ember.data.db.entity.GoalVersionEntity
import com.nhowe.ember.data.db.entity.ProgramEntity
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [GoalEntity::class, GoalVersionEntity::class, CompletionEntity::class, DayOverrideEntity::class, CelebrationEntity::class, ProgramEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class EmberDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun goalVersionDao(): GoalVersionDao
    abstract fun completionDao(): CompletionDao
    abstract fun dayOverrideDao(): DayOverrideDao
    abstract fun celebrationDao(): CelebrationDao
    abstract fun programDao(): ProgramDao

    companion object {
        /** v2: weekly/monthly cadence on goal versions. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE goal_version ADD COLUMN cadence TEXT NOT NULL DEFAULT 'DAILY'")
            }
        }

        /** v3: enrolled programs. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS program (id TEXT NOT NULL PRIMARY KEY, templateId TEXT NOT NULL, title TEXT NOT NULL, emoji TEXT NOT NULL, " +
                        "startDate INTEGER NOT NULL, lengthDays INTEGER NOT NULL, goalIds TEXT NOT NULL, strict INTEGER NOT NULL, " +
                        "graduationTitle TEXT NOT NULL, graduationIcon TEXT NOT NULL, graduationXp INTEGER NOT NULL, nextTemplateId TEXT, abandonedOn INTEGER)"
                )
            }
        }

        fun build(context: Context, name: String = "ember.db"): EmberDatabase =
            Room.databaseBuilder<EmberDatabase>(context.applicationContext, name)
                .setDriver(BundledSQLiteDriver())
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }
}
