package com.almalaki.cafe.royaltv

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RoyalTVActivationCode(
    val code: String,
    val expiresAt: String
)

/**
 * Owner-only API for issuing one-time ROYAL TV pairing codes.
 * The caller supplies the current signed-in owner's JWT.
 */
object RoyalTVDeviceOwnerApi {

    private const val PROJECT_URL = "https://duvxxskgdmgrtaleedqu.supabase.co"
    private const val PUBLISHABLE_KEY = "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

    fun issueActivationCode(
        accessToken: String,
        restaurantId: String
    ): RoyalTVActivationCode {
        require(accessToken.isNotBlank()) {
            "انتهت جلسة المالك. سجّل الدخول مجددًا."
        }
        require(restaurantId.isNotBlank()) {
            "اختر المطعم أولًا."
        }

        val connection = (URL(
            "$PROJECT_URL/functions/v1/royal-tv-device-auth"
        ).openConnection() as HttpURLConnection).apply {
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
            val body = JSONObject()
                .put("action", "issue_code")
                .put("restaurant_id", restaurantId)

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
                        else -> "تعذر إصدار رمز ربط التلفاز (HTTP $status)."
                    }
                )
            }

            val activationCode = response.optString("activation_code")
            val expiresAt = response.optString("expires_at")
            require(activationCode.matches(Regex("^[0-9a-f]{64}$")) && expiresAt.isNotBlank()) {
                "وصل رد غير مكتمل من خدمة ربط التلفاز."
            }

            return RoyalTVActivationCode(
                code = activationCode,
                expiresAt = expiresAt
            )
        } finally {
            connection.disconnect()
        }
    }
}
