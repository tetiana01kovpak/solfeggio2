package io.github.tetiana01kovpak.solfeggio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Streams a pure sine tone through an AudioTrack. Changing the tone fades the
 * old one out before fading the new one in, so switching never clicks.
 */
class ToneEngine {
    @Volatile private var targetHz = 0.0
    @Volatile private var fadeSeconds = 0.08
    @Volatile private var volume = 0.2
    @Volatile private var running = false
    private var thread: Thread? = null

    fun setVolume(value: Float) {
        volume = value.toDouble()
    }

    fun play(hz: Double, fade: Double) {
        fadeSeconds = fade
        targetHz = hz
        if (!running) {
            running = true
            thread = Thread(::loop, "tone").also { it.start() }
        }
    }

    fun stop(fade: Double) {
        fadeSeconds = fade
        targetHz = 0.0
    }

    fun release() {
        running = false
        thread?.join(500)
        thread = null
    }

    private fun loop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val minBuffer = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(max(minBuffer, CHUNK * 4 * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track.play()

        val buffer = FloatArray(CHUNK)
        var hz = 0.0
        var phase = 0.0
        var envelope = 0.0
        var gain = volume
        while (running) {
            val target = targetHz
            val step = 1.0 / (max(fadeSeconds, 0.005) * SAMPLE_RATE)
            val targetGain = volume
            for (i in buffer.indices) {
                if (target > 0 && hz == target) {
                    envelope = min(1.0, envelope + step)
                } else {
                    envelope = max(0.0, envelope - step)
                    if (envelope == 0.0 && hz != target) {
                        hz = target
                        phase = 0.0
                    }
                }
                gain += (targetGain - gain) * GAIN_SMOOTHING
                buffer[i] = if (hz > 0) (sin(phase) * envelope * gain).toFloat() else 0f
                phase += TWO_PI * hz / SAMPLE_RATE
                if (phase >= TWO_PI) phase -= TWO_PI
            }
            track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
        }
        track.pause()
        track.flush()
        track.release()
    }

    private companion object {
        const val SAMPLE_RATE = 48_000
        const val CHUNK = 512
        const val TWO_PI = 2 * PI
        const val GAIN_SMOOTHING = 0.0005 // ~40 ms time constant at 48 kHz
    }
}
