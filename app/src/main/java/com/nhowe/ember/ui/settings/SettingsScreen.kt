package com.nhowe.ember.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhowe.ember.di.LocalAppContainer
import com.nhowe.ember.domain.engine.Engine
import com.nhowe.ember.domain.model.ThemeMode
import com.nhowe.ember.ui.components.SectionLabel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val vm: SettingsViewModel = viewModel { SettingsViewModel(container) }
    val settings by vm.settings.collectAsStateWithLifecycle(initialValue = null)
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    val s = settings ?: return
    var threshold by remember(s.streakThreshold) { mutableStateOf(s.streakThreshold.toFloat()) }
    var name by remember(s.userName) { mutableStateOf(s.userName) }
    var showTime by remember { mutableStateOf(false) }
    var showWindow by remember { mutableStateOf<String?>(null) } // "start" | "end"
    var confirmReset by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }

    var pendingToggle by remember { mutableStateOf("reminder") }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { if (pendingToggle == "hourly") vm.setHourlyEnabled(true) else vm.setReminderEnabled(true) }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { vm.exportBackup(context, it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { pendingImport = it }
    }

    val previewStreak = remember(threshold, snapshot) {
        val snap = snapshot ?: return@remember null
        Engine.compute(snap.history, snap.settings.copy(streakThreshold = threshold.toDouble()), snap.today).streak.current
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
                Text("Settings", style = MaterialTheme.typography.headlineSmall)
            }

            SectionLabel("You")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(24) },
                label = { Text("Your name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
            if (name != s.userName) {
                TextButton(onClick = { vm.setUserName(name) }, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Save name") }
            }

            SectionLabel("Streak rule")
            Group {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Keep the streak at", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text("${(threshold * 100).roundToInt()}%", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = threshold,
                    onValueChange = { threshold = (it * 20).roundToInt() / 20f },
                    onValueChangeFinished = { vm.setThreshold(threshold.toDouble()); container.haptics.tick() },
                    valueRange = 0.5f..1f,
                    steps = 9,
                )
                Text(
                    buildString {
                        append("A day counts toward your streak when its score reaches this bar. ")
                        if (previewStreak != null) append("With this setting your current streak is $previewStreak days.")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionLabel("Day ends at")
            Group {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(0, 2, 3, 4, 6).forEachIndexed { i, h ->
                        SegmentedButton(
                            selected = s.dayBoundaryHour == h,
                            onClick = { vm.setBoundaryHour(h) },
                            shape = SegmentedButtonDefaults.itemShape(i, 5),
                            label = { Text(if (h == 0) "12am" else "${h}am") },
                        )
                    }
                }
                Text(
                    "Check-offs before this hour still count for the previous day. Handy for night owls.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionLabel("Reminder")
            Group {
                SwitchRow("Daily nudge", "Only fires if today is still below your bar", s.reminderEnabled) { on ->
                    if (on && Build.VERSION.SDK_INT >= 33) { pendingToggle = "reminder"; notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    else vm.setReminderEnabled(on)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Time", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showTime = true }) { Text(s.reminderTime.format(DateTimeFormatter.ofPattern("h:mm a"))) }
                }
            }

            SectionLabel("Hourly progress card")
            Group {
                SwitchRow("Keep what's left in the shade", "A silent notification with today's open goals, your %, and a quote. Refreshes hourly and whenever you check something off.", s.hourlyEnabled) { on ->
                    if (on && Build.VERSION.SDK_INT >= 33) { pendingToggle = "hourly"; notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    else vm.setHourlyEnabled(on)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Active between", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showWindow = "start" }) { Text(s.hourlyStart.format(DateTimeFormatter.ofPattern("h:mm a"))) }
                    Text("and", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { showWindow = "end" }) { Text(s.hourlyEnd.format(DateTimeFormatter.ofPattern("h:mm a"))) }
                }
            }

            SectionLabel("Feel")
            Group {
                SwitchRow("Haptics", "Taps, pops and celebrations you can feel", s.hapticsEnabled) { vm.setHaptics(it) }
                SwitchRow("Completed goals sink", "Finished goals drop to the bottom of today's list", s.completedSinkToBottom) { vm.setSinkCompleted(it) }
            }

            SectionLabel("Appearance")
            Group {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { i, m ->
                        SegmentedButton(
                            selected = s.themeMode == m,
                            onClick = { vm.setTheme(m) },
                            shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                            label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
                SwitchRow("Dynamic color", "Use your wallpaper's palette instead of ember orange", s.dynamicColor) { vm.setDynamicColor(it) }
            }

            SectionLabel("Backup")
            Group {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { exportLauncher.launch("ember-backup-${LocalDate.now()}.json") }) { Text("Export JSON") }
                    TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text("Import JSON") }
                }
                Text(
                    "Everything lives on this phone. Export a backup before switching devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionLabel("Danger zone")
            Group {
                TextButton(onClick = { confirmReset = true }) { Text("Erase all data", color = MaterialTheme.colorScheme.error) }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) { Snackbar(it) }
    }

    if (showTime) {
        val state = rememberTimePickerState(initialHour = s.reminderTime.hour, initialMinute = s.reminderTime.minute)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = { TextButton(onClick = { vm.setReminderTime(LocalTime.of(state.hour, state.minute)); showTime = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) },
        )
    }
    showWindow?.let { which ->
        val initial = if (which == "start") s.hourlyStart else s.hourlyEnd
        val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute)
        AlertDialog(
            onDismissRequest = { showWindow = null },
            confirmButton = {
                TextButton(onClick = {
                    val t = LocalTime.of(state.hour, state.minute)
                    if (which == "start") vm.setHourlyWindow(t, s.hourlyEnd) else vm.setHourlyWindow(s.hourlyStart, t)
                    showWindow = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showWindow = null }) { Text("Cancel") } },
            text = { TimePicker(state = state) },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Erase everything?") },
            text = { Text("All goals, history, streaks and badges will be gone. Export a backup first if you might want them back.") },
            confirmButton = { TextButton(onClick = { confirmReset = false; vm.resetAll() }) { Text("Erase", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Restore this backup?") },
            text = { Text("It replaces everything currently in the app.") },
            confirmButton = { TextButton(onClick = { vm.importBackup(context, uri); pendingImport = null }) { Text("Restore") } },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Group(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
    Spacer(Modifier.height(2.dp))
}
