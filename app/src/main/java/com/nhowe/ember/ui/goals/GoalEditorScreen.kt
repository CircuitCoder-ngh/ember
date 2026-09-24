package com.nhowe.ember.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhowe.ember.core.time.ALL_WEEKDAYS
import com.nhowe.ember.di.LocalAppContainer
import com.nhowe.ember.domain.model.GoalType
import com.nhowe.ember.ui.components.SectionLabel
import com.nhowe.ember.ui.theme.GoalPalette
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val CuratedEmojis = listOf(
    "✅", "🔥", "🏃", "🏋️", "🚴", "🧘", "🚶", "💪", "🥗", "🍎", "💧", "☕", "🛌", "💤", "🌙", "☀️",
    "📚", "📖", "📝", "🧠", "💻", "🎨", "🎸", "🎧", "🧹", "🧺", "🧑‍🍳", "🌱", "🐶", "📵", "🚭", "🙏",
    "🗣️", "📞", "💰", "🪥", "🧴", "🎯", "⭐", "🏆", "❤️", "🧊", "🍵", "🚿", "🧩", "✍️", "📸", "🛒",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditorScreen(goalId: String?, oneOffDate: LocalDate?, onClose: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: GoalEditorViewModel = viewModel(key = "editor-${goalId ?: "new"}-${oneOffDate ?: ""}") {
        GoalEditorViewModel(container, goalId, oneOffDate)
    }
    var showDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
            Text(
                when {
                    !vm.isNew -> "Edit goal"
                    vm.isOneOff -> "One-off goal"
                    else -> "New daily goal"
                },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { vm.save(onClose) }, enabled = vm.loaded && vm.canSave) { Text("Save") }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            // Preview + title
            Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(GoalPalette[vm.colorIndex.mod(GoalPalette.size)].copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) { Text(vm.emoji, style = MaterialTheme.typography.headlineMedium) }
                Spacer(Modifier.width(14.dp))
                OutlinedTextField(
                    value = vm.title,
                    onValueChange = { vm.title = it },
                    label = { Text("What's the goal?") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            SectionLabel("Icon")
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CuratedEmojis.forEach { e ->
                    val selected = e == vm.emoji
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainer)
                            .clickable { vm.emoji = e; container.haptics.tick() },
                        contentAlignment = Alignment.Center,
                    ) { Text(e, style = MaterialTheme.typography.titleLarge) }
                }
            }

            SectionLabel("Color")
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoalPalette.forEachIndexed { i, color ->
                    val selected = i == vm.colorIndex
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(if (selected) 3.dp else 0.dp, if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent, CircleShape)
                            .clickable { vm.colorIndex = i; container.haptics.tick() },
                        contentAlignment = Alignment.Center,
                    ) { if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                }
            }

            SectionLabel("Type")
            SingleChoiceSegmentedButtonRow(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                SegmentedButton(selected = vm.type == GoalType.CHECK, onClick = { vm.type = GoalType.CHECK }, shape = SegmentedButtonDefaults.itemShape(0, 2), label = { Text("Check off") })
                SegmentedButton(selected = vm.type == GoalType.QUANTITY, onClick = { vm.type = GoalType.QUANTITY }, shape = SegmentedButtonDefaults.itemShape(1, 2), label = { Text("Count up") })
            }
            if (vm.type == GoalType.QUANTITY) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = vm.target,
                        onValueChange = { vm.target = it.filter { ch -> ch.isDigit() }.take(5) },
                        label = { Text("Target") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = vm.unit,
                        onValueChange = { vm.unit = it.take(16) },
                        label = { Text("Unit (pages, glasses…)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.6f),
                    )
                }
                Text(
                    "Partial progress counts: 12 of 20 is 60% of this goal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            if (vm.isOneOff) {
                SectionLabel("Date")
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(vm.oneOffDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showDate = true }) { Text("Change") }
                }
            } else {
                SectionLabel("Days")
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        val bit = 1 shl (day.value - 1)
                        val on = (vm.weekdayMask and bit) != 0
                        FilterChip(
                            selected = on,
                            onClick = { vm.toggleWeekday(bit); container.haptics.tick() },
                            label = { Text(day.getDisplayName(TextStyle.NARROW, Locale.getDefault())) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Row(Modifier.padding(horizontal = 12.dp)) {
                    TextButton(onClick = { vm.weekdayMask = ALL_WEEKDAYS }) { Text("Every day") }
                    TextButton(onClick = { vm.weekdayMask = 0b0011111 }) { Text("Weekdays") }
                    TextButton(onClick = { vm.weekdayMask = 0b1100000 }) { Text("Weekends") }
                }
            }

            SectionLabel("Importance")
            SingleChoiceSegmentedButtonRow(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                listOf(1 to "Normal", 2 to "Big", 3 to "Huge").forEachIndexed { i, (w, label) ->
                    SegmentedButton(selected = vm.weight == w, onClick = { vm.weight = w }, shape = SegmentedButtonDefaults.itemShape(i, 3), label = { Text(label) })
                }
            }
            Text(
                "Weighs ${vm.weight}× in your daily score and XP.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )

            SectionLabel("Note")
            OutlinedTextField(
                value = vm.note,
                onValueChange = { vm.note = it.take(120) },
                placeholder = { Text("Why this matters (optional)") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                maxLines = 3,
            )

            if (!vm.isNew && !vm.isOneOff) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Edits apply from today. Every past day keeps the goal exactly as it was.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            if (!vm.isNew) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.padding(horizontal = 12.dp)) {
                    if (!vm.isOneOff && !vm.isArchived) TextButton(onClick = { vm.archive(onClose) }) { Text("Archive") }
                    TextButton(onClick = { confirmDelete = true }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
            }
        }

        Button(
            onClick = { vm.save(onClose) },
            enabled = vm.loaded && vm.canSave,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(52.dp),
        ) { Text(if (vm.isNew) "Add goal" else "Save changes", style = MaterialTheme.typography.titleMedium) }
    }

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = vm.oneOffDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { vm.oneOffDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this goal?") },
            text = { Text("This erases it from every day in your history. Archiving keeps the record.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(onClose) }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
