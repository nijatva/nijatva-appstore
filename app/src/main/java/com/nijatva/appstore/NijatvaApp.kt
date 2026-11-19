package com.nijatva.appstore

import android.app.Application
import com.nijatva.appstore.data.SettingsRepository

class NijatvaApp : Application() {
    lateinit var settings: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
    }
}
