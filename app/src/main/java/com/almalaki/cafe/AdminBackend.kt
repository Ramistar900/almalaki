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
            onMessage(e.message ?: "حدث خطأ أثناء الحفظ.")
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

    val connection = url.openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"
        connection.setRequestProperty("apikey", ADMIN_SUPABASE_KEY)
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

        val json = JSONArray(
            connection.inputStream.bufferedReader().readText()
        )

        val result = mutableListOf<AdminOrder>()

        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)

            result.add(
                AdminOrder(
                    id = item.getLong("id"),
                    orderNumber = item.optString(
                        "order_number",
                        ""
                    ),
                    customerName = item.optString(
                        "customer_name",
                        ""
                    ),
                    customerPhone = item.optString(
                        "customer_phone",
                        ""
                    ),
                    deliveryAddress = item.optString(
                        "delivery_address",
                        ""
                    ),
                    fulfillmentType = item.optString(
                        "fulfillment_type",
                        ""
                    ),
                    totalAmount = item.optDouble(
                        "total
