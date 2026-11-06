package com.almalaki.royaltv

import android.content.Context

/**
 * Remembers that this installation has entered secure authorization mode.
 * This prevents a cleared/revoked credential from downgrading the TV to the
 * legacy unauthenticated Realtime command channel.
 */
object RoyalTVSecureModeStore {
    private const val PREFS_NAME = "royal_tv_secure_mode"
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
    }

    fun enable(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, true)
            .commit()
    }
}
