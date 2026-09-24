package com.nhowe.ember.di

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }
