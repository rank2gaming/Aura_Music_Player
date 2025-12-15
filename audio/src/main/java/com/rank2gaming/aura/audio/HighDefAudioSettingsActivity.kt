package com.rank2gaming.aura.audio

import android.graphics.Color
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.rank2gaming.aura.audio.databinding.ActivityHighDefAudioSettingsBinding

class HighDefAudioSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHighDefAudioSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHighDefAudioSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.setBackgroundColor(Color.parseColor("#121212"))

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val isEnabled = HDAudioManager.isHdEnabled(this)
        binding.switchHdMaster.isChecked = isEnabled
        toggleControls(isEnabled)

        val vol = HDAudioManager.getVolumeBoost(this)
        binding.seekVolumeBoost.progress = vol
        binding.txtVolumeVal.text = "$vol%"

        val surround = AudioEffectManager.getVirtualizerStrength(this) / 10
        binding.seekSurround.progress = surround
        binding.txtSurroundVal.text = "$surround%"

        binding.switchStereoTreble.isChecked = HDAudioManager.isStereoTreble(this)

        setupListeners()
    }

    private fun setupListeners() {
        binding.switchHdMaster.setOnCheckedChangeListener { _, isChecked ->
            HDAudioManager.setHdEnabled(this, isChecked)
            toggleControls(isChecked)
        }

        binding.seekVolumeBoost.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.txtVolumeVal.text = "$progress%"
                    HDAudioManager.setVolumeBoost(this@HighDefAudioSettingsActivity, progress)
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        binding.seekSurround.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.txtSurroundVal.text = "$progress%"
                    AudioEffectManager.saveVirtualizerStrength(this@HighDefAudioSettingsActivity, progress * 10)
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        binding.switchStereoTreble.setOnCheckedChangeListener { _, isChecked ->
            HDAudioManager.setStereoTreble(this, isChecked)
        }
    }

    private fun toggleControls(enabled: Boolean) {
        binding.seekVolumeBoost.isEnabled = enabled
        binding.seekSurround.isEnabled = enabled
        binding.switchStereoTreble.isEnabled = enabled
        binding.containerControls.alpha = if (enabled) 1.0f else 0.5f
    }
}