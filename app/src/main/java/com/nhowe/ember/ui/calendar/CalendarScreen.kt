package com.nhowe.ember.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhowe.ember.di.LocalAppContainer
import com.nhowe.ember.domain.engine.DayPlanResolver
import com.nhowe.ember.domain.engine.PeriodResolver
import com.nhowe.ember.domain.engine.Scoring
import com.nhowe.ember.domain.model.DayState
import com.nhowe.ember.domain.model.EngineSnapshot
import com.nhowe.ember.domain.model.GoalKind
import com.nhowe.ember.domain.model.GoalVersion
import com.nhowe.ember.ui.goals.GoalsViewModel
import com.nhowe.ember.ui.theme.goalColor
import com.nhowe.ember.ui.components.ScreenHeader
import com.nhowe.ember.ui.theme.ember
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun CalendarScreen(onOpenSettings: () -> Unit, onNewOneOff: (LocalDate) -> Unit) {
    val container = LocalAppContainer.current
    val vm: CalendarViewModel = viewModel { CalendarViewModel(container) }
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    val snap = snapshot ?: return

    val thisMonth = YearMonth.from(snap.today)
    val firstMonth = remember(snap.history.firstDate) { YearMonth.from(snap.history.firstDate ?: snap.today).let { if (it > thisMonth) thisMonth else it } }
    val lastMonth = thisMonth.plusMonths(1)
    val pageCount = (ChronoUnit.MONTHS.between(firstMonth, lastMonth) + 1).toInt()
    val todayPage = ChronoUnit.MONTHS.between(firstMonth, thisMonth).toInt()
    val pager = rememberPagerState(initialPage = todayPage) { pageCount }
    val scope = rememberCoroutineScope()
    val upcoming = remember(snap.history, snap.today) { upcomingOneOffs(snap) }
    val hugeDates = remember(upcoming) { upcoming.filter { it.version.weight >= 3 }.map { it.date }.toSet() }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        ScreenHeader(title = "Calendar", subtitle = "Every day tells a story", onSettings = onOpenSettings)

        val month = firstMonth.plusMonths(pager.currentPage.toLong())
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { scope.launch { pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0)) } }, enabled = pager.currentPage > 0) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = { scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(pageCount - 1)) } }, enabled = pager.currentPage < pageCount - 1) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month")
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            DayOfWeek.entries.forEach { d ->
                Text(
                    d.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth(), beyondViewportPageCount = 1) { page ->
            MonthGrid(month = firstMonth.plusMonths(page.toLong()), snapshot = snap, hugeDates = hugeDates, onDayClick = { selected = it })
        }

        MonthSummary(month = month, snapshot = snap)
        Legend()
        UpcomingSection(upcoming, today = snap.today, onClick = { selected = it })
        Spacer(Modifier.height(24.dp))
    }

    selected?.let { date ->
        DayDetailSheet(
            date = date,
            plan = vm.planFor(snap, date),
            state = snap.dayStates[date] ?: if (date > snap.today) DayState.FUTURE else null,
            today = snap.today,
            onDismiss = { selected = null },
            onToggle = { vm.toggle(it, date) },
            onCountChange = { goal, count -> vm.setCount(goal, count, date) },
            onSkip = { id, skipped -> vm.setSkipped(id, date, skipped) },
            onNewOneOff = { selected = null; onNewOneOff(date) },
            periodFor = { goalId -> PeriodResolver.progressFor(goalId, date, snap.history) },
        )
    }
}

@Composable
private fun MonthGrid(month: YearMonth, snapshot: EngineSnapshot, hugeDates: Set<LocalDate>, onDayClick: (LocalDate) -> Unit) {
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value - 1 // Monday = 0
    val days = month.lengthOfMonth()
    val cells = leading + days
    val rows = (cells + 6) / 7
    val futurePlans = remember(month, snapshot.history) {
        if (month.atEndOfMonth() > snapshot.today) {
            DayPlanResolver.resolveRange(maxOf(first, snapshot.today.plusDays(1)), month.atEndOfMonth(), snapshot.history)
        } else emptyMap()
    }
    Column(Modifier.padding(horizontal = 12.dp)) {
        repeat(rows) { r ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { c ->
                    val index = r * 7 + c
                    val day = index - leading + 1
                    if (day in 1..days) {
                        val date = month.atDay(day)
                        val plan = snapshot.plans[date] ?: futurePlans[date]
                        DayCell(
                            day = day,
                            state = snapshot.dayStates[date] ?: if (date > snapshot.today) DayState.FUTURE else null,
                            score = plan?.score,
                            isToday = date == snapshot.today,
                            plannedCount = plan?.goals?.size ?: 0,
                            onClick = { onDayClick(date) },
                            modifier = Modifier.weight(1f),
                            starred = date in hugeDates,
                        )
                    } else {
                        Box(Modifier.weight(1f).height(1.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSummary(month: YearMonth, snapshot: EngineSnapshot) {
    val stats = remember(month, snapshot) {
        Scoring.periodStats(snapshot.plans, month.atDay(1), month.atEndOfMonth(), snapshot.today, snapshot.periods)
    }
    val states = remember(month, snapshot) { snapshot.dayStates.filterKeys { YearMonth.from(it) == month } }
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Stat(stats.score?.let { "${(it * 100).roundToInt()}%" } ?: "—", "month score")
        Stat("${states.values.count { it == DayState.PERFECT }}", "perfect days")
        Stat("${states.values.count { it == DayState.HIT || it == DayState.PERFECT }}", "days on target")
        Stat("${stats.goalsDone}", "goals done")
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Legend() {
    val c = MaterialTheme.ember
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        LegendDot(c.perfect, "Perfect")
        LegendDot(c.hit, "On target")
        LegendDot(c.miss, "Missed")
        LegendDot(c.frozen, "Frozen")
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(10.dp).height(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A one-off goal on a day that hasn't passed yet. */
data class UpcomingOneOff(val goalId: String, val date: LocalDate, val version: GoalVersion)

private fun upcomingOneOffs(snap: EngineSnapshot): List<UpcomingOneOff> = snap.history.goals
    .filter { it.kind == GoalKind.ONE_OFF && !it.isArchived && it.oneOffDate != null && it.oneOffDate >= snap.today }
    .mapNotNull { g -> GoalsViewModel.currentVersion(snap.history, g.id)?.let { UpcomingOneOff(g.id, g.oneOffDate!!, it) } }
    .sortedWith(compareBy({ it.date }, { -it.version.weight }))

@Composable
private fun UpcomingSection(items: List<UpcomingOneOff>, today: LocalDate, onClick: (LocalDate) -> Unit) {
    if (items.isEmpty()) return
    Column(Modifier.padding(top = 18.dp)) {
        Text(
            "COMING UP",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        )
        items.take(8).forEach { item ->
            val huge = item.version.weight >= 3
            val color = goalColor(item.version.colorIndex)
            Row(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(if (huge) MaterialTheme.ember.perfect.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { onClick(item.date) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(40.dp).height(40.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Text(item.version.emoji, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.version.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(
                        item.date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")) + relativeDays(item.date, today),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (huge) {
                    Box(
                        Modifier.clip(CircleShape).background(MaterialTheme.ember.perfect).padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text("★ HUGE", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF3B2A00), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun relativeDays(date: LocalDate, from: LocalDate): String = when (val n = ChronoUnit.DAYS.between(from, date)) {
    0L -> " · today"
    1L -> " · tomorrow"
    else -> " · in $n days"
}
