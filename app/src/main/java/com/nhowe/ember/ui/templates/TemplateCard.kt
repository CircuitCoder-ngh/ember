package com.nhowe.ember.ui.templates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Check
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
import com.nhowe.ember.data.templates.GoalTemplate
import com.nhowe.ember.data.templates.TemplateGoal
import com.nhowe.ember.ui.theme.goalColor

/** One bundle: title, blurb, goal count, a select toggle, and an expandable list of its goals. */
@Composable
fun TemplateCard(
    template: GoalTemplate,
    selected: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, if (selected) accent.copy(alpha = 0.5f) else Color.Transparent, MaterialTheme.shapes.medium)
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
                    "${template.goals.size} goals · ${template.dailyCount} daily",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onExpand, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.ExpandMore, contentDescription = "Details", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.rotate(if (expanded) 180f else 0f))
            }
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(if (selected) accent else MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable(onClick = onToggle)
                    .padding(horizontal = if (actionLabel != null) 12.dp else 0.dp)
                    .size(if (actionLabel != null) 0.dp else 30.dp),
                contentAlignment = Alignment.Center,
            ) { if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(18.dp)) }
            if (actionLabel != null) {
                Box(
                    Modifier.clip(CircleShape).background(accent).clickable(onClick = onToggle).padding(horizontal = 14.dp, vertical = 8.dp),
                ) { Text(actionLabel, style = MaterialTheme.typography.labelLarge, color = Color.White) }
            }
        }
        Text(template.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(top = 8.dp)) {
                template.goals.forEach { g -> GoalLine(g) }
            }
        }
    }
}

@Composable
private fun GoalLine(g: TemplateGoal) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(goalColor(g.colorIndex).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Text(g.emoji, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.width(8.dp))
        Text(g.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            when (g.cadence) {
                "WEEKLY" -> if (g.type == "QUANTITY") "${g.target} ${g.unit ?: ""}/week".trim() else "${g.target}× a week"
                "MONTHLY" -> if (g.type == "QUANTITY") "${g.target} ${g.unit ?: ""}/month".trim() else "${g.target}× a month"
                else -> when {
                    g.type == "QUANTITY" -> "${g.target} ${g.unit ?: ""}".trim()
                    g.weekdays == 31 -> "weekdays"
                    g.weekdays == 96 -> "weekends"
                    else -> "daily"
                }
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun StackingBanner(text: String?) {
    if (text == null) return
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f))
            .padding(12.dp),
    )
    Spacer(Modifier.height(2.dp))
}
