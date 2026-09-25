package com.nhowe.ember.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhowe.ember.di.LocalAppContainer
import com.nhowe.ember.domain.model.CelebrationEvent
import com.nhowe.ember.ui.calendar.CalendarScreen
import com.nhowe.ember.ui.celebration.CelebrationOverlay
import com.nhowe.ember.ui.goals.GoalsScreen
import com.nhowe.ember.ui.stats.StatsScreen
import com.nhowe.ember.ui.today.TodayScreen
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class HomeTab(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Rounded.LocalFireDepartment),
    CALENDAR("Calendar", Icons.Rounded.CalendarMonth),
    STATS("Progress", Icons.Rounded.BarChart),
    GOALS("Goals", Icons.Rounded.Checklist),
}

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onEditGoal: (String) -> Unit,
    onNewGoal: () -> Unit,
    onNewOneOff: (LocalDate) -> Unit,
) {
    val container = LocalAppContainer.current
    var tab by rememberSaveable { mutableStateOf(HomeTab.TODAY) }
    val snapshot by container.engineStore.snapshot.collectAsStateWithLifecycle()

    // Celebrations: show one at a time, highest priority first, after a beat so the check animation lands.
    var celebration by rememberSaveable(stateSaver = CelebrationSaver) { mutableStateOf<CelebrationEvent?>(null) }
    val pending = snapshot?.pendingCelebrations.orEmpty()
    LaunchedEffect(pending) {
        if (celebration == null && pending.isNotEmpty()) {
            delay(900)
            celebration = pending.sortedBy { priority(it) }.first()
            container.haptics.celebrate()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
                windowInsets = NavigationBarDefaults.windowInsets,
            ) {
                HomeTab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { if (tab != t) { container.haptics.tick(); tab = t } },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (slideInHorizontally(tween(260)) { if (forward) it / 10 else -it / 10 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(200)) { if (forward) -it / 10 else it / 10 } + fadeOut(tween(160)))
                },
                label = "tab",
            ) { current ->
                when (current) {
                    HomeTab.TODAY -> TodayScreen(onOpenSettings = onOpenSettings, onEditGoal = onEditGoal, onNewOneOff = onNewOneOff, onNewGoal = onNewGoal)
                    HomeTab.CALENDAR -> CalendarScreen(onOpenSettings = onOpenSettings, onNewOneOff = onNewOneOff)
                    HomeTab.STATS -> StatsScreen(onOpenSettings = onOpenSettings)
                    HomeTab.GOALS -> GoalsScreen(onOpenSettings = onOpenSettings, onEditGoal = onEditGoal, onNewGoal = onNewGoal, onNewOneOff = onNewOneOff)
                }
            }
        }
    }

    val snap = snapshot
    AnimatedVisibility(
        visible = celebration != null && snap != null,
        enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.92f, animationSpec = tween(350)),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 1.05f, animationSpec = tween(200)),
    ) {
        val event = celebration
        if (event != null && snap != null) {
            CelebrationOverlay(event = event, snapshot = snap, onDismiss = {
                container.appScope.launch { container.historyRepository.markCelebrationShown(event.key) }
                celebration = null
            })
        }
    }
}

private fun priority(e: CelebrationEvent): Int = when (e) {
    is CelebrationEvent.StreakMilestone -> 0
    is CelebrationEvent.ComebackComplete -> 0
    is CelebrationEvent.LevelUp -> 1
    is CelebrationEvent.QuestComplete -> 1
    is CelebrationEvent.PerfectDay -> 2
    is CelebrationEvent.BadgeEarned -> 3
}
