package com.nhowe.ember.domain.model

import java.time.LocalTime

enum class ThemeMode { DARK, LIGHT, SYSTEM }

data class Settings(
    val streakThreshold: Double = 0.8,
    val dayBoundaryHour: Int = 0,
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(21, 0),
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColor: Boolean = false,
    val onboardingDone: Boolean = false,
    val completedSinkToBottom: Boolean = true,
    val userName: String = "",
)
