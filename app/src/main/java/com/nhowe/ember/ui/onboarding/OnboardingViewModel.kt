package com.nhowe.ember.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhowe.ember.core.time.ALL_WEEKDAYS
import com.nhowe.ember.data.repo.GoalDraft
import com.nhowe.ember.di.AppContainer
import com.nhowe.ember.domain.model.Cadence
import com.nhowe.ember.domain.model.GoalType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class StarterGoal(val draft: GoalDraft, val defaultOn: Boolean)

val StarterGoals = listOf(
    StarterGoal(GoalDraft("Move for 30 minutes", "🏃", 0), true),
    StarterGoal(GoalDraft("Read", "📚", 3, GoalType.QUANTITY, 20, "pages"), true),
    StarterGoal(GoalDraft("Drink water", "💧", 3, GoalType.QUANTITY, 8, "glasses"), true),
    StarterGoal(GoalDraft("No phone in bed", "📵", 4), true),
    StarterGoal(GoalDraft("Journal", "📝", 1), false),
    StarterGoal(GoalDraft("Meditate", "🧘", 8), false),
    StarterGoal(GoalDraft("Eat a real vegetable", "🥗", 2), false),
    StarterGoal(GoalDraft("In bed by 11", "🛌", 4), false),
    StarterGoal(GoalDraft("Tidy for 10 minutes", "🧹", 9), false),
    StarterGoal(GoalDraft("Gym session", "🏋️", 6, targetCount = 3, cadence = Cadence.WEEKLY), false),
    StarterGoal(GoalDraft("Call someone you love", "📞", 5, targetCount = 1, cadence = Cadence.WEEKLY), false),
    StarterGoal(GoalDraft("Deep work block", "💻", 6, weekdayMask = 0b0011111), false),
    StarterGoal(GoalDraft("Finish a book", "📖", 3, targetCount = 1, cadence = Cadence.MONTHLY), false),
    StarterGoal(GoalDraft("Stretch", "🤸", 7), false),
)

class OnboardingViewModel(private val c: AppContainer) : ViewModel() {
    var step by mutableStateOf(0)
    var name by mutableStateOf("")
    val selected = mutableStateListOf<Int>().apply { addAll(StarterGoals.indices.filter { StarterGoals[it].defaultOn }) }
    var threshold by mutableStateOf(0.8f)
    var reminder by mutableStateOf(true)
    var saving by mutableStateOf(false)
        private set

    fun toggle(index: Int) {
        if (index in selected) selected.remove(index) else selected.add(index)
        c.haptics.tick()
    }

    fun finish(onDone: () -> Unit) {
        if (saving) return
        saving = true
        c.haptics.celebrate()
        viewModelScope.launch {
            val today = c.today.value
            selected.sorted().forEach { i -> c.goalRepository.createRecurring(StarterGoals[i].draft, today) }
            c.settingsRepository.setUserName(name)
            c.settingsRepository.setThreshold(threshold.toDouble())
            c.settingsRepository.setReminderEnabled(reminder)
            c.settingsRepository.setOnboardingDone(true)
            c.reminderScheduler.sync(c.settingsRepository.settings.first())
            onDone()
        }
    }

    @Suppress("unused")
    private val allDays = ALL_WEEKDAYS
}
