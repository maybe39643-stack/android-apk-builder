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
    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)

        // Restore or create initial fragment
        if (savedInstanceState == null) {
            currentFragment = HomeFragment()
            switchFragment(currentFragment!!)
        } else {
            // Restore current fragment
            val frag = supportFragmentManager.findFragmentById(R.id.container)
            if (frag != null) {
                currentFragment = frag
            } else {
                currentFragment = HomeFragment()
                switchFragment(currentFragment!!)
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            val newFragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_files -> FilesFragment()
                R.id.nav_favorites -> FavoritesFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> null
            }
            if (newFragment != null) {
                // Prevent recreating the same fragment type
                if (currentFragment?.javaClass != newFragment.javaClass) {
                    currentFragment = newFragment
                    switchFragment(newFragment)
                }
            }
            true
        }

        // Set default selected tab to Home
        bottomNav.selectedItemId = R.id.nav_home
    }

    override fun onResume() {
        super.onResume()
        // Show banner ad after activity is ready
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

    override fun onDestroy() {
        super.onDestroy()
        bannerShown = false
    }

    private fun showBannerAd() {
        if (bannerShown) return
        bannerShown = true
        try {
            val bannerContainer = findViewById<View>(R.id.banner_container)
            if (bannerContainer != null) {
                bannerContainer.visibility = View.VISIBLE
                AdManager.loadBanner(this, bannerContainer as android.widget.FrameLayout)
            }
        } catch (e: Exception) {
            // Log but don't crash on banner ad issues
            android.util.Log.e("MainActivity", "Failed to load banner: ${e.message}")
        }
    }

    private fun switchFragment(fragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            // Fallback: try commit instead
            try {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit()
            } catch (e2: Exception) {
                android.util.Log.e("MainActivity", "Fragment transaction failed: ${e2.message}")
            }
        }
    }
}
