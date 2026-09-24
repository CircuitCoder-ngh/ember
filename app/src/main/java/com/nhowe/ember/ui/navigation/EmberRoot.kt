package com.nhowe.ember.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.nhowe.ember.core.time.toLocalDate
import com.nhowe.ember.di.LocalAppContainer
import com.nhowe.ember.ui.goals.GoalEditorScreen
import com.nhowe.ember.ui.onboarding.OnboardingScreen
import com.nhowe.ember.ui.settings.SettingsScreen
import com.nhowe.ember.ui.theme.EmberTheme

@Composable
fun EmberRoot() {
    val container = LocalAppContainer.current
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
    val current = settings ?: return

    EmberTheme(mode = current.themeMode, dynamicColor = current.dynamicColor) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground) {
            val start = remember { if (current.onboardingDone) HomeRoute else OnboardingRoute }
            val backStack = rememberNavBackStack(start)

            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                transitionSpec = {
                    (slideInVertically(tween(320)) { it / 6 } + fadeIn(tween(240))) togetherWith fadeOut(tween(200))
                },
                popTransitionSpec = {
                    fadeIn(tween(200)) togetherWith (slideOutVertically(tween(280)) { it / 6 } + fadeOut(tween(200)))
                },
                predictivePopTransitionSpec = {
                    fadeIn(tween(200)) togetherWith (slideOutVertically(tween(280)) { it / 6 } + fadeOut(tween(200)))
                },
                entryProvider = entryProvider {
                    entry<OnboardingRoute> {
                        OnboardingScreen(onDone = { backStack.clear(); backStack.add(HomeRoute) })
                    }
                    entry<HomeRoute> {
                        HomeScreen(
                            onOpenSettings = { backStack.add(SettingsRoute) },
                            onEditGoal = { id -> backStack.add(GoalEditorRoute(goalId = id)) },
                            onNewGoal = { backStack.add(GoalEditorRoute()) },
                            onNewOneOff = { date -> backStack.add(GoalEditorRoute(oneOffEpochDay = date.toEpochDay().toInt())) },
                        )
                    }
                    entry<SettingsRoute> {
                        SettingsScreen(onBack = { backStack.removeLastOrNull() })
                    }
                    entry<GoalEditorRoute> { route ->
                        GoalEditorScreen(
                            goalId = route.goalId,
                            oneOffDate = route.oneOffEpochDay?.toLocalDate(),
                            onClose = { backStack.removeLastOrNull() },
                        )
                    }
                },
            )
        }
    }
}
