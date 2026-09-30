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
            45,
            70
        )
    }

    // نقرة خفيفة للأزرار
    fun buttonClick(context: Context) {
        play(
            context,
            ToneGenerator.TONE_PROP_BEEP,
            55,
            60
        )
    }

    // تنبيه عند كلمة المرور الخاطئة
    fun wrongPassword(context: Context) {
        play(
            context,
            ToneGenerator.TONE_PROP_NACK,
            180,
            70
        )
    }

    // صوت نجاح عند تغيير كلمة المرور
    fun passwordChanged(context: Context) {
        play(
            context,
            ToneGenerator.TONE_PROP_ACK,
            220,
            65
        )
    }

    // صوت نجاح عند تأكيد الطلب للعميل
    fun orderSuccess(context: Context) {
        play(
            context,
            ToneGenerator.TONE_PROP_ACK,
            300,
            65
        )
    }

    // 🔔 إشعار وصول طلب جديد للمالك
    // أعلى قليلًا وواضح للتنبيه
    fun newOrder(context: Context) {
        play(
            context,
            ToneGenerator.TONE_PROP_PROMPT,
            350,
            90
        )
    }

    // 💳 إشعار الدفع
    // هادئ وسلس ومختلف عن صوت الطلب
    fun paymentSuccess(context: Context) {
        play(
            context,
            ToneGenerator.TONE_PROP_ACK,
            420,
            55
        )
    }

    private fun play(
        context: Context,
        tone: Int,
        duration: Int,
        volume: Int
    ) {
        try {
            toneGenerator?.release()

            toneGenerator = ToneGenerator(
                AudioManager.STREAM_MUSIC,
                volume
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
