package com.almalaki.royaltv

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val ROYAL_TV_SUPABASE_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val ROYAL_TV_PUBLISHABLE_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

private const val ROYAL_TV_DEVICE_AUTH_ENDPOINT =
    "$ROYAL_TV_SUPABASE_URL/functions/v1/royal-tv-device-auth"

data class RoyalTVDeviceRegistration(
    val deviceId: String,
    val channelTopic: String,
    val displayId: String,
    val deviceName: String
)

class RoyalTVDeviceAuthException(
    val httpStatus: Int,
    message: String
) : Exception(message)

/**
 * Device-only Supabase identity. The anonymous Auth session is encrypted
 * through Android Keystore and is never shared with the restaurant owner app.
 */
object RoyalTVDeviceAuthorization {

    @Volatile
    private var supabaseClient: SupabaseClient? = null

    @Volatile
    private var replaceAnonymousSessionBeforePairing = false

    fun client(context: Context): SupabaseClient {
        supabaseClient?.let { return it }

        return synchronized(this) {
            supabaseClient ?: createSupabaseClient(
                supabaseUrl = ROYAL_TV_SUPABASE_URL,
                supabaseKey = ROYAL_TV_PUBLISHABLE_KEY
            ) {
                install(Auth) {
                    sessionManager = RoyalTVEncryptedSessionManager(context)
                    alwaysAutoRefresh = true
                    autoLoadFromStorage = true
                    autoSaveToStorage = true
                }

                install(Realtime) {
                    accessToken = {
                        auth.currentAccessTokenOrNull()
                    }
                }
            }.also {
                supabaseClient = it
            }
        }
    }

    private suspend fun ensureAnonymousSession(
        context: Context,
        forceNew: Boolean = false
    ): String {
        val client = client(context)
        client.auth.awaitInitialization()

        if (forceNew) {
            client.auth.clearSession()
        }

        if (client.auth.currentSessionOrNull() == null) {
            client.auth.signInAnonymously()
        }

        val session = client.auth.currentSessionOrNull()
            ?: error("تعذر إنشاء جلسة آمنة لجهاز ROYAL TV")

        return session.accessToken
    }

    suspend fun checkRegistration(
        context: Context
    ): RoyalTVDeviceRegistration? {
        val token = ensureAnonymousSession(context)
        val response = invokeEdge("device_check", token)

        if (response.first == 404) {
            return null
        }

        if (response.first == 409) {
            replaceAnonymousSessionBeforePairing = true
            throw RoyalTVDeviceAuthException(
                response.first,
                response.second.optString(
                    "error",
                    "تم إلغاء ربط هذا التلفاز. أدخل رمزًا جديدًا لإعادة ربطه."
                )
            )
        }

        if (response.first != 200) {
            throw RoyalTVDeviceAuthException(
                response.first,
                response.second.optString(
                    "error",
                    "تعذر التحقق من تسجيل جهاز ROYAL TV"
                )
            )
        }

        replaceAnonymousSessionBeforePairing = false

        return registrationFrom(
            response.second,
            displayId = response.second.optString("channel_device_id"),
            deviceName = response.second.optString("device_name")
        )
    }

    suspend fun pair(
        context: Context,
        activationCode: String,
        displayId: String,
        deviceName: String
    ): RoyalTVDeviceRegistration {
        val cleanCode = activationCode.trim().lowercase()
        require(cleanCode.matches(Regex("^[0-9a-f]{16}$"))) {
            "رمز التفعيل يجب أن يتكون من 16 خانة سداسية عشرية"
        }

        val cleanDisplayId = displayId.trim()
        val cleanName = deviceName.trim()
        require(cleanDisplayId.isNotBlank() && cleanDisplayId.length <= 80)
        require(cleanName.isNotBlank() && cleanName.length <= 80)

        val token = ensureAnonymousSession(
            context = context,
            forceNew = replaceAnonymousSessionBeforePairing
        )

        val body = JSONObject()
            .put("activation_code", cleanCode)
            .put("channel_device_id", cleanDisplayId)
            .put("device_name", cleanName)

        val response = invokeEdge(
            action = "redeem",
            accessToken = token,
            additionalBody = body
        )

        if (response.first != 200) {
            throw RoyalTVDeviceAuthException(
                response.first,
                response.second.optString(
                    "error",
                    "تعذر ربط هذا التلفاز بالمطعم"
                )
            )
        }

        replaceAnonymousSessionBeforePairing = false

        return registrationFrom(
            response.second,
            displayId = cleanDisplayId,
            deviceName = cleanName
        )
    }

    suspend fun refreshAuthorization(
        context: Context
    ): RoyalTVDeviceRegistration {
        val token = ensureAnonymousSession(context)
        val response = invokeEdge("device_check", token)

        if (response.first != 200) {
            if (response.first == 409) {
                replaceAnonymousSessionBeforePairing = true
            }
            throw RoyalTVDeviceAuthException(
                response.first,
                response.second.optString(
                    "error",
                    "لم يعد هذا التلفاز مخولًا لاستقبال أوامر المطعم"
                )
            )
        }

        return registrationFrom(
            response.second,
            displayId = response.second.optString("channel_device_id"),
            deviceName = response.second.optString("device_name")
        )
    }

    suspend fun signOutDevice(context: Context) {
        val client = client(context)
        client.auth.awaitInitialization()
        client.auth.clearSession()
        replaceAnonymousSessionBeforePairing = false
    }

    private suspend fun invokeEdge(
        action: String,
        accessToken: String,
        additionalBody: JSONObject = JSONObject()
    ): Pair<Int, JSONObject> = withContext(Dispatchers.IO) {
        val body = JSONObject(additionalBody.toString())
            .put("action", action)

        val connection = URL(ROYAL_TV_DEVICE_AUTH_ENDPOINT)
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 12_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            connection.setRequestProperty(
                "apikey",
                ROYAL_TV_PUBLISHABLE_KEY
            )
            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )
            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=utf-8"
            )
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = stream?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            val json = try {
                JSONObject(responseText)
            } catch (_: Exception) {
                JSONObject().put(
                    "error",
                    "استجابة غير صالحة من خادم تسجيل التلفاز"
                )
            }

            status to json
        } finally {
            connection.disconnect()
        }
    }

    private fun registrationFrom(
        json: JSONObject,
        displayId: String,
        deviceName: String
    ): RoyalTVDeviceRegistration {
        val deviceId = json.optString("device_id")
        val topic = json.optString("channel_topic")

        require(
            deviceId.matches(
                Regex(
                    "^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
                    RegexOption.IGNORE_CASE
                )
            )
        ) {
            "خادم ROYAL TV أعاد هوية جهاز غير صالحة"
        }

        require(topic == "royal-tv:$deviceId") {
            "موضوع قناة ROYAL TV لا يطابق هوية الجهاز"
        }

        return RoyalTVDeviceRegistration(
            deviceId = deviceId,
            channelTopic = topic,
            displayId = displayId,
            deviceName = deviceName
        )
    }
}
