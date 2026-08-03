package com.filemanager.vip

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.filemanager.vip.ads.AdManager
import com.filemanager.vip.util.Preferences

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        Preferences.init(this)

        // Apply saved dark mode setting
        AppCompatDelegate.setDefaultNightMode(
            if (Preferences.isDarkMode()) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )

        AdManager.init(this)
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
