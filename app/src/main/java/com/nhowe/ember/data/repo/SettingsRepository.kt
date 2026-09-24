package com.nhowe.ember.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nhowe.ember.domain.model.Settings
import com.nhowe.ember.domain.model.ThemeMode
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsStore

    private object Keys {
        val threshold = doublePreferencesKey("streak_threshold")
        val boundaryHour = intPreferencesKey("day_boundary_hour")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderMinutes = intPreferencesKey("reminder_minutes")
        val haptics = booleanPreferencesKey("haptics")
        val sound = booleanPreferencesKey("sound")
        val theme = stringPreferencesKey("theme")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val sinkCompleted = booleanPreferencesKey("sink_completed")
        val userName = stringPreferencesKey("user_name")
    }

    val settings: Flow<Settings> = store.data.map { p ->
        Settings(
            streakThreshold = p[Keys.threshold] ?: 0.8,
            dayBoundaryHour = p[Keys.boundaryHour] ?: 0,
            reminderEnabled = p[Keys.reminderEnabled] ?: false,
            reminderTime = (p[Keys.reminderMinutes] ?: (21 * 60)).let { LocalTime.of(it / 60, it % 60) },
            hapticsEnabled = p[Keys.haptics] ?: true,
            soundEnabled = p[Keys.sound] ?: false,
            themeMode = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            dynamicColor = p[Keys.dynamicColor] ?: false,
            onboardingDone = p[Keys.onboardingDone] ?: false,
            completedSinkToBottom = p[Keys.sinkCompleted] ?: true,
            userName = p[Keys.userName] ?: "",
        )
    }

    suspend fun setThreshold(value: Double) = store.edit { it[Keys.threshold] = value.coerceIn(0.5, 1.0) }
    suspend fun setBoundaryHour(hour: Int) = store.edit { it[Keys.boundaryHour] = hour.coerceIn(0, 6) }
    suspend fun setReminderEnabled(enabled: Boolean) = store.edit { it[Keys.reminderEnabled] = enabled }
    suspend fun setReminderTime(time: LocalTime) = store.edit { it[Keys.reminderMinutes] = time.hour * 60 + time.minute }
    suspend fun setHaptics(enabled: Boolean) = store.edit { it[Keys.haptics] = enabled }
    suspend fun setSound(enabled: Boolean) = store.edit { it[Keys.sound] = enabled }
    suspend fun setTheme(mode: ThemeMode) = store.edit { it[Keys.theme] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = store.edit { it[Keys.dynamicColor] = enabled }
    suspend fun setOnboardingDone(done: Boolean) = store.edit { it[Keys.onboardingDone] = done }
    suspend fun setSinkCompleted(enabled: Boolean) = store.edit { it[Keys.sinkCompleted] = enabled }
    suspend fun setUserName(name: String) = store.edit { it[Keys.userName] = name.trim() }

    suspend fun replaceAll(s: Settings) = store.edit {
        it[Keys.threshold] = s.streakThreshold
        it[Keys.boundaryHour] = s.dayBoundaryHour
        it[Keys.reminderEnabled] = s.reminderEnabled
        it[Keys.reminderMinutes] = s.reminderTime.hour * 60 + s.reminderTime.minute
        it[Keys.haptics] = s.hapticsEnabled
        it[Keys.sound] = s.soundEnabled
        it[Keys.theme] = s.themeMode.name
        it[Keys.dynamicColor] = s.dynamicColor
        it[Keys.onboardingDone] = s.onboardingDone
        it[Keys.sinkCompleted] = s.completedSinkToBottom
        it[Keys.userName] = s.userName
    }
}
