package com.nhowe.ember.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhowe.ember.di.LocalAppContainer
import com.nhowe.ember.domain.model.ResolvedGoal
import com.nhowe.ember.ui.components.ConfettiOverlay
import com.nhowe.ember.ui.components.ConfettiTrigger
import com.nhowe.ember.ui.components.FlameMascot
import com.nhowe.ember.ui.components.GoalRow
import com.nhowe.ember.ui.components.ProgressRing
import com.nhowe.ember.ui.components.ScreenHeader
import com.nhowe.ember.ui.components.SectionLabel
import com.nhowe.ember.ui.components.StreakChip
import com.nhowe.ember.ui.components.XpBar
import com.nhowe.ember.ui.components.flameStateFor
import com.nhowe.ember.ui.goals.GoalActionsSheet
import com.nhowe.ember.ui.theme.ember
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun TodayScreen(
    onOpenSettings: () -> Unit,
    onEditGoal: (String) -> Unit,
    onNewGoal: () -> Unit,
    onNewOneOff: (LocalDate) -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: TodayViewModel = viewModel { TodayViewModel(container) }
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle(initialValue = null)

    var confetti by remember { mutableStateOf<ConfettiTrigger?>(null) }
    var xpToast by remember { mutableStateOf<Int?>(null) }
    var actionsFor by remember { mutableStateOf<ResolvedGoal?>(null) }

    LaunchedEffect(Unit) {
        vm.effects.collect { effect ->
            when (effect) {
                is TodayEffect.XpGained -> { xpToast = effect.amount; delay(1400); xpToast = null }
                TodayEffect.GoalDone -> confetti = ConfettiTrigger(System.nanoTime(), count = 28, originY = 0.45f)
                TodayEffect.DayComplete -> confetti = ConfettiTrigger(System.nanoTime(), count = 160, originY = 0.3f)
            }
        }
    }

    val snap = snapshot
    Box(Modifier.fillMaxSize()) {
        if (snap == null) return@Box
        val plan = vm.mergedPlan(snap)
        val score = plan.score
        val sink = settings?.completedSinkToBottom ?: true
        val ordered = if (sink) plan.goals.sortedBy { it.isDone } else plan.goals

        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header") {
                ScreenHeader(
                    title = greeting(snap.settings.userName),
                    subtitle = snap.today.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    onSettings = onOpenSettings,
                )
            }
            item(key = "hero") {
                HeroCard(
                    progress = (score ?: 0.0).toFloat(),
                    done = plan.doneCount,
                    total = plan.goals.size,
                    isRest = !plan.hasGoals,
                    snapshot = snap,
                    threshold = snap.settings.streakThreshold,
                )
            }
            if (plan.hasGoals) {
                item(key = "label") { SectionLabel("Today's goals") }
                items(ordered, key = { it.id }) { goal ->
                    GoalRow(
                        goal = goal,
                        modifier = Modifier.padding(horizontal = 16.dp).animateItem(),
                        onToggle = { vm.toggle(goal) },
                        onCountChange = { vm.setCount(goal, it) },
                        onLongPress = { container.haptics.click(); actionsFor = goal },
                    )
                }
            } else {
                item(key = "rest") { RestDayCard(onNewGoal = onNewGoal, onNewOneOff = { onNewOneOff(snap.today) }) }
            }
            if (plan.skipped.isNotEmpty()) {
                item(key = "skipped-label") { SectionLabel("Skipped today") }
                items(plan.skipped, key = { "skip-${it.id}" }) { goal ->
                    Row(
                        Modifier.padding(horizontal = 16.dp).fillMaxWidth().animateItem(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${goal.version.emoji}  ${goal.version.title}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.setSkipped(goal.id, false) }) { Text("Undo") }
                    }
                }
            }
            if (plan.hasGoals) {
                item(key = "add-oneoff") {
                    TextButton(onClick = { onNewOneOff(snap.today) }, modifier = Modifier.padding(horizontal = 12.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add a one-off goal for today")
                    }
                }
            }
        }

        ConfettiOverlay(trigger = confetti)

        AnimatedVisibility(
            visible = xpToast != null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp),
            enter = slideInVertically(tween(220)) { it / 2 } + fadeIn(),
            exit = slideOutVertically(tween(300)) { -it } + fadeOut(),
        ) {
            val amount = xpToast ?: 0
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(if (amount >= 0) MaterialTheme.ember.xp else MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    if (amount >= 0) "+$amount XP" else "$amount XP",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (amount >= 0) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        actionsFor?.let { goal ->
            GoalActionsSheet(
                goal = goal,
                date = snap.today,
                onDismiss = { actionsFor = null },
                onEdit = { actionsFor = null; onEditGoal(goal.id) },
                onSkip = { actionsFor = null; vm.setSkipped(goal.id, true) },
            )
        }
    }
}

@Composable
private fun HeroCard(
    progress: Float,
    done: Int,
    total: Int,
    isRest: Boolean,
    snapshot: com.nhowe.ember.domain.model.EngineSnapshot,
    threshold: Double,
) {
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            ProgressRing(progress = progress, size = 168.dp, strokeWidth = 16.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (isRest) "—" else "${(progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        if (isRest) "rest day" else "$done of $total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                FlameMascot(state = flameStateFor(snapshot), size = 110.dp)
                Text(
                    statusLine(progress, threshold, snapshot, isRest),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            StreakChip(snapshot.streak)
        }
        Spacer(Modifier.height(14.dp))
        XpBar(snapshot.xp)
    }
}

private fun statusLine(progress: Float, threshold: Double, snap: com.nhowe.ember.domain.model.EngineSnapshot, isRest: Boolean): String = when {
    isRest -> "Nothing scheduled. Rest up."
    progress >= 0.999f -> "Perfect day. Legendary."
    snap.streak.todaySecured -> "Streak secured. Push for perfect?"
    else -> {
        val need = ((threshold - progress) * 100).roundToInt().coerceAtLeast(1)
        "$need% more to keep the flame"
    }
}

private fun greeting(name: String): String {
    val hour = java.time.LocalTime.now().hour
    val base = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Night owl"
    }
    return if (name.isBlank()) base else "$base, $name"
}

@Composable
private fun RestDayCard(onNewGoal: () -> Unit, onNewOneOff: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Nothing on the plan today", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            "Rest days don't break your streak. Add something if you're feeling it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Row {
            TextButton(onClick = onNewOneOff) { Text("One-off for today") }
            TextButton(onClick = onNewGoal) { Text("New daily goal") }
        }
    }
}
