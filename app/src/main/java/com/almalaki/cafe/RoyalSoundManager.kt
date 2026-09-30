package com.almalaki.cafe

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Elegant short UI sounds generated locally; no audio files or network needed. */
object RoyalSoundManager {
    private const val SAMPLE_RATE = 44100
    private var clickSamples: ShortArray? = null
    private var orderSamples: ShortArray? = null

    private fun envelope(t: Double, duration: Double): Double {
        val attack = 0.008
        val release = 0.045
        val a = if (t < attack) t / attack else 1.0
        val r = if (t > duration - release) (duration - t) / release else 1.0
        return a.coerceIn(0.0, 1.0) * r.coerceIn(0.0, 1.0)
    }

    private fun makeClick(): ShortArray {
        val duration = 0.075
        val count = (SAMPLE_RATE * duration).toInt()
        return ShortArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val tone = sin(2.0 * PI * 1550.0 * t) * 0.62 +
                    sin(2.0 * PI * 2300.0 * t) * 0.28
            (tone * envelope(t, duration) * Short.MAX_VALUE * 0.34).toInt().toShort()
        }
    }

    private fun makeOrder(): ShortArray {
        val notes = doubleArrayOf(880.0, 1174.66, 1567.98)
        val noteLength = 0.16
        val gap = 0.025
        val duration = notes.size * noteLength + (notes.size - 1) * gap + 0.08
        val count = (SAMPLE_RATE * duration).toInt()
        return ShortArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            var value = 0.0
            for (n in notes.indices) {
                val start = n * (noteLength + gap)
                val local = t - start
                if (local >= 0.0 && local < noteLength) {
                    val e = envelope(local, noteLength)
                    value += (sin(2.0 * PI * notes[n] * local) * 0.55 +
                            sin(2.0 * PI * notes[n] * 2.0 * local) * 0.18) * e
                }
            }
            // Gentle overall fade so the notification feels polished rather than harsh.
            (value * Short.MAX_VALUE * 0.24).toInt().toShort()
        }
    }

    fun playClick() = play(clickSamples ?: makeClick().also { clickSamples = it })

    fun playOrderNotification() =
        play(orderSamples ?: makeOrder().also { orderSamples = it })

    fun playOrderSent() = playOrderNotification()

    private fun play(samples: ShortArray) {
        Thread {
            try {
                val minBuffer = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(minBuffer, samples.size * 2)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                track.play()
                Thread.sleep((samples.size * 1000L / SAMPLE_RATE) + 40L)
                track.stop()
                track.release()
            } catch (_: Exception) {
                // Sound is optional; never let audio failure affect the app.
            }
        }.start()
    }
}
