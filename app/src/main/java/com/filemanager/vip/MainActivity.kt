package com.filemanager.vip

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
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
    private lateinit var bannerContainer: FrameLayout
    private var bannerShown = false
    private var currentFragment: Fragment? = null
    private var isSwitching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)
        bannerContainer = findViewById(R.id.banner_container)

        // Restore or create initial fragment
        if (savedInstanceState == null) {
            currentFragment = HomeFragment()
            switchFragment(currentFragment!!)
        } else {
            // Restore current fragment state
            val frag = supportFragmentManager.findFragmentById(R.id.container)
            if (frag != null) {
                currentFragment = frag
            } else {
                currentFragment = HomeFragment()
                switchFragment(currentFragment!!)
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            if (isSwitching) return@setOnItemSelectedListener true
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
                    isSwitching = true
                    currentFragment = newFragment
                    switchFragment(newFragment)
                    isSwitching = false
                }
            }
            true
        }

        // Set default selected tab to Home
        try {
            bottomNav.selectedItemId = R.id.nav_home
        } catch (e: Exception) {
            // If not available, create default
            if (currentFragment == null) {
                currentFragment = HomeFragment()
                switchFragment(currentFragment!!)
            }
        }
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
            bannerContainer.visibility = View.VISIBLE
            AdManager.loadBanner(this, bannerContainer)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to load banner: ${e.message}")
        }
    }

    private fun switchFragment(fragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment, fragment.javaClass.simpleName)
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
