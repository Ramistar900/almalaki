package com.almalaki.cafe

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

object AppSounds {

    private var toneGenerator: ToneGenerator? = null

    fun button(context: Context) {
        play(context, ToneGenerator.TONE_PROP_BEEP)
    }

    fun orderSuccess(context: Context) {
        play(context, ToneGenerator.TONE_PROP_ACK)
    }

    private fun play(
        context: Context,
        tone: Int
    ) {
        try {
            toneGenerator?.release()

            toneGenerator = ToneGenerator(
                AudioManager.STREAM_MUSIC,
                70
            )

            toneGenerator?.startTone(
                tone,
                120
            )
        } catch (_: Exception) {
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
