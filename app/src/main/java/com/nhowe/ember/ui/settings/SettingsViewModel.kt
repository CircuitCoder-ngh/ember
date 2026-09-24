package com.nhowe.ember.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhowe.ember.data.backup.BackupCodec
import com.nhowe.ember.di.AppContainer
import com.nhowe.ember.domain.model.EngineSnapshot
import com.nhowe.ember.domain.model.Settings
import com.nhowe.ember.domain.model.ThemeMode
import com.nhowe.ember.notifications.ProgressNotifier
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val settings: Flow<Settings> = c.settingsRepository.settings
    val snapshot: StateFlow<EngineSnapshot?> = c.engineStore.snapshot

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun setThreshold(v: Double) = launch { c.settingsRepository.setThreshold(v) }
    fun setBoundaryHour(h: Int) = launch { c.settingsRepository.setBoundaryHour(h) }
    fun setHaptics(b: Boolean) = launch { c.settingsRepository.setHaptics(b) }
    fun setSinkCompleted(b: Boolean) = launch { c.settingsRepository.setSinkCompleted(b) }
    fun setTheme(m: ThemeMode) = launch { c.settingsRepository.setTheme(m) }
    fun setDynamicColor(b: Boolean) = launch { c.settingsRepository.setDynamicColor(b) }
    fun setUserName(n: String) = launch { c.settingsRepository.setUserName(n) }

    fun setReminderEnabled(b: Boolean) = launch {
        c.settingsRepository.setReminderEnabled(b)
        c.reminderScheduler.sync(c.settingsRepository.settings.first())
    }

    fun setHourlyEnabled(b: Boolean) = launch {
        c.settingsRepository.setHourlyEnabled(b)
        c.reminderScheduler.sync(c.settingsRepository.settings.first())
    }

    fun setHourlyAlert(alert: Boolean, context: Context) = launch {
        c.settingsRepository.setHourlyAlert(alert)
        val settings = c.settingsRepository.settings.first()
        snapshot.value?.let { ProgressNotifier.repost(context.applicationContext, it, settings) }
    }

    fun setHourlyWindow(start: LocalTime, end: LocalTime) = launch {
        c.settingsRepository.setHourlyWindow(start, end)
        c.reminderScheduler.sync(c.settingsRepository.settings.first())
    }

    fun setReminderTime(t: LocalTime) = launch {
        c.settingsRepository.setReminderTime(t)
        c.reminderScheduler.sync(c.settingsRepository.settings.first())
    }

    fun exportBackup(context: Context, uri: Uri) = launch {
        runCatching {
            val history = c.historyRepository.history.first()
            val settings = c.settingsRepository.settings.first()
            val text = BackupCodec.encode(history, settings, Instant.now().toString())
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } ?: error("Could not open file")
            }
        }.onSuccess { _messages.trySend("Backup saved") }
            .onFailure { _messages.trySend("Export failed: ${it.message}") }
    }

    fun importBackup(context: Context, uri: Uri) = launch {
        runCatching {
            val text = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("Could not read file")
            }
            val (history, settings) = BackupCodec.decode(text)
            c.historyRepository.replaceAll(history)
            c.settingsRepository.replaceAll(settings)
            c.reminderScheduler.sync(settings)
        }.onSuccess { c.haptics.success(); _messages.trySend("Backup restored") }
            .onFailure { c.haptics.heavy(); _messages.trySend("Import failed: ${it.message}") }
    }

    fun resetAll() = launch {
        c.historyRepository.clearAll()
        c.haptics.heavy()
        _messages.trySend("All data erased")
    }

    private fun launch(block: suspend () -> Unit) { viewModelScope.launch { block() } }
}
