package com.nhowe.ember.domain

import com.nhowe.ember.domain.engine.Engine
import com.nhowe.ember.domain.model.EngineSnapshot
import com.nhowe.ember.domain.model.History
import com.nhowe.ember.domain.model.Settings
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/** Single shared computation of the engine snapshot; every ViewModel reads from here. */
class EngineStore(
    history: Flow<History>,
    settings: Flow<Settings>,
    today: Flow<LocalDate>,
    shownCelebrationKeys: Flow<Set<String>>,
    scope: CoroutineScope,
) {
    val snapshot: StateFlow<EngineSnapshot?> =
        combine(history, settings, today, shownCelebrationKeys) { h, s, t, keys -> Engine.compute(h, s, t, keys) }
            .flowOn(Dispatchers.Default)
            .stateIn(scope, SharingStarted.Eagerly, null)
}
