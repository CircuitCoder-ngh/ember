package com.nhowe.ember.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nhowe.ember.domain.model.Cadence
import com.nhowe.ember.domain.model.GoalType
import com.nhowe.ember.domain.model.PeriodGoalProgress
import com.nhowe.ember.domain.model.ResolvedGoal
import com.nhowe.ember.ui.theme.ember
import com.nhowe.ember.ui.theme.goalColor
import java.time.LocalDate

/**
 * A weekly or monthly goal on a given day. Shows progress across the whole period; the control
 * logs this day's contribution (a check for "times" goals, a stepper for quantities).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PeriodGoalRow(
    goal: ResolvedGoal,
    period: PeriodGoalProgress?,
    date: LocalDate,
    modifier: Modifier = Modifier,
    editable: Boolean = true,
    onToggle: () -> Unit,
    onCountChange: (Int) -> Unit,
    onLongPress: () -> Unit,
) {
    val color = goalColor(goal.version.colorIndex)
    val isQuantity = goal.version.type == GoalType.QUANTITY
    val todayUnits = if (isQuantity) goal.count else if (goal.checked) 1 else 0
    val target = goal.version.target
    val progress = (period?.progress ?: 0) - (period?.contributionOn(date) ?: 0) + todayUnits
    val done = progress >= target
    val periodName = if (goal.version.cadence == Cadence.WEEKLY) "this week" else "this month"
    val daysLeft = period?.daysLeft(date) ?: 0
    val unit = goal.version.unit?.let { " $it" } ?: if (isQuantity) "" else if (target == 1) " time" else " times"
    val checkScale by animateFloatAsState(
        targetValue = if (goal.checked) 1f else 0.001f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "check",
    )
    val bar by animateFloatAsState((progress.toFloat() / target).coerceIn(0f, 1f), spring(stiffness = Spring.StiffnessLow), label = "bar")

    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (done) MaterialTheme.ember.perfect.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainer)
            .combinedClickable(
                enabled = editable,
                onClick = { if (isQuantity) onCountChange(goal.count + 1) else onToggle() },
                onLongClick = onLongPress,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Text(goal.version.emoji, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.version.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    buildString {
                        append("$progress / $target$unit $periodName")
                        if (done) append(" · done!") else if (daysLeft > 0) append(" · $daysLeft day${if (daysLeft == 1) "" else "s"} left") else append(" · last day")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (done) MaterialTheme.ember.perfect else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            if (isQuantity) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onCountChange((goal.count - 1).coerceAtLeast(0)) }, enabled = editable && goal.count > 0, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Minus one", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                        Text("${goal.count}", style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { onCountChange(goal.count + 1) }, enabled = editable, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = "Plus one", tint = color)
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (goal.checked) color else Color.Transparent)
                            .border(2.5.dp, if (goal.checked) color else MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.scale(checkScale)) }
                    Text(if (goal.checked) "logged" else "log today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { bar },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = if (done) MaterialTheme.ember.perfect else color,
            trackColor = color.copy(alpha = 0.15f),
            drawStopIndicator = {},
        )
    }
}
