package com.almalaki.cafe

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

object AppSounds {

    private var toneGenerator: ToneGenerator? = null

    // نقرة راقية عند اختيار المنتج
    fun productClick(context: Context) {
        play(
            context,
            ToneGenerator.TONE_PROP_BEEP2,
            45
        )
    }

    // نقرة خفيفة للأزرار
    fun buttonClick(context: Context) {
        play(
            context,
            ToneGenerator.TONE_PROP_BEEP,
            55
        )
    }

    // تنبيه عند كلمة المرور الخاطئة
    fun wrongPassword(context: Context) {
        play(
            context,
            ToneGenerator.TONE_PROP_NACK,
            180
        )
    }

    // صوت نجاح عند تغيير كلمة المرور
    fun passwordChanged(context: Context) {
        play(
            context,
            ToneGenerator.TONE_PROP_ACK,
            220
        )
    }

    // صوت نجاح عند تأكيد الطلب
    fun orderSuccess(context: Context) {
        play(
            context,
            ToneGenerator.TONE_PROP_ACK,
            300
        )
    }

    private fun play(
        context: Context,
        tone: Int,
        duration: Int
    ) {
        try {
            toneGenerator?.release()

            toneGenerator = ToneGenerator(
                AudioManager.STREAM_MUSIC,
                70
            )

            toneGenerator?.startTone(
                tone,
                duration
            )
        } catch (_: Exception) {
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
