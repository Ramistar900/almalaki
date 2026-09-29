package com.almalaki.cafe

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun adminSectionTitle(section: AdminSection): String {
    return when (section) {
        AdminSection.HOME -> "الرئيسية"
        AdminSection.PRODUCTS -> "تعديل المنتجات"
        AdminSection.ORDERS -> "الطلبات"
        AdminSection.SALES -> "المبيعات"
        AdminSection.TOP_PRODUCTS -> "الأكثر طلبًا"
    }
}

fun saveProduct(
    accessToken: String,
    name: String,
    category: String,
    price: Double,
    imageUrl: String
) {
    val connection = URL(
        "$ADMIN_SUPABASE_URL/rest/v1/products"
    ).openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("apikey", ADMIN_SUPABASE_KEY)
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Prefer", "return=minimal")

        val body = JSONObject().apply {
            put("name", name.trim())
            put("category", category.trim())
            put("price", price)
            put("image_url", imageUrl)
            put("description", name.trim())
        }.toString()

        connection.outputStream.use {
            it.write(body.toByteArray(Charsets.UTF_8))
        }

        val code = connection.responseCode

        if (code !in 200..299) {
            throw Exception(
                "HTTP $code: " +
                    (connection.errorStream?.bufferedReader()?.readText()
                        ?: "تعذر إضافة المنتج.")
            )
        }
    } finally {
        connection.disconnect()
    }
}

fun loadAdminOrders(
    accessToken: String
): List<AdminOrder> {

    val connection = URL(
        "$ADMIN_SUPABASE_URL/rest/v1/orders" +
            "?select=id,customer_name,customer_phone,total_amount,status,created_at" +
            "&order=id.desc"
    ).openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"
        connection.setRequestProperty("apikey", ADMIN_SUPABASE_KEY)
        connection.setRequestProperty("Authorization", "Bearer $accessToken")

        val code = connection.responseCode

        if (code !in 200..299) {
            throw Exception(
                "HTTP $code: " +
                    (connection.errorStream?.bufferedReader()?.readText()
                        ?: "تعذر تحميل الطلبات.")
            )
        }

        val response = connection.inputStream
            .bufferedReader()
            .readText()

        val json = JSONArray(response)
        val result = mutableListOf<AdminOrder>()

        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)

            result.add(
                AdminOrder(
                    id = item.getLong("id"),
                    customerName = item.optString("customer_name", ""),
                    customerPhone = item.optString("customer_phone", ""),
                    totalAmount = item.optDouble("total_amount", 0.0),
                    status = item.optString("status", ""),
                    createdAt = item.optString("created_at", "")
                )
            )
        }

        return result
    } finally {
        connection.disconnect()
    }
}

fun loadOrderItems(
    accessToken: String,
    orderId: Long
): List<AdminOrderItem> {

    val connection = URL(
        "$ADMIN_SUPABASE_URL/rest/v1/order_items" +
            "?select=id,order_id,product_id,quantity,unit_price,item_total" +
            "&order_id=eq.$orderId" +
            "&order=id.asc"
    ).openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"
        connection.setRequestProperty("apikey", ADMIN_SUPABASE_KEY)
        connection.setRequestProperty("Authorization", "Bearer $accessToken")

        val code = connection.responseCode

        if (code !in 200..299) {
            throw Exception(
                "HTTP $code: " +
                    (connection.errorStream?.bufferedReader()?.readText()
                        ?: "تعذر تحميل تفاصيل الطلب.")
            )
        }

        val response = connection.inputStream
            .bufferedReader()
            .readText()

        val json = JSONArray(response)
        val result = mutableListOf<AdminOrderItem>()

        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)

            result.add(
                AdminOrderItem(
                    id = item.getLong("id"),
                    orderId = item.getLong("order_id"),
                    productId = item.getInt("product_id"),
                    quantity = item.getInt("quantity"),
                    unitPrice = item.optDouble("unit_price", 0.0),
                    subtotal = item.optDouble("item_total", 0.0)
                )
            )
        }

        return result
    } finally {
        connection.disconnect()
    }
}

fun calculateSalesStats(
    orders: List<AdminOrder>
): SalesStats {

    val completedOrders = orders.filter {
        it.status.equals("completed", ignoreCase = true)
    }

    val now = Date()

    val calendar = java.util.Calendar.getInstance()
    calendar.time = now

    val todayStart = calendar.clone() as java.util.Calendar
    todayStart.set(java.util.Calendar.HOUR_OF_DAY, 0)
    todayStart.set(java.util.Calendar.MINUTE, 0)
    todayStart.set(java.util.Calendar.SECOND, 0)
    todayStart.set(java.util.Calendar.MILLISECOND, 0)

    val weekStart = calendar.clone() as java.util.Calendar
    weekStart.set(java.util.Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
    weekStart.set(java.util.Calendar.HOUR_OF_DAY, 0)
    weekStart.set(java.util.Calendar.MINUTE, 0)
    weekStart.set(java.util.Calendar.SECOND, 0)
    weekStart.set(java.util.Calendar.MILLISECOND, 0)

    val monthStart = calendar.clone() as java.util.Calendar
    monthStart.set(java.util.Calendar.DAY_OF_MONTH, 1)
    monthStart.set(java.util.Calendar.HOUR_OF_DAY, 0)
    monthStart.set(java.util.Calendar.MINUTE, 0)
    monthStart.set(java.util.Calendar.SECOND, 0)
    monthStart.set(java.util.Calendar.MILLISECOND, 0)

    var todaySales = 
