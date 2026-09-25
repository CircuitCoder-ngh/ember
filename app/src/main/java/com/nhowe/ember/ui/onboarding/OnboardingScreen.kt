package com.nhowe.ember.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhowe.ember.di.LocalAppContainer
import com.nhowe.ember.ui.components.FlameMascot
import com.nhowe.ember.ui.components.FlameState
import com.nhowe.ember.ui.templates.StackingBanner
import com.nhowe.ember.ui.templates.TemplateCard
import kotlin.math.roundToInt

private const val STEPS = 4

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: OnboardingViewModel = viewModel { OnboardingViewModel(container) }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.reminder = granted
        vm.finish(onDone)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.Center) {
            repeat(STEPS) { i ->
                Box(
                    Modifier
                        .padding(4.dp)
                        .size(if (i == vm.step) 22.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(if (i == vm.step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
        }

        AnimatedContent(
            targetState = vm.step,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(300)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(250))) togetherWith
                    (slideOutHorizontally(tween(220)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(180)))
            },
            label = "onboarding",
        ) { step ->
            when (step) {
                0 -> Intro()
                1 -> Categories(vm)
                2 -> Bundles(vm)
                else -> Rules(vm)
            }
        }

        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (vm.step > 0) TextButton(onClick = { vm.step-- }) { Text("Back") }
            Spacer(Modifier.weight(1f))
            if (vm.step == 2) {
                Text("${vm.totalGoalCount} goals", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp))
            }
            Button(
                onClick = {
                    when (vm.step) {
                        0, 1, 2 -> { container.haptics.click(); vm.step++ }
                        else -> if (vm.reminder && Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.finish(onDone)
                    }
                },
                enabled = !vm.saving && (vm.step != 2 || vm.selectedTemplates.isNotEmpty()),
                modifier = Modifier.height(50.dp),
            ) {
                Text(
                    when (vm.step) {
                        0 -> "Let's go"
                        1 -> if (vm.selectedCategories.isEmpty()) "Show me everything" else "Next"
                        2 -> "Next"
                        else -> "Light it up"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun Intro() {
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        FlameMascot(state = FlameState.BLAZING, size = 180.dp)
        Spacer(Modifier.height(24.dp))
        Text("Meet Ember", style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "Decide what your best day looks like. Check things off. Watch your streak grow.\n\nEvery day gets a score. Every week tells you how close you came.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Categories(vm: OnboardingViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("What do you want to work on?", style = MaterialTheme.typography.headlineMedium)
        Text("Pick one or two. You can always add more later.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            vm.categories.forEach { cat ->
                FilterChip(
                    selected = cat.id in vm.selectedCategories,
                    onClick = { vm.toggleCategory(cat.id) },
                    label = { Text("${cat.emoji}  ${cat.title}") },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        vm.selectedCategories.mapNotNull { id -> vm.categories.firstOrNull { it.id == id } }.forEach { cat ->
            Text("${cat.emoji} ${cat.title}: ${cat.blurb}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 2.dp))
        }
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = vm.name,
            onValueChange = { vm.name = it.take(24) },
            label = { Text("What should Ember call you?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Bundles(vm: OnboardingViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("Pick your bundles", style = MaterialTheme.typography.headlineMedium)
        Text("Each bundle is a few goals that work together. Tap to see what's inside.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        StackingBanner(vm.stackingWarning)
        vm.offered.forEach { t ->
            TemplateCard(
                template = t,
                selected = t.id in vm.selectedTemplates,
                expanded = vm.expanded == t.id,
                onToggle = { vm.toggleTemplate(t.id) },
                onExpand = { vm.expanded = if (vm.expanded == t.id) null else t.id },
                modifier = Modifier.padding(vertical = 5.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Rules(vm: OnboardingViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("Set your rules", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Streak survives at", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${(vm.threshold * 100).roundToInt()}%", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = vm.threshold, onValueChange = { vm.threshold = (it * 20).roundToInt() / 20f }, valueRange = 0.5f..1f, steps = 9)
        Text(
            "Life happens. Hit this share of your day and the flame stays lit. Perfect days earn extra.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Evening nudge at 9 pm", style = MaterialTheme.typography.titleMedium)
                Text("Only if the day is still below your bar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = vm.reminder, onCheckedChange = { vm.reminder = it })
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "Streak freezes: hit a 7-day milestone and Ember hands you a freeze that covers one bad day automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
