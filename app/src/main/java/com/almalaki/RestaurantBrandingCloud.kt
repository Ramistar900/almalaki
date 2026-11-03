package com.almalaki.cafe

import android.content.Context
import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

private const val BRANDING_URL = "https://duvxxskgdmgrtaleedqu.supabase.co"
private const val BRANDING_KEY = "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"
private const val BRANDING_BUCKET = "product-images"

data class RoyalRestaurantBranding(
    val newsText: String,
    val activeLogoId: String,
    val activeLogoName: String,
    val activeLogoPath: String?,
    val activeLogoAnimation: String,
    val textLogoConfig: RoyalTextLogoConfig
)

private fun encodeBrandingPath(path: String): String =
    path.split("/").joinToString("/") {
        URLEncoder.encode(it, "UTF-8").replace("+", "%20")
    }

private fun brandingConnection(url: String, method: String, token: String? = null): HttpURLConnection {
    val c = URL(url).openConnection() as HttpURLConnection
    c.requestMethod = method
    c.connectTimeout = 15000
    c.readTimeout = 30000
    c.setRequestProperty("apikey", BRANDING_KEY)
    c.setRequestProperty("Authorization", "Bearer ${token?.takeIf { it.isNotBlank() } ?: BRANDING_KEY}")
    c.setRequestProperty("Accept", "application/json")
    return c
}

fun uploadRestaurantBrandingLogo(
    accessToken: String,
    restaurantId: String,
    bytes: ByteArray,
    mimeType: String
): String {
    require(restaurantId.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))) {
        "معرّف المطعم غير صالح."
    }
    val extension = when (mimeType.lowercase()) {
        "image/png" -> "png"
        "image/jpeg", "image/jpg" -> "jpg"
        "image/webp" -> "webp"
        else -> throw Exception("نوع صورة الشعار غير مدعوم.")
    }
    require(bytes.isNotEmpty() && bytes.size <= 10 * 1024 * 1024) {
        "حجم صورة الشعار يجب ألا يتجاوز 10 ميغابايت."
    }
    val objectPath = "$restaurantId/branding/${UUID.randomUUID()}.$extension"
    val url = "$BRANDING_URL/storage/v1/object/$BRANDING_BUCKET/${encodeBrandingPath(objectPath)}"
    val connection = brandingConnection(url, "POST", accessToken)
    try {
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", mimeType.lowercase())
        connection.outputStream.use { it.write(bytes) }
        val code = connection.responseCode
        val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw Exception("تعذر رفع شعار المطعم (HTTP $code): ${response.take(180)}")
    } finally {
        connection.disconnect()
    }
    return objectPath
}

fun publishRoyalRestaurantBranding(
    context: Context,
    accessToken: String,
    restaurantId: String,
    newsText: String,
    textLogoConfig: RoyalTextLogoConfig
) {
    require(accessToken.isNotBlank()) { "جلسة المالك غير متوفرة." }
    require(restaurantId.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))) {
        "معرّف المطعم غير صالح."
    }
    val activeLogo = loadActiveRoyalLogo(context, restaurantId)
    var remoteLogoPath: String? = null
    if (activeLogo.id != ROYAL_DEFAULT_LOGO_ID) {
        val file = File(activeLogo.filePath)
        if (!file.isFile || !file.canRead()) throw Exception("ملف الشعار النشط غير موجود على هذا الجهاز.")
        val mimeType = when (file.extension.lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> throw Exception("صيغة ملف الشعار غير مدعومة.")
        }
        remoteLogoPath = uploadRestaurantBrandingLogo(accessToken, restaurantId, file.readBytes(), mimeType)
    }
    val payload = JSONObject().apply {
        put("restaurant_id", restaurantId)
        put("news_text", newsText.trim().ifEmpty { "أهلاً بكم في Royal Coffee ☕" })
        put("active_logo_id", if (remoteLogoPath == null) ROYAL_DEFAULT_LOGO_ID else activeLogo.id)
        put("active_logo_name", if (remoteLogoPath == null) "الشعار الأساسي" else activeLogo.name.take(100))
        put("active_logo_path", remoteLogoPath ?: JSONObject.NULL)
        put("active_logo_animation", if (remoteLogoPath == null) ROYAL_ANIMATION_LAYERED else activeLogo.animationType)
        put("text_logo_text", textLogoConfig.text.trim().ifEmpty { "Royal Coffee" }.take(120))
        put("text_logo_style", textLogoConfig.style.name)
        put("text_logo_font_size", textLogoConfig.fontSizeSp.coerceIn(16, 56))
        put("updated_at", java.time.Instant.now().toString())
    }
    val url = "$BRANDING_URL/rest/v1/restaurant_interface_settings?on_conflict=restaurant_id"
    val connection = brandingConnection(url, "POST", accessToken)
    try {
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw Exception("تعذر مزامنة هوية المطعم (HTTP $code): ${response.take(220)}")
    } finally {
        connection.disconnect()
    }
}

fun loadAndCacheRoyalRestaurantBranding(context: Context, restaurantId: String): RoyalRestaurantBranding? {
    if (!restaurantId.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))) return null
    val id = URLEncoder.encode(restaurantId, "UTF-8")
    val connection = brandingConnection("$BRANDING_URL/rest/v1/restaurant_interface_settings?select=*&restaurant_id=eq.$id&limit=1", "GET")
    val body: String
    try {
        val code = connection.responseCode
        body = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw Exception("تعذر تحميل هوية المطعم (HTTP $code).")
    } finally { connection.disconnect() }
    val rows = JSONArray(body)
    if (rows.length() == 0) return null
    val row = rows.getJSONObject(0)
    val rawPath = row.optString("active_logo_path").takeIf {
        it.isNotBlank() && it != "null" && it.startsWith("$restaurantId/branding/") &&
            it.matches(Regex("^[0-9a-fA-F-]{36}/branding/[A-Za-z0-9._-]{1,140}$"))
    }
    var localPath: String? = null
    if (rawPath != null) {
        val download = brandingConnection("$BRANDING_URL/storage/v1/object/public/$BRANDING_BUCKET/${encodeBrandingPath(rawPath)}", "GET")
        try {
            if (download.responseCode in 200..299) {
                val bytes = download.inputStream.use { it.readBytes() }
                if (bytes.isNotEmpty() && bytes.size <= 10 * 1024 * 1024) {
                    val ext = rawPath.substringAfterLast('.', "png").lowercase()
                        .takeIf { it in setOf("png", "jpg", "jpeg", "webp") } ?: "png"
                    val file = File(context.filesDir, "royal_remote_logo_${restaurantId}.$ext")
                    file.writeBytes(bytes)
                    localPath = file.absolutePath
                }
            }
        } finally { download.disconnect() }
    }
    val style = runCatching {
        RoyalTextLogoStyle.valueOf(row.optString("text_logo_style", "MODERN_SIGNATURE"))
    }.getOrDefault(RoyalTextLogoStyle.MODERN_SIGNATURE)
    val config = RoyalTextLogoConfig(
        text = row.optString("text_logo_text", "Royal Coffee").ifBlank { "Royal Coffee" }.take(120),
        style = style,
        fontSizeSp = row.optInt("text_logo_font_size", 27).coerceIn(16, 56)
    )
    val activeId = if (localPath != null) row.optString("active_logo_id").ifBlank { "remote_active_logo" } else ROYAL_DEFAULT_LOGO_ID
    val activeName = if (localPath != null) row.optString("active_logo_name").ifBlank { "شعار المطعم" } else "الشعار الأساسي"
    val animation = if (localPath != null && row.optString("active_logo_animation") == ROYAL_ANIMATION_GENERIC_3D) ROYAL_ANIMATION_GENERIC_3D else ROYAL_ANIMATION_LAYERED
    val news = row.optString("news_text", "")
    cacheRemoteRoyalInterfaceSettings(context, restaurantId, news, activeId, activeName, localPath, animation, config)
    return RoyalRestaurantBranding(news, activeId, activeName, localPath, animation, config)
}
