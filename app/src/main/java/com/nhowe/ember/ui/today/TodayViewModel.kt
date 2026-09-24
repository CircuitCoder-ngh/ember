package com.nhowe.ember.ui.today

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhowe.ember.di.AppContainer
import com.nhowe.ember.domain.engine.XpEngine
import com.nhowe.ember.domain.model.Completion
import com.nhowe.ember.domain.model.DayPlan
import com.nhowe.ember.domain.model.EngineSnapshot
import com.nhowe.ember.domain.model.ResolvedGoal
import com.nhowe.ember.domain.model.Settings
import java.time.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface TodayEffect {
    data class XpGained(val amount: Int) : TodayEffect
    data object GoalDone : TodayEffect
    data object DayComplete : TodayEffect
}

class TodayViewModel(private val c: AppContainer) : ViewModel() {
    val snapshot: StateFlow<EngineSnapshot?> = c.engineStore.snapshot
    val settings: Flow<Settings> = c.settingsRepository.settings

    private val _effects = Channel<TodayEffect>(Channel.BUFFERED)
    val effects: Flow<TodayEffect> = _effects.receiveAsFlow()

    val today: LocalDate get() = c.today.value

    /**
     * Counts the user has tapped to but the database has not echoed back yet. Without this,
     * rapid taps would each compute "count + 1" from a stale row and increments would be lost.
     */
    val localCounts: SnapshotStateMap<String, Int> = mutableStateMapOf()

    init {
        viewModelScope.launch {
            snapshot.filterNotNull().collect { snap ->
                snap.todayPlan.goals.forEach { g -> if (localCounts[g.id] == g.count) localCounts.remove(g.id) }
            }
        }
    }

    /** Today's plan with pending counts applied, so the UI never lags behind the user's taps. */
    fun mergedPlan(snap: EngineSnapshot): DayPlan {
        if (localCounts.isEmpty()) return snap.todayPlan
        val plan = snap.todayPlan
        return plan.copy(goals = plan.goals.map { g ->
            val pending = localCounts[g.id] ?: return@map g
            g.copy(completion = completionFor(g, checked = g.checked, count = pending))
        })
    }

    fun toggle(goal: ResolvedGoal, date: LocalDate = today) {
        val nowDone = !goal.checked
        applyChange(goal, date, goal.copy(completion = completionFor(goal, checked = nowDone, count = goal.count)))
        viewModelScope.launch { c.progressRepository.setChecked(goal.id, date, nowDone, today) }
    }

    fun setCount(goal: ResolvedGoal, count: Int, date: LocalDate = today) {
        val clamped = count.coerceAtLeast(0)
        if (clamped == goal.count) return
        applyChange(goal, date, goal.copy(completion = completionFor(goal, checked = goal.checked, count = clamped)))
        if (date == today) localCounts[goal.id] = clamped
        viewModelScope.launch { c.progressRepository.setCount(goal.id, date, clamped, today) }
    }

    fun setSkipped(goalId: String, skipped: Boolean, date: LocalDate = today) {
        c.haptics.tick()
        viewModelScope.launch { c.progressRepository.setSkipped(goalId, date, skipped) }
    }

    /** Haptics and XP toast are computed locally so they fire instantly, before the DB round-trips. */
    private fun applyChange(before: ResolvedGoal, date: LocalDate, after: ResolvedGoal) {
        val snap = snapshot.value ?: return
        if (date != snap.today) { c.haptics.tick(); return }
        val plan = mergedPlan(snap)
        val newPlan = plan.copy(goals = plan.goals.map { if (it.id == before.id) after else it })
        val streak = snap.streak.current
        val delta = XpEngine.dayXp(newPlan, streak) - XpEngine.dayXp(plan, streak)
        val becameDone = !before.isDone && after.isDone
        when {
            newPlan.isPerfect && !plan.isPerfect -> { c.haptics.celebrate(); _effects.trySend(TodayEffect.DayComplete) }
            becameDone -> { c.haptics.success(); _effects.trySend(TodayEffect.GoalDone) }
            else -> c.haptics.tick()
        }
        if (delta != 0) _effects.trySend(TodayEffect.XpGained(delta))
    }

    private fun completionFor(goal: ResolvedGoal, checked: Boolean, count: Int) =
        Completion(goal.id, goal.completion?.date ?: today, checked, count, System.currentTimeMillis())

}
