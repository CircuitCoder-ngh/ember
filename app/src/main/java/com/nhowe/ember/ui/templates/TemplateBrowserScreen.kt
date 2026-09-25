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
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = category == null, onClick = { category = null }, label = { Text("All") })
                repo.categories.forEach { c ->
                    FilterChip(selected = category == c.id, onClick = { category = c.id }, label = { Text("${c.emoji} ${c.title}") })
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp)) {
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
