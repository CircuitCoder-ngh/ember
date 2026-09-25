package com.nhowe.ember.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhowe.ember.core.time.endOfMonth
import com.nhowe.ember.core.time.endOfWeek
import com.nhowe.ember.core.time.startOfMonth
import com.nhowe.ember.core.time.startOfWeek
import com.nhowe.ember.di.LocalAppContainer
import com.nhowe.ember.domain.engine.Scoring
import com.nhowe.ember.domain.model.Badge
import com.nhowe.ember.domain.model.BadgeGroup
import com.nhowe.ember.domain.model.DayState
import com.nhowe.ember.domain.model.EngineSnapshot
import com.nhowe.ember.domain.model.GoalKind
import com.nhowe.ember.ui.components.ScreenHeader
import com.nhowe.ember.ui.components.SectionLabel
import com.nhowe.ember.ui.components.WeeklyRecapCard
import com.nhowe.ember.ui.components.ProgramCard
import com.nhowe.ember.domain.engine.RecapEngine
import com.nhowe.ember.domain.model.Unlocks
import com.nhowe.ember.domain.model.FlameForm
import com.nhowe.ember.ui.goals.GoalsViewModel
import com.nhowe.ember.ui.theme.EmberOrange
import com.nhowe.ember.ui.theme.ember
import com.nhowe.ember.ui.theme.goalColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(onOpenSettings: () -> Unit) {
    val container = LocalAppContainer.current
    val snapshot by container.engineStore.snapshot.collectAsStateWithLifecycle()
    val snap = snapshot ?: return
    val today = snap.today

    val week = remember(snap) { Scoring.periodStats(snap.plans, today.startOfWeek(), today.endOfWeek(), today, snap.periods) }
    val month = remember(snap) { Scoring.periodStats(snap.plans, today.startOfMonth(), today.endOfMonth(), today, snap.periods) }
    val all = remember(snap) { Scoring.periodStats(snap.plans, LocalDate.MIN.plusDays(1), LocalDate.MAX.minusDays(1), today, snap.periods) }
    val weeklySeries = remember(snap) { weeklyScores(snap, 12) }
    val daily = remember(snap) { (29 downTo 0).map { today.minusDays(it.toLong()) }.map { d -> snap.plans[d]?.score } }
    val goalRates = remember(snap) { goalRates(snap) }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        ScreenHeader(title = "Progress", subtitle = "The long game", onSettings = onOpenSettings)

        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(Modifier.weight(1f), pct(week.score), "this week", MaterialTheme.colorScheme.primary)
            Tile(Modifier.weight(1f), pct(month.score), "this month", MaterialTheme.ember.hit)
            Tile(Modifier.weight(1f), pct(all.score), "all time", MaterialTheme.ember.xp)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(Modifier.weight(1f), "${snap.streak.current}", "streak · best ${snap.streak.best}", EmberOrange)
            Tile(Modifier.weight(1f), "${snap.dayStates.values.count { it == DayState.PERFECT }}", "perfect days", MaterialTheme.ember.perfect)
            Tile(Modifier.weight(1f), "${snap.xp.totalXp}", "XP · level ${snap.xp.level}", MaterialTheme.ember.xp)
        }

        val nextUnlock = remember(snap.xp.level) { Unlocks.next(snap.xp.level) }
        Text(
            buildString {
                append("${FlameForm.forLevel(snap.xp.level).title} form")
                nextUnlock?.let { append(" · next unlock at level ${it.level}: ${it.title}") }
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        if (snap.activePrograms.isNotEmpty()) {
            SectionLabel("Programs", Modifier.padding(top = 8.dp))
            snap.activePrograms.forEach { p -> ProgramCard(p = p, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
        }
        val lastWeek = remember(snap) { RecapEngine.recap(today.startOfWeek().minusWeeks(1), snap) }
        if (lastWeek != null) {
            SectionLabel("Last week", Modifier.padding(top = 8.dp))
            WeeklyRecapCard(recap = lastWeek, modifier = Modifier.padding(horizontal = 16.dp))
        }
        val questsDone = snap.allQuests.count { it.isComplete }
        if (snap.allQuests.isNotEmpty()) {
            Text("Quests completed: $questsDone of ${snap.allQuests.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        }

        SectionLabel("Last 12 weeks", Modifier.padding(top = 16.dp))
        Card { WeeklyBars(weeklySeries) }

        SectionLabel("Last 30 days", Modifier.padding(top = 8.dp))
        Card { Sparkline(daily) }

        if (goalRates.isNotEmpty()) {
            SectionLabel("Goals, last 30 days", Modifier.padding(top = 8.dp))
            Card {
                goalRates.forEach { (label, colorIndex, rate) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.width(110.dp).height(8.dp).clip(CircleShape).background(goalColor(colorIndex).copy(alpha = 0.15f))) {
                            Box(Modifier.fillMaxWidth(rate.toFloat().coerceIn(0f, 1f)).height(8.dp).background(goalColor(colorIndex)))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("${(rate * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                    }
                }
            }
        }

        if (snap.programBadges.isNotEmpty()) {
            SectionLabel("Program badges · ${snap.programBadges.size}", Modifier.padding(top = 8.dp))
            FlowRow(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                snap.programBadges.forEach { b ->
                    Column(
                        Modifier.width(104.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer).padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.ember.perfect.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Text(b.icon, style = MaterialTheme.typography.titleLarge) }
                        Spacer(Modifier.height(6.dp))
                        Text(b.title, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(b.earnedOn.format(DateTimeFormatter.ofPattern("MMM d")), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        SectionLabel("Badges · ${snap.badges.size} of ${Badge.entries.size}", Modifier.padding(top = 8.dp))
        BadgeGroup.entries.forEach { group ->
            FlowRow(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge.entries.filter { it.group == group }.forEach { badge ->
                    BadgeTile(badge, snap.badges[badge])
                }
            }
        }
    }
}

private fun pct(v: Double?): String = v?.let { "${(it * 100).roundToInt()}%" } ?: "—"

@Composable
private fun Tile(modifier: Modifier, value: String, label: String, accent: Color) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = accent)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) { content() }
}

private data class WeekPoint(val start: LocalDate, val score: Double?)

private fun weeklyScores(snap: EngineSnapshot, weeks: Int): List<WeekPoint> {
    val thisWeek = snap.today.startOfWeek()
    return (weeks - 1 downTo 0).map { i ->
        val start = thisWeek.minusWeeks(i.toLong())
        WeekPoint(start, Scoring.periodScore(snap.plans, start, start.plusDays(6), snap.today, snap.periods))
    }
}

private fun goalRates(snap: EngineSnapshot): List<Triple<String, Int, Double>> {
    val from = snap.today.minusDays(29)
    return snap.history.goals
        .filter { it.kind == GoalKind.RECURRING && !it.isArchived }
        .mapNotNull { g ->
            val v = GoalsViewModel.currentVersion(snap.history, g.id) ?: return@mapNotNull null
            val rate = if (v.isPeriodic) {
                snap.periods.filter { it.goal.id == g.id && it.end >= from && it.start <= snap.today }.map { it.credit }
                    .takeIf { it.isNotEmpty() }?.average() ?: return@mapNotNull null
            } else Scoring.goalRate(snap.plans, g.id, from, snap.today, snap.today) ?: return@mapNotNull null
            Triple("${v.emoji} ${v.title}", v.colorIndex, rate)
        }
        .sortedBy { it.third }
}

@Composable
private fun WeeklyBars(points: List<WeekPoint>) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.ember.ringTrack
    val perfect = MaterialTheme.ember.perfect
    Column {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val n = points.size
            val gap = 6.dp.toPx()
            val barW = (size.width - gap * (n - 1)) / n
            points.forEachIndexed { i, p ->
                val x = i * (barW + gap)
                drawRoundRect(track, Offset(x, 0f), Size(barW, size.height), CornerRadius(barW / 3))
                val s = p.score
                if (s != null) {
                    val h = (size.height * s).toFloat().coerceAtLeast(6f)
                    drawRoundRect(if (s >= 0.999) perfect else primary, Offset(x, size.height - h), Size(barW, h), CornerRadius(barW / 3))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val fmt = DateTimeFormatter.ofPattern("MMM d")
            Text(points.first().start.format(fmt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("this week", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Sparkline(values: List<Double?>) {
    val line = MaterialTheme.ember.hit
    val dot = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.ember.ringTrack
    Canvas(Modifier.fillMaxWidth().height(90.dp)) {
        val n = values.size
        val stepX = size.width / (n - 1).coerceAtLeast(1)
        // guide lines at 50% and 100%
        drawLine(track, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
        drawLine(track, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.dp.toPx())
        drawLine(track, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        val path = Path()
        var started = false
        values.forEachIndexed { i, v ->
            if (v == null) { started = false; return@forEachIndexed }
            val p = Offset(i * stepX, (size.height * (1 - v)).toFloat())
            if (!started) { path.moveTo(p.x, p.y); started = true } else path.lineTo(p.x, p.y)
        }
        drawPath(path, line, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        values.forEachIndexed { i, v ->
            if (v != null) drawCircle(dot, 3.dp.toPx(), Offset(i * stepX, (size.height * (1 - v)).toFloat()))
        }
    }
}

@Composable
private fun BadgeTile(badge: Badge, earnedOn: LocalDate?) {
    val earned = earnedOn != null
    Column(
        Modifier
            .width(104.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(10.dp)
            .alpha(if (earned) 1f else 0.45f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(if (earned) MaterialTheme.ember.perfect.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) { Text(if (earned) badge.icon else "🔒", style = MaterialTheme.typography.titleLarge) }
        Spacer(Modifier.height(6.dp))
        Text(badge.title, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            earnedOn?.format(DateTimeFormatter.ofPattern("MMM d")) ?: badge.blurb,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
