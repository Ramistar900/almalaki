package com.almalaki.cafe

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    totalAmount: Double,
    items: List<Product>
): OrderResult {

    if (items.isEmpty()) {
        throw Exception("السلة فارغة.")
    }

    if (
        fulfillmentType != "داخل المحل" &&
        fulfillmentType != "توصيل إلى المنزل"
    ) {
        throw Exception("نوع الطلب غير صحيح.")
    }

    val orderNumber = generateOrderNumber()

    val orderId = insertOrder(
        orderNumber = orderNumber,
        customerName = customerName,
        customerPhone = customerPhone,
        deliveryAddress = deliveryAddress,
        fulfillmentType = fulfillmentType,
        totalAmount = totalAmount
    )

    insertOrderItems(
        orderId = orderId,
        items = items
    )

    return OrderResult(orderNumber)
}

private fun generateOrderNumber(): String {
    val formatter =
        SimpleDateFormat("yyyyMMddHHmmss", Locale.US)

    return "RC-" + formatter.format(Date())
}

private fun insertOrder(
    orderNumber: String,
    customerName: String,
    customerPhone: String,
    deliveryAddress: String,
    fulfillmentType: String,
    totalAmount: Double
): Long {

    val url = URL(
        "$ORDER_SUPABASE_URL/rest/v1/orders"
    )

    val connection =
        url.openConnection() as HttpURLConnection

    connection.requestMethod = "POST"
    connection.doOutput = true

    connection.setRequestProperty(
        "apikey",
        ORDER_SUPABASE_KEY
    )

    connection.setRequestProperty(
        "Authorization",
        "Bearer $ORDER_SUPABASE_KEY"
    )

    connection.setRequestProperty(
        "Content-Type",
        "application/json"
    )

    connection.setRequestProperty(
        "Prefer",
        "return=representation"
    )

    val body = JSONObject().apply {
        put("order_number", orderNumber)
        put("customer_name", customerName.trim())
        put("customer_phone", customerPhone.trim())
        put("delivery_address", deliveryAddress.trim())
        put("fulfillment_type", fulfillmentType)
        put("total_amount", totalAmount)
        put("status", "new")
    }.toString()

    connection.outputStream.use {
        it.write(body.toByteArray(Charsets.UTF_8))
    }

    val code = connection.responseCode

    if (code !in 200..299) {
        val error =
            connection.errorStream
                ?.bufferedReader()
                ?.readText()
                ?: "فشل إنشاء الطلب."

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

    if (json.length() == 0) {
        throw Exception("لم يتم إنشاء الطلب.")
    }

    return json
        .getJSONObject(0)
        .getLong("id")
}

private fun insertOrderItems(
    orderId: Long,
    items: List<Product>
) {

    val url = URL(
        "$ORDER_SUPABASE_URL/rest/v1/order_items"
    )

    val connection =
        url.openConnection() as HttpURLConnection

    connection.requestMethod = "POST"
    connection.doOutput = true

    connection.setRequestProperty(
        "apikey",
        ORDER_SUPABASE_KEY
    )

    connection.setRequestProperty(
        "Authorization",
        "Bearer $ORDER_SUPABASE_KEY"
    )

    connection.setRequestProperty(
        "Content-Type",
        "application/json"
    )

    connection.setRequestProperty(
        "Prefer",
        "return=minimal"
    )

    val jsonArray = JSONArray()

    items.forEach { product ->

        val item = JSONObject().apply {
            put("order_id", orderId)
            put("product_id", product.id)
            put("product_name", product.name)
            put("unit_price", product.price)
            put("quantity", 1)
            put("item_total", product.price)
        }

        jsonArray.put(item)
    }

    connection.outputStream.use {
        it.write(
            jsonArray
                .toString()
                .toByteArray(Charsets.UTF_8)
        )
    }

    val code = connection.responseCode

    if (code !in 200..299) {

        val error =
            connection.errorStream
                ?.bufferedReader()
                ?.readText()
                ?: "فشل حفظ تفاصيل الطلب."

        connection.disconnect()

        throw Exception(
            "HTTP $code: $error"
        )
    }

    connection.disconnect()
}
