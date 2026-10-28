package com.almalaki.cafe

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val SUPABASE_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val SUPABASE_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

data class Product(
    val id: Int,
    val name: String,
    val category: String,
    val price: Double,
    val imageUrl: String
)

fun loadProducts(
    restaurantId: String? = null,
    restaurantCode: String? = null,
    accessToken: String? = null
): List<Product> {
    val useCodeLookup = !restaurantCode.isNullOrBlank()
    val url = if (useCodeLookup) {
        URL("$SUPABASE_URL/rest/v1/rpc/get_restaurant_products")
    } else {
        URL(
            "$SUPABASE_URL/rest/v1/products" +
                "?select=id,name,category,price,image_url" +
                "&order=id.asc" +
                (restaurantId?.let {
                    "&restaurant_id=eq.${URLEncoder.encode(it, "UTF-8")}"
                } ?: "")
        )
    }

    val connection =
        url.openConnection() as HttpURLConnection

    connection.requestMethod = if (useCodeLookup) "POST" else "GET"
    connection.doOutput = useCodeLookup

    connection.setRequestProperty(
        "apikey",
        SUPABASE_KEY
    )

    connection.setRequestProperty(
        "Authorization",
        "Bearer ${accessToken?.takeIf { it.isNotBlank() } ?: SUPABASE_KEY}"
    )

    if (useCodeLookup) {
        connection.setRequestProperty("Content-Type", "application/json")
        val body = JSONObject().put("p_code", restaurantCode).toString()
        connection.outputStream.use {
            it.write(body.toByteArray(Charsets.UTF_8))
        }
    }

    val code = connection.responseCode

    if (code !in 200..299) {

        val error =
            connection.errorStream
                ?.bufferedReader()
                ?.readText()
                ?: "خطأ في تحميل المنتجات"

        connection.disconnect()

        throw Exception(
            "HTTP $code: $error"
        )
    }

    val response =
        connection.inputStream
            .bufferedReader()
            .readText()

    connection.disconnect()

    val json = JSONArray(response)

    val result =
        mutableListOf<Product>()

    for (i in 0 until json.length()) {

        val item =
            json.getJSONObject(i)

        result.add(
            Product(
                id = item.getInt("id"),
                name = item.getString("name"),
                category =
                    item.optString(
                        "category",
                        ""
                    ),
                price =
                    item.getDouble("price"),
                imageUrl =
                    item.optString(
                        "image_url",
                        ""
                    )
            )
        )
    }

    return result
}

fun addProduct(
    accessToken: String,
    name: String,
    category: String,
    price: Double,
    imageUrl: String,
    restaurantId: String? = null
) {

    val url = URL(
        "$SUPABASE_URL/rest/v1/products"
    )

    val connection =
        url.openConnection() as HttpURLConnection

    connection.requestMethod = "POST"
    connection.doOutput = true

    connection.setRequestProperty(
        "apikey",
        SUPABASE_KEY
    )

    connection.setRequestProperty(
        "Authorization",
        "Bearer $accessToken"
    )

    connection.setRequestProperty(
        "Content-Type",
        "application/json"
    )

    connection.setRequestProperty(
        "Prefer",
        "return=minimal"
    )

    val body =
        JSONObject().apply {

            put(
                "name",
                name.trim()
            )

            put(
                "price",
                price
            )

            put(
                "category",
                category.trim()
            )

            put(
                "image_url",
                imageUrl
            )

            put(
                "description",
                name.trim()
            )

            if (!restaurantId.isNullOrBlank()) {
                put("restaurant_id", restaurantId)
            }
        }.toString()

    connection.outputStream.use {
        it.write(
            body.toByteArray(
                Charsets.UTF_8
            )
        )
    }

    val code =
        connection.responseCode

    if (code !in 200..299) {

        val error =
            connection.errorStream
                ?.bufferedReader()
                ?.readText()
                ?: "خطأ أثناء إضافة المنتج"

        connection.disconnect()

        throw Exception(
            "HTTP $code: $error"
        )
    }

    connection.disconnect()
}

fun deleteProduct(
    accessToken: String,
    id: Int,
    restaurantId: String? = null
) {

    val tenantFilter = restaurantId?.takeIf { it.isNotBlank() }?.let {
        "&restaurant_id=eq.${URLEncoder.encode(it, "UTF-8")}"
    }.orEmpty()

    val url = URL(
        "$SUPABASE_URL/rest/v1/products?id=eq.$id$tenantFilter"
    )

    val connection =
        url.openConnection() as HttpURLConnection

    connection.requestMethod = "DELETE"

    connection.setRequestProperty(
        "apikey",
        SUPABASE_KEY
    )

    connection.setRequestProperty(
        "Authorization",
        "Bearer $accessToken"
    )

    val code =
        connection.responseCode

    if (code !in 200..299) {

        val error =
            connection.errorStream
                ?.bufferedReader()
                ?.readText()
                ?: "خطأ أثناء حذف المنتج"

        connection.disconnect()

        throw Exception(
            "HTTP $code: $error"
        )
    }

    connection.disconnect()
}

fun formatPrice(
    price: Double
): String {

    return "%,.0f ل.س".format(price)
}
