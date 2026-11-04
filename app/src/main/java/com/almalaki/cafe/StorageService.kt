package com.almalaki.cafe

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private const val STORAGE_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val STORAGE_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

data class OwnerRestaurant(
    val id: String,
    val name: String,
    val status: String
)

/** Load every active owner restaurant visible to the authenticated account. */
fun loadOwnerRestaurants(accessToken: String): List<OwnerRestaurant> {
    require(accessToken.isNotBlank()) { "جلسة المالك غير متوفرة." }
    val membershipUrl = URL(
        "$STORAGE_URL/rest/v1/restaurant_members?select=restaurant_id" +
            "&role=eq.owner&is_active=eq.true&order=created_at.asc"
    )
    val membershipConnection = membershipUrl.openConnection() as HttpURLConnection
    val restaurantIds = try {
        membershipConnection.requestMethod = "GET"
        membershipConnection.connectTimeout = 15000
        membershipConnection.readTimeout = 15000
        membershipConnection.setRequestProperty("apikey", STORAGE_KEY)
        membershipConnection.setRequestProperty("Authorization", "Bearer $accessToken")
        val code = membershipConnection.responseCode
        val response = (if (code in 200..299) membershipConnection.inputStream else membershipConnection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw Exception("تعذر تحميل مطاعم حساب المالك (HTTP $code).")
        val rows = JSONArray(response)
        buildList {
            for (i in 0 until rows.length()) {
                val id = rows.getJSONObject(i).optString("restaurant_id").trim()
                if (id.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) && id !in this) add(id)
            }
        }
    } finally {
        membershipConnection.disconnect()
    }
    if (restaurantIds.isEmpty()) throw Exception("لا يوجد مطعم نشط مرتبط بحساب المالك.")

    val restaurantsUrl = URL(
        "$STORAGE_URL/rest/v1/restaurants?select=id,name,status&id=in.(${restaurantIds.joinToString(",")})&order=created_at.asc"
    )
    val restaurantsConnection = restaurantsUrl.openConnection() as HttpURLConnection
    try {
        restaurantsConnection.requestMethod = "GET"
        restaurantsConnection.connectTimeout = 15000
        restaurantsConnection.readTimeout = 15000
        restaurantsConnection.setRequestProperty("apikey", STORAGE_KEY)
        restaurantsConnection.setRequestProperty("Authorization", "Bearer $accessToken")
        val code = restaurantsConnection.responseCode
        val response = (if (code in 200..299) restaurantsConnection.inputStream else restaurantsConnection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw Exception("تعذر تحميل أسماء المطاعم (HTTP $code).")
        val rows = JSONArray(response)
        val byId = linkedMapOf<String, OwnerRestaurant>()
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            val id = row.optString("id").trim()
            if (id in restaurantIds) byId[id] = OwnerRestaurant(
                id = id,
                name = row.optString("name", "مطعم"),
                status = row.optString("status", "")
            )
        }
        val result = restaurantIds.mapNotNull { byId[it] }
        if (result.isEmpty()) throw Exception("تعذر العثور على المطاعم المرتبطة بحسابك.")
        return result
    } finally {
        restaurantsConnection.disconnect()
    }
}

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
    mimeType: String,
    restaurantId: String? = null
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

    val scopedRestaurantId = restaurantId?.takeIf { it.isNotBlank() }
        ?: resolveOwnerRestaurantId(accessToken)
    val fileName = "$scopedRestaurantId/${UUID.randomUUID()}.$extension"
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
