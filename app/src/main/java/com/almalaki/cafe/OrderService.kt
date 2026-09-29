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

        /*
         * مهم:
         * لا نستخدم return=representation
         * لأنه يحتاج SELECT.
         *
         * headers-only يعيد Location
         * بدون إعادة بيانات الصف.
         */
        connection.setRequestProperty(
            "Prefer",
            "return=headers-only"
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

        connection.outputStream.use { output ->

            output.write(
                body.toString()
                    .toByteArray(Charsets.UTF_8)
            )

            output.flush()
        }

        val responseCode =
            connection.responseCode

        if (responseCode !in 200..299) {

            val error =
                connection.errorStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: "فشل إنشاء الطلب."

            throw Exception(
                "HTTP $responseCode: $error"
            )
        }

        /*
         * PostgREST يعيد عادة:
         *
         * /orders?id=eq.123
         *
         * أو رابطًا كاملًا.
         */

        val location =
            connection.getHeaderField("Location")

        val contentLocation =
            connection.getHeaderField(
                "Content-Location"
            )

        val orderId =
            extractOrderId(location)
                ?: extractOrderId(contentLocation)

        if (orderId != null) {
            return orderId
        }

        /*
         * نجرب جميع الرؤوس لأن بعض الخوادم
         * قد تعيد الرأس باسم مختلف.
         */

        val headerFields =
            connection.headerFields

        for ((headerName, values) in headerFields) {

            if (values.isNullOrEmpty()) {
                continue
            }

            for (value in values) {

                val id =
                    extractOrderId(value)

                if (id != null) {
                    return id
                }
            }
        }

        throw Exception(
            "تم إنشاء الطلب، لكن لم يتم الحصول على رقم الطلب الداخلي."
        )

    } finally {

        connection.disconnect()
    }
}

private fun extractOrderId(
    value: String?
): Long? {

    if (value.isNullOrBlank()) {
        return null
    }

    /*
     * أمثلة مقبولة:
     *
     * /orders?id=eq.22
     *
     * https://.../orders?id=eq.22
     *
     * /orders?id=eq.22&...
     */

    val patterns = listOf(
        Regex("""[?&]id=eq[.]([0-9]+)"""),
        Regex("""id=
