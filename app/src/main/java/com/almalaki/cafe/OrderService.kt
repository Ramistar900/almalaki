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
 * Submits checkout through the server-side atomic RPC.
 * Product prices and line totals are deliberately not sent by the APK.
 */
fun createOrder(
    customerName: String,
    customerPhone: String,
    deliveryAddress: String,
    fulfillmentType: String,
    products: List<Product>,
    cart: Map<Int, Int>,
    restaurantCode: String = ROYAL_DEFAULT_RESTAURANT_CODE
): OrderResult {
    if (cart.isEmpty() || cart.values.none { it > 0 }) {
        throw Exception("السلة فارغة.")
    }

    if (fulfillmentType != "داخل المحل" && fulfillmentType != "توصيل إلى المنزل") {
        throw Exception("نوع الطلب غير صحيح.")
    }

    if (fulfillmentType == "توصيل إلى المنزل" && deliveryAddress.trim().isEmpty()) {
        throw Exception("عنوان التوصيل مطلوب.")
    }

    if (restaurantCode.isBlank()) {
        throw Exception("رمز المطعم غير متوفر. أعد اختيار المطعم.")
    }

    if (cart.values.any { it < 0 || it > 1000 }) {
        throw Exception("كمية أحد المنتجات غير صحيحة.")
    }

    val loadedProductIds = products.map { it.id }.toSet()
    val selectedProducts = cart.filterValues { it > 0 }
    if (selectedProducts.keys.any { it !in loadedProductIds }) {
        throw Exception("تغيّرت قائمة المنتجات. حدّث القائمة ثم حاول مجددًا.")
    }

    val itemsPayload = JSONArray()
    selectedProducts.toSortedMap().forEach { (productId, quantity) ->
        itemsPayload.put(
            JSONObject()
                .put("product_id", productId)
                .put("quantity", quantity)
        )
    }

    val body = JSONObject()
        .put("p_code", restaurantCode.trim().uppercase())
        .put("p_customer_name", customerName.trim())
        .put("p_customer_phone", customerPhone.trim())
        .put("p_delivery_address", deliveryAddress.trim())
        .put("p_fulfillment_type", fulfillmentType)
        .put("p_items", itemsPayload)

    val connection = URL("$ORDER_SUPABASE_URL/rest/v1/rpc/create_restaurant_order")
        .openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.doOutput = true
        connection.setRequestProperty("apikey", ORDER_SUPABASE_KEY)
        connection.setRequestProperty("Authorization", "Bearer $ORDER_SUPABASE_KEY")
        connection.setRequestProperty("Content-Type", "application/json")

        connection.outputStream.use { output ->
            output.write(body.toString().toByteArray(Charsets.UTF_8))
        }

        val responseCode = connection.responseCode
        val responseBody = (
            if (responseCode in 200..299) connection.inputStream else connection.errorStream
        )?.bufferedReader()?.use { it.readText() }.orEmpty()

        if (responseCode !in 200..299) {
            throw Exception(
                "تعذر إرسال الطلب (HTTP $responseCode): " +
                    responseBody.ifBlank { "تحقق من الاتصال وحاول مجددًا." }
            )
        }

        val resultRows = JSONArray(responseBody)
        if (resultRows.length() != 1) {
            throw Exception("لم تُرجع قاعدة البيانات تأكيدًا واضحًا للطلب.")
        }

        val result = resultRows.getJSONObject(0)
        val serverItems = result.optJSONArray("items") ?: JSONArray()
        val invoiceItems = buildList {
            for (i in 0 until serverItems.length()) {
                val item = serverItems.getJSONObject(i)
                add(
                    RoyalInvoiceItem(
                        name = item.optString("name", "منتج"),
                        quantity = item.getInt("quantity"),
                        unitPrice = item.getDouble("unit_price")
                    )
                )
            }
        }

        if (invoiceItems.isEmpty()) {
            throw Exception("لم تُرجع قاعدة البيانات تفاصيل الطلب.")
        }

        return OrderResult(
            orderNumber = result.getString("order_number"),
            totalAmount = result.getDouble("total_amount"),
            items = invoiceItems
        )
    } finally {
        connection.disconnect()
    }
}
