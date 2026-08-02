package com.filemanager.vip

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.filemanager.vip.ads.AdManager
import com.filemanager.vip.ui.FavoritesFragment
import com.filemanager.vip.ui.FilesFragment
import com.filemanager.vip.ui.HomeFragment
import com.filemanager.vip.ui.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private var bannerShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)

        if (savedInstanceState == null) {
            switchFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchFragment(HomeFragment())
                R.id.nav_files -> switchFragment(FilesFragment())
                R.id.nav_favorites -> switchFragment(FavoritesFragment())
                R.id.nav_settings -> switchFragment(SettingsFragment())
            }
            true
        }

        // Show banner ad on main activity
        showBannerAd()
    }

    override fun onStart() {
        super.onStart()
        AdManager.onActivityStarted(this)
    }

    override fun onStop() {
        super.onStop()
        AdManager.onActivityStopped(this)
    }

    private fun showBannerAd() {
        if (bannerShown) return
        bannerShown = true
        val bannerContainer = findViewById<View>(R.id.banner_container)
        bannerContainer.visibility = View.VISIBLE
        AdManager.loadBanner(this, bannerContainer as android.widget.FrameLayout)
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
