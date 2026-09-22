package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class RetroAudioSynthesizer(private val context: Context) {
    private val sampleRate = 22050
    private val scope = CoroutineScope(Dispatchers.Default)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var isEnabled: Boolean = true
    var volume: Float = 0.85f

    fun triggerVibration(milliseconds: Long = 40, amplitude: Int = 180) {
        if (!isEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(milliseconds)
            }
        } catch (_: Exception) {
        }
    }

    fun playPs1StartupChord() {
        if (!isEnabled) return
        scope.launch {
            try {
                val durationSec = 3.0
                val totalSamples = (sampleRate * durationSec).toInt()
                val buffer = ShortArray(totalSamples)

                val chordFrequencies = doubleArrayOf(65.4, 130.8, 196.0, 261.6, 392.0, 523.2) // C2, C3, G3, C4, G4, C5

                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    var sample = 0.0

                    // Swelling envelope
                    val envelope = when {
                        t < 0.8 -> (t / 0.8)
                        t < 2.2 -> 1.0
                        else -> ((3.0 - t) / 0.8).coerceAtLeast(0.0)
                    }

                    for (f in chordFrequencies) {
                        sample += sin(2.0 * PI * f * t) * 0.15
                    }

                    // Shimmering harmonic overtone
                    if (t > 1.2) {
                        val chimeT = t - 1.2
                        sample += sin(2.0 * PI * 1046.5 * chimeT) * 0.25 * ((1.8 - chimeT) / 1.8).coerceAtLeast(0.0)
                    }

                    val finalSample = (sample * envelope * volume * Short.MAX_VALUE).coerceIn(
                        Short.MIN_VALUE.toDouble(),
                        Short.MAX_VALUE.toDouble()
                    )
                    buffer[i] = finalSample.toInt().toShort()
                }

                playBuffer(buffer)
            } catch (_: Exception) {
            }
        }
    }

    fun playN64Jump() {
        if (!isEnabled) return
        scope.launch {
            try {
                val durationSec = 0.18
                val totalSamples = (sampleRate * durationSec).toInt()
                val buffer = ShortArray(totalSamples)

                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = t / durationSec
                    // Pitch bend up from 220Hz to 660Hz
                    val currentFreq = 220.0 + (440.0 * progress * progress)
                    val envelope = 1.0 - progress
                    val sample = sin(2.0 * PI * currentFreq * t) * envelope * volume * Short.MAX_VALUE
                    buffer[i] = sample.toInt().toShort()
                }
                playBuffer(buffer)
            } catch (_: Exception) {
            }
        }
    }

    fun playN64Coin() {
        if (!isEnabled) return
        scope.launch {
            try {
                val durationSec = 0.35
                val totalSamples = (sampleRate * durationSec).toInt()
                val buffer = ShortArray(totalSamples)

                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val sample = if (t < 0.1) {
                        sin(2.0 * PI * 987.77 * t) // B5
                    } else {
                        val t2 = t - 0.1
                        sin(2.0 * PI * 1318.51 * t2) * (1.0 - (t2 / 0.25)).coerceAtLeast(0.0) // E6
                    }
                    buffer[i] = (sample * volume * 0.7 * Short.MAX_VALUE).toInt().toShort()
                }
                playBuffer(buffer)
            } catch (_: Exception) {
            }
        }
    }

    fun playLaserShot() {
        if (!isEnabled) return
        scope.launch {
            try {
                val durationSec = 0.15
                val totalSamples = (sampleRate * durationSec).toInt()
                val buffer = ShortArray(totalSamples)

                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = t / durationSec
                    val freq = 1200.0 * (1.0 - progress) + 80.0
                    val envelope = 1.0 - progress
                    val sample = sin(2.0 * PI * freq * t) * envelope * volume * Short.MAX_VALUE
                    buffer[i] = sample.toInt().toShort()
                }
                playBuffer(buffer)
            } catch (_: Exception) {
            }
        }
    }

    fun playEnginePulse(rpmPercent: Float) {
        if (!isEnabled) return
        scope.launch {
            try {
                val durationSec = 0.08
                val totalSamples = (sampleRate * durationSec).toInt()
                val buffer = ShortArray(totalSamples)
                val freq = 70.0 + (rpmPercent * 160.0)

                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    // Low growl sawtooth-like blend
                    val sample = (sin(2.0 * PI * freq * t) + 0.5 * sin(2.0 * PI * (freq * 2) * t)) * 0.4 * volume * Short.MAX_VALUE
                    buffer[i] = sample.toInt().toShort()
                }
                playBuffer(buffer)
            } catch (_: Exception) {
            }
        }
    }

    private fun playBuffer(buffer: ShortArray) {
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        audioTrack.setNotificationMarkerPosition(buffer.size)
        audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack?) {
                track?.release()
            }
            override fun onPeriodicNotification(track: AudioTrack?) {}
        })
    }
}
