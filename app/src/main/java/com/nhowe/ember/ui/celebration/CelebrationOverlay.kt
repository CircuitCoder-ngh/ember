package com.nhowe.ember.ui.celebration

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nhowe.ember.domain.model.CelebrationEvent
import com.nhowe.ember.domain.model.EngineSnapshot
import com.nhowe.ember.domain.model.FlameForm
import com.nhowe.ember.domain.model.Unlocks
import com.nhowe.ember.ui.components.ConfettiOverlay
import com.nhowe.ember.ui.components.ConfettiTrigger
import com.nhowe.ember.ui.components.FlameMascot
import com.nhowe.ember.ui.components.FlameState
import com.nhowe.ember.ui.theme.ember

/** Full-screen moment for milestones, level-ups, perfect days and badges. Tap anywhere to continue. */
@Composable
fun CelebrationOverlay(event: CelebrationEvent, snapshot: EngineSnapshot, onDismiss: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    var confetti by remember { mutableStateOf<ConfettiTrigger?>(null) }
    LaunchedEffect(event.key) {
        shown = true
        confetti = ConfettiTrigger(System.nanoTime(), count = 180, originY = 0.25f)
    }
    val flameScale by animateFloatAsState(if (shown) 1f else 0.3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "flame")
    val textScale by animateFloatAsState(if (shown) 1f else 0.7f, spring(dampingRatio = Spring.DampingRatioLowBouncy), label = "text")

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            FlameMascot(state = FlameState.BLAZING, size = 160.dp, modifier = Modifier.scale(flameScale), form = FlameForm.forLevel(snapshot.xp.level), skin = snapshot.settings.flameSkin)
            Spacer(Modifier.height(20.dp))
            when (event) {
                is CelebrationEvent.StreakMilestone -> {
                    val days by animateIntAsState(if (shown) event.days else 0, tween(900), label = "days")
                    Text("$days", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.scale(textScale))
                    Text("day streak", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(10.dp))
                    Text(milestoneLine(event.days), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    if (event.freezeGranted) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            Modifier.clip(CircleShape).background(MaterialTheme.ember.frozen.copy(alpha = 0.18f)).padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.AcUnit, contentDescription = null, tint = MaterialTheme.ember.frozen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("+1 Streak Freeze earned", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.ember.frozen)
                        }
                    }
                }
                is CelebrationEvent.PerfectDay -> {
                    Text("Perfect day", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.ember.perfect, modifier = Modifier.scale(textScale), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text("Every single goal. That's the best version of you, today.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text("+${snapshot.xp.todayXp} XP today", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.ember.xp)
                }
                is CelebrationEvent.LevelUp -> {
                    Text("Level ${event.level}", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.ember.xp, modifier = Modifier.scale(textScale))
                    Text(event.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(10.dp))
                    Text("${snapshot.xp.totalXp} XP and climbing.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    val unlocks = Unlocks.atLevel(event.level)
                    if (unlocks.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        unlocks.forEach { u ->
                            Box(Modifier.padding(vertical = 3.dp).clip(CircleShape).background(MaterialTheme.ember.perfect.copy(alpha = 0.18f)).padding(horizontal = 14.dp, vertical = 6.dp)) {
                                Text("Unlocked: ${u.title}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.ember.perfect)
                            }
                        }
                        Text("Pick it in Settings › Style", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is CelebrationEvent.ProgramGraduated -> {
                    Box(Modifier.size(84.dp).clip(CircleShape).background(MaterialTheme.ember.perfect.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Text(event.badgeIcon, style = MaterialTheme.typography.displaySmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Graduated", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.ember.perfect, modifier = Modifier.scale(textScale), textAlign = TextAlign.Center)
                    Text(event.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text("Badge earned: ${event.badgeTitle} · +${event.xp} XP", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    if (event.nextTemplateId != null) {
                        Spacer(Modifier.height(8.dp))
                        Text("What's next? There's a follow-on program waiting under Goals › Browse bundles › Programs.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
                is CelebrationEvent.QuestComplete -> {
                    Text("Quest complete", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.ember.xp, modifier = Modifier.scale(textScale), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text(event.quest.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text("+${event.quest.xpReward} XP", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.ember.xp)
                }
                is CelebrationEvent.ComebackComplete -> {
                    Text("Comeback complete", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.ember.hit, modifier = Modifier.scale(textScale), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Three days on target after losing a ${event.brokenStreak}-day streak. That's the hard part, and you did it.",
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier.clip(CircleShape).background(MaterialTheme.ember.frozen.copy(alpha = 0.18f)).padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.AcUnit, contentDescription = null, tint = MaterialTheme.ember.frozen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (event.freezeGranted) "+1 Streak Freeze earned back" else "Freezes already full. Nice.", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.ember.frozen)
                    }
                }
                is CelebrationEvent.BadgeEarned -> {
                    Box(Modifier.size(84.dp).clip(CircleShape).background(MaterialTheme.ember.perfect.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Text(event.badge.icon, style = MaterialTheme.typography.displaySmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(event.badge.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.scale(textScale), textAlign = TextAlign.Center)
                    Text(event.badge.blurb, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(32.dp))
            Button(onClick = onDismiss) { Text("Keep going", color = Color.White) }
        }
        ConfettiOverlay(trigger = confetti)
    }
}

private fun milestoneLine(days: Int): String = when {
    days >= 365 -> "A whole year. There's no one like you."
    days >= 100 -> "Triple digits. This is who you are now."
    days >= 30 -> "A full month. The habit is real."
    days >= 14 -> "Two weeks straight. Momentum is yours."
    days >= 7 -> "One week. The flame is officially lit."
    else -> "Three in a row. Keep feeding it."
}
