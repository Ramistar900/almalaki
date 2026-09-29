package com.almalaki.cafe

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun adminSectionTitle(section: AdminSection): String = when (section) {
    AdminSection.HOME -> "الرئيسية"
    AdminSection.PRODUCTS -> "تعديل المنتجات"
    AdminSection.ORDERS -> "الطلبات"
    AdminSection.SALES -> "المبيعات"
    AdminSection.TOP_PRODUCTS -> "الأكثر طلبًا"
}

fun saveProduct(
    context: android.content.Context,
    accessToken: String,
    name: String,
    category: String,
    price: String,
    selectedImageUri: Uri?,
    editingProductId: Int?,
    onLoading: (Boolean) -> Unit,
    onMessage: (String) -> Unit,
    onProductsLoaded: (List<Product>) -> Unit,
    onClear: () -> Unit
) {
    if (name.isBlank()) {
        onMessage("اكتب اسم المنتج.")
        return
    }

    val priceValue = price.replace(",", ".").toDoubleOrNull()
    if (priceValue == null) {
        onMessage("أدخل سعرًا صحيحًا.")
        return
    }

    onLoading(true)
    onMessage("جاري الحفظ...")

    Thread {
        try {
            var imageUrl = ""

            selectedImageUri?.let { uri ->
                val bytes = context.contentResolver
                    .openInputStream(uri)
                    ?.use { it.readBytes() }
                    ?: throw Exception("تعذر قراءة الصورة.")

                val mimeType =
                    context.contentResolver.getType(uri) ?: "image/jpeg"

                imageUrl = uploadProductImage(
                    accessToken = accessToken,
                    bytes = bytes,
                    mimeType = mimeType
                )
            }

            if (editingProductId == null) {
                addProduct(
                    accessToken = accessToken,
                    name = name.trim(),
                    category = category.trim(),
                    price = priceValue,
                    imageUrl = imageUrl
                )

                onMessage("تمت إضافة المنتج بنجاح ✅")
            } else {
                updateAdminProduct(
                    accessToken = accessToken,
                    id = editingProductId,
                    name = name.trim(),
                    category = category.trim(),
                    price = priceValue,
                    imageUrl = imageUrl
                )

                onMessage("تم تعديل المنتج بنجاح ✅")
            }

            onProductsLoaded(loadProducts())
            onClear()

        } catch (e: Exception) {
            onMessage(
                e.message ?: "حدث خطأ أثناء الحفظ."
            )
        } finally {
            onLoading(false)
        }
    }.start()
}

fun loadAdminOrders(
    accessToken: String
): List<AdminOrder> {

    val url = URL(
        "$ADMIN_SUPABASE_URL/rest/v1/orders" +
                "?select=id,order_number,customer_name," +
                "customer_phone,delivery_address,fulfillment_type," +
                "total_amount,status&order=id.desc"
    )

    val connection =
        url.openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"

        connection.setRequestProperty(
            "apikey",
            ADMIN_SUPABASE_KEY
        )

        connection.setRequestProperty(
            "Authorization",
            "Bearer $accessToken"
        )

        val code =
            connection.responseCode

        if (code !in 200..299) {
            throw Exception(
                "HTTP $code: " +
                        (
                                connection.errorStream
                                    ?.bufferedReader()
                                    ?.readText()
                                    ?: "تعذر تحميل الطلبات."
                                )
            )
        }

        val json =
            JSONArray(
                connection.inputStream
                    .bufferedReader()
                    .readText()
            )

        val result =
            mutableListOf<AdminOrder>()

        for (i in 0 until json.length()) {

            val item =
                json.getJSONObject(i)

            result.add(
                AdminOrder(
                    id = item.getLong("id"),

                    orderNumber =
                        item.optString(
                            "order_number",
                            ""
                        ),

                    customerName =
                        item.optString(
                            "customer_name",
                            ""
                        ),

                    customerPhone =
                        item.optString(
                            "customer_phone",
                            ""
                        ),

                    deliveryAddress =
                        item.optString(
                            "delivery_address",
                            ""
                        ),

                    fulfillmentType =
                        item.optString(
                            "fulfillment_type",
                            ""
                        ),

                    totalAmount =
                        item.optDouble(
                            "total_amount",
                            0.0
                        ),

                    status =
                        item.optString(
                            "status",
                            "new"
                        )
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

    val url = URL(
        "$ADMIN_SUPABASE_URL/rest/v1/order_items" +
                "?select=id,order_id,product_id,quantity,unit_price,item_total" +
                "&order_id=eq.$orderId&order=id.asc"
    )

    val connection =
        url.openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"

        connection.setRequestProperty(
            "apikey",
            ADMIN_SUPABASE_KEY
        )

        connection.setRequestProperty(
            "Authorization",
            "Bearer $accessToken"
        )

        val code =
            connection.responseCode

        if (code !in 200..299) {
            throw Exception(
                "HTTP $code: " +
                        (
                                connection.errorStream
                                    ?.bufferedReader()
                                    ?.readText()
                                    ?: "تعذر تحميل تفاصيل الطلب."
                                )
            )
        }

        val json =
            JSONArray(
                connection.inputStream
                    .bufferedReader()
                    .readText()
            )

        val result =
            mutableListOf<AdminOrderItem>()

        for (i in 0 until json.length()) {

            val item =
                json.getJSONObject(i)

            result.add(
                AdminOrderItem(
                    id =
                        item.getLong("id"),

                    orderId =
                        item.getLong("order_id"),

                    productId =
                        item.optInt(
                            "product_id",
                            0
                        ),

                    quantity =
                        item.optInt(
                            "quantity",
                            0
                        ),

                    unitPrice =
                        item.optDouble(
                            "unit_price",
                            0.0
                        ),

                    subtotal =
                        item.optDouble(
                            "item_total",
                            0.0
                        )
                )
            )
        }

        return result

    } finally {
        connection.disconnect()
    }
}

fun calculateSalesStats(
    accessToken: String
): AdminSalesStats {

    val url = URL(
        "$ADMIN_SUPABASE_URL/rest/v1/orders" +
                "?select=total_amount,created_at,status" +
                "&status=eq.completed" +
                "&order=created_at.desc" +
                "&limit=1000"
    )

    val connection =
        url.openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"

        connection.setRequestProperty(
            "apikey",
            ADMIN_SUPABASE_KEY
        )

        connection.setRequestProperty(
            "Authorization",
            "Bearer $accessToken"
        )

        val code =
            connection.responseCode

        if (code !in 200..299) {
            throw Exception(
                "HTTP $code: " +
                        (
                                connection.errorStream
                                    ?.bufferedReader()
                                    ?.readText()
                                    ?: "تعذر تحميل المبيعات."
                                )
            )
        }

        val json =
            JSONArray(
                connection.inputStream
                    .bufferedReader()
                    .readText()
            )

        val todayStart =
            Calendar.getInstance().apply {

                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                set(
                    Calendar.MINUTE,
                    0
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

        val weekStart =
            Calendar.getInstance().apply {

                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                set(
                    Calendar.MINUTE,
                    0
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )

                set(
                    Calendar.DAY_OF_WEEK,
                    firstDayOfWeek
                )
            }

        val monthStart =
            Calendar.getInstance().apply {

                set(
                    Calendar.DAY_OF_MONTH,
                    1
                )

                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                set(
                    Calendar.MINUTE,
                    0
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

        var today = 0.0
        var week = 0.0
        var month = 0.0

        var todayOrders = 0
        var weekOrders = 0
        var monthOrders = 0

        for (i in 0 until json.length()) {

            val item =
                json.getJSONObject(i)

            val date =
                parseSupabaseDate(
                    item.optString(
                        "created_at"
                    )
                ) ?: continue

            val amount =
                item.optDouble(
                    "total_amount",
                    0.0
                )

            if (!date.before(todayStart.time)) {
                today += amount
                todayOrders++
            }

            if (!date.before(weekStart.time)) {
                week += amount
                weekOrders++
            }

            if (!date.before(monthStart.time)) {
                month += amount
                monthOrders++
            }
        }

        return AdminSalesStats(
            today = today,
            week = week,
            month = month,
            todayOrders = todayOrders,
            weekOrders = weekOrders,
            monthOrders = monthOrders
        )

    } finally {
        connection.disconnect()
    }
}

fun calculateTopProducts(
    accessToken: String,
    completedOrders: List<AdminOrder>,
    products: List<Product>
): List<AdminTopProduct> {

    if (completedOrders.isEmpty()) {
        return emptyList()
    }

    val ids =
        completedOrders.map {
            it.id
        }

    val inValue =
        ids.joinToString(",")

    val url = URL(
        "$ADMIN_SUPABASE_URL/rest/v1/order_items" +
                "?select=product_id,quantity,item_total" +
                "&order_id=in.($inValue)" +
                "&limit=5000"
    )

    val connection =
        url.openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"

        connection.setRequestProperty(
            "apikey",
            ADMIN_SUPABASE_KEY
        )

        connection.setRequestProperty(
            "Authorization",
            "Bearer $accessToken"
        )

        val code =
            connection.responseCode

        if (code !in 200..299) {
            throw Exception(
                "HTTP $code: " +
                        (
                                connection.errorStream
                                    ?.bufferedReader()
                                    ?.readText()
                                    ?: "تعذر حساب الأكثر طلبًا."
                                )
            )
        }

        val json =
