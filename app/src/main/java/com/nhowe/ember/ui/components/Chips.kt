package com.nhowe.ember.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nhowe.ember.domain.model.StreakState
import com.nhowe.ember.domain.model.XpState
import com.nhowe.ember.ui.theme.EmberOrange
import com.nhowe.ember.ui.theme.ember

@Composable
fun StreakChip(streak: StreakState, modifier: Modifier = Modifier) {
    var last by remember { mutableStateOf(streak.current) }
    var pop by remember { mutableStateOf(false) }
    LaunchedEffect(streak.current) {
        if (streak.current > last) pop = true
        last = streak.current
    }
    val scale by animateFloatAsState(
        targetValue = if (pop) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        finishedListener = { pop = false },
        label = "pop",
    )
    val active = streak.current > 0
    val tint = if (active) EmberOrange else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier
            .scale(scale)
            .clip(CircleShape)
            .background(if (active) EmberOrange.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = tint)
        Spacer(Modifier.width(6.dp))
        Text(
            text = when {
                streak.current == 0 -> "Start a streak"
                streak.current == 1 -> "1 day streak"
                else -> "${streak.current} day streak"
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (streak.freezesHeld > 0) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.AcUnit, contentDescription = "Streak freezes", tint = MaterialTheme.ember.frozen, modifier = Modifier.width(16.dp))
            Spacer(Modifier.width(2.dp))
            Text("×${streak.freezesHeld}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.ember.frozen)
        }
    }
}

@Composable
fun XpBar(xp: XpState, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(xp.levelProgress, spring(stiffness = Spring.StiffnessLow), label = "xp")
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.clip(CircleShape).background(MaterialTheme.ember.xp).padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text("LVL ${xp.level}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Text(xp.levelTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Text("${xp.xpIntoLevel} / ${xp.xpForNextLevel} XP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = MaterialTheme.ember.xp,
            trackColor = MaterialTheme.ember.xp.copy(alpha = 0.18f),
            drawStopIndicator = {},
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onSettings: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            if (subtitle != null) {
                Text(subtitle.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        trailing()
        if (onSettings != null) {
            IconButton(onClick = onSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
fun EmberCard(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}
