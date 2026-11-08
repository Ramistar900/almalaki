package com.almalaki.royaltv

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pairing and session storage for one ROYAL TV receiver.
 *
 * Access and refresh tokens are encrypted with an AES key held by Android Keystore.
 * The service-role key and the generated device password never enter this app.
 */
object RoyalTVDeviceAuthManager {

    private const val PROJECT_URL = "https://duvxxskgdmgrtaleedqu.supabase.co"
    private const val PUBLISHABLE_KEY = "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"
    private const val SESSION_PREFS = "royal_tv_device_auth"
    private const val SESSION_KEY = "encrypted_session"
    private const val KEY_ALIAS = "royal_tv_device_session_key"

    private data class DeviceSession(
        val accessToken: String,
        val refreshToken: String,
        val expiresAtMillis: Long,
        val restaurantId: String
    )

    fun hasSession(context: Context): Boolean =
        readSession(context.applicationContext) != null

    suspend fun redeemActivationCode(
        context: Context,
        activationCode: String
    ) = withContext(Dispatchers.IO) {
        val code = activationCode.trim().lowercase()
        require(code.matches(Regex("^[0-9a-f]{64}$"))) {
            "رمز التفعيل غير صحيح. أدخل الرمز الكامل المكوّن من 64 خانة."
        }

        val appContext = context.applicationContext
        // The receiver listens with this legacy display ID. Keep it separate from
        // the UUID registration ID sent as device_id above.
        val response = postJson(
            "$PROJECT_URL/functions/v1/royal-tv-device-auth",
            JSONObject()
                .put("action", "redeem")
                .put("activation_code", code)
                .put("device_id", RoyalTVDeviceIdentity.getRegistrationId(appContext))
                .put("channel_device_id", RoyalTVDisplayIdAllocator.getDisplayId(appContext))
                .put("device_name", RoyalTVDeviceIdentity.getDeviceName(appContext))
        )

        val accessToken = response.optString("access_token")
        val refreshToken = response.optString("refresh_token")
        val expiresIn = response.optLong("expires_in", 3600L)
        val restaurantId = response.optString("restaurant_id")

        require(accessToken.isNotBlank() && refreshToken.isNotBlank() &&
            restaurantId.isNotBlank() && expiresIn > 0L) {
            "وصل رد غير مكتمل من خدمة تسجيل التلفاز."
        }

        storeSession(
            appContext,
            DeviceSession(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresAtMillis = System.currentTimeMillis() + expiresIn * 1000L,
                restaurantId = restaurantId
            )
        )
    }

    /**
     * Returns a usable device JWT, refreshing it before expiry.
     * A failed network refresh never falls back to an expired access token.
     */
    suspend fun validAccessToken(context: Context): String? =
        withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            val session = readSession(appContext) ?: return@withContext null

            if (session.expiresAtMillis > System.currentTimeMillis() + 90_000L) {
                return@withContext session.accessToken
            }

            try {
                val refreshed = postJson(
                    "$PROJECT_URL/auth/v1/token?grant_type=refresh_token",
                    JSONObject().put("refresh_token", session.refreshToken)
                )
                val accessToken = refreshed.optString("access_token")
                val refreshToken = refreshed.optString("refresh_token")
                val expiresIn = refreshed.optLong("expires_in", 3600L)

                if (accessToken.isBlank() || refreshToken.isBlank() || expiresIn <= 0L) {
                    return@withContext null
                }

                val updated = session.copy(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresAtMillis = System.currentTimeMillis() + expiresIn * 1000L
                )
                storeSession(appContext, updated)
                accessToken
            } catch (error: Exception) {
                android.util.Log.w(
                    "RoyalTVDeviceAuth",
                    "تعذر تحديث جلسة الجهاز: ${error.javaClass.simpleName}"
                )
                null
            }
        }

    fun clearSession(context: Context) {
        context.applicationContext
            .getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(SESSION_KEY)
            .apply()
    }

    private fun postJson(endpoint: String, body: JSONObject): JSONObject {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("apikey", PUBLISHABLE_KEY)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        try {
            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val response = runCatching { JSONObject(responseText) }.getOrElse { JSONObject() }

            if (status !in 200..299) {
                val code = response.optString("error")
                throw IllegalStateException(
                    when (code) {
                        "activation_code_invalid_or_used" ->
                            "رمز التفعيل منتهي أو مستخدم أو غير صالح. اطلب رمزًا جديدًا من المالك."
                        "device_registration_unavailable" ->
                            "تعذر تسجيل هذا التلفاز الآن. حاول إصدار رمز جديد."
                        "service_not_configured" ->
                            "خدمة تسجيل التلفاز غير مهيأة بعد."
                        "device_session_creation_failed_request_new_code" ->
                            "فشل إنشاء جلسة آمنة للتلفاز. اطلب رمز تفعيل جديدًا."
                        else -> "تعذر إكمال ربط التلفاز (HTTP $status)."
                    }
                )
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun storeSession(context: Context, session: DeviceSession) {
        val json = JSONObject()
            .put("access_token", session.accessToken)
            .put("refresh_token", session.refreshToken)
            .put("expires_at", session.expiresAtMillis)
            .put("restaurant_id", session.restaurantId)
            .toString()
            .toByteArray(Charsets.UTF_8)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(json)
        val combined = byteArrayOf(cipher.iv.size.toByte()) + cipher.iv + encrypted
        val encoded = Base64.encodeToString(combined, Base64.NO_WRAP)

        context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(SESSION_KEY, encoded)
            .apply()
    }

    private fun readSession(context: Context): DeviceSession? {
        val encoded = context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
            .getString(SESSION_KEY, null) ?: return null

        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            val ivLength = combined.firstOrNull()?.toInt()?.and(0xff) ?: return null
            if (ivLength !in 12..16 || combined.size <= ivLength + 1) return null

            val iv = combined.copyOfRange(1, 1 + ivLength)
            val encrypted = combined.copyOfRange(1 + ivLength, combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), javax.crypto.spec.GCMParameterSpec(128, iv))
            val json = JSONObject(String(cipher.doFinal(encrypted), Charsets.UTF_8))

            val session = DeviceSession(
                accessToken = json.optString("access_token"),
                refreshToken = json.optString("refresh_token"),
                expiresAtMillis = json.optLong("expires_at"),
                restaurantId = json.optString("restaurant_id")
            )
            if (session.accessToken.isBlank() || session.refreshToken.isBlank()) null else session
        } catch (error: Exception) {
            android.util.Log.w(
                "RoyalTVDeviceAuth",
                "تعذر فك جلسة الجهاز المشفرة: ${error.javaClass.simpleName}"
            )
            clearSession(context)
            null
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            generator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generator.generateKey()
        }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }
}
