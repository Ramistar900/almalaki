package com.almalaki.cafe

import android.content.Context
import com.almalaki.royalsounds.RoyalUiSound
import com.almalaki.royalsounds.RoyalUiSounds

/** All Almalaki UI feedback is played from the shared Royal sound library. */
object AppSounds {
    fun productClick(context: Context) =
        playUi(context, RoyalUiSound.NAV_SELECT)

    fun productAdded(context: Context) =
        playUi(context, RoyalUiSound.ACTION_ADD_TO_CART)

    fun productRemoved(context: Context) =
        playUi(context, RoyalUiSound.ACTION_REMOVE)

    fun buttonClick(context: Context) =
        playUi(context, RoyalUiSound.NAV_SELECT)

    fun wrongPassword(context: Context) =
        playUi(context, RoyalUiSound.ACTION_ERROR)

    fun passwordChanged(context: Context) =
        playUi(context, RoyalUiSound.ACTION_SUCCESS)

    fun orderSuccess(context: Context) =
        playUi(context, RoyalUiSound.ACTION_SUCCESS)

    fun newOrder(context: Context) =
        playUi(context, RoyalUiSound.SYSTEM_NOTIFICATION)

    fun paymentSuccess(context: Context) =
        playUi(context, RoyalUiSound.ACTION_SUCCESS)

    private fun playUi(context: Context, sound: RoyalUiSound) {
        // Do not fall back to legacy generated beeps: the Royal library is the
        // single source of UI sound effects throughout Almalaki.
        RoyalUiSounds.play(context, sound)
    }
}
