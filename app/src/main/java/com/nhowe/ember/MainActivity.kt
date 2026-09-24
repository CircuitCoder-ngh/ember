package com.nhowe.ember

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.nhowe.ember.di.LocalAppContainer
import com.nhowe.ember.ui.navigation.EmberRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalAppContainer provides appContainer) {
                EmberRoot()
            }
        }
    }
}
