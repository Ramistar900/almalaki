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

fun adminSectionTitle(section: AdminSection): String = when (section) {
    AdminSection.HOME -> "الرئيسية"
    AdminSection.PRODUCTS -> "تعديل المنتجات"
    AdminSection.ORDERS -> "الطلبات"
    AdminSection.SALES -> "المبيعات"
    AdminSection.TOP_PRODUCTS -> "الأكثر طلبًا"
}

private fun adminRequest(method: String, path: String, accessToken: String, body: String? = null): String {
    val connection = (URL("$ADMIN_SUPABASE_URL/rest/v1/$path").openConnection() as HttpURLConnection)
    connection.requestMethod = method
    connection.setRequestProperty("apikey", ADMIN_SUPABASE_KEY)
    connection.setRequestProperty("Authorization", "Bearer $accessToken")
    connection.setRequestProperty("Content-Type", "application/json")
    connection.setRequestProperty("Accept", "application/json")
    if (body != null) {
        connection.doOutput = true
        connection.setRequestProperty("Prefer", "return=minimal")
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
    }
    val code = connection.responseCode
    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
    val response = stream?.bufferedReader()?.readText() ?: ""
    connection.disconnect()
    if (code !in 200..299) throw Exception("HTTP $code: $response")
    return response
}

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
    val cleanName = name.trim()
    val cleanCategory = category.trim()
    val priceValue = price.replace(",", ".").toDoubleOrNull()
    if (cleanName.isBlank()) { onMessage("اكتب اسم المنتج."); return }
    if (priceValue == null || priceValue < 0) { onMessage("أدخل سعرًا صحيحًا."); return }
    onLoading(true)
    onMessage("جاري الحفظ...")
    Thread {
        try {
            var imageUrl = ""
            if (selectedImageUri != null) {
                val bytes = context.contentResolver.openInputStream(selectedImageUri)?.use { it.readBytes() }
                    ?: throw Exception("تعذر قراءة الصورة.")
                imageUrl = uploadProductImage(
                    accessToken = accessToken,
                    bytes = bytes,
                    mimeType = context.contentResolver.getType(selectedImageUri) ?: "image/jpeg"
                )
            }
            if (editingProductId == null) {
                addProduct(accessToken, cleanName, cleanCategory, priceValue, imageUrl)
                onMessage("تمت إضافة المنتج بنجاح ✅")
            } else {
                val old = loadProducts().firstOrNull { it.id == editingProductId }
                updateAdminProduct(accessToken, editingProductId, cleanName, cleanCategory, priceValue, if (imageUrl.isNotBlank()) imageUrl else old?.imageUrl.orEmpty())
                onMessage("تم تعديل المنتج بنجاح ✅")
            }
            onProductsLoaded(loadProducts())
            onClear()
        } catch (e: Exception) {
            onMessage(e.message ?: "حدث خطأ أثناء الحفظ.")
        } finally { onLoading(false) }
    }.start()
}

fun updateAdminProduct(accessToken: String, id: Int, name: String, category: String, price: Double, imageUrl: String) {
    val body = JSONObject().apply {
        put("name", name.trim()); put("category", category.trim()); put("price", price)
        put("image_url", imageUrl); put("description", name.trim())
    }.toString()
    adminRequest("PATCH", "products?id=eq.$id", accessToken, body)
}

fun loadAdminOrders(accessToken: String): List<AdminOrder> {
    val response = adminRequest("GET", "orders?select=id,order_number,customer_name,customer_phone,delivery_address,fulfillment_type,total_amount,status,created_at&order=id.desc", accessToken)
    val json = JSONArray(response)
    val result = mutableListOf<AdminOrder>()
    for (i in 0 until json.length()) {
        val item = json.getJSONObject(i)
        result.add(AdminOrder(
            id = item.getLong("id"),
            orderNumber = item.optString("order_number", item.getLong("id").toString()),
            customerName = item.optString("customer_name", ""),
            customerPhone = item.optString("customer_phone", ""),
            deliveryAddress = item.optString("delivery_address", ""),
            fulfillmentType = item.optString("fulfillment_type", ""),
            totalAmount = item.optDouble("total_amount", 0.0),
            status = item.optString("status", ""),
            createdAt = item.optString("created_at", "")
        ))
    }
    return result
}

fun loadOrderItems(accessToken: String, orderId: Long): List<AdminOrderItem> {
    val response = adminRequest("GET", "order_items?select=id,order_id,product_id,quantity,unit_price,item_total&order_id=eq.$orderId", accessToken)
    val json = JSONArray(response)
    val result = mutableListOf<AdminOrderItem>()
    for (i in 0 until json.length()) {
        val item = json.getJSONObject(i)
        val quantity = item.optInt("quantity", 0)
        val unitPrice = item.optDouble("unit_price", 0.0)
        result.add(AdminOrderItem(item.getLong("id"), item.getLong("order_id"), item.getInt("product_id"), quantity, unitPrice, item.optDouble("item_total", quantity * unitPrice)))
    }
    return result
}

fun updateOrderStatusAsync(accessToken: String, orderId: Long, status: String) {
    adminRequest("PATCH", "orders?id=eq.$orderId", accessToken, JSONObject().put("status", status).toString())
}

fun deleteCancelledOrder(accessToken: String, orderId: Long) {
    adminRequest("DELETE", "orders?id=eq.$orderId&status=eq.cancelled", accessToken)
}

private data class SaleRecord(
    val totalAmount: Double,
    val soldAt: Date
)

private fun loadSalesHistory(accessToken: String): List<SaleRecord> {
    val response = adminRequest(
        "GET",
        "sales_history?select=total_amount,sold_at&order=sold_at.desc",
        accessToken
    )
    val json = JSONArray(response)
    val result = mutableListOf<SaleRecord>()

    for (i in 0 until json.length()) {
        val item = json.getJSONObject(i)
        val soldAt = parseSupabaseDate(item.optString("sold_at", "")) ?: continue
        result.add(
            SaleRecord(
                totalAmount = item.optDouble("total_amount", 0.0),
                soldAt = soldAt
            )
        )
    }
    return result
}

fun calculateSalesStats(accessToken: String): AdminSalesStats {
    // المبيعات تُقرأ من sales_history، وليس من orders.
    // لذلك تبقى الإحصائيات محفوظة حتى بعد حذف الطلبات.
    val sales = loadSalesHistory(accessToken)

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val week = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val month = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    var td = 0.0
    var wk = 0.0
    var mo = 0.0
    var tdo = 0
    var wko = 0
    var moo = 0

    for (sale in sales) {
        if (!sale.soldAt.before(month.time)) {
            mo += sale.totalAmount
            moo++
        }
        if (!sale.soldAt.before(week.time)) {
            wk += sale.totalAmount
            wko++
        }
        if (!sale.soldAt.before(today.time)) {
            td += sale.totalAmount
            tdo++
        }
    }

    return AdminSalesStats(td, wk, mo, tdo, wko, moo)
}

fun calculateTopProducts(accessToken: String, completedOrders: List<AdminOrder>, products: List<Product>): List<AdminTopProduct> {
    val quantities = mutableMapOf<Int, Int>()
    val revenues = mutableMapOf<Int, Double>()
    val names = products.associateBy { it.id }
    for (order in completedOrders) for (item in loadOrderItems(accessToken, order.id)) {
        quantities[item.productId] = (quantities[item.productId] ?: 0) + item.quantity
        revenues[item.productId] = (revenues[item.productId] ?: 0.0) + item.subtotal
    }
    return quantities.keys.map { id -> AdminTopProduct(id, names[id]?.name ?: "منتج #$id", quantities[id] ?: 0, revenues[id] ?: 0.0) }
        .sortedByDescending { it.quantity }
}

fun parseSupabaseDate(value: String): Date? {
    if (value.isBlank()) return null
    val formats = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'")
    for (format in formats) try { return SimpleDateFormat(format, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(value) } catch (_: Exception) {}
    return null
}

fun adminStatusText(status: String): String = when (status.lowercase(Locale.ROOT)) {
    "pending" -> "قيد الانتظار"
    "confirmed" -> "تم التأكيد"
    "preparing" -> "قيد التحضير"
    "ready" -> "جاهز"
    "completed" -> "مكتمل"
    "cancelled" -> "ملغى"
    "delivered" -> "تم التسليم"
    else -> status
}
