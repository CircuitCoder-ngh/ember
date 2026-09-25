package com.nhowe.ember.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nhowe.ember.domain.model.WeeklyRecap
import com.nhowe.ember.ui.theme.ember
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** The week in one card. [onDismiss] is null when shown in Progress, where it just lives. */
@Composable
fun WeeklyRecapCard(recap: WeeklyRecap, modifier: Modifier = Modifier, title: String = "Your week", onDismiss: (() -> Unit)? = null) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(scheme.primary.copy(alpha = 0.22f), scheme.tertiary.copy(alpha = 0.18f))))
            .background(scheme.surfaceContainer.copy(alpha = 0.6f))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${recap.weekStart.format(DateTimeFormatter.ofPattern("MMM d"))} – ${recap.weekEnd.format(DateTimeFormatter.ofPattern("MMM d"))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Text(
                recap.score?.let { "${(it * 100).roundToInt()}%" } ?: "—",
                style = MaterialTheme.typography.displaySmall,
                color = scheme.primary,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat(recap.bestDay?.dayOfWeek?.getDisplayName(TextStyle.SHORT, Locale.getDefault()) ?: "—", recap.bestDayScore?.let { "best day · ${(it * 100).roundToInt()}%" } ?: "best day")
            Stat("${recap.goalsDone}/${recap.goalsTotal}", "goals nailed")
            Stat("${recap.perfectDays}", "perfect days")
            Stat("+${recap.xpEarned}", "XP earned")
        }
        Spacer(Modifier.height(12.dp))
        Text(recap.observation, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
        if (recap.questsCompleted > 0) {
            Spacer(Modifier.height(4.dp))
            Text("${recap.questsCompleted} of 2 quests completed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ember.xp)
        }
        if (onDismiss != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Got it") }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
