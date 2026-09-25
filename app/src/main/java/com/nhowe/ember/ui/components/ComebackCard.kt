package com.nhowe.ember.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nhowe.ember.domain.model.ComebackQuest
import com.nhowe.ember.ui.theme.ember

/** Shown on Today while a comeback quest is open. Three dots, one per day on target. */
@Composable
fun ComebackCard(quest: ComebackQuest, todaySecured: Boolean, modifier: Modifier = Modifier) {
    val hit = MaterialTheme.ember.hit
    val filled = quest.progress
    val left = quest.target - filled
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(hit.copy(alpha = 0.10f))
            .border(1.dp, hit.copy(alpha = 0.35f), MaterialTheme.shapes.large)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Comeback quest", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    when {
                        filled == 0 && !todaySecured -> "Lost a ${quest.brokenStreak}-day streak. Hit your bar 3 days in a row to earn a freeze back."
                        left == 1 -> "One more day on target and the freeze is yours."
                        else -> "$left more days on target and the freeze is yours."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Rounded.AcUnit, contentDescription = null, tint = MaterialTheme.ember.frozen)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(quest.target) { i ->
                val done = i < filled
                val isToday = i == filled && !todaySecured
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (done) hit else Color.Transparent)
                        .border(2.dp, if (done || isToday) hit else MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    else Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = if (isToday) hit else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                if (todaySecured) "Today counted" else "Today counts at your bar",
                style = MaterialTheme.typography.labelSmall,
                color = if (todaySecured) hit else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}
