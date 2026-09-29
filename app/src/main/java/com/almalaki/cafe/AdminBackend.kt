package com.almalaki.cafe

import android.content.Context
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

fun adminSectionTitle(section: AdminSection): String {
    return when (section) {
        AdminSection.HOME -> "الرئيسية"
        AdminSection.PRODUCTS -> "تعديل المنتجات"
        AdminSection.ORDERS -> "الطلبات"
        AdminSection.SALES -> "المبيعات"
        AdminSection.TOP_PRODUCTS -> "الأكثر طلبًا"
    }
}

/* =========================
   إضافة / تعديل المنتجات
   ========================= */

fun saveProduct(
    context: Context,
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
    Thread {
        try {
            onLoading(true)

            val cleanName = name.trim()
            val cleanCategory = category.trim()
            val cleanPrice = price.trim().toDoubleOrNull()

            if (cleanName.isBlank()) {
                throw Exception("يرجى إدخال اسم المنتج.")
            }

            if (cleanCategory.isBlank()) {
                throw Exception("يرجى إدخال تصنيف المنتج.")
            }

            if (cleanPrice == null || cleanPrice < 0) {
                throw Exception("يرجى إدخال سعر صحيح.")
            }

            var imageUrl = ""

            if (selectedImageUri != null) {
                val bytes = context.contentResolver
                    .openInputStream(selectedImageUri)
                    ?.use { it.readBytes() }
                    ?: throw Exception("تعذر قراءة الصورة.")

                val mimeType =
                    context.contentResolver.getType(selectedImageUri)
                        ?: "image/jpeg"

                imageUrl = uploadProductImage(
                    accessToken = accessToken,
                    bytes = bytes,
                    mimeType = mimeType
                )
            }

            if (editingProductId != null) {

                updateAdminProduct(
                    accessToken = accessToken,
                    id = editingProductId,
                    name = cleanName,
                    category = cleanCategory,
                    price = cleanPrice,
                    imageUrl = imageUrl
                )

                onMessage("تم تعديل المنتج بنجاح ✅")

            } else {

                addProduct(
                    accessToken = accessToken,
                    name = cleanName,
                    category = cleanCategory,
                    price = cleanPrice,
                    imageUrl = imageUrl
                )

                onMessage("تمت إضافة المنتج بنجاح ✅")
            }

            val products = loadProducts()
            onProductsLoaded(products)
            onClear()

        } catch (e: Exception) {

            onMessage(
                e.message ?: "حدث خطأ أثناء حفظ المنتج."
            )

        } finally {
            onLoading(false)
        }
    }.start()
}

/* =========================
   الطلبات
   ========================= */

fun loadAdminOrders(
    accessToken: String
): List<AdminOrder> {

    val connection = URL(
        "$ADMIN_SUPABASE_URL/rest/v1/orders" +
            "?select=id,order_number,customer_name,customer_phone," +
            "delivery_address,fulfillment_type,total_amount,status,created_at" +
            "&order=id.desc"
    ).openConnection() as HttpURLConnection

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

        val code = connection.responseCode

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

                    orderNumber =
                        item.optString(
                            "order_number",
                            item.getLong("id").toString()
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
                            ""
                        ),

                    createdAt =
                        item.optString(
                            "created_at",
                            ""
                        )
                )
            )
        }

        return result

    } finally {
        connection.disconnect()
    }
}

/* =========================
   تفاصيل الطلب
   ========================= */

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

        connection.setRequestProperty(
            "apikey",
            ADMIN_SUPABASE_KEY
        )

        connection.setRequestProperty(
            "Authorization",
            "Bearer $accessToken"
        )

        val code = connection.responseCode

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
                    unitPrice = item.optDouble(
                        "unit_price",
                        0.0
                    ),
                    subtotal = item.optDouble(
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

/* =========================
   إحصائيات المبيعات
   ========================= */

fun calculateSalesStats(
    accessToken: String
): AdminSalesStats {

    val orders = loadAdminOrders(accessToken)

    val completedOrders = orders.filter {
        it.status.equals(
            "completed",
            ignoreCase = true
        )
    }

    val now = Calendar.getInstance()

    val todayStart =
        now.clone() as Calendar

    todayStart.set(
        Calendar.HOUR_OF_DAY,
        0
    )
    todayStart.set(
        Calendar.MINUTE,
        0
    )
    todayStart.set(
        Calendar.SECOND,
        0
    )
    todayStart.set(
        Calendar.MILLISECOND,
        0
    )

    val weekStart =
        now.clone() as Calendar

    weekStart.set(
        Calendar.DAY_OF_WEEK,
        weekStart.firstDayOfWeek
    )
    weekStart.set(
        Calendar.HOUR_OF_DAY,
        0
    )
    weekStart.set(
        Calendar.MINUTE,
        0
    )
    weekStart.set(
        Calendar.SECOND,
        0
    )
    weekStart.set(
        Calendar.MILLISECOND,
        0
    )

    val monthStart =
        now.clone() as Calendar

    monthStart.set(
        Calendar.DAY_OF_MONTH,
        1
    )
    monthStart.set(
        Calendar.HOUR_OF_DAY,
        0
    )
    monthStart.set(
        Calendar.MINUTE,
        0
    )
    monthStart.set(
        Calendar.SECOND,
        0
    )
    monthStart.set(
        Calendar.MILLISECOND,
        0
    )

    var todaySales = 0.0
    var weekSales = 0.0
    var monthSales = 0.0

    var todayOrders = 0
    var weekOrders = 0
    var monthOrders = 0

    for (order in completedOrders) {

        val date =
            parseSupabaseDate(
                order.createdAt
            ) ?: continue

        val amount =
            order.totalAmount

        if (
            date.time >=
            todayStart.timeInMillis
        ) {
            todaySales += amount
            todayOrders++
        }

        if (
            date.time >=
            weekStart.timeInMillis
        ) {
            weekSales += amount
            weekOrders++
        }

        if (
            date.time >=
            monthStart.timeInMillis
        ) {
            monthSales += amount
            monthOrders++
        }
    }

    return AdminSalesStats(
        today = todaySales,
        week = weekSales,
        month = monthSales,
        todayOrders = todayOrders,
        weekOrders = weekOrders,
        monthOrders = monthOrders
    )
}

/* =========================
   الأكثر طلبًا
   ========================= */

fun calculateTopProducts(
    accessToken: String,
    completedOrders: List<AdminOrder>,
    products: List<Product>
): List<AdminTopProduct> {

    if (completedOrders.isEmpty()) {
        return emptyList()
    }

    val quantities =
        mutableMapOf<Int, Int>()

    val revenues =
        mutableMapOf<Int, Double>()

    for (order in completedOrders) {

        val items =
            loadOrderItems(
                accessToken = accessToken,
                orderId = order.id
            )

        for (item in items) {

            quantities[item.productId] =
                (
