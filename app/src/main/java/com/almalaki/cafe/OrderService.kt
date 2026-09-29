package com.almalaki.cafe

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

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

    /*
     * ننشئ رقم ID من التطبيق نفسه.
     *
     * بهذه الطريقة لا نحتاج:
     * RETURNING
     * ولا SELECT
     * ولا Location header
     *
     * وهذا يتجنب مشكلة RLS التي ظهرت عند
     * محاولة إرجاع الصف الذي تم إدخاله.
     */
    val orderId = generateOrderId()

    insertOrder(
        orderId = orderId,
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

private fun generateOrderId(): Long {

    /*
     * نستخدم الوقت الحالي بالميلي ثانية
     * مع رقم عشوائي صغير لتقليل احتمال التكرار.
     *
     * الناتج يبقى ضمن نطاق Long بسهولة.
     */
    val timePart =
        System.currentTimeMillis() * 1000L

    val randomPart =
        Random.nextInt(0, 1000).toLong()

    return timePart + randomPart
}

private fun insertOrder(
    orderId: Long,
    orderNumber: String,
    customerName: String,
    customerPhone: String,
    deliveryAddress: String,
    fulfillmentType: String,
    totalAmount: Double
) {

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
         * لا نستخدم return=representation
         * ولا headers-only.
         *
         * الإدخال فقط.
         */
        connection.setRequestProperty(
            "Prefer",
            "return=minimal"
        )

        val body = JSONObject()

        body.put(
            "id",
            orderId
        )

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

    } finally {

        connection.disconnect()
    }
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

        connection.outputStream.use { output ->

            output.write(
                jsonArray.toString()
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
                    ?: "فشل حفظ تفاصيل الطلب."

            throw Exception(
                "HTTP $responseCode: $error"
            )
        }

    } finally {

        connection.disconnect()
    }
}
