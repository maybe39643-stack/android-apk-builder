package com.filemanager.vip

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.filemanager.vip.ads.AdManager
import com.filemanager.vip.util.Preferences
import android.content.Context

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            Preferences.init(this)

            // Apply saved dark mode setting
            AppCompatDelegate.setDefaultNightMode(
                if (Preferences.isDarkMode()) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
            )

            // Initialize AdMob with test ads
            AdManager.init(this)
        } catch (e: Exception) {
            // Never crash in Application.onCreate
            android.util.Log.e("App", "Init error: ${e.message}")
        }
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
