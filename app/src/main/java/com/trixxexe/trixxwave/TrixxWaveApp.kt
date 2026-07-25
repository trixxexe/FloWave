package com.trixxexe.trixxwave

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.trixxexe.trixxwave.data.db.TrixxWaveDatabase
import com.trixxexe.trixxwave.data.preferences.ThemePreferencesRepository
import com.trixxexe.trixxwave.service.LyricsCleanupWorker
import java.util.concurrent.TimeUnit

class TrixxWaveApp : Application() {

    val database: TrixxWaveDatabase by lazy {
        TrixxWaveDatabase.getDatabase(this)
    }

    val themePreferences: ThemePreferencesRepository by lazy {
        ThemePreferencesRepository(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        scheduleLyricsCleanup()
    }

    private fun scheduleLyricsCleanup() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val cleanupWorkRequest = PeriodicWorkRequestBuilder<LyricsCleanupWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            LyricsCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupWorkRequest
        )
    }

    companion object {
        lateinit var instance: TrixxWaveApp
            private set
    }
}
