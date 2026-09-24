package com.nhowe.ember.di

import android.content.Context
import com.nhowe.ember.core.haptics.Haptics
import com.nhowe.ember.core.time.DayClock
import com.nhowe.ember.data.db.EmberDatabase
import com.nhowe.ember.data.repo.GoalRepository
import com.nhowe.ember.data.repo.HistoryRepository
import com.nhowe.ember.data.repo.ProgressRepository
import com.nhowe.ember.data.repo.SettingsRepository
import com.nhowe.ember.domain.EngineStore
import com.nhowe.ember.notifications.ReminderScheduler
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Hand-rolled dependency graph. One instance per process, owned by [com.nhowe.ember.EmberApp]. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val db: EmberDatabase = EmberDatabase.build(context)
    val settingsRepository = SettingsRepository(context)
    val historyRepository = HistoryRepository(db)
    val goalRepository = GoalRepository(db.goalDao(), db.goalVersionDao())
    val progressRepository = ProgressRepository(db.completionDao(), db.dayOverrideDao())
    val dayClock = DayClock()
    val haptics = Haptics(context, settingsRepository.settings, appScope)
    val reminderScheduler = ReminderScheduler(context)

    val today: StateFlow<LocalDate> = dayClock
        .todayFlow(settingsRepository.settings.map { it.dayBoundaryHour }.distinctUntilChanged())
        .stateIn(appScope, SharingStarted.Eagerly, dayClock.today(0))

    val engineStore = EngineStore(
        history = historyRepository.history,
        settings = settingsRepository.settings,
        today = today,
        shownCelebrationKeys = historyRepository.shownCelebrationKeys,
        scope = appScope,
    )
}
