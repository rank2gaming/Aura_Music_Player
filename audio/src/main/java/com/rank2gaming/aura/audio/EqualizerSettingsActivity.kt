package com.rank2gaming.aura.audio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.rank2gaming.aura.audio.databinding.ActivityEqualizerSettingsBinding

class EqualizerSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEqualizerSettingsBinding
    private var audioService: IAudioService? = null
    private val bandSeekBars = ArrayList<SeekBar>()
    private var isUserInteracting = false

    private val customPresetNames = listOf("Custom", "Rock", "Pop", "Jazz", "Heavy Metal", "Folk", "Dance", "Flat", "Normal")
    private val customPresetValues = mapOf(
        "Rock" to shortArrayOf(500, 300, -100, 300, 500),
        "Pop" to shortArrayOf(-100, 200, 500, 100, -200),
        "Jazz" to shortArrayOf(400, 200, -200, 200, 500),
        "Heavy Metal" to shortArrayOf(400, 100, 900, 300, 0),
        "Folk" to shortArrayOf(300, 0, -100, 200, -100),
        "Dance" to shortArrayOf(600, 0, 400, 400, 100),
        "Flat" to shortArrayOf(0, 0, 0, 0, 0),
        "Normal" to shortArrayOf(300, 0, 0, 0, 300)
    )

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is AudioBinder) {
                audioService = service.getService()
                if (audioService?.getEqualizer() != null) {
                    binding.txtStatus.visibility = View.GONE
                    binding.layoutBands.visibility = View.VISIBLE
                    setupDynamicBands()
                    setupPresets()
                } else {
                    binding.txtStatus.text = "Play a song to enable Equalizer"
                }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) { audioService = null }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEqualizerSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Self-contained theme logic (Dark Background)
        binding.root.setBackgroundColor(Color.parseColor("#121212"))

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val intent = Intent()
        intent.component = ComponentName("com.rank2gaming.aura", "com.rank2gaming.aura.service.MusicService")
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        setupStaticControls()
        setupWaterEjector()
    }

    private fun setupStaticControls() {
        binding.switchEqMaster.isChecked = AudioEffectManager.isEqEnabled(this)
        binding.switchEqMaster.setOnCheckedChangeListener { _, isChecked ->
            AudioEffectManager.setEqEnabled(this, isChecked)
            audioService?.getEqualizer()?.enabled = isChecked
            toggleBands(isChecked)
        }

        binding.seekBass.progress = AudioEffectManager.getBassStrength(this) / 10
        binding.seekBass.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                if(u) {
                    audioService?.setBassStrength((p * 10).toShort())
                    AudioEffectManager.saveBassStrength(this@EqualizerSettingsActivity, p * 10)
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        binding.seekTreble.progress = (HDAudioManager.getTrebleLevel(this) + 1500) / 30
        binding.seekTreble.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                if(u) HDAudioManager.setTrebleLevel(this@EqualizerSettingsActivity, (p * 30) - 1500)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        binding.switchMono.isChecked = HDAudioManager.isMonoAudio(this)
        binding.switchMono.setOnCheckedChangeListener { _, c -> HDAudioManager.setMonoAudio(this, c) }

        val hpTypes = arrayOf("Default", "Earbuds", "Over-Ear", "Bluetooth Speaker")
        binding.spinnerHeadphoneType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, hpTypes)
        binding.spinnerHeadphoneType.setSelection(AudioEffectManager.getHeadphoneType(this))
        binding.spinnerHeadphoneType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if(isUserInteracting) {
                    AudioEffectManager.setHeadphoneType(this@EqualizerSettingsActivity, pos)
                    applyHeadphoneCurve(pos)
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        binding.spinnerHeadphoneType.setOnTouchListener { _, _ -> isUserInteracting = true; false }

        binding.btnReset.setOnClickListener {
            HDAudioManager.resetEffects(this)
            AudioEffectManager.saveBassStrength(this, 0)
            recreate()
        }
        binding.btnClose.setOnClickListener { finish() }
    }

    private fun applyHeadphoneCurve(type: Int) {
        val eq = audioService?.getEqualizer() ?: return
        val bands = eq.numberOfBands.toInt()
        val min = eq.bandLevelRange[0]
        for(i in 0 until bands) {
            val offset = when(type) { 1-> if(i==0||i==bands-1) 300 else 0; 2-> if(i==1||i==2) 200 else 0; 3-> if(i==0||i==1) 400 else 0; else->0 }
            val level = (0+offset).toShort()
            audioService?.setEQBandLevel(i.toShort(), level)
            AudioEffectManager.saveBandLevel(this, i.toShort(), level.toInt())
            if(i < bandSeekBars.size) bandSeekBars[i].progress = level - min
        }
    }

    private fun setupPresets() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, customPresetNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPresets.adapter = adapter
        binding.spinnerPresets.setSelection(AudioEffectManager.getPreset(this))
        binding.spinnerPresets.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                AudioEffectManager.savePreset(this@EqualizerSettingsActivity, position)
                if (position > 0) {
                    customPresetValues[customPresetNames[position]]?.let { applyManualPreset(it) }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun applyManualPreset(values: ShortArray) {
        val eq = audioService?.getEqualizer() ?: return
        val min = eq.bandLevelRange[0]
        for (i in 0 until eq.numberOfBands) {
            val level = if (i < values.size) values[i] else 0
            audioService?.setEQBandLevel(i.toShort(), level.toShort())
            AudioEffectManager.saveBandLevel(this, i.toShort(), level.toInt())
            if (i < bandSeekBars.size) bandSeekBars[i].progress = level - min
        }
    }

    private fun setupDynamicBands() {
        val eq = audioService?.getEqualizer() ?: return
        binding.layoutBands.removeAllViews()
        bandSeekBars.clear()
        val min = eq.bandLevelRange[0]
        val max = eq.bandLevelRange[1]
        val tealColor = ContextCompat.getColor(this, R.color.teal_200)

        for (i in 0 until eq.numberOfBands) {
            val idx = i.toShort()
            val freq = eq.getCenterFreq(idx) / 1000
            val label = TextView(this).apply {
                text = if(freq < 1000) "$freq Hz" else "${freq/1000} kHz"
                setTextColor(Color.WHITE)
                textSize = 14f
                setPadding(12, 16, 0, 4)
            }
            binding.layoutBands.addView(label)
            val seek = SeekBar(this).apply {
                this.max = max - min
                this.progress = AudioEffectManager.getBandLevel(context, idx) - min
                thumbTintList = ColorStateList.valueOf(tealColor)
                progressTintList = ColorStateList.valueOf(tealColor)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                        if(u) {
                            audioService?.setEQBandLevel(idx, (p + min).toShort())
                            if(binding.spinnerPresets.selectedItemPosition != 0) {
                                binding.spinnerPresets.setSelection(0)
                                AudioEffectManager.savePreset(context, 0)
                            }
                        }
                    }
                    override fun onStartTrackingTouch(p: SeekBar?) {}
                    override fun onStopTrackingTouch(p: SeekBar?) {
                        AudioEffectManager.saveBandLevel(context, idx, (progress + min))
                    }
                })
            }
            bandSeekBars.add(seek)
            binding.layoutBands.addView(seek)
        }
    }

    private fun toggleBands(enabled: Boolean) {
        bandSeekBars.forEach { it.isEnabled = enabled }
        binding.layoutBands.alpha = if(enabled) 1f else 0.5f
    }

    private fun setupWaterEjector() {
        try {
            binding.seekWaterEject.max = 150
            binding.seekWaterEject.progress = HDAudioManager.getVolumeBoost(this)
            binding.seekWaterEject.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                    if(u) {
                        HDAudioManager.setVolumeBoost(this@EqualizerSettingsActivity, p)
                        binding.txtWaterStatus.text = if(p > 100) "⚠️ EJECT MODE" else "Standard"
                        binding.txtWaterStatus.setTextColor(if(p > 100) Color.RED else Color.WHITE)
                    }
                }
                override fun onStartTrackingTouch(p: SeekBar?) {}
                override fun onStopTrackingTouch(p: SeekBar?) {}
            })
        } catch(e: Exception){}
    }

    override fun onDestroy() {
        super.onDestroy()
        if(audioService != null) unbindService(serviceConnection)
    }
}