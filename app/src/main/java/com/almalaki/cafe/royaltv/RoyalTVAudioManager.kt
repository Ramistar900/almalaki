package com.almalaki.cafe.royaltv

import android.content.Context
import android.media.AudioManager

object RoyalTVAudioManager {

    private const val PREFS_NAME =
        "royal_tv_audio"

    private const val KEY_VOLUME =
        "volume"

    private const val KEY_MUTED =
        "muted"

    private const val DEFAULT_VOLUME =
        100

    private const val MIN_VOLUME =
        0

    private const val MAX_VOLUME =
        100

    private var initialized =
        false

    private var savedVolume =
        DEFAULT_VOLUME

    private var muted =
        false

    fun initialize(
        context: Context
    ) {

        if (initialized) {
            return
        }

        val prefs =
            context.applicationContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

        savedVolume =
            prefs.getInt(
                KEY_VOLUME,
                DEFAULT_VOLUME
            ).coerceIn(
                MIN_VOLUME,
                MAX_VOLUME
            )

        muted =
            prefs.getBoolean(
                KEY_MUTED,
                false
            )

        initialized = true
    }

    fun setVolume(
        context: Context,
        volume: Int
    ) {

        initialize(context)

        savedVolume =
            volume.coerceIn(
                MIN_VOLUME,
                MAX_VOLUME
            )

        savePreferences(context)

        applyVolume(
            context
        )
    }

    fun getVolume(
        context: Context
    ): Int {

        initialize(context)

        return savedVolume
    }

    fun increaseVolume(
        context: Context,
        amount: Int = 10
    ) {

        initialize(context)

        val newVolume =
            (
                savedVolume +
                    amount.coerceAtLeast(1)
            ).coerceAtMost(
                MAX_VOLUME
            )

        setVolume(
            context,
            newVolume
        )
    }

    fun decreaseVolume(
        context: Context,
        amount: Int = 10
    ) {

        initialize(context)

        val newVolume =
            (
                savedVolume -
                    amount.coerceAtLeast(1)
            ).coerceAtLeast(
                MIN_VOLUME
            )

        setVolume(
            context,
            newVolume
        )
    }

    fun mute(
        context: Context
    ) {

        initialize(context)

        muted = true

        savePreferences(
            context
        )

        applyVolume(
            context
        )
    }

    fun unmute(
        context: Context
    ) {

        initialize(context)

        muted = false

        savePreferences(
            context
        )

        applyVolume(
            context
        )
    }

    fun toggleMute(
        context: Context
    ) {

        initialize(context)

        if (muted) {
            unmute(context)
        } else {
            mute(context)
        }
    }

    fun isMuted(
        context: Context
    ): Boolean {

        initialize(context)

        return muted
    }

    fun getEffectiveVolume(
        context: Context
    ): Int {

        initialize(context)

        return if (muted) {
            0
        } else {
            savedVolume
        }
    }

    fun restore(
        context: Context
    ) {

        initialize(context)

        applyVolume(
            context
        )
    }

    private fun applyVolume(
        context: Context
    ) {

        val audioManager =
            context.applicationContext
                .getSystemService(
                    Context.AUDIO_SERVICE
                ) as? AudioManager
                ?: return

        val maxSystemVolume =
            audioManager.getStreamMaxVolume(
                AudioManager.STREAM_MUSIC
            )

        if (maxSystemVolume <= 0) {
            return
        }

        val effectiveVolume =
            if (muted) {
                0
            } else {
                savedVolume
            }

        val systemVolume =
            (
                maxSystemVolume *
                    effectiveVolume /
                    MAX_VOLUME
            ).coerceIn(
                0,
                maxSystemVolume
            )

        try {

            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                systemVolume,
                0
            )

        } catch (e: Exception) {

            android.util.Log.e(
                "RoyalTVAudioManager",
                "Unable to apply volume: ${e.message}",
                e
            )
        }
    }

    private fun savePreferences(
        context: Context
    ) {

        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putInt(
                KEY_VOLUME,
                savedVolume
            )
            .putBoolean(
                KEY_MUTED,
                muted
            )
            .apply()
    }

    fun reset(
        context: Context
    ) {

        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .clear()
            .apply()

        savedVolume =
            DEFAULT_VOLUME

        muted =
            false

        initialized =
            true
    }
}
