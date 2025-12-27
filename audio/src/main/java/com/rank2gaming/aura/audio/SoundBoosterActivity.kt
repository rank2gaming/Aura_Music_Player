package com.rank2gaming.aura.audio

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.rank2gaming.aura.audio.databinding.ActivitySoundBoosterBinding
import com.rank2gaming.aura.audio.views.RotaryKnobView
import kotlin.math.roundToInt

class SoundBoosterActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySoundBoosterBinding
    private var audioService: IAudioService? = null
    private var hapticsEnabled = true
    private var isMasterEnabled = true
    private lateinit var audioManager: AudioManager

    private val presets = listOf("Custom", "Normal", "Rock", "Jazz", "Classical", "Dance", "Hip-Hop", "Pop")

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is AudioBinder) { audioService = service.getService() }
        }
        override fun onServiceDisconnected(name: ComponentName?) { audioService = null }
    }

    @RequiresPermission(Manifest.permission.INTERNET)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySoundBoosterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.decorView.setBackgroundColor(Color.WHITE)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        checkBatteryOptimization()
        setupUI()
        loadBannerAd()
        bindMusicService()
        syncGainKnobToSystemVolume()
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = packageName
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent()
                    intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    private fun bindMusicService() {
        val intent = Intent()
        intent.component = ComponentName("com.rank2gaming.aura", "com.rank2gaming.aura.service.MusicService")
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    @RequiresPermission(Manifest.permission.INTERNET)
    private fun loadBannerAd() {
        try {
            val adView = AdView(this)
            adView.setAdSize(AdSize.BANNER)
            adView.adUnitId = "ca-app-pub-3940256099942544/6300978111"
            binding.adContainer.addView(adView)
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun syncGainKnobToSystemVolume() {
        try {
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            var mappedKnobValue = (currentVol.toFloat() / maxVol.toFloat()) * 15f

            if (currentVol == maxVol) {
                val savedBoost = AudioEnhancerManager.getValue(this, AudioEnhancerManager.KEY_GAIN_BOOST)
                mappedKnobValue = 15f + savedBoost
            }

            binding.knobGain.setValue(mappedKnobValue)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun setupUI() {
        setupKnobGroup(binding.layoutDef.knob, binding.layoutDef.label, "Definition", AudioEnhancerManager.KEY_DEFINITION, Color.parseColor("#4FC3F7"))
        setupKnobGroup(binding.layoutClarity.knob, binding.layoutClarity.label, "Clarity", AudioEnhancerManager.KEY_CLARITY, Color.parseColor("#BA68C8"))
        setupKnobGroup(binding.layoutBright.knob, binding.layoutBright.label, "Brightness", AudioEnhancerManager.KEY_BRIGHTNESS, Color.parseColor("#FFD54F"))

        setupKnobGroup(binding.layoutEquake.knob, binding.layoutEquake.label, "e-Quake", AudioEnhancerManager.KEY_EQUAKE, Color.BLACK)
        setupKnobGroup(binding.layoutBass.knob, binding.layoutBass.label, "Bass", AudioEnhancerManager.KEY_BASS, Color.parseColor("#8BC34A"))
        setupKnobGroup(binding.layoutBoxy.knob, binding.layoutBoxy.label, "Anti-Boxy", AudioEnhancerManager.KEY_BOXY, Color.parseColor("#29B6F6"))

        setupKnobGroup(binding.layoutBassBoostApi.knob, binding.layoutBassBoostApi.label, "Bass Boost", AudioEnhancerManager.KEY_BASS_BOOST_API, Color.parseColor("#FF5722"))
        setupKnobGroup(binding.layoutTreble.knob, binding.layoutTreble.label, "Treble", AudioEnhancerManager.KEY_TREBLE, Color.parseColor("#00BCD4"))
        setupKnobGroup(binding.layoutSurround.knob, binding.layoutSurround.label, "Surround", AudioEnhancerManager.KEY_SURROUND, Color.parseColor("#9C27B0"))

        setupKnob(binding.knobGain, "gain", Color.parseColor("#FF9800"), min = 0f, max = 30f)
        binding.knobGain.onValueChanged = { value ->
            if (isMasterEnabled) {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

                if (value <= 15f) {
                    val ratio = value / 15f
                    val targetVol = (ratio * maxVol).roundToInt().coerceIn(0, maxVol)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                    AudioEnhancerManager.saveValue(this, AudioEnhancerManager.KEY_GAIN_BOOST, 0f)
                } else {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
                    val boostAmount = value - 15f
                    AudioEnhancerManager.saveValue(this, AudioEnhancerManager.KEY_GAIN_BOOST, boostAmount)
                }
            }
        }

        setupKnob(binding.knobStereo, AudioEnhancerManager.KEY_STEREO, Color.parseColor("#03A9F4"), min = 0f, max = 10f)

        binding.switchMaster.setOnCheckedChangeListener { _, isChecked ->
            isMasterEnabled = isChecked
            updateEnableStates()
            if (!isChecked) {
                disableAllEffects()
            } else {
                syncGainKnobToSystemVolume()
                saveCurrentKnobValues()
            }
        }
        binding.switchMaster.isChecked = true

        binding.btnHaptic.setOnClickListener {
            hapticsEnabled = !hapticsEnabled
            updateHapticsState()
            Toast.makeText(this, if(hapticsEnabled) "Vibration ON" else "Vibration OFF", Toast.LENGTH_SHORT).show()
        }

        val adapter = ArrayAdapter(this, R.layout.spinner_item_dark, presets)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPresets.adapter = adapter
        binding.spinnerPresets.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos > 0) loadPreset(presets[pos])
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        binding.btnReset.setOnClickListener { resetAll() }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun updateEnableStates() {
        val allKnobs = getAllKnobs() + listOf(binding.knobGain, binding.knobStereo)
        val alpha = if (isMasterEnabled) 1f else 0.4f
        allKnobs.forEach { it.isEnabled = isMasterEnabled; it.alpha = alpha }
        binding.spinnerPresets.isEnabled = isMasterEnabled; binding.spinnerPresets.alpha = alpha
        binding.btnReset.isEnabled = isMasterEnabled; binding.btnReset.alpha = alpha
    }

    private fun disableAllEffects() {
        val keys = listOf(
            AudioEnhancerManager.KEY_DEFINITION, AudioEnhancerManager.KEY_CLARITY, AudioEnhancerManager.KEY_BRIGHTNESS,
            AudioEnhancerManager.KEY_EQUAKE, AudioEnhancerManager.KEY_BASS, AudioEnhancerManager.KEY_BOXY,
            AudioEnhancerManager.KEY_BASS_BOOST_API, AudioEnhancerManager.KEY_TREBLE, AudioEnhancerManager.KEY_SURROUND,
            AudioEnhancerManager.KEY_STEREO, AudioEnhancerManager.KEY_GAIN_BOOST
        )
        keys.forEach { AudioEnhancerManager.saveValue(this, it, 0f) }
    }

    private fun updateHapticsState() {
        val allKnobs = getAllKnobs() + listOf(binding.knobGain, binding.knobStereo)
        allKnobs.forEach { it.setHapticsEnabled(hapticsEnabled) }
    }

    private fun getAllKnobs() = listOf(
        binding.layoutDef.knob, binding.layoutClarity.knob, binding.layoutBright.knob,
        binding.layoutEquake.knob, binding.layoutBass.knob, binding.layoutBoxy.knob,
        binding.layoutBassBoostApi.knob, binding.layoutTreble.knob, binding.layoutSurround.knob
    )

    private fun setupKnobGroup(knob: RotaryKnobView, label: TextView, title: String, key: String, color: Int) {
        label.text = title
        setupKnob(knob, key, color, -10f, 10f)
    }

    private fun setupKnob(knob: RotaryKnobView, key: String, color: Int, min: Float, max: Float) {
        knob.setTrackColor(color)
        knob.setRange(min, max)
        if (key != "gain") {
            knob.setValue(AudioEnhancerManager.getValue(this, key))
            knob.onValueChanged = { value ->
                if (isMasterEnabled) AudioEnhancerManager.saveValue(this, key, value)
            }
        }
    }

    private fun loadPreset(name: String) {
        if (!isMasterEnabled) return
        getAllKnobs().forEach { it.setValue(0f) }
        when(name) {
            "Rock" -> {
                binding.layoutBass.knob.setValue(7f)
                binding.layoutBassBoostApi.knob.setValue(8f)
                binding.layoutTreble.knob.setValue(5f)
                binding.layoutBoxy.knob.setValue(2f)
                // setGain(17f) REMOVED
            }
            "Pop" -> {
                binding.layoutClarity.knob.setValue(5f)
                binding.layoutBass.knob.setValue(3f)
                binding.layoutTreble.knob.setValue(4f)
                // setGain(16f) REMOVED
                binding.knobStereo.setValue(3f)
            }
            "Jazz" -> {
                binding.layoutDef.knob.setValue(3f)
                binding.layoutClarity.knob.setValue(4f)
                binding.layoutBass.knob.setValue(2f)
                binding.layoutSurround.knob.setValue(4f)
            }
            "Dance" -> {
                binding.layoutBass.knob.setValue(8f)
                binding.layoutBassBoostApi.knob.setValue(6f)
                binding.layoutBright.knob.setValue(3f)
                // setGain(22f) REMOVED
                binding.knobStereo.setValue(6f)
            }
            "Classical" -> {
                binding.layoutDef.knob.setValue(4f)
                binding.layoutClarity.knob.setValue(3f)
                binding.layoutTreble.knob.setValue(3f)
                binding.layoutSurround.knob.setValue(5f)
            }
            "Hip-Hop" -> {
                binding.layoutBass.knob.setValue(6f)
                binding.layoutBassBoostApi.knob.setValue(7f)
                binding.layoutEquake.knob.setValue(5f)
                // setGain(20f) REMOVED
            }
            "Normal" -> {
                // syncGainKnobToSystemVolume() REMOVED
                binding.knobStereo.setValue(0f)
            }
        }
        saveCurrentKnobValues()
    }

    private fun setGain(value: Float) {
        binding.knobGain.setValue(value)
        binding.knobGain.onValueChanged?.invoke(value)
    }

    private fun saveCurrentKnobValues() {
        if (!isMasterEnabled) return
        AudioEnhancerManager.saveValue(this, AudioEnhancerManager.KEY_BASS, binding.layoutBass.knob.getValue())
        AudioEnhancerManager.saveValue(this, AudioEnhancerManager.KEY_EQUAKE, binding.layoutEquake.knob.getValue())
        AudioEnhancerManager.saveValue(this, AudioEnhancerManager.KEY_BRIGHTNESS, binding.layoutBright.knob.getValue())
        AudioEnhancerManager.saveValue(this, AudioEnhancerManager.KEY_CLARITY, binding.layoutClarity.knob.getValue())
        AudioEnhancerManager.saveValue(this, AudioEnhancerManager.KEY_DEFINITION, binding.layoutDef.knob.getValue())
        AudioEnhancerManager.saveValue(this, AudioEnhancerManager.KEY_BOXY, binding.layoutBoxy.knob.getValue())
        AudioEnhancerManager.saveValue(this, AudioEnhancerManager.KEY_BASS_BOOST_API, binding.layoutBassBoostApi.knob.getValue())
        AudioEnhancerManager.saveValue(this, AudioEnhancerManager.KEY_TREBLE, binding.layoutTreble.knob.getValue())
        AudioEnhancerManager.saveValue(this, AudioEnhancerManager.KEY_SURROUND, binding.layoutSurround.knob.getValue())
        AudioEnhancerManager.saveValue(this, AudioEnhancerManager.KEY_STEREO, binding.knobStereo.getValue())
    }

    private fun resetAll() {
        if (!isMasterEnabled) return
        getAllKnobs().forEach { it.setValue(0f) }
        binding.knobStereo.setValue(0f)
        disableAllEffects()
        syncGainKnobToSystemVolume()
        Toast.makeText(this, "Reset", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }
}