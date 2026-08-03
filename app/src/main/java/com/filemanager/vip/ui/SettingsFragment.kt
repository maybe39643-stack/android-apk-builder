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
        txtVipStatus = view.findViewById(R.id.txt_vip_status)
        btnWatchAd = view.findViewById(R.id.btn_watch_ad_vip)
        switchDark = view.findViewById(R.id.switch_dark)
        vipCard = view.findViewById(R.id.vip_card_settings)

        updateVipUi()

        vipCard.setOnClickListener {
            if (!Preferences.isVip()) {
                btnWatchAd.performClick()
            }
        }

        btnWatchAd.setOnClickListener {
            AdManager.showRewarded(requireActivity()) {
                Preferences.setVip(true)
                updateVipUi()
                Toast.makeText(requireContext(), R.string.vip_active, Toast.LENGTH_LONG).show()
            }
        }

        switchDark.isChecked = Preferences.isDarkMode()
        switchDark.setOnCheckedChangeListener { _, isChecked ->
            Preferences.setDarkMode(isChecked)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            requireActivity().recreate()
        }
        return view
    }

    private fun updateVipUi() {
        val isVip = Preferences.isVip()
        if (isVip) {
            txtVipStatus.text = getString(R.string.vip_active)
            btnWatchAd.visibility = View.GONE
        } else {
            txtVipStatus.text = getString(R.string.upgrade_vip)
            btnWatchAd.visibility = View.VISIBLE
        }
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}
