package com.almalaki.cafe

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private const val STORAGE_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val STORAGE_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

/**
 * Resolve the first active owner restaurant using the authenticated session.
 * The query is constrained by restaurant_members RLS, so it cannot return
 * another account's tenant.
 */
fun resolveOwnerRestaurantId(accessToken: String): String {
    require(accessToken.isNotBlank()) { "جلسة المالك غير متوفرة." }

    val url = URL(
        "$STORAGE_URL/rest/v1/restaurant_members" +
            "?select=restaurant_id&role=eq.owner&is_active=eq.true" +
            "&order=created_at.asc&limit=1"
    )
    val connection = url.openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("apikey", STORAGE_KEY)
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Accept", "application/json")

        val code = connection.responseCode
        val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            throw Exception("تعذر تحديد مطعم المالك (HTTP $code).")
        }

        val rows = JSONArray(response)
        if (rows.length() == 0) {
            throw Exception("لا يوجد مطعم نشط مرتبط بحساب المالك.")
        }

        val restaurantId = rows.getJSONObject(0).optString("restaurant_id").trim()
        if (!restaurantId.matches(
                Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
            )
        ) {
            throw Exception("تعذر التحقق من معرّف المطعم.")
        }
        return restaurantId
    } finally {
        connection.disconnect()
    }
}

fun uploadProductImage(
    accessToken: String,
    bytes: ByteArray,
    mimeType: String
): String {
    val normalizedMimeType = mimeType.lowercase()
    val extension = when (normalizedMimeType) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/jpeg", "image/jpg" -> "jpg"
        else -> throw Exception("نوع الصورة غير مدعوم. استخدم JPG أو PNG أو WEBP.")
    }

    if (bytes.isEmpty()) throw Exception("ملف الصورة فارغ.")
    if (bytes.size > 10 * 1024 * 1024) {
        throw Exception("حجم الصورة أكبر من 10 ميغابايت.")
    }

    val restaurantId = resolveOwnerRestaurantId(accessToken)
    val fileName = "$restaurantId/${UUID.randomUUID()}.$extension"
    val encodedPath = fileName.split("/").joinToString("/") {
        java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20")
    }
    val url = URL(
        "$STORAGE_URL/storage/v1/object/product-images/$encodedPath"
    )
    val connection = url.openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.doOutput = true
        connection.setRequestProperty("apikey", STORAGE_KEY)
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", normalizedMimeType)
        connection.setRequestProperty("x-upsert", "true")
        connection.outputStream.use { it.write(bytes) }

        val code = connection.responseCode
        val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            throw Exception("فشل رفع صورة المطعم (HTTP $code): " + response.take(240))
        }
    } finally {
        connection.disconnect()
    }

    return "$STORAGE_URL/storage/v1/object/public/product-images/$encodedPath"
}
