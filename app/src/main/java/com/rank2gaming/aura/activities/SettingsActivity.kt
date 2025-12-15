package com.rank2gaming.aura.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rank2gaming.aura.audio.EqualizerSettingsActivity // IMPORT FROM NEW MODULE
import com.rank2gaming.aura.audio.HighDefAudioSettingsActivity // IMPORT FROM NEW MODULE
import com.rank2gaming.aura.databinding.ActivitySettingsBinding
import com.rank2gaming.aura.service.WaterMonitorService
import com.rank2gaming.aura.utils.SettingsManager
import com.rank2gaming.aura.utils.ThemeManager

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ThemeManager.applyTheme(this, binding.root)

        setupListeners()
        setupWaterFeatures()
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnTheme.setOnClickListener {
            startActivity(Intent(this, ThemeActivity::class.java))
        }

        binding.btnHdAudioSettings.setOnClickListener {
            startActivity(Intent(this, HighDefAudioSettingsActivity::class.java))
        }

        binding.btnOpenEqualizer.setOnClickListener {
            startActivity(Intent(this, EqualizerSettingsActivity::class.java))
        }

        binding.btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        binding.btnPrivacy.setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
        }
    }

    private fun setupWaterFeatures() {
        try {
            binding.switchWaterDetect.isChecked = SettingsManager.isWaterDetectEnabled(this)
            binding.switchWaterDetect.setOnCheckedChangeListener { _, isChecked ->
                SettingsManager.setWaterDetectEnabled(this, isChecked)
                if (isChecked) {
                    WaterMonitorService.startMonitoring(this)
                    Toast.makeText(this, "Water Detector Active", Toast.LENGTH_SHORT).show()
                } else {
                    WaterMonitorService.stopMonitoring(this)
                    Toast.makeText(this, "Water Detector Disabled", Toast.LENGTH_SHORT).show()
                }
            }

            binding.switchWaterNotif.isChecked = SettingsManager.isWaterNotifEnabled(this)
            binding.switchWaterNotif.setOnCheckedChangeListener { _, isChecked ->
                SettingsManager.setWaterNotifEnabled(this, isChecked)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}