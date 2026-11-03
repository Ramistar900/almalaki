package com.almalaki.cafe

import android.content.Context
import java.util.UUID

/**
 * Privacy-preserving app installation identifier.
 * This does not read hardware identifiers, contacts, files, photos, or location.
 * It is an identifier only; authentication and RLS remain the access controls.
 */
object RoyalDeviceIdentity {
    private const val PREFS_NAME = "royal_device_identity"
    private const val INSTALLATION_ID_KEY = "installation_id"

    @Synchronized
    fun getOrCreate(context: Context): String {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(INSTALLATION_ID_KEY, null)
        if (!existing.isNullOrBlank()) return existing

        val created = UUID.randomUUID().toString()
        prefs.edit().putString(INSTALLATION_ID_KEY, created).apply()
        return created
    }
}
