package com.nhowe.ember.ui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhowe.ember.data.templates.GoalTemplate
import com.nhowe.ember.data.templates.ProgramTemplate
import com.nhowe.ember.core.time.startOfWeek
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import com.nhowe.ember.data.templates.Stacking
import com.nhowe.ember.di.LocalAppContainer
import com.nhowe.ember.domain.model.GoalKind
import com.nhowe.ember.ui.goals.GoalsViewModel
import kotlinx.coroutines.launch

/** Browse the bundle library and add one to your goals. Goals you already have are skipped by title. */
@Composable
fun TemplateBrowserScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val repo = container.templateRepository
    val snapshot by container.engineStore.snapshot.collectAsStateWithLifecycle()
    var category by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf<Pair<GoalTemplate, String>?>(null) }
    var showPrograms by remember { mutableStateOf(false) }
    var enrol by remember { mutableStateOf<ProgramTemplate?>(null) }
    var startToday by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val history = snapshot?.history
    val activeDaily = history?.goals?.count { g ->
        g.kind == GoalKind.RECURRING && !g.isArchived && GoalsViewModel.currentVersion(history, g.id)?.isPeriodic == false
    } ?: 0
    val activeTitles = history?.goals?.filter { !it.isArchived }?.mapNotNull { GoalsViewModel.currentVersion(history, it.id)?.title }?.toSet() ?: emptySet()

    fun add(t: GoalTemplate) {
        scope.launch {
            val (added, skipped) = container.goalRepository.addAll(t.goals.map { it.toDraft() }, activeTitles, container.today.value)
            container.haptics.success()
            snackbar.showSnackbar(
                when {
                    added == 0 -> "You already have all of these."
                    skipped == 0 -> "Added $added goals from ${t.title}."
                    else -> "Added $added goals, skipped $skipped you already had."
                }
            )
        }
    }

    fun tryAdd(t: GoalTemplate) {
        val newDaily = t.goals.count { it.isDaily && it.title !in activeTitles }
        val warning = Stacking.warning(activeDaily + newDaily)
        if (warning != null && activeDaily + newDaily > Stacking.COMFORTABLE_DAILY) confirm = t to warning else add(t)
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                Column {
                    Text("Bundles", style = MaterialTheme.typography.headlineSmall)
                    Text("$activeDaily daily goals right now", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SingleChoiceSegmentedButtonRow(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                SegmentedButton(selected = !showPrograms, onClick = { showPrograms = false }, shape = SegmentedButtonDefaults.itemShape(0, 2), label = { Text("Bundles") })
                SegmentedButton(selected = showPrograms, onClick = { showPrograms = true }, shape = SegmentedButtonDefaults.itemShape(1, 2), label = { Text("Programs") })
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = category == null, onClick = { category = null }, label = { Text("All") })
                repo.categories.forEach { c ->
                    FilterChip(selected = category == c.id, onClick = { category = c.id }, label = { Text("${c.emoji} ${c.title}") })
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp)) {
                if (showPrograms) {
                    val activeTemplateIds = snapshot?.activePrograms?.map { it.program.templateId }?.toSet() ?: emptySet()
                    Text(
                        "A program changes its goals week by week and ends with a badge. One at a time works best.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp),
                    )
                    repo.programs.filter { category == null || it.category == category }.forEach { t ->
                        ProgramTemplateCard(
                            template = t, expanded = expanded == t.id, enrolled = t.id in activeTemplateIds,
                            onExpand = { expanded = if (expanded == t.id) null else t.id },
                            onStart = { startToday = false; enrol = t },
                            modifier = Modifier.padding(vertical = 5.dp),
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    return@Column
                }
                repo.templates.filter { category == null || it.category == category }.forEach { t ->
                    val alreadyHave = t.goals.all { it.title in activeTitles }
                    TemplateCard(
                        template = t,
                        selected = alreadyHave,
                        expanded = expanded == t.id,
                        onToggle = { if (!alreadyHave) tryAdd(t) },
                        onExpand = { expanded = if (expanded == t.id) null else t.id },
                        modifier = Modifier.padding(vertical = 5.dp),
                        actionLabel = if (alreadyHave) "Added" else "Add",
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) { Snackbar(it) }
    }

    enrol?.let { t ->
        val today = container.today.value
        val monday = if (today.dayOfWeek == DayOfWeek.MONDAY) today else today.startOfWeek().plusWeeks(1)
        val canStartToday = !t.hasPeriodic || today.dayOfWeek == DayOfWeek.MONDAY
        val start = if (startToday && canStartToday) today else monday
        val active = snapshot?.activePrograms?.size ?: 0
        val newDaily = t.dailyCount
        val warning = Stacking.warning(activeDaily + newDaily, active + 1)
        AlertDialog(
            onDismissRequest = { enrol = null },
            title = { Text("Start ${t.title}?") },
            text = {
                Column {
                    Text("${t.weeks} weeks, ${t.goals.size} goal${if (t.goals.size == 1) "" else "s"}. Begins ${start.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))} and ends ${start.plusDays((t.lengthDays - 1).toLong()).format(DateTimeFormatter.ofPattern("MMM d"))}.")
                    if (canStartToday && today.dayOfWeek != DayOfWeek.MONDAY) {
                        Spacer(Modifier.height(8.dp))
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            SegmentedButton(selected = !startToday, onClick = { startToday = false }, shape = SegmentedButtonDefaults.itemShape(0, 2), label = { Text("Next Monday") })
                            SegmentedButton(selected = startToday, onClick = { startToday = true }, shape = SegmentedButtonDefaults.itemShape(1, 2), label = { Text("Today") })
                        }
                    } else if (!canStartToday) {
                        Spacer(Modifier.height(8.dp))
                        Text("Weekly goals line up with Monday-to-Sunday weeks, so this one starts on a Monday.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (active >= 1) {
                        Spacer(Modifier.height(8.dp))
                        Text("You already have $active program${if (active > 1) "s" else ""} running. Finishing one beats starting two.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    } else if (warning != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val tpl = t; enrol = null
                    scope.launch {
                        container.programRepository.enrol(tpl, start)
                        container.haptics.celebrate()
                        snackbar.showSnackbar("${tpl.title} starts ${if (start == today) "today" else start.format(DateTimeFormatter.ofPattern("EEE, MMM d"))}.")
                    }
                }) { Text("Start") }
            },
            dismissButton = { TextButton(onClick = { enrol = null }) { Text("Not now") } },
        )
    }

    confirm?.let { (t, warning) ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Add ${t.title}?") },
            text = { Text(warning) },
            confirmButton = { TextButton(onClick = { add(t); confirm = null }) { Text("Add anyway") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Not now") } },
        )
    }
}
