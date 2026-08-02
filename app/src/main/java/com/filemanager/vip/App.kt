package com.filemanager.vip

import android.app.Application
import com.filemanager.vip.ads.AdManager
import com.filemanager.vip.util.Preferences

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        Preferences.init(this)
        AdManager.init(this)
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
