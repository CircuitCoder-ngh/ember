package com.nhowe.ember.data.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
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
    version = 1,
    exportSchema = true,
)
abstract class EmberDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun goalVersionDao(): GoalVersionDao
    abstract fun completionDao(): CompletionDao
    abstract fun dayOverrideDao(): DayOverrideDao
    abstract fun celebrationDao(): CelebrationDao

    companion object {
        fun build(context: Context, name: String = "ember.db"): EmberDatabase =
            Room.databaseBuilder<EmberDatabase>(context.applicationContext, name)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }
}
