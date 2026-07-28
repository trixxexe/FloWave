package com.trixxexe.trixxwave

import android.app.Application
import com.trixxexe.trixxwave.data.db.TrixxWaveDatabase
import com.trixxexe.trixxwave.data.preferences.ThemePreferencesRepository

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
    }

    companion object {
        lateinit var instance: TrixxWaveApp
            private set
    }
}
