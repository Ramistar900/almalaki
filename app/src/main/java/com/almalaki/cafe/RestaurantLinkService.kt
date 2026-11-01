package com.almalaki.cafe

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val RESTAURANT_LINK_URL = "https://duvxxskgdmgrtaleedqu.supabase.co"
private const val RESTAURANT_LINK_KEY = "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"
private const val RESTAURANT_LINK_PREFS = "royal_restaurant_links"
const val ROYAL_DEFAULT_RESTAURANT_ID = "b3acfc71-25b1-47c6-912f-e0e6c8567f6c"
const val ROYAL_DEFAULT_RESTAURANT_CODE = "ROYAL-93E22632214A"

data class RoyalLinkedRestaurant(
    val id: String,
    val name: String,
    val joinCode: String = ""
)

fun loadRoyalLinkedRestaurants(context: Context): List<RoyalLinkedRestaurant> {
    val raw = context.getSharedPreferences(RESTAURANT_LINK_PREFS, Context.MODE_PRIVATE)
        .getString("linked_restaurants", "[]") ?: "[]"
    val linked = runCatching {
        val json = JSONArray(raw)
        buildList {
            for (i in 0 until json.length()) {
                val item = json.optJSONObject(i) ?: continue
                val id = item.optString("id").trim()
                val name = item.optString("name").trim()
                val code = item.optString("join_code").trim()
                if (id.isNotBlank() && name.isNotBlank() && code.isNotBlank()) {
                    add(RoyalLinkedRestaurant(id, name, code))
                }
            }
        }
    }.getOrDefault(emptyList())
    return listOf(RoyalLinkedRestaurant(ROYAL_DEFAULT_RESTAURANT_ID, "الملكي", ROYAL_DEFAULT_RESTAURANT_CODE)) +
        linked.filterNot { it.id == ROYAL_DEFAULT_RESTAURANT_ID }
}

fun loadRoyalActiveRestaurant(context: Context): RoyalLinkedRestaurant {
    val prefs = context.getSharedPreferences(RESTAURANT_LINK_PREFS, Context.MODE_PRIVATE)
    val id = prefs.getString("active_restaurant_id", ROYAL_DEFAULT_RESTAURANT_ID)
        ?: ROYAL_DEFAULT_RESTAURANT_ID
    return loadRoyalLinkedRestaurants(context).firstOrNull { it.id == id }
        ?: RoyalLinkedRestaurant(ROYAL_DEFAULT_RESTAURANT_ID, "الملكي", ROYAL_DEFAULT_RESTAURANT_CODE)
}

fun saveRoyalActiveRestaurant(context: Context, restaurant: RoyalLinkedRestaurant) {
    context.getSharedPreferences(RESTAURANT_LINK_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString("active_restaurant_id", restaurant.id)
        .apply()
}

fun linkRoyalRestaurant(context: Context, code: String): RoyalLinkedRestaurant {
    val normalizedCode = code.trim().uppercase()
    require(normalizedCode.isNotBlank()) { "أدخل رمز المطعم." }
    val resolved = restaurantRpc("resolve_restaurant_join_code", JSONObject().put("p_code", normalizedCode))
    val rows = JSONArray(resolved)
    if (rows.length() == 0) throw Exception("رمز المطعم غير صحيح أو أن المطعم غير متاح.")
    val row = rows.getJSONObject(0)
    val restaurant = RoyalLinkedRestaurant(
        id = row.getString("restaurant_id"),
        name = row.getString("restaurant_name"),
        joinCode = normalizedCode
    )
    val existing = loadRoyalLinkedRestaurants(context)
        .filterNot { it.id == ROYAL_DEFAULT_RESTAURANT_ID || it.id == restaurant.id }
    val all = existing + restaurant
    val array = JSONArray()
    all.forEach {
        array.put(JSONObject()
            .put("id", it.id)
            .put("name", it.name)
            .put("join_code", it.joinCode))
    }
    context.getSharedPreferences(RESTAURANT_LINK_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString("linked_restaurants", array.toString())
        .putString("active_restaurant_id", restaurant.id)
        .apply()
    return restaurant
}

fun selectRoyalRestaurant(context: Context, restaurant: RoyalLinkedRestaurant) {
    saveRoyalActiveRestaurant(context, restaurant)
}

fun loadProductsForRestaurantCode(code: String): List<Product> {
    val response = restaurantRpc(
        "get_restaurant_products",
        JSONObject().put("p_code", code.trim().uppercase())
    )
    val json = JSONArray(response)
    return buildList {
        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)
            add(Product(
                id = item.getInt("id"),
                name = item.getString("name"),
                category = item.optString("category", ""),
                price = item.optDouble("price", 0.0),
                imageUrl = item.optString("image_url", "")
            ))
        }
    }
}

fun loadCustomerSectionsForRestaurantCode(code: String): List<CustomerSection> {
    val response = restaurantRpc(
        "get_restaurant_sections",
        JSONObject().put("p_code", code.trim().uppercase())
    )
    val json = JSONArray(response)
    return buildList {
        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)
            val categoriesJson = item.optJSONArray("category_names") ?: JSONArray()
            val categories = buildList {
                for (j in 0 until categoriesJson.length()) {
                    categoriesJson.optString(j).trim().takeIf { it.isNotEmpty() }?.let { add(it) }
                }
            }
            add(CustomerSection(
                sectionKey = item.optString("section_key"),
                title = item.optString("title"),
                categoryNames = categories,
                enabled = item.optBoolean("enabled", true),
                sortOrder = item.optInt("sort_order", 0)
            ))
        }
    }
}

private fun restaurantRpc(functionName: String, body: JSONObject): String {
    val connection = URL("$RESTAURANT_LINK_URL/rest/v1/rpc/$functionName")
        .openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.doOutput = true
        connection.setRequestProperty("apikey", RESTAURANT_LINK_KEY)
        connection.setRequestProperty("Authorization", "Bearer $RESTAURANT_LINK_KEY")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            throw Exception("تعذر الاتصال بالمطعم (HTTP $code): " + response.ifBlank { "حاول مجددًا." })
        }
        return response
    } finally {
        connection.disconnect()
    }
}
