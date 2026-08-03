package com.filemanager.vip.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.filemanager.vip.R
import com.filemanager.vip.ads.AdManager
import com.filemanager.vip.util.Preferences
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment() {

    private lateinit var txtVipStatus: TextView
    private lateinit var btnWatchAd: MaterialButton
    private lateinit var switchDark: SwitchMaterial
    private lateinit var vipCard: MaterialCardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        try {
            txtVipStatus = view.findViewById(R.id.txt_vip_status)
            btnWatchAd = view.findViewById(R.id.btn_watch_ad_vip)
            switchDark = view.findViewById(R.id.switch_dark)
            vipCard = view.findViewById(R.id.vip_card_settings)

            updateVipUi()

            vipCard.setOnClickListener {
                try {
                    if (!Preferences.isVip()) {
                        btnWatchAd.performClick()
                    }
                } catch (e: Exception) { /* ignore */ }
            }

            btnWatchAd.setOnClickListener {
                try {
                    val activity = requireActivity()
                    AdManager.showRewarded(activity) {
                        try {
                            Preferences.setVip(true)
                            updateVipUi()
                            Toast.makeText(requireContext(), R.string.vip_active, Toast.LENGTH_LONG).show()
                        } catch (e: Exception) { /* ignore */ }
                    }
                } catch (e: Exception) {
                    try {
                        Toast.makeText(requireContext(), getString(R.string.error), Toast.LENGTH_SHORT).show()
                    } catch (e2: Exception) { /* ignore */ }
                }
            }

            switchDark.isChecked = Preferences.isDarkMode()
            switchDark.setOnCheckedChangeListener { _, isChecked ->
                try {
                    Preferences.setDarkMode(isChecked)
                    AppCompatDelegate.setDefaultNightMode(
                        if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                    )
                    requireActivity().recreate()
                } catch (e: Exception) { /* ignore */ }
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsFragment", "Setup error: ${e.message}")
        }
        return view
    }

    private fun updateVipUi() {
        try {
            val isVip = Preferences.isVip()
            if (isVip) {
                txtVipStatus.text = getString(R.string.vip_active)
                btnWatchAd.visibility = View.GONE
            } else {
                txtVipStatus.text = getString(R.string.upgrade_vip)
                btnWatchAd.visibility = View.VISIBLE
            }
        } catch (e: Exception) { /* ignore */ }
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}
