package com.almalaki.cafe

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import com.almalaki.royalsounds.RoyalUiSound
import com.almalaki.royalsounds.RoyalUiSounds

object AppSounds {
    private var toneGenerator: ToneGenerator? = null

    fun productClick(context: Context) =
        playUi(context, RoyalUiSound.NAV_SELECT, ToneGenerator.TONE_PROP_BEEP2, 45, 70)

    fun productAdded(context: Context) =
        playUi(context, RoyalUiSound.ACTION_ADD_TO_CART, ToneGenerator.TONE_PROP_ACK, 90, 70)

    fun productRemoved(context: Context) =
        playUi(context, RoyalUiSound.ACTION_REMOVE, ToneGenerator.TONE_PROP_BEEP, 55, 60)

    fun buttonClick(context: Context) =
        playUi(context, RoyalUiSound.NAV_SELECT, ToneGenerator.TONE_PROP_BEEP, 55, 60)

    fun wrongPassword(context: Context) =
        playUi(context, RoyalUiSound.ACTION_ERROR, ToneGenerator.TONE_PROP_NACK, 180, 70)

    fun passwordChanged(context: Context) =
        playUi(context, RoyalUiSound.ACTION_SUCCESS, ToneGenerator.TONE_PROP_ACK, 220, 65)

    fun orderSuccess(context: Context) =
        playUi(context, RoyalUiSound.ACTION_SUCCESS, ToneGenerator.TONE_PROP_ACK, 300, 65)

    fun newOrder(context: Context) =
        playUi(context, RoyalUiSound.SYSTEM_NOTIFICATION, ToneGenerator.TONE_PROP_PROMPT, 350, 90)

    fun paymentSuccess(context: Context) =
        playUi(context, RoyalUiSound.ACTION_SUCCESS, ToneGenerator.TONE_PROP_ACK, 420, 55)

    private fun playUi(
        context: Context,
        sound: RoyalUiSound,
        fallbackTone: Int,
        duration: Int,
        volume: Int
    ) {
        if (!RoyalUiSounds.isEnabled(context)) return
        if (!RoyalUiSounds.play(context, sound)) {
            playLegacy(fallbackTone, duration, volume)
        }
    }

    private fun playLegacy(tone: Int, duration: Int, volume: Int) {
        try {
            toneGenerator?.release()
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, volume)
            toneGenerator?.startTone(tone, duration)
        } catch (_: Exception) {
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
