package com.nhowe.ember.ui.goals

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhowe.ember.di.AppContainer
import com.nhowe.ember.domain.model.EngineSnapshot
import com.nhowe.ember.domain.model.Goal
import com.nhowe.ember.domain.model.GoalKind
import com.nhowe.ember.domain.model.GoalVersion
import com.nhowe.ember.domain.model.History
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** A goal paired with the definition that currently applies (or last applied). */
data class GoalListItem(val goal: Goal, val version: GoalVersion)

class GoalsViewModel(private val c: AppContainer) : ViewModel() {
    val snapshot: StateFlow<EngineSnapshot?> = c.engineStore.snapshot

    /** Optimistic order while dragging; persisted on drop. */
    var orderOverride by mutableStateOf<List<String>?>(null)
        private set

    fun activeGoals(history: History): List<GoalListItem> {
        val items = history.goals
            .filter { it.kind == GoalKind.RECURRING && !it.isArchived }
            .mapNotNull { g -> currentVersion(history, g.id)?.let { GoalListItem(g, it) } }
        val order = orderOverride ?: return items.sortedWith(compareBy({ it.goal.sortOrder }, { it.goal.createdAt }))
        return items.sortedBy { order.indexOf(it.goal.id).let { i -> if (i == -1) Int.MAX_VALUE else i } }
    }

    fun oneOffGoals(history: History): List<GoalListItem> = history.goals
        .filter { it.kind == GoalKind.ONE_OFF && !it.isArchived }
        .mapNotNull { g -> currentVersion(history, g.id)?.let { GoalListItem(g, it) } }
        .sortedByDescending { it.goal.oneOffDate }

    fun archivedGoals(history: History): List<GoalListItem> = history.goals
        .filter { it.isArchived }
        .mapNotNull { g -> currentVersion(history, g.id)?.let { GoalListItem(g, it) } }
        .sortedByDescending { it.goal.archivedAt }

    fun move(current: List<GoalListItem>, from: Int, to: Int) {
        if (from == to || from !in current.indices || to !in current.indices) return
        val ids = current.map { it.goal.id }.toMutableList()
        ids.add(to, ids.removeAt(from))
        orderOverride = ids
    }

    fun commitOrder() {
        val ids = orderOverride ?: return
        viewModelScope.launch { c.goalRepository.reorder(ids) }
    }

    fun archive(goalId: String) {
        c.haptics.click()
        viewModelScope.launch {
            val snap = snapshot.value
            val hasProgress = snap?.todayPlan?.goals?.firstOrNull { it.id == goalId }?.let { it.credit > 0 } ?: false
            c.goalRepository.archive(goalId, c.today.value, hasProgress)
        }
    }

    fun unarchive(goalId: String) {
        c.haptics.click()
        viewModelScope.launch { c.goalRepository.unarchive(goalId, c.today.value) }
    }

    fun delete(goalId: String) {
        c.haptics.heavy()
        viewModelScope.launch { c.goalRepository.delete(goalId) }
    }

    companion object {
        fun currentVersion(history: History, goalId: String): GoalVersion? {
            val versions = history.versions.filter { it.goalId == goalId }
            return versions.firstOrNull { it.validTo == null } ?: versions.maxByOrNull { it.validFrom }
        }
    }
}
