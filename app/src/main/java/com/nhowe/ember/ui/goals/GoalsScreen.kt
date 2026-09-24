package com.nhowe.ember.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhowe.ember.core.time.ALL_WEEKDAYS
import com.nhowe.ember.di.LocalAppContainer
import com.nhowe.ember.domain.model.GoalType
import com.nhowe.ember.ui.components.ScreenHeader
import com.nhowe.ember.ui.theme.goalColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class GoalsTab(val label: String) { ACTIVE("Daily"), ONE_OFF("One-offs"), ARCHIVED("Archived") }

@Composable
fun GoalsScreen(
    onOpenSettings: () -> Unit,
    onEditGoal: (String) -> Unit,
    onNewGoal: () -> Unit,
    onNewOneOff: (LocalDate) -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: GoalsViewModel = viewModel { GoalsViewModel(container) }
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(GoalsTab.ACTIVE) }
    var confirmDelete by remember { mutableStateOf<GoalListItem?>(null) }

    val snap = snapshot ?: return
    val history = snap.history

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader(title = "Goals", subtitle = "Design your best day", onSettings = onOpenSettings)
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onNewGoal, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Daily goal")
            }
            FilledTonalButton(onClick = { onNewOneOff(snap.today) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.Event, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("One-off")
            }
        }
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
            GoalsTab.entries.forEachIndexed { i, t ->
                SegmentedButton(
                    selected = tab == t,
                    onClick = { tab = t },
                    shape = SegmentedButtonDefaults.itemShape(i, GoalsTab.entries.size),
                    label = { Text(t.label) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        when (tab) {
            GoalsTab.ACTIVE -> {
                val items = vm.activeGoals(history)
                if (items.isEmpty()) EmptyHint("No daily goals yet. Add one to start scoring your days.")
                else ReorderableGoalList(
                    items = items,
                    onMove = { from, to -> vm.move(items, from, to) },
                    onDrop = { vm.commitOrder() },
                    onEdit = { onEditGoal(it.goal.id) },
                    onArchive = { vm.archive(it.goal.id) },
                )
            }
            GoalsTab.ONE_OFF -> {
                val items = vm.oneOffGoals(history)
                if (items.isEmpty()) EmptyHint("One-off goals live on a single day. Try 'Call the dentist' on Thursday.")
                else LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(items, key = { _, it -> it.goal.id }) { _, item ->
                        GoalListRow(
                            item = item,
                            subtitle = item.goal.oneOffDate?.format(DateTimeFormatter.ofPattern("EEE, MMM d")) ?: "",
                            onClick = { onEditGoal(item.goal.id) },
                            trailing = {
                                IconButton(onClick = { confirmDelete = item }) { Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            },
                        )
                    }
                }
            }
            GoalsTab.ARCHIVED -> {
                val items = vm.archivedGoals(history)
                if (items.isEmpty()) EmptyHint("Archived goals keep their history but stop appearing on new days.")
                else LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(items, key = { _, it -> it.goal.id }) { _, item ->
                        GoalListRow(
                            item = item,
                            subtitle = scheduleLabel(item.version.weekdayMask),
                            onClick = {},
                            trailing = {
                                IconButton(onClick = { vm.unarchive(item.goal.id) }) { Icon(Icons.Rounded.Unarchive, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary) }
                                IconButton(onClick = { confirmDelete = item }) { Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            },
                        )
                    }
                }
            }
        }
    }

    confirmDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete \"${item.version.title}\"?") },
            text = { Text("This erases the goal and every day it was ever checked off. Archiving keeps the history.") },
            confirmButton = { TextButton(onClick = { vm.delete(item.goal.id); confirmDelete = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun ReorderableGoalList(
    items: List<GoalListItem>,
    onMove: (Int, Int) -> Unit,
    onDrop: () -> Unit,
    onEdit: (GoalListItem) -> Unit,
    onArchive: (GoalListItem) -> Unit,
) {
    val container = LocalAppContainer.current
    val listState = rememberLazyListState()
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(items, key = { _, it -> it.goal.id }) { _, item ->
            val isDragging = draggingId == item.goal.id
            val handle = Modifier.pointerInput(item.goal.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { draggingId = item.goal.id; dragOffset = 0f; container.haptics.click() },
                    onDragEnd = { draggingId = null; dragOffset = 0f; onDrop() },
                    onDragCancel = { draggingId = null; dragOffset = 0f; onDrop() },
                    onDrag = { change, delta ->
                        change.consume()
                        dragOffset += delta.y
                        val info = listState.layoutInfo.visibleItemsInfo
                        val current = info.firstOrNull { it.key == draggingId } ?: return@detectDragGesturesAfterLongPress
                        val centerY = current.offset + current.size / 2f + dragOffset
                        val target = info.firstOrNull { it.key != draggingId && centerY >= it.offset && centerY <= it.offset + it.size }
                        if (target != null) {
                            val from = items.indexOfFirst { it.goal.id == draggingId }
                            val to = items.indexOfFirst { it.goal.id == target.key }
                            if (from != -1 && to != -1) {
                                onMove(from, to)
                                dragOffset -= (target.offset - current.offset)
                                container.haptics.tick()
                            }
                        }
                    },
                )
            }
            Box(
                Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffset else 0f
                        scaleX = if (isDragging) 1.02f else 1f
                        scaleY = if (isDragging) 1.02f else 1f
                        shadowElevation = if (isDragging) 24f else 0f
                    }
                    .animateItem(placementSpec = if (isDragging) null else androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntOffset>()),
            ) {
                GoalListRow(
                    item = item,
                    subtitle = scheduleLabel(item.version.weekdayMask) + typeSuffix(item),
                    onClick = { onEdit(item) },
                    leading = {
                        Icon(Icons.Rounded.DragHandle, contentDescription = "Reorder", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = handle.padding(end = 6.dp))
                    },
                    trailing = {
                        IconButton(onClick = { onArchive(item) }) { Icon(Icons.Rounded.Archive, contentDescription = "Archive", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    },
                )
            }
        }
    }
}

@Composable
private fun GoalListRow(
    item: GoalListItem,
    subtitle: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    val color = goalColor(item.version.colorIndex)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Text(item.version.emoji, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.version.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

private fun typeSuffix(item: GoalListItem): String =
    if (item.version.type == GoalType.QUANTITY) " · ${item.version.target}${item.version.unit?.let { " $it" } ?: ""}" else ""

fun scheduleLabel(mask: Int): String {
    if (mask == ALL_WEEKDAYS || mask == 0) return "Every day"
    val weekdays = DayOfWeek.entries.filter { it.value <= 5 }.sumOf { 1 shl (it.value - 1) }
    val weekend = ALL_WEEKDAYS and weekdays.inv()
    if (mask == weekdays) return "Weekdays"
    if (mask == weekend) return "Weekends"
    return DayOfWeek.entries
        .filter { (mask and (1 shl (it.value - 1))) != 0 }
        .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}
