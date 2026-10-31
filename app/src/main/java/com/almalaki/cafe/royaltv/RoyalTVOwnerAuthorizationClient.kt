package com.almalaki.cafe.royaltv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class RoyalTVPairingChallenge(
    val code: String,
    val expiresAt: String
)

/**
 * Owner-side client for the server authorization API.
 *
 * The caller supplies the current signed-in owner's Supabase access token.
 * The token is sent only as a bearer credential over HTTPS; this class never
 * accepts or embeds a service-role key.
 */
class RoyalTVOwnerAuthorizationClient(
    private val accessTokenProvider: suspend () -> String
) {
    companion object {
        private const val ENDPOINT =
            "https://duvxxskgdmgrtaleedqu.supabase.co/functions/v1/royal-tv-authorize"
        private const val PUBLIC_API_KEY =
            "sb_publishable_L-BfALjb0TkdTlyLTWbFBIg_aez3fjQd"
        private const val UUID_PATTERN =
            "^[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$"
    }

    suspend fun createPairingChallenge(restaurantId: String): RoyalTVPairingChallenge {
        require(restaurantId.matches(Regex(UUID_PATTERN))) { "معرّف المطعم غير صالح" }
        val response = ownerPost(
            JSONObject()
                .put("action", "create_challenge")
                .put("restaurant_id", restaurantId)
        )
        val code = response.optString("pairing_code")
        val expiresAt = response.optString("expires_at")
        if (code.length !in 24..128 || expiresAt.isBlank()) {
            throw IOException("استجابة رمز الاقتران غير صالحة")
        }
        return RoyalTVPairingChallenge(code, expiresAt)
    }

    suspend fun sendCommand(
        restaurantId: String,
        deviceId: String,
        commandType: String,
        payload: Any = JSONObject()
    ): String {
        require(restaurantId.matches(Regex(UUID_PATTERN))) { "معرّف المطعم غير صالح" }
        require(deviceId.length in 8..128) { "معرّف جهاز ROYAL TV غير صالح" }
        require(commandType in setOf(
            "SHOW_ORDER", "SHOW_YOUTUBE", "SHOW_MEDIA", "SHOW_AD",
            "SHOW_CONTENT", "REMOTE_COMMAND", "CLEAR_SCREEN"
        )) { "نوع أمر ROYAL TV غير مسموح" }

        val response = ownerPost(
            JSONObject()
                .put("action", "send_command")
                .put("restaurant_id", restaurantId)
                .put("device_id", deviceId)
                .put("command_type", commandType)
                .put("payload", payload)
        )
        return response.optJSONObject("command")?.optString("id")
            ?.takeIf { it.isNotBlank() }
            ?: throw IOException("لم يؤكد الخادم إنشاء الأمر")
    }

    suspend fun revokeDevice(restaurantId: String, deviceId: String) {
        require(restaurantId.matches(Regex(UUID_PATTERN))) { "معرّف المطعم غير صالح" }
        require(deviceId.length in 8..128) { "معرّف جهاز ROYAL TV غير صالح" }
        ownerPost(
            JSONObject()
                .put("action", "revoke_device")
                .put("restaurant_id", restaurantId)
                .put("device_id", deviceId)
        )
    }

    private suspend fun ownerPost(body: JSONObject): JSONObject {
        val token = accessTokenProvider().trim()
        if (token.isBlank()) throw IOException("يرجى تسجيل الدخول بحساب مالك المطعم")
        return withContext(Dispatchers.IO) {
            val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 15000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("apikey", PUBLIC_API_KEY)
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.outputStream.use { stream ->
                    stream.write(body.toString().toByteArray(Charsets.UTF_8))
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val response = try {
                    JSONObject(responseText)
                } catch (_: Exception) {
                    JSONObject()
                }
                if (status !in 200..299) {
                    throw IOException(response.optString("error").ifBlank { "HTTP $status" })
                }
                response
            } finally {
                connection.disconnect()
            }
        }
    }
}
