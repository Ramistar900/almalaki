package com.almalaki.cafe

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.UUID
import kotlin.math.abs

private const val ORDER_SUPABASE_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val ORDER_SUPABASE_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

private const val MAX_ORDER_ITEMS = 100
private const val MAX_ITEM_QUANTITY = 1000
private const val MAX_CUSTOMER_NAME_LENGTH = 120
private const val MAX_CUSTOMER_PHONE_LENGTH = 40
private const val MAX_DELIVERY_ADDRESS_LENGTH = 500

data class OrderResult(
    val orderNumber: String,
    val totalAmount: Double,
    val items: List<RoyalInvoiceItem>
)

/**
 * Submits checkout through the server-side atomic and idempotent RPC.
 * Only product IDs and quantities are sent; the database validates tenant
 * ownership and calculates authoritative prices and totals.
 */
fun createOrder(
    customerName: String,
    customerPhone: String,
    deliveryAddress: String,
    fulfillmentType: String,
    @Suppress("UNUSED_PARAMETER") totalAmount: Double,
    products: List<Product>,
    cart: Map<Int, Int>,
    @Suppress("UNUSED_PARAMETER") restaurantId: String = ROYAL_DEFAULT_RESTAURANT_ID,
    restaurantCode: String = ROYAL_DEFAULT_RESTAURANT_CODE,
    paymentMethod: String = "cash",
    idempotencyKey: String
): OrderResult {
    val normalizedName = customerName.trim()
    val normalizedPhone = customerPhone.trim()
    val normalizedAddress = deliveryAddress.trim()
    val normalizedKey = try {
        UUID.fromString(idempotencyKey).toString()
    } catch (_: IllegalArgumentException) {
        throw Exception("معرّف محاولة الطلب غير صالح. أعد المحاولة.")
    }

    if (normalizedName.isEmpty() || normalizedName.length > MAX_CUSTOMER_NAME_LENGTH) {
        throw Exception("أدخل اسمًا صحيحًا لا يتجاوز 120 حرفًا.")
    }
    if (normalizedPhone.length > MAX_CUSTOMER_PHONE_LENGTH) {
        throw Exception("رقم الهاتف طويل جدًا.")
    }
    if (cart.isEmpty() || cart.values.none { it > 0 }) {
        throw Exception("السلة فارغة.")
    }
    if (paymentMethod != "cash") {
        throw Exception("طريقة الدفع الإلكتروني غير متاحة حاليًا.")
    }
    if (fulfillmentType != "داخل المحل" && fulfillmentType != "توصيل إلى المنزل") {
        throw Exception("نوع الطلب غير صحيح.")
    }
    if (fulfillmentType == "توصيل إلى المنزل") {
        if (normalizedAddress.isEmpty()) throw Exception("عنوان التوصيل مطلوب.")
        if (normalizedAddress.length > MAX_DELIVERY_ADDRESS_LENGTH) {
            throw Exception("عنوان التوصيل طويل جدًا.")
        }
    }
    if (restaurantCode.isBlank() || restaurantCode.length > 128) {
        throw Exception("رمز المطعم غير متوفر. أعد اختيار المطعم.")
    }
    if (cart.values.any { it < 0 || it > MAX_ITEM_QUANTITY }) {
        throw Exception("كمية أحد المنتجات غير صحيحة.")
    }

    val selectedProducts = cart.filterValues { it > 0 }.toSortedMap()
    if (selectedProducts.isEmpty() || selectedProducts.size > MAX_ORDER_ITEMS) {
        throw Exception("عدد أصناف الطلب غير صحيح.")
    }

    val loadedProductIds = products.map { it.id }.toSet()
    if (selectedProducts.keys.any { it <= 0 || it !in loadedProductIds }) {
        throw Exception("تغيّرت قائمة المنتجات. حدّث القائمة ثم حاول مجددًا.")
    }

    val itemsPayload = JSONArray()
    selectedProducts.forEach { (productId, quantity) ->
        itemsPayload.put(
            JSONObject()
                .put("product_id", productId)
                .put("quantity", quantity)
        )
    }

    val body = JSONObject()
        .put("p_idempotency_key", normalizedKey)
        .put("p_code", restaurantCode.trim().uppercase())
        .put("p_customer_name", normalizedName)
        .put("p_customer_phone", normalizedPhone)
        .put(
            "p_delivery_address",
            if (fulfillmentType == "توصيل إلى المنزل") normalizedAddress else ""
        )
        .put("p_fulfillment_type", fulfillmentType)
        .put("p_items", itemsPayload)
        .put("p_payment_method", paymentMethod)

    val connection = URL(
        "$ORDER_SUPABASE_URL/rest/v1/rpc/create_restaurant_order_with_payment_idempotent"
    ).openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 20000
        connection.doOutput = true
        connection.setRequestProperty("apikey", ORDER_SUPABASE_KEY)
        // Preserve the customer's JWT so the database links the order to auth.uid().
        connection.setRequestProperty(
            "Authorization",
            "Bearer ${RoyalSupabaseAuth.accessTokenOrNull() ?: ORDER_SUPABASE_KEY}"
        )
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")

        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

        val responseCode = connection.responseCode
        val responseBody = (
            if (responseCode in 200..299) connection.inputStream else connection.errorStream
        )?.bufferedReader()?.use { it.readText() }.orEmpty()

        if (responseCode !in 200..299) {
            throw Exception(
                "تعذر إرسال الطلب (رمز $responseCode). تحقق من البيانات والاتصال ثم حاول مجددًا."
            )
        }

        val rows = try {
            JSONArray(responseBody)
        } catch (_: Exception) {
            throw Exception("وصل رد غير صالح من خدمة الطلبات. لم يتم اعتماد الفاتورة.")
        }
        if (rows.length() != 1) {
            throw Exception("لم تُرجع قاعدة البيانات تأكيدًا واضحًا للطلب.")
        }

        val result = rows.optJSONObject(0)
            ?: throw Exception("بيانات تأكيد الطلب غير صالحة.")
        val orderNumber = result.optString("order_number").trim()
        if (orderNumber.isEmpty()) throw Exception("لم يُرجع الخادم رقمًا صالحًا للطلب.")

        val serverTotal = result.optDouble("total_amount", Double.NaN)
        if (!serverTotal.isFinite() || serverTotal <= 0.0) {
            throw Exception("أعاد الخادم إجماليًا غير صالح للطلب.")
        }

        val serverItems = result.optJSONArray("items")
            ?: throw Exception("لم يُرجع الخادم تفاصيل الطلب.")
        val invoiceItems = buildList {
            for (index in 0 until serverItems.length()) {
                val item = serverItems.optJSONObject(index)
                    ?: throw Exception("توجد تفاصيل غير صالحة ضمن الطلب.")
                val itemName = item.optString("name").trim()
                val quantity = item.optInt("quantity", 0)
                val unitPrice = item.optDouble("unit_price", Double.NaN)
                if (
                    itemName.isEmpty() ||
                    quantity !in 1..MAX_ITEM_QUANTITY ||
                    !unitPrice.isFinite() ||
                    unitPrice < 0.0
                ) {
                    throw Exception("أعاد الخادم تفاصيل غير صالحة لأحد المنتجات.")
                }
                add(RoyalInvoiceItem(itemName, quantity, unitPrice))
            }
        }

        if (invoiceItems.isEmpty() || invoiceItems.size != selectedProducts.size) {
            throw Exception("عدد أصناف الفاتورة لا يطابق الأصناف المطلوبة.")
        }
        val calculatedTotal = invoiceItems.sumOf { it.unitPrice * it.quantity }
        if (!calculatedTotal.isFinite() || abs(calculatedTotal - serverTotal) > 0.01) {
            throw Exception("إجمالي الفاتورة لا يطابق تفاصيل المنتجات المستلمة.")
        }

        return OrderResult(orderNumber, serverTotal, invoiceItems)
    } catch (error: SocketTimeoutException) {
        // Keep the same local key: the server may have committed before the timeout.
        throw Exception(
            "انتهت مهلة الاتصال. أعد المحاولة من نافذة الطلب نفسها؛ لن يُنشأ طلب مكرر.",
            error
        )
    } finally {
        connection.disconnect()
    }
}
