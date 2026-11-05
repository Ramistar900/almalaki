package com.almalaki.cafe.royaltv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val ROYAL_TV_OWNER_SUPABASE_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val ROYAL_TV_OWNER_PUBLISHABLE_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFBIg_aez3fjQd"

private const val ROYAL_TV_OWNER_DEVICE_ENDPOINT =
    "$ROYAL_TV_OWNER_SUPABASE_URL/functions/v1/royal-tv-device-auth"

data class RoyalTVManagedDevice(
    val deviceId: String,
    val displayId: String,
    val deviceName: String,
    val channelTopic: String,
    val createdAt: String,
    val lastSeenAt: String?,
    val revokedAt: String?
) {
    val isActive: Boolean
        get() = revokedAt.isNullOrBlank()
}

data class RoyalTVActivationCode(
    val code: String,
    val expiresAt: String
)

class RoyalTVOwnerDeviceException(
    val httpStatus: Int,
    message: String
) : Exception(message)

/**
 * Owner-only device management.
 * All ownership checks are repeated by the Edge Function and database RLS.
 */
object RoyalTVOwnerDeviceManager {

    suspend fun issueActivationCode(
        accessToken: String,
        restaurantId: String
    ): RoyalTVActivationCode {
        require(accessToken.isNotBlank())
        require(restaurantId.isNotBlank())

        val response = invoke(
            accessToken = accessToken,
            body = JSONObject()
                .put("action", "issue_code")
                .put("restaurant_id", restaurantId)
        )

        if (response.first != 200) {
            throw errorFrom(response)
        }

        val code = response.second.optString("activation_code")
        val expiresAt = response.second.optString("expires_at")
        require(code.matches(Regex("^[0-9a-f]{16}$")) && expiresAt.isNotBlank()) {
            "استجابة رمز الربط غير مكتملة"
        }

        return RoyalTVActivationCode(code, expiresAt)
    }

    suspend fun listDevices(
        accessToken: String,
        restaurantId: String
    ): List<RoyalTVManagedDevice> {
        require(accessToken.isNotBlank())
        require(restaurantId.isNotBlank())

        val response = invoke(
            accessToken = accessToken,
            body = JSONObject()
                .put("action", "list_devices")
                .put("restaurant_id", restaurantId)
        )

        if (response.first != 200) throw errorFrom(response)

        val rows = response.second.optJSONArray("devices") ?: JSONArray()
        return buildList {
            for (index in 0 until rows.length()) {
                val item = rows.optJSONObject(index) ?: continue
                val deviceId = item.optString("device_id")
                val topic = item.optString("channel_topic")
                if (!deviceId.matches(UUID_PATTERN) ||
                    topic != "royal-tv:$deviceId"
                ) continue

                add(
                    RoyalTVManagedDevice(
                        deviceId = deviceId,
                        displayId = item.optString("channel_device_id"),
                        deviceName = item.optString("device_name"),
                        channelTopic = topic,
                        createdAt = item.optString("created_at"),
                        lastSeenAt = item.optString("last_seen_at")
                            .takeIf { it.isNotBlank() && it != "null" },
                        revokedAt = item.optString("revoked_at")
                            .takeIf { it.isNotBlank() && it != "null" }
                    )
                )
            }
        }
    }

    suspend fun revokeDevice(
        accessToken: String,
        restaurantId: String,
        deviceId: String
    ) {
        require(accessToken.isNotBlank())
        require(restaurantId.isNotBlank())
        require(deviceId.matches(UUID_PATTERN))

        val response = invoke(
            accessToken = accessToken,
            body = JSONObject()
                .put("action", "revoke_device")
                .put("restaurant_id", restaurantId)
                .put("device_id", deviceId)
        )

        if (response.first != 200) throw errorFrom(response)
    }

    suspend fun sendCommand(
        accessToken: String,
        channelTopic: String,
        commandType: String,
        payload: String,
        createdAt: Long = System.currentTimeMillis()
    ): Boolean {
        require(accessToken.isNotBlank())
        require(channelTopic.matches(TOPIC_PATTERN))
        require(commandType.isNotBlank())

        val enumType = try {
            RoyalTVCommandType.valueOf(commandType)
        } catch (_: IllegalArgumentException) {
            return false
        }

        return RoyalTVRealtime.sendCommand(
            ownerAccessToken = accessToken,
            channelTopic = channelTopic,
            command = RoyalTVCommand(
                type = enumType,
                payload = payload,
                createdAt = createdAt
            )
        )
    }

    private suspend fun invoke(
        accessToken: String,
        body: JSONObject
    ): Pair<Int, JSONObject> = withContext(Dispatchers.IO) {
        val connection = URL(ROYAL_TV_OWNER_DEVICE_ENDPOINT)
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 12_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            connection.setRequestProperty(
                "apikey",
                ROYAL_TV_OWNER_PUBLISHABLE_KEY
            )
            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )
            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=utf-8"
            )
            connection.setRequestProperty("Accept", "application/json")

            connection.outputStream.use {
                it.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val text = stream?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            val json = try {
                JSONObject(text)
            } catch (_: Exception) {
                JSONObject().put("error", "استجابة غير صالحة من خادم ROYAL TV")
            }
            status to json
        } finally {
            connection.disconnect()
        }
    }

    private fun errorFrom(
        response: Pair<Int, JSONObject>
    ) = RoyalTVOwnerDeviceException(
        response.first,
        response.second.optString(
            "error",
            "تعذر تنفيذ طلب إدارة أجهزة ROYAL TV"
        )
    )

    private val UUID_PATTERN = Regex(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
        RegexOption.IGNORE_CASE
    )

    private val TOPIC_PATTERN = Regex(
        "^royal-tv:[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
        RegexOption.IGNORE_CASE
    )
}
