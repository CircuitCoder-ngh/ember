package com.nhowe.ember.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhowe.ember.data.templates.GoalTemplate
import com.nhowe.ember.data.templates.Stacking
import com.nhowe.ember.data.templates.TemplateCategory
import com.nhowe.ember.di.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OnboardingViewModel(private val c: AppContainer) : ViewModel() {
    val categories: List<TemplateCategory> = c.templateRepository.categories.filter { it.id != "everyday" }

    var step by mutableStateOf(0)
    var name by mutableStateOf("")
    val selectedCategories = mutableStateListOf<String>()
    val selectedTemplates = mutableStateListOf<String>().apply { add("basics") }
    var expanded by mutableStateOf<String?>(null)
    var threshold by mutableStateOf(0.8f)
    var reminder by mutableStateOf(true)
    var saving by mutableStateOf(false)
        private set

    /** Bundles to offer: everyday basics first, then the chosen categories (or everything if none chosen). */
    val offered: List<GoalTemplate>
        get() {
            val all = c.templateRepository.templates
            val basics = all.filter { it.category == "everyday" }
            val rest = if (selectedCategories.isEmpty()) all.filter { it.category != "everyday" }
            else all.filter { it.category in selectedCategories }
            return basics + rest
        }

    val chosenBundles: List<GoalTemplate> get() = offered.filter { it.id in selectedTemplates }
    val dailyGoalCount: Int get() = chosenBundles.sumOf { it.dailyCount }
    val totalGoalCount: Int get() = chosenBundles.sumOf { it.goals.size }
    val stackingWarning: String? get() = Stacking.warning(dailyGoalCount, chosenBundles.count { it.category != "everyday" })

    fun toggleCategory(id: String) {
        if (id in selectedCategories) selectedCategories.remove(id) else selectedCategories.add(id)
        c.haptics.tick()
    }

    fun toggleTemplate(id: String) {
        if (id in selectedTemplates) selectedTemplates.remove(id) else selectedTemplates.add(id)
        c.haptics.tick()
    }

    fun finish(onDone: () -> Unit) {
        if (saving) return
        saving = true
        c.haptics.celebrate()
        viewModelScope.launch {
            val today = c.today.value
            val drafts = chosenBundles.flatMap { t -> t.goals.map { it.toDraft() } }
            c.goalRepository.addAll(drafts, emptySet(), today)
            c.settingsRepository.setUserName(name)
            c.settingsRepository.setThreshold(threshold.toDouble())
            c.settingsRepository.setReminderEnabled(reminder)
            c.settingsRepository.setOnboardingDone(true)
            c.reminderScheduler.sync(c.settingsRepository.settings.first())
            onDone()
        }
    }
}
