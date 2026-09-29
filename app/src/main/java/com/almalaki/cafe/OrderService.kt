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
    products: List<Product>,
    cart: Map<Int, Int>
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
        products = products,
        cart = cart
    )

    return OrderResult(orderNumber)
}

private fun generateOrderNumber(): String {

    val formatter = SimpleDateFormat(
        "yyyyMMddHHmmssSSS",
        Locale.US
    )

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

    try {

        connection.requestMethod = "POST"
        connection.doOutput = true

        connection.setRequestProperty(
            "apikey",
            ORDER_SUPABASE_KEY
        )

        connection.setRequestProperty(
            "Content-Type",
            "application/json"
        )

        // مهم:
        // لا نطلب إرجاع الصف حتى لا يحتاج anon إلى SELECT.
        // Supabase يعيد رابط الصف الجديد في Location.
        connection.setRequestProperty(
            "Prefer",
            "return=headers"
        )

        val body = JSONObject()

        body.put(
            "order_number",
            orderNumber
        )

        body.put(
            "customer_name",
            customerName.trim()
        )

        body.put(
            "customer_phone",
            customerPhone.trim()
        )

        body.put(
            "delivery_address",
            deliveryAddress.trim()
        )

        body.put(
            "fulfillment_type",
            fulfillmentType
        )

        body.put(
            "total_amount",
            totalAmount
        )

        body.put(
            "status",
            "new"
        )

        connection.outputStream.use {
            it.write(
                body.toString()
                    .toByteArray(Charsets.UTF_8)
            )
        }

        val code = connection.responseCode

        if (code !in 200..299) {

            val error =
                connection.errorStream
                    ?.bufferedReader()
                    ?.readText()
                    ?: "فشل إنشاء الطلب."

            throw Exception(
                "HTTP $code: $error"
            )
        }

        /*
         * مع Prefer: return=headers
         * يعيد PostgREST رابط الصف الجديد في Location.
         *
         * مثال تقريبي:
         * /rest/v1/orders?id=eq.123
         *
         * نستخرج منه رقم id بدون إجراء SELECT.
         */
        val location =
            connection.getHeaderField("Location")
                ?: throw Exception(
                    "تم إنشاء الطلب لكن لم يتم الحصول على رقم الطلب الداخلي."
                )

        val orderId =
            extractOrderIdFromLocation(location)

        if (orderId <= 0) {
            throw Exception(
                "تعذر استخراج رقم الطلب من استجابة Supabase."
            )
        }

        return orderId

    } finally {
        connection.disconnect()
    }
}

private fun extractOrderIdFromLocation(
    location: String
): Long {

    val match =
        Regex(
            """id=eq\.([0-9]+)"""
        ).find(location)

    if (match != null) {

        return match
            .groupValues[1]
            .toLong()
    }

    /*
     * احتياط إضافي لبعض صيغ Location.
     */
    val numbers =
        Regex(
            """[0-9]+"""
        ).findAll(location)
            .map {
                it.value
            }
            .toList()

    if (numbers.isNotEmpty()) {

        return numbers.last().toLong()
    }

    throw Exception(
        "تعذر قراءة رقم الطلب من Location."
    )
}

private fun insertOrderItems(
    orderId: Long,
    products: List<Product>,
    cart: Map<Int, Int>
) {

    val jsonArray = JSONArray()

    products.forEach { product ->

        val quantity =
            cart[product.id] ?: 0

        if (quantity > 0) {

            val item = JSONObject()

            item.put(
                "order_id",
                orderId
            )

            item.put(
                "product_id",
                product.id
            )

            item.put(
                "product_name",
                product.name
            )

            item.put(
                "unit_price",
                product.price
            )

            item.put(
                "quantity",
                quantity
            )

            item.put(
                "item_total",
                product.price * quantity
            )

            jsonArray.put(item)
        }
    }

    if (jsonArray.length() == 0) {

        throw Exception(
            "لا توجد منتجات في الطلب."
        )
    }

    val url = URL(
        "$ORDER_SUPABASE_URL/rest/v1/order_items"
    )

    val connection =
        url.openConnection() as HttpURLConnection

    try {

        connection.requestMethod = "POST"
        connection.doOutput = true

        connection.setRequestProperty(
            "apikey",
            ORDER_SUPABASE_KEY
        )

        connection.setRequestProperty(
            "Content-Type",
            "application/json"
        )

        connection.setRequestProperty(
            "Prefer",
            "return=minimal"
        )

        connection.outputStream.use {
            it.write(
                jsonArray.toString()
                    .toByteArray(Charsets.UTF_8)
            )
        }

        val code =
            connection.responseCode

        if (code !in 200..299) {

            val error =
                connection.errorStream
                    ?.bufferedReader()
                    ?.readText()
                    ?: "فشل حفظ تفاصيل الطلب."

            throw Exception(
                "HTTP $code: $error"
            )
        }

    } finally {
        connection.disconnect()
    }
}
