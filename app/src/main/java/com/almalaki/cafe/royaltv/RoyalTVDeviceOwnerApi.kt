package com.almalaki.cafe.royaltv

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RoyalTVActivationCode(
    val code: String,
    val expiresAt: String
)

data class RoyalTVRegisteredDevice(
    val deviceId: String,
    val channelDeviceId: String,
    val deviceName: String,
    val createdAt: String,
    val lastSeenAt: String,
    val revokedAt: String?
) {
    val isRevoked: Boolean
        get() = !revokedAt.isNullOrBlank()
}

/**
 * Owner-only API for issuing codes and managing devices belonging to the
 * selected restaurant. Every operation is authenticated by the current
 * owner's access token and re-authorized by the server.
 */
object RoyalTVDeviceOwnerApi {

    private const val PROJECT_URL = "https://duvxxskgdmgrtaleedqu.supabase.co"
    private const val PUBLISHABLE_KEY = "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"
    private const val ENDPOINT = "$PROJECT_URL/functions/v1/royal-tv-device-auth"

    fun issueActivationCode(
        accessToken: String,
        restaurantId: String
    ): RoyalTVActivationCode {
        val response = postOwnerAction(
            accessToken = accessToken,
            action = "issue_code",
            restaurantId = restaurantId
        )
        val activationCode = response.optString("activation_code")
        val expiresAt = response.optString("expires_at")
        require(activationCode.matches(Regex("^[0-9a-f]{64}$")) && expiresAt.isNotBlank()) {
            "وصل رد غير مكتمل من خدمة ربط التلفاز."
        }
        return RoyalTVActivationCode(activationCode, expiresAt)
    }

    fun listDevices(
        accessToken: String,
        restaurantId: String
    ): List<RoyalTVRegisteredDevice> {
        val response = postOwnerAction(
            accessToken = accessToken,
            action = "list_devices",
            restaurantId = restaurantId
        )
        val rows: JSONArray = response.optJSONArray("devices") ?: JSONArray()
        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val deviceId = row.optString("device_id").takeUnless {
                    it.isBlank() || it == "null"
                } ?: continue
                add(
                    RoyalTVRegisteredDevice(
                        deviceId = deviceId,
                        channelDeviceId = row.optString("channel_device_id").orEmpty(),
                        deviceName = row.optString("device_name").ifBlank { "ROYAL TV" },
                        createdAt = row.optString("created_at").takeUnless { it == "null" }.orEmpty(),
                        lastSeenAt = row.optString("last_seen_at").takeUnless { it == "null" }.orEmpty(),
                        revokedAt = row.optString("revoked_at").takeUnless {
                            it.isBlank() || it == "null"
                        }
                    )
                )
            }
        }
    }

    fun revokeDevice(
        accessToken: String,
        deviceId: String
    ) {
        require(deviceId.isNotBlank()) { "معرّف الجهاز غير صالح." }
        val response = postOwnerAction(
            accessToken = accessToken,
            action = "revoke_device",
            extra = JSONObject().put("device_id", deviceId)
        )
        check(response.optBoolean("revoked")) {
            "لم يؤكد الخادم إلغاء صلاحية الجهاز."
        }
    }

    private fun postOwnerAction(
        accessToken: String,
        action: String,
        restaurantId: String? = null,
        extra: JSONObject = JSONObject()
    ): JSONObject {
        require(accessToken.isNotBlank()) {
            "انتهت جلسة المالك. سجّل الدخول مجددًا."
        }

        if (restaurantId != null) {
            require(restaurantId.isNotBlank()) { "اختر المطعم أولًا." }
        }

        val body = JSONObject()
            .put("action", action)
        if (restaurantId != null) body.put("restaurant_id", restaurantId)
        val keys = extra.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            body.put(key, extra.get(key))
        }

        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("apikey", PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $accessToken")
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
                        "owner_auth_required", "owner_auth_invalid" ->
                            "انتهت جلسة المالك. سجّل الدخول مجددًا."
                        "restaurant_owner_required" ->
                            "هذا الحساب لا يملك صلاحية إدارة أجهزة هذا المطعم."
                        "invalid_restaurant" ->
                            "معرّف المطعم غير صالح."
                        "device_not_found" ->
                            "لم يتم العثور على جهاز بهذا المعرّف."
                        "device_already_revoked" ->
                            "هذا الجهاز ملغى بالفعل."
                        "device_revoked_but_session_ban_pending" ->
                            "أُلغيت صلاحية الجهاز في سجل المطعم، لكن تعذر إنهاء جلسته بالكامل. أعد تحديث القائمة وتواصل مع المطور."
                        else -> "تعذر تنفيذ طلب إدارة التلفاز (HTTP $status)."
                    }
                )
            }
            return response
        } finally {
            connection.disconnect()
        }
    }
}
