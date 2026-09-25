package com.nhowe.ember.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.ui.unit.dp
import com.nhowe.ember.domain.model.ProgramProgress
import com.nhowe.ember.ui.theme.ember
import kotlin.math.roundToInt

/** "Week 3 of 9" with a bar; strict programs show their unbroken day count instead. */
@Composable
fun ProgramCard(p: ProgramProgress, modifier: Modifier = Modifier, onMenu: (() -> Unit)? = null, onClick: (() -> Unit)? = null) {
    val accent = MaterialTheme.colorScheme.tertiary
    val bar by animateFloatAsState(p.fraction, spring(stiffness = Spring.StiffnessLow), label = "program")
    val strict = p.program.strict
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(if (p.isPaused) MaterialTheme.ember.frozen.copy(alpha = 0.10f) else accent.copy(alpha = 0.12f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Text(p.program.emoji, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.program.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    buildString {
                        if (p.isPaused) append("Paused · day ${p.dayIndex} of ${p.program.lengthDays} so far")
                        else if (p.dayIndex == 0) append("Starts ${p.program.startDate.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d"))} · ${p.program.totalWeeks} weeks")
                        else if (strict && !p.isPaused) append("Day ${p.strictDay} unbroken · calendar day ${p.dayIndex} of ${p.program.lengthDays}")
                        else if (!p.isPaused) append("Week ${p.week} of ${p.program.totalWeeks} · day ${p.dayIndex} of ${p.program.lengthDays}")
                        if (p.dayIndex > 0 && !p.isPaused) p.adherence?.let { append(" · ${(it * 100).roundToInt()}%") }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onMenu != null) IconButton(onClick = onMenu, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Program options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { if (strict) (p.strictDay.toFloat() / p.program.lengthDays).coerceIn(0f, 1f) else bar },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = if (strict) MaterialTheme.ember.miss else accent,
            trackColor = accent.copy(alpha = 0.18f),
            drawStopIndicator = {},
        )
        if (p.isPaused) {
            Text(
                "Paused since ${p.program.openPause!!.from.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))}. Its goals are off your plan; resume from the menu when you're back.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ember.frozen, modifier = Modifier.padding(top = 6.dp),
            )
        } else if (strict && p.strictBrokenOn != null && p.strictDay == 0) {
            Text(
                "Missed a goal on ${p.strictBrokenOn}. Strict rules: the count restarts. Use the menu to start over from today.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ember.miss, modifier = Modifier.padding(top = 6.dp),
            )
        } else if (p.dayIndex == 0) {
            Text("Its goals appear on your plan from day one.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        } else if (p.daysLeft > 0) {
            Text(
                if (p.daysLeft == 1) "Last day tomorrow." else "${p.daysLeft} days to go.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp),
            )
        } else {
            Text("Final day. Finish strong.", style = MaterialTheme.typography.labelSmall, color = accent, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
