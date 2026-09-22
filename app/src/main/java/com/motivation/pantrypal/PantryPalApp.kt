package com.motivation.pantrypal

import android.app.Application
import android.util.Log
import com.motivation.pantrypal.di.AppContainer

/**
 * Application entry point. Owns the simple hand-rolled [AppContainer] service
 * locator that wires Room, Retrofit and the repositories together, so every
 * screen's ViewModel gets the same shared instances instead of creating its own.
 */
class PantryPalApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "PantryPalApp:onCreate - initialising app container")
        container = AppContainer(this)
    }

    companion object {
        private const val TAG = "PantryPalApp"
    }
}
