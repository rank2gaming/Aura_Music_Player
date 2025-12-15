package com.rank2gaming.aura.audio

import android.content.Context
import android.content.SharedPreferences

class AudioSettingsWatcher(
    private val context: Context,
    private val listener: AudioFeatureListener
) : SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("AuraAudioPrefs", Context.MODE_PRIVATE)

    fun startWatching() {
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    fun stopWatching() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == null) return

        when (key) {
            HDAudioManager.KEY_HD_ENABLED -> {
                val enabled = HDAudioManager.isHdEnabled(context)
                val boost = HDAudioManager.getVolumeBoost(context)
                listener.onHdAudioChanged(enabled, boost)
            }

            // Gain Knob (Decoupled: Only Boost part sent here)
            AudioEnhancerManager.KEY_GAIN_BOOST -> {
                val boostVal = AudioEnhancerManager.getValue(context, AudioEnhancerManager.KEY_GAIN_BOOST)
                val targetMb = (boostVal * 300).toInt() // Scale 0-5 to approx 1500mB
                listener.onVolumeBoostChanged(targetMb)
            }

            // Stereo Knob (Boosted)
            AudioEnhancerManager.KEY_STEREO -> {
                val stereoVal = AudioEnhancerManager.getValue(context, AudioEnhancerManager.KEY_STEREO)
                // Boosted Strength: 0-10 -> 0-2000 (Stronger effect)
                val strength = (stereoVal * 200).toInt().coerceIn(0, 1000)
                val isMono = HDAudioManager.isMonoAudio(context)
                listener.onVirtualizerChanged(strength, isMono)
            }

            // API Bass Boost Knob
            AudioEnhancerManager.KEY_BASS_BOOST_API -> {
                val strength = (AudioEnhancerManager.getValue(context, key) * 100).toInt().coerceIn(0, 1000)
                listener.onBassBoostChanged(strength)
            }

            // Surround Knob (Maps to Virtualizer)
            AudioEnhancerManager.KEY_SURROUND -> {
                val strength = (AudioEnhancerManager.getValue(context, key) * 100).toInt().coerceIn(0, 1000)
                val isMono = HDAudioManager.isMonoAudio(context)
                listener.onVirtualizerChanged(strength, isMono)
            }

            // Booster EQ Knobs (Trigger Update)
            AudioEnhancerManager.KEY_DEFINITION,
            AudioEnhancerManager.KEY_CLARITY,
            AudioEnhancerManager.KEY_BRIGHTNESS,
            AudioEnhancerManager.KEY_EQUAKE,
            AudioEnhancerManager.KEY_BASS,
            AudioEnhancerManager.KEY_BOXY,
            AudioEnhancerManager.KEY_TREBLE -> {
                listener.onBoosterEffectChanged(0, 0f)
            }
        }
    }
}