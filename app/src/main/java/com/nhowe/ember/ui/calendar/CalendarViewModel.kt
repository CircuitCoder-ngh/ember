package com.nhowe.ember.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhowe.ember.di.AppContainer
import com.nhowe.ember.domain.engine.DayPlanResolver
import com.nhowe.ember.domain.model.DayPlan
import com.nhowe.ember.domain.model.EngineSnapshot
import com.nhowe.ember.domain.model.ResolvedGoal
import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CalendarViewModel(private val c: AppContainer) : ViewModel() {
    val snapshot: StateFlow<EngineSnapshot?> = c.engineStore.snapshot

    fun planFor(snapshot: EngineSnapshot, date: LocalDate): DayPlan =
        snapshot.plans[date] ?: DayPlanResolver.resolve(date, snapshot.history)

    fun toggle(goal: ResolvedGoal, date: LocalDate) {
        if (goal.checked) c.haptics.tick() else c.haptics.success()
        viewModelScope.launch { c.progressRepository.setChecked(goal.id, date, !goal.checked, c.today.value) }
    }

    fun setCount(goal: ResolvedGoal, count: Int, date: LocalDate) {
        c.haptics.tick()
        viewModelScope.launch { c.progressRepository.setCount(goal.id, date, count, c.today.value) }
    }

    fun setSkipped(goalId: String, date: LocalDate, skipped: Boolean) {
        c.haptics.tick()
        viewModelScope.launch { c.progressRepository.setSkipped(goalId, date, skipped) }
    }
}
