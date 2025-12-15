package com.rank2gaming.aura.audio

/**
 * Centralized Interface for Audio Signals including new Booster features.
 */
interface AudioFeatureListener {
    // High-Definition Audio Signals
    fun onHdAudioChanged(enabled: Boolean, volumeBoost: Int)
    fun onVolumeBoostChanged(boost: Int) // <--- ADDED THIS MISSING METHOD

    // Equalizer & Effects Signals
    fun onBassBoostChanged(strength: Int)
    fun onVirtualizerChanged(strength: Int, isMono: Boolean)
    fun onEqualizerEnabled(enabled: Boolean)
    fun onEqualizerBandChanged(band: Short, level: Int)

    // Global Settings
    fun onMonoAudioChanged(enabled: Boolean)

    // Sound Booster Signals
    fun onBoosterEffectChanged(effectType: Int, valueDb: Float)

    companion object {
        const val EFFECT_DEFINITION = 0
        const val EFFECT_CLARITY = 1
        const val EFFECT_BRIGHTNESS = 2
        const val EFFECT_SUB_BASS = 3
        const val EFFECT_BASS_EQ = 4
        const val EFFECT_ANTI_BOXY = 5
        const val EFFECT_GAIN_BOOST = 6
        const val EFFECT_STEREO_WIDEN = 7
    }
}