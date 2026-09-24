package com.nhowe.ember

import android.app.Application
import com.nhowe.ember.di.AppContainer
import com.nhowe.ember.notifications.Notifications

class EmberApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.ensureChannel(this)
    }
}

val android.content.Context.appContainer: AppContainer
    get() = (applicationContext as EmberApp).container
