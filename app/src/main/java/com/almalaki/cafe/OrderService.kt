package com.almalaki.cafe

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private const val ORDER_SUPABASE_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val ORDER_SUPABASE_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

data class OrderResult(
    val orderNumber: String
)

fun createOrder(
    customerName: String,
    customerPhone: String,
    deliveryAddress: String,
    fulfillmentType: String,
    @Suppress("UNUSED_PARAMETER") totalAmount: Double,
    products: List<Product>,
    cart: Map<Int, Int>,
    @Suppress("UNUSED_PARAMETER") restaurantId: String = ROYAL_DEFAULT_RESTAURANT_ID,
    restaurantCode: String = "",
    idempotencyKey: String = UUID.randomUUID().toString()
): OrderResult {
    if (cart.isEmpty()) {
        throw Exception("السلة فارغة.")
    }

    if (
        fulfillmentType != "داخل المحل" &&
        fulfillmentType != "توصيل إلى المنزل"
    ) {
        throw Exception("نوع الطلب غير صحيح.")
    }

    if (
        fulfillmentType == "توصيل إلى المنزل" &&
        deliveryAddress.trim().isEmpty()
    ) {
        throw Exception("عنوان التوصيل مطلوب.")
    }

    // All customer orders use the same server-side atomic checkout.
    // An empty code intentionally selects the protected legacy default restaurant.
    return createRestaurantOrderViaRpc(
        restaurantCode = restaurantCode,
        customerName = customerName,
        customerPhone = customerPhone,
        deliveryAddress = deliveryAddress,
        fulfillmentType = fulfillmentType,
        products = products,
        cart = cart,
        idempotencyKey = idempotencyKey
    )
}

private fun createRestaurantOrderViaRpc(
    restaurantCode: String,
    customerName: String,
    customerPhone: String,
    deliveryAddress: String,
    fulfillmentType: String,
    products: List<Product>,
    cart: Map<Int, Int>,
    idempotencyKey: String
): OrderResult {
    val items = JSONArray()
    products.forEach { product ->
        val quantity = cart[product.id] ?: 0
        if (quantity > 0) {
            items.put(
                JSONObject()
                    .put("product_id", product.id)
                    .put("quantity", quantity)
            )
        }
    }
    if (items.length() == 0) throw Exception("لا توجد منتجات في الطلب.")

    val body = JSONObject()
        .put("p_code", restaurantCode.trim().uppercase())
        .put("p_customer_name", customerName.trim())
        .put("p_customer_phone", customerPhone.trim())
        .put("p_delivery_address", deliveryAddress.trim())
        .put("p_fulfillment_type", fulfillmentType)
        .put("p_items", items)
        .put("p_idempotency_key", idempotencyKey)

    val connection = URL("$ORDER_SUPABASE_URL/rest/v1/rpc/create_restaurant_order")
        .openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 20000
        connection.doOutput = true
        connection.setRequestProperty("apikey", ORDER_SUPABASE_KEY)
        connection.setRequestProperty(
            "Authorization",
            "Bearer ${RoyalSupabaseAuth.accessTokenOrNull() ?: ORDER_SUPABASE_KEY}"
        )
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            throw Exception("تعذر إرسال الطلب (HTTP $code): " + response.take(240))
        }
        val rows = JSONArray(response)
        if (rows.length() == 0) throw Exception("لم يرجع الخادم رقم الطلب.")
        return OrderResult(rows.getJSONObject(0).optString("order_number"))
    } finally {
        connection.disconnect()
    }
}
