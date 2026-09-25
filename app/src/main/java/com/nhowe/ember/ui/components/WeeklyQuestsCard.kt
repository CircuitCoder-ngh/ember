package com.nhowe.ember.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nhowe.ember.domain.model.WeeklyQuest
import com.nhowe.ember.ui.theme.ember
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** This week's two quests with progress bars and their XP reward. */
@Composable
fun WeeklyQuestsCard(quests: List<WeeklyQuest>, today: LocalDate, modifier: Modifier = Modifier) {
    if (quests.isEmpty()) return
    val xpColor = MaterialTheme.ember.xp
    val daysLeft = ChronoUnit.DAYS.between(today, quests.first().weekStart.plusDays(6)).toInt().coerceAtLeast(0)
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("This week's quests", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                when (daysLeft) { 0 -> "ends today"; 1 -> "1 day left"; else -> "$daysLeft days left" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        quests.forEachIndexed { i, q ->
            if (i > 0) Spacer(Modifier.height(12.dp))
            val progress by animateFloatAsState(q.fraction, spring(stiffness = Spring.StiffnessLow), label = "quest")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(if (q.isComplete) xpColor else xpColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (q.isComplete) Icon(Icons.Rounded.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(18.dp))
                    else Text(q.emoji.ifEmpty { "◆" }, style = MaterialTheme.typography.labelMedium, color = xpColor)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(q.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    Text(
                        if (q.isComplete) "Done · +${q.xpReward} XP" else "${q.progress} / ${q.target} · +${q.xpReward} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (q.isComplete) xpColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                color = xpColor,
                trackColor = xpColor.copy(alpha = 0.15f),
                drawStopIndicator = {},
            )
        }
    }
}
