package com.nhowe.ember.ui.templates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nhowe.ember.data.templates.ProgramTemplate
import com.nhowe.ember.ui.theme.goalColor

/** A program in the library: length, goals, graduation reward, and a Start button. */
@Composable
fun ProgramTemplateCard(
    template: ProgramTemplate,
    expanded: Boolean,
    enrolled: Boolean,
    onExpand: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    onExport: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    val accent = MaterialTheme.colorScheme.tertiary
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onExpand)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Text(template.emoji, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(template.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${template.weeks} weeks · ${template.goals.size} goal${if (template.goals.size == 1) "" else "s"} · ${template.graduation.icon} ${template.graduation.title} +${template.graduation.xp} XP" + (if (template.strict) " · strict" else "") + (if (template.custom) " · imported" else "") + (if (template.season != null) " · seasonal" else ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onExpand, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.ExpandMore, contentDescription = "Details", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.rotate(if (expanded) 180f else 0f))
            }
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier.clip(CircleShape).background(if (enrolled) MaterialTheme.colorScheme.surfaceContainerHighest else accent)
                    .clickable(enabled = !enrolled, onClick = onStart).padding(horizontal = 14.dp, vertical = 8.dp),
            ) { Text(if (enrolled) "Active" else "Start", style = MaterialTheme.typography.labelLarge, color = if (enrolled) MaterialTheme.colorScheme.onSurfaceVariant else Color.White) }
        }
        Text(template.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(top = 8.dp)) {
                template.goals.forEach { g ->
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(goalColor(g.colorIndex).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Text(g.emoji, style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(g.title, style = MaterialTheme.typography.bodyMedium)
                            if (g.phases.size > 1) Text("${g.phases.size} phases: ${g.phases.first().note ?: g.phases.first().title ?: "start"} → ${g.phases.last().note ?: g.phases.last().title ?: "finish"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                        Text(
                            when (g.cadence) { "WEEKLY" -> "${g.target}× a week"; "MONTHLY" -> "${g.target}× a month"; else -> if (g.weekdays == 21) "Mon/Wed/Fri" else if (g.weekdays == 96) "weekends" else "daily" },
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (template.strict) Text("Strict: a missed day restarts the count at day 1.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.padding(top = 4.dp)) {
                    if (onExport != null) androidx.compose.material3.TextButton(onClick = onExport) { Text("Share as file") }
                    if (onRemove != null) androidx.compose.material3.TextButton(onClick = onRemove) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}
