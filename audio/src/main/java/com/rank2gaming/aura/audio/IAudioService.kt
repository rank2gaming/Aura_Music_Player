package com.rank2gaming.aura.audio

import android.media.audiofx.Equalizer

interface IAudioService {
    fun getEqualizer(): Equalizer?
    fun setBassStrength(strength: Short)
    fun setEQBandLevel(band: Short, level: Short)
}

interface AudioBinder {
    fun getService(): IAudioService
}