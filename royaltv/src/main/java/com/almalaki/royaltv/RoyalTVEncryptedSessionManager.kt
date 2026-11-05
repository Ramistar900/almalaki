package com.almalaki.royaltv

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.exception.AuthSessionMissingException
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the Supabase TV-device session encrypted with an Android Keystore key.
 * Refresh tokens are never kept as plain text in SharedPreferences.
 */
class RoyalTVEncryptedSessionManager(
    context: Context
) : SessionManager {

    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override suspend fun saveSession(session: UserSession) {
        val plainText = json.encodeToString(
            UserSession.serializer(),
            session
        )
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(
            cipher.iv + encrypted,
            Base64.NO_WRAP
        )

        check(
            preferences.edit()
                .putString(KEY_SESSION, encoded)
                .commit()
        ) {
            "Unable to persist the encrypted ROYAL TV session"
        }
    }

    override suspend fun loadSession(): UserSession? {
        val encoded = preferences.getString(KEY_SESSION, null)
            ?: return null

        return try {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            require(payload.size > GCM_IV_LENGTH)

            val iv = payload.copyOfRange(0, GCM_IV_LENGTH)
            val encrypted = payload.copyOfRange(GCM_IV_LENGTH, payload.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_BITS, iv)
            )

            val plainText = cipher.doFinal(encrypted)
                .toString(Charsets.UTF_8)

            json.decodeFromString(
                UserSession.serializer(),
                plainText
            )
        } catch (_: Exception) {
            // A damaged or unavailable Keystore entry must never create a
            // partially restored authentication session.
            preferences.edit().remove(KEY_SESSION).commit()
            null
        }
    }

    override suspend fun deleteSession() {
        check(
            preferences.edit()
                .remove(KEY_SESSION)
                .commit()
        ) {
            "Unable to remove the ROYAL TV session"
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
            load(null)
        }

        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE
        )

        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )

        return generator.generateKey()
    }

    private companion object {
        const val PREFS_NAME = "royal_tv_secure_auth"
        const val KEY_SESSION = "encrypted_user_session"
        const val KEY_ALIAS = "royal_tv_device_auth_v1"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_BITS = 128
    }
}
