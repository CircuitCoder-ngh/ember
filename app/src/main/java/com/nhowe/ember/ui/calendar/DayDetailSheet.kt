package com.nhowe.ember.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nhowe.ember.domain.model.DayPlan
import com.nhowe.ember.domain.model.DayState
import com.nhowe.ember.domain.model.ResolvedGoal
import com.nhowe.ember.ui.components.GoalRow
import com.nhowe.ember.ui.components.ProgressRing
import com.nhowe.ember.ui.theme.ember
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailSheet(
    date: LocalDate,
    plan: DayPlan,
    state: DayState?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onToggle: (ResolvedGoal) -> Unit,
    onCountChange: (ResolvedGoal, Int) -> Unit,
    onSkip: (String, Boolean) -> Unit,
    onNewOneOff: () -> Unit,
) {
    val editable = date <= today
    val isPast = date < today
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(progress = (plan.score ?: 0.0).toFloat(), size = 64.dp, strokeWidth = 7.dp, animate = false) {
                    Text(plan.score?.let { "${(it * 100).roundToInt()}" } ?: "—", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")), style = MaterialTheme.typography.titleLarge)
                    Text(stateLabel(state, plan, date == today), style = MaterialTheme.typography.bodySmall, color = stateColor(state))
                }
            }
            if (isPast && plan.hasGoals) {
                Text(
                    "Editing a past day. Your streak and XP will be recalculated.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(10.dp),
                )
            }
            if (!plan.hasGoals && plan.skipped.isEmpty()) {
                Text(
                    if (date > today) "Nothing planned yet." else "No goals were scheduled. A rest day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            plan.goals.forEach { goal ->
                GoalRow(
                    goal = goal,
                    editable = editable,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onToggle = { onToggle(goal) },
                    onCountChange = { onCountChange(goal, it) },
                    onLongPress = { if (editable) onSkip(goal.id, true) },
                )
            }
            plan.skipped.forEach { goal ->
                Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${goal.version.emoji}  ${goal.version.title} · skipped", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    if (editable) TextButton(onClick = { onSkip(goal.id, false) }) { Text("Undo") }
                }
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onNewOneOff, modifier = Modifier.padding(horizontal = 12.dp)) {
                Text("+ Add a one-off goal for this day")
            }
        }
    }
}

private fun stateLabel(state: DayState?, plan: DayPlan, isToday: Boolean): String = when (state) {
    DayState.PERFECT -> "Perfect day"
    DayState.HIT -> "On target · ${plan.doneCount} of ${plan.goals.size} done"
    DayState.MISS -> "Missed · ${plan.doneCount} of ${plan.goals.size} done"
    DayState.FROZEN -> "Missed, but a streak freeze saved you"
    DayState.REST -> "Rest day"
    DayState.PENDING -> "In progress · ${plan.doneCount} of ${plan.goals.size} done"
    DayState.FUTURE, null -> if (isToday) "Today" else if (plan.hasGoals) "${plan.goals.size} planned" else "Upcoming"
}

@Composable
private fun stateColor(state: DayState?) = when (state) {
    DayState.PERFECT -> MaterialTheme.ember.perfect
    DayState.HIT -> MaterialTheme.ember.hit
    DayState.MISS -> MaterialTheme.ember.miss
    DayState.FROZEN -> MaterialTheme.ember.frozen
    DayState.PENDING -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
