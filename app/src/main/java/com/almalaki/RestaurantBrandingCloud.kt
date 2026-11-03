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
