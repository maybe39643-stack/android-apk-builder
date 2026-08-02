package com.filemanager.vip.util

import android.content.Context
import android.content.SharedPreferences
import com.filemanager.vip.App

object Preferences {
    private const val PREFS_NAME = "filemanager_vip_prefs"
    private const val KEY_IS_VIP = "is_vip"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_LAST_DIR = "last_dir"

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Crash-proof accessor: falls back to app context if init() hasn't been called yet */
    private fun getPrefs(): SharedPreferences {
        prefs?.let { return it }
        synchronized(this) {
            prefs?.let { return it }
            val app = App.instance
            val p = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = p
            return p
        }
    }

    fun isVip(): Boolean = getPrefs().getBoolean(KEY_IS_VIP, false)

    fun setVip(vip: Boolean) {
        getPrefs().edit().putBoolean(KEY_IS_VIP, vip).apply()
    }

    fun isDarkMode(): Boolean = getPrefs().getBoolean(KEY_DARK_MODE, false)

    fun setDarkMode(dark: Boolean) {
        getPrefs().edit().putBoolean(KEY_DARK_MODE, dark).apply()
    }

    fun getFavorites(): MutableSet<String> =
        getPrefs().getStringSet(KEY_FAVORITES, mutableSetOf())?.toMutableSet() ?: mutableSetOf()

    fun isFavorite(path: String): Boolean = getFavorites().contains(path)

    fun addFavorite(path: String) {
        val favs = getFavorites()
        favs.add(path)
        getPrefs().edit().putStringSet(KEY_FAVORITES, favs).apply()
    }

    fun removeFavorite(path: String) {
        val favs = getFavorites()
        favs.remove(path)
        getPrefs().edit().putStringSet(KEY_FAVORITES, favs).apply()
    }

    fun getLastDir(): String = getPrefs().getString(KEY_LAST_DIR, "/") ?: "/"

    fun setLastDir(path: String) {
        getPrefs().edit().putString(KEY_LAST_DIR, path).apply()
    }
}
