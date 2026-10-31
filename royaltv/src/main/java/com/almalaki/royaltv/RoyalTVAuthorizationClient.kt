package com.almalaki.royaltv

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class RoyalTVServerCommand(
    val id: String,
    val type: String,
    val payload: String,
    val createdAt: String,
    val expiresAt: String
)

/**
 * Client for the server-authorized ROYAL TV command queue.
 * Call methods from a background dispatcher. Local channel names and device IDs
 * are identifiers only; authorization depends on the server-issued credential.
 */
class RoyalTVAuthorizationClient(
    private val context: Context
) {
    companion object {
        private const val ENDPOINT =
            "https://duvxxskgdmgrtaleedqu.supabase.co/functions/v1/royal-tv-authorize"
        private const val PUBLIC_API_KEY =
            "sb_publishable_L-BfALjb0TkdTlyLTWbFBIg_aez3fjQd"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 15000
    }

    fun isPaired(): Boolean = RoyalTVCredentialStore.read(context) != null

    fun pair(pairingCode: String): String {
        val cleanCode = pairingCode.trim()
        require(cleanCode.length in 24..128) { "رمز الاقتران غير صالح" }
        val deviceId = RoyalTVDeviceIdentity.getDeviceId(context)
        val deviceName = RoyalTVDeviceIdentity.getDeviceName(context)
            .take(100).ifBlank { "ROYAL TV" }
        val response = post(
            JSONObject()
                .put("action", "pair_device")
                .put("pairing_code", cleanCode)
                .put("device_id", deviceId)
                .put("device_name", deviceName),
            credential = null
        )
        val credential = response.optString("device_credential")
        val restaurantId = response.optString("restaurant_id")
        if (credential.length !in 32..256 || restaurantId.isBlank()) {
            throw IOException("استجابة الاقتران غير صالحة")
        }
        RoyalTVCredentialStore.save(context, credential)
        return restaurantId
    }

    fun pollCommands(): List<RoyalTVServerCommand> {
        val credential = RoyalTVCredentialStore.read(context)
            ?: throw IOException("جهاز ROYAL TV غير مقترن")
        val response = post(JSONObject().put("action", "poll_commands"), credential)
        val array: JSONArray = response.optJSONArray("commands") ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id")
                val type = item.optString("command_type")
                if (id.isBlank() || type.isBlank()) continue
                val payloadValue = item.opt("payload")
                val payload = when (payloadValue) {
                    is JSONObject, is JSONArray -> payloadValue.toString()
                    null, JSONObject.NULL -> "{}"
                    else -> payloadValue.toString()
                }
                add(
                    RoyalTVServerCommand(
                        id = id,
                        type = type,
                        payload = payload,
                        createdAt = item.optString("created_at"),
                        expiresAt = item.optString("expires_at")
                    )
                )
            }
        }
    }

    fun acknowledge(commandId: String) {
        require(commandId.matches(
            Regex("^[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}$")
        )) { "Invalid command identifier" }
        val credential = RoyalTVCredentialStore.read(context)
            ?: throw IOException("جهاز ROYAL TV غير مقترن")
        post(
            JSONObject()
                .put("action", "acknowledge")
                .put("command_id", commandId),
            credential
        )
    }

    fun unpairLocally() {
        RoyalTVCredentialStore.clear(context)
    }

    private fun post(body: JSONObject, credential: String?): JSONObject {
        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("apikey", PUBLIC_API_KEY)
            if (credential != null) {
                connection.setRequestProperty("x-royal-device-credential", credential)
            }
            connection.outputStream.use { stream ->
                stream.write(body.toString().toByteArray(Charsets.UTF_8))
            }
            val status = connection.responseCode
            val input = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = input?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = try {
                JSONObject(text)
            } catch (_: Exception) {
                JSONObject()
            }
            if (status !in 200..299) {
                throw IOException(json.optString("error").ifBlank { "HTTP $status" })
            }
            return json
        } finally {
            connection.disconnect()
        }
    }
}
