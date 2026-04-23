package com.calorieko.app

import android.app.Application
import com.calorieko.app.data.local.AppDatabase
import com.calorieko.app.util.MealPlanReminderWorker
import com.calorieko.app.util.StreakReminderWorker
import com.mapbox.common.MapboxOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class CalorieKoApplication : Application() {
    // No need to cancel this scope as it'll be torn down with the process
    val applicationScope = CoroutineScope(SupervisorJob())

    // Using by lazy so the database and the repository are only created when they're needed
    // rather than when the application starts
    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }

    override fun onCreate() {
        super.onCreate()

        // Initialize Mapbox access token BEFORE any MapView is created.
        // The token is injected as a string resource by build.gradle.kts from local.properties.
        MapboxOptions.accessToken = getString(R.string.mapbox_access_token)

        // Schedule the daily streak reminder notification (8 PM local time).
        // Uses WorkManager KEEP policy — safe to call on every app start.
        StreakReminderWorker.schedule(this)

        // Schedule hourly meal plan reminder notifications.
        // Only posts if user has upcoming planned meals and reminders are enabled.
        MealPlanReminderWorker.schedule(this)
    }
}
