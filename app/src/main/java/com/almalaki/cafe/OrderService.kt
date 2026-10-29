package com.almalaki.cafe

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val ORDER_SUPABASE_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val ORDER_SUPABASE_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

data class OrderResult(
    val orderNumber: String,
    val totalAmount: Double,
    val items: List<RoyalInvoiceItem>
)

/**
 * Submit a restaurant-code-scoped checkout through one database transaction.
 * Prices and totals are recalculated by Supabase from the restaurant's products.
 */
fun createOrder(
    customerName: String,
    customerPhone: String,
    deliveryAddress: String,
    fulfillmentType: String,
    products: List<Product>,
    cart: Map<Int, Int>,
    restaurantCode: String
): OrderResult {
    if (cart.isEmpty()) {
        throw Exception("السلة فارغة.")
    }

    if (fulfillmentType != "داخل المحل" && fulfillmentType != "توصيل إلى المنزل") {
        throw Exception("نوع الطلب غير صحيح.")
    }

    if (fulfillmentType == "توصيل إلى المنزل" && deliveryAddress.trim().isEmpty()) {
        throw Exception("عنوان التوصيل مطلوب.")
    }

    if (restaurantCode.isBlank()) {
        throw Exception("رمز المطعم غير موجود. أعد ربط المطعم ثم حاول مجددًا.")
    }

    // Send only product IDs and quantities. Never send client-authoritative prices.
    val jsonItems = JSONArray()
    products.forEach { product ->
        val quantity = cart[product.id] ?: 0
        if (quantity > 0) {
            jsonItems.put(
                JSONObject()
                    .put("product_id", product.id)
                    .put("quantity", quantity)
            )
        }
    }

    if (jsonItems.length() == 0) {
        throw Exception("لا توجد منتجات في الطلب.")
    }

    val body = JSONObject()
        .put("p_code", restaurantCode.trim())
        .put("p_customer_name", customerName.trim())
        .put("p_customer_phone", customerPhone.trim())
        .put("p_delivery_address", deliveryAddress.trim())
        .put("p_fulfillment_type", fulfillmentType)
        .put("p_items", jsonItems)

    val connection = URL(
        "$ORDER_SUPABASE_URL/rest/v1/rpc/create_restaurant_order"
    ).openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 20000
        connection.doOutput = true
        connection.setRequestProperty("apikey", ORDER_SUPABASE_KEY)
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Prefer", "return=representation")

        connection.outputStream.use { output ->
            output.write(body.toString().toByteArray(Charsets.UTF_8))
            output.flush()
        }

        val responseCode = connection.responseCode
        val responseText = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }

        if (responseCode !in 200..299) {
            throw Exception(
                "تعذر حفظ الطلب (HTTP $responseCode): " +
                    (responseText.ifBlank { "يرجى المحاولة مجددًا." })
            )
        }

        val rows = JSONArray(responseText)
        if (rows.length() == 0) {
            throw Exception("لم يؤكد الخادم حفظ الطلب. لم يتم عرض الطلب كناجح.")
        }

        val row = rows.getJSONObject(0)
        val serverItems = row.optJSONArray("items") ?: JSONArray()
        val invoiceItems = buildList {
            for (index in 0 until serverItems.length()) {
                val item = serverItems.getJSONObject(index)
                add(
                    RoyalInvoiceItem(
                        name = item.optString("name"),
                        quantity = item.optInt("quantity"),
                        unitPrice = item.optDouble("unit_price")
                    )
                )
            }
        }

        if (invoiceItems.isEmpty()) {
            throw Exception("حُفظ الطلب لكن لم تُرجع تفاصيل الفاتورة؛ راجع الطلب قبل إعادة المحاولة.")
        }

        return OrderResult(
            orderNumber = row.getString("order_number"),
            totalAmount = row.getDouble("total_amount"),
            items = invoiceItems
        )
    } finally {
        connection.disconnect()
    }
}
