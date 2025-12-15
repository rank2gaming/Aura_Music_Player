package com.rank2gaming.aura.utils

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Custom AudioProcessor to handle Left/Right Stereo Balance for ExoPlayer.
 * Supports both 16-bit Integer and 32-bit Float PCM audio.
 */
@UnstableApi
class BalanceAudioProcessor : AudioProcessor {

    private var leftVolume = 1.0f
    private var rightVolume = 1.0f
    private var inputAudioFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat: AudioFormat = AudioFormat.NOT_SET
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    /**
     * Sets the stereo balance.
     * @param balance Value between 0 (Left) and 100 (Right). 50 is Center.
     */
    fun setBalance(balance: Float) {
        // Map 0..100 -> -1.0..1.0
        val norm = (balance - 50) / 50f

        // Logic:
        // Center (0.0): Left=1.0, Right=1.0
        // Full Left (-1.0): Left=1.0, Right=0.0
        // Full Right (1.0): Left=0.0, Right=1.0
        leftVolume = if (norm <= 0) 1.0f else 1.0f - norm
        rightVolume = if (norm >= 0) 1.0f else 1.0f + norm
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        val encoding = inputAudioFormat.encoding

        // We only support Stereo (2 channels)
        // We support 16-bit PCM (standard) and Float PCM (high definition)
        val isSupported = (encoding == C.ENCODING_PCM_16BIT || encoding == C.ENCODING_PCM_FLOAT) &&
                inputAudioFormat.channelCount == 2

        if (!isSupported) {
            return AudioFormat.NOT_SET
        }

        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        return outputAudioFormat
    }

    override fun isActive(): Boolean {
        // Processor is active only if volumes are modified (not 1.0/1.0)
        return inputAudioFormat != AudioFormat.NOT_SET && (leftVolume != 1.0f || rightVolume != 1.0f)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val position = inputBuffer.position()
        val limit = inputBuffer.limit()
        val remaining = limit - position

        // Resize buffer if needed
        if (buffer.capacity() < remaining) {
            buffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }

        if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
            // --- Process 32-bit Float PCM ---
            // 4 bytes per sample
            while (inputBuffer.hasRemaining()) {
                val left = inputBuffer.float
                val right = inputBuffer.float

                buffer.putFloat(left * leftVolume)
                buffer.putFloat(right * rightVolume)
            }
        } else {
            // --- Process 16-bit Int PCM ---
            // 2 bytes per sample
            while (inputBuffer.hasRemaining()) {
                val left = inputBuffer.short
                val right = inputBuffer.short

                buffer.putShort((left * leftVolume).toInt().toShort())
                buffer.putShort((right * rightVolume).toInt().toShort())
            }
        }

        inputBuffer.position(limit)
        buffer.flip()
        outputBuffer = buffer
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer == AudioProcessor.EMPTY_BUFFER
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
    }

    override fun reset() {
        flush()
        buffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
    }
}