package com.nhowe.ember.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nhowe.ember.domain.model.GoalType
import com.nhowe.ember.domain.model.ResolvedGoal
import com.nhowe.ember.ui.theme.goalColor

/**
 * One goal on a day. Check goals toggle on tap; quantity goals step on tap (+1) and via the
 * stepper. Long-press opens the row's actions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GoalRow(
    goal: ResolvedGoal,
    modifier: Modifier = Modifier,
    editable: Boolean = true,
    onToggle: () -> Unit,
    onCountChange: (Int) -> Unit,
    onLongPress: () -> Unit,
) {
    val color = goalColor(goal.version.colorIndex)
    val done = goal.isDone
    val rowAlpha by animateFloatAsState(if (done) 0.62f else 1f, label = "alpha")
    val checkScale by animateFloatAsState(
        targetValue = if (done) 1f else 0.001f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "check",
    )
    val bg by animateColorAsState(
        if (done) color.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainer,
        label = "bg",
    )
    val isQuantity = goal.version.type == GoalType.QUANTITY

    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(bg)
            .combinedClickable(
                enabled = editable,
                onClick = { if (isQuantity) onCountChange(goal.count + 1) else onToggle() },
                onLongClick = onLongPress,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(goal.version.emoji, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    goal.version.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = rowAlpha),
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = when {
                    isQuantity -> "${goal.count} / ${goal.version.target}${goal.version.unit?.let { " $it" } ?: ""}"
                    goal.version.note != null -> goal.version.note
                    else -> null
                }
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = rowAlpha),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            if (isQuantity) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onCountChange((goal.count - 1).coerceAtLeast(0)) }, enabled = editable && goal.count > 0, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Minus one", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(if (done) color else color.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(Icons.Rounded.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.scale(checkScale))
                        else Text("${goal.count}", style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { onCountChange(goal.count + 1) }, enabled = editable, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = "Plus one", tint = color)
                    }
                }
            } else {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (done) color else Color.Transparent)
                        .border(2.5.dp, if (done) color else MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.scale(checkScale))
                }
            }
        }
        if (isQuantity) {
            Spacer(Modifier.height(10.dp))
            val progress by animateFloatAsState(goal.credit.toFloat(), spring(stiffness = Spring.StiffnessLow), label = "bar")
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.15f),
                drawStopIndicator = {},
            )
        }
    }
}
