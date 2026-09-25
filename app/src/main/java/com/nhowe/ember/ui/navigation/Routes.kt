package com.nhowe.ember.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey

@Serializable data object HomeRoute : Route
@Serializable data object OnboardingRoute : Route
@Serializable data object SettingsRoute : Route
@Serializable data object TemplatesRoute : Route

/** Create or edit a goal. [oneOffEpochDay] set = a one-off goal for that date. */
@Serializable data class GoalEditorRoute(val goalId: String? = null, val oneOffEpochDay: Int? = null) : Route
