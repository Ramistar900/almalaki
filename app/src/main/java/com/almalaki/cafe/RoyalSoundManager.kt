package com.almalaki.cafe

import com.almalaki.royalsounds.RoyalUiSound
import com.almalaki.royalsounds.RoyalUiSounds

/** Maps owner/admin actions directly to their matching Royal UI sound. */
object RoyalSoundManager {
    fun playClick() {
        RoyalUiSounds.play(RoyalUiSound.NAV_SELECT)
    }

    fun playOrderNotification() {
        RoyalUiSounds.play(RoyalUiSound.SYSTEM_NOTIFICATION)
    }

    fun playOrderSent() {
        RoyalUiSounds.play(RoyalUiSound.ACTION_CONFIRM)
    }

    fun playConfirm() {
        RoyalUiSounds.play(RoyalUiSound.ACTION_CONFIRM)
    }

    fun playSuccess() {
        RoyalUiSounds.play(RoyalUiSound.ACTION_SUCCESS)
    }

    fun playWarning() {
        RoyalUiSounds.play(RoyalUiSound.ACTION_WARNING)
    }

    fun playDelete() {
        RoyalUiSounds.play(RoyalUiSound.ACTION_DELETE)
    }
}
