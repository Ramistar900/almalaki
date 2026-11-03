package com.almalaki.cafe

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RoyalCustomerCloudSnapshot(
    val profile: RoyalCustomerProfile?,
    val restaurants: List<RoyalLinkedRestaurant>,
    val orders: List<RoyalCustomerOrderRecord>
)

/**
 * Customer-owned data access. Every request uses the signed-in user's JWT;
 * the publishable key is public and RLS remains the authority.
 */
object RoyalCustomerCloud {
    private fun request(method: String, path: String, body: String? = null, prefer: String? = null): String {
        val userId = RoyalSupabaseAuth.currentUserId()
            ?: throw Exception("سجّل الدخول أولاً لحفظ بياناتك.")
        val accessToken = RoyalSupabaseAuth.accessTokenOrNull()
            ?: throw Exception("انتهت جلسة الدخول. سجّل الدخول مجددًا.")

        val connection = URL("${RoyalSupabaseAuth.PROJECT_URL}${path}")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("apikey", RoyalSupabaseAuth.PUBLISHABLE_KEY)
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
            }
            if (prefer != null) connection.setRequestProperty("Prefer", prefer)
            if (body != null) {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }

            val code = connection.responseCode
            val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val detail = runCatching {
                    val json = JSONObject(response)
                    json.optString("message").ifBlank { json.optString("msg") }
                }.getOrDefault("")
                throw Exception(detail.ifBlank { "تعذر حفظ بيانات الحساب (HTTP $code)." })
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    fun loadProfile(): RoyalCustomerProfile? {
        val userId = RoyalSupabaseAuth.currentUserId() ?: return null
        val query = URLEncoder.encode(userId, "UTF-8")
        val response = request(
            "GET",
            "/rest/v1/customer_profiles?select=full_name,phone,address,fulfillment_type&user_id=eq.$query"
        )
        val rows = JSONArray(response)
        if (rows.length() == 0) return null
        val row = rows.getJSONObject(0)
        return RoyalCustomerProfile(
            name = row.optString("full_name"),
            phone = row.optString("phone"),
            address = row.optString("address"),
            fulfillmentType = row.optString("fulfillment_type").ifBlank { "داخل المحل" }
        )
    }

    fun saveProfile(profile: RoyalCustomerProfile) {
        val userId = RoyalSupabaseAuth.currentUserId()
            ?: throw Exception("سجّل الدخول أولاً لحفظ بياناتك.")
        val body = JSONObject()
            .put("user_id", userId)
            .put("full_name", profile.name.trim())
            .put("phone", profile.phone.trim())
            .put("address", profile.address.trim())
            .put("fulfillment_type", profile.fulfillmentType)
            .toString()
        request(
            "POST",
            "/rest/v1/customer_profiles?on_conflict=user_id",
            body,
            "resolution=merge-duplicates,return=minimal"
        )
    }

    fun linkRestaurant(code: String): RoyalLinkedRestaurant {
        val body = JSONObject().put("p_code", code.trim().uppercase()).toString()
        val rows = JSONArray(request("POST", "/rest/v1/rpc/link_customer_restaurant", body))
        if (rows.length() == 0) throw Exception("لم يتم العثور على مطعم نشط بهذا الرمز.")
        val row = rows.getJSONObject(0)
        return RoyalLinkedRestaurant(
            id = row.getString("restaurant_id"),
            name = row.getString("restaurant_name"),
            joinCode = row.getString("join_code")
        )
    }

    fun loadRestaurants(): List<RoyalLinkedRestaurant> {
        val rows = JSONArray(request("POST", "/rest/v1/rpc/get_customer_restaurants", "{}"))
        return buildList {
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                add(
                    RoyalLinkedRestaurant(
                        id = row.optString("restaurant_id"),
                        name = row.optString("restaurant_name"),
                        joinCode = row.optString("join_code")
                    )
                )
            }
        }
    }

    fun loadOrders(): List<RoyalCustomerOrderRecord> {
        val userId = RoyalSupabaseAuth.currentUserId() ?: return emptyList()
        val query = URLEncoder.encode(userId, "UTF-8")
        val path = "/rest/v1/orders?select=order_number,customer_name,customer_phone,delivery_address,fulfillment_type,total_amount,created_at,order_items!order_items_order_restaurant_fkey(product_name,quantity,unit_price)&customer_user_id=eq.$query&order=created_at.desc&limit=30"
        val rows = JSONArray(request("GET", path))
        return buildList {
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val itemRows = row.optJSONArray("order_items") ?: JSONArray()
                val items = buildList {
                    for (j in 0 until itemRows.length()) {
                        val item = itemRows.optJSONObject(j) ?: continue
                        add(
                            RoyalInvoiceItem(
                                name = item.optString("product_name"),
                                quantity = item.optInt("quantity"),
                                unitPrice = item.optDouble("unit_price")
                            )
                        )
                    }
                }
                add(
                    RoyalCustomerOrderRecord(
                        orderNumber = row.optString("order_number"),
                        customerName = row.optString("customer_name"),
                        customerPhone = row.optString("customer_phone"),
                        address = row.optString("delivery_address"),
                        fulfillmentType = row.optString("fulfillment_type"),
                        totalAmount = row.optDouble("total_amount"),
                        createdAt = parseTimestamp(row.optString("created_at")),
                        items = items
                    )
                )
            }
        }
    }

    fun loadSnapshot(): RoyalCustomerCloudSnapshot {
        // Keep profile and restaurant sync available even if order history is temporarily unavailable.
        val profile = runCatching { loadProfile() }.getOrNull()
        val restaurants = runCatching { loadRestaurants() }.getOrDefault(emptyList())
        val orders = runCatching { loadOrders() }.getOrDefault(emptyList())
        return RoyalCustomerCloudSnapshot(profile, restaurants, orders)
    }

    private fun parseTimestamp(value: String): Long {
        if (value.isBlank()) return System.currentTimeMillis()
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX"
        )
        for (pattern in patterns) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).parse(value)?.time
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return System.currentTimeMillis()
    }
}
