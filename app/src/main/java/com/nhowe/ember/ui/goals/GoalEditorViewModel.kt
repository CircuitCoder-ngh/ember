package com.nhowe.ember.ui.goals

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhowe.ember.core.time.ALL_WEEKDAYS
import com.nhowe.ember.data.repo.GoalDraft
import com.nhowe.ember.di.AppContainer
import com.nhowe.ember.domain.model.Cadence
import com.nhowe.ember.domain.model.GoalKind
import com.nhowe.ember.domain.model.GoalType
import java.time.LocalDate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GoalEditorViewModel(
    private val c: AppContainer,
    val goalId: String?,
    initialOneOffDate: LocalDate?,
) : ViewModel() {
    val isNew: Boolean = goalId == null
    var isOneOff by mutableStateOf(initialOneOffDate != null)
        private set
    var oneOffDate by mutableStateOf(initialOneOffDate ?: c.today.value)
    var loaded by mutableStateOf(goalId == null)
        private set
    var isArchived by mutableStateOf(false)
        private set

    var title by mutableStateOf("")
    var emoji by mutableStateOf("✅")
    var colorIndex by mutableStateOf(0)
    var type by mutableStateOf(GoalType.CHECK)
    var target by mutableStateOf("")
    var unit by mutableStateOf("")
    var weekdayMask by mutableStateOf(ALL_WEEKDAYS)
    var weight by mutableStateOf(1)
    var note by mutableStateOf("")
    var cadence by mutableStateOf(Cadence.DAILY)

    init {
        if (goalId != null) {
            viewModelScope.launch {
                val snap = c.engineStore.snapshot.filterNotNull().first()
                val goal = snap.history.goals.firstOrNull { it.id == goalId }
                val version = GoalsViewModel.currentVersion(snap.history, goalId)
                if (goal != null && version != null) {
                    isOneOff = goal.kind == GoalKind.ONE_OFF
                    goal.oneOffDate?.let { oneOffDate = it }
                    isArchived = goal.isArchived
                    title = version.title
                    emoji = version.emoji
                    colorIndex = version.colorIndex
                    type = version.type
                    target = version.targetCount?.toString() ?: ""
                    unit = version.unit ?: ""
                    weekdayMask = version.weekdayMask
                    weight = version.weight
                    note = version.note ?: ""
                    cadence = version.cadence
                }
                loaded = true
            }
        }
    }

    val isPeriodic: Boolean get() = !isOneOff && cadence != Cadence.DAILY

    val canSave: Boolean
        get() = title.isNotBlank() &&
            (type == GoalType.CHECK && !isPeriodic || (target.toIntOrNull() ?: 0) > 0) &&
            (isOneOff || isPeriodic || weekdayMask != 0)

    fun toggleWeekday(bit: Int) { weekdayMask = weekdayMask xor bit }

    fun save(onDone: () -> Unit) {
        if (!canSave) return
        val draft = GoalDraft(
            title = title, emoji = emoji, colorIndex = colorIndex, type = type,
            targetCount = target.toIntOrNull(), unit = unit, weekdayMask = weekdayMask, weight = weight, note = note,
            cadence = if (isOneOff) Cadence.DAILY else cadence,
        )
        c.haptics.success()
        viewModelScope.launch {
            val today = c.today.value
            when {
                goalId != null -> c.goalRepository.edit(goalId, draft, today, if (isOneOff) oneOffDate else null)
                isOneOff -> c.goalRepository.createOneOff(draft, oneOffDate)
                else -> c.goalRepository.createRecurring(draft, today)
            }
            onDone()
        }
    }

    fun archive(onDone: () -> Unit) {
        val id = goalId ?: return
        c.haptics.click()
        viewModelScope.launch {
            val snap = c.engineStore.snapshot.value
            val hasProgress = snap?.todayPlan?.goals?.firstOrNull { it.id == id }?.let { it.credit > 0 } ?: false
            c.goalRepository.archive(id, c.today.value, hasProgress)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = goalId ?: return
        c.haptics.heavy()
        viewModelScope.launch { c.goalRepository.delete(id); onDone() }
    }
}
