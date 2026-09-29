package com.almalaki.cafe

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val ADMIN_SUPABASE_URL = "https://duvxxskgdmgrtaleedqu.supabase.co"
private const val ADMIN_SUPABASE_KEY = "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"
private val AdminGold = Color(0xFFD4AF37)
private val AdminBlack = Color(0xFF050505)
private val AdminCream = Color(0xFFF5F0E5)

data class AdminOrder(val id: Long, val orderNumber: String, val customerName: String, val customerPhone: String, val deliveryAddress: String, val fulfillmentType: String, val totalAmount: Double, val status: String)
data class AdminOrderItem(val id: Long, val orderId: Long, val productId: Int, val quantity: Int, val unitPrice: Double, val subtotal: Double)

@Composable
fun AdminScreen(accessToken: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var editingProductId by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var orders by remember { mutableStateOf<List<AdminOrder>>(emptyList()) }
    var loadingOrders by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
        message = if (uri != null) "تم اختيار الصورة ✅" else ""
    }

    fun refreshProducts() { Thread { try { products = loadProducts() } catch (e: Exception) { message = e.message ?: "تعذر تحميل المنتجات." } }.start() }
    fun refreshOrders() { loadingOrders = true; Thread { try { orders = loadAdminOrders(accessToken) } catch (e: Exception) { message = e.message ?: "تعذر تحميل الطلبات." } finally { loadingOrders = false } }.start() }
    LaunchedEffect(Unit) { refreshProducts(); refreshOrders() }

    Column(Modifier.fillMaxSize().background(AdminBlack).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("لوحة المالك 👑", color = AdminGold, fontSize = 26.sp)
            TextButton(onClick = onLogout) { Text("خروج", color = AdminGold) }
        }
        Spacer(Modifier.height(12.dp))
        Text(if (editingProductId == null) "إضافة منتج" else "تعديل المنتج", color = AdminGold, fontSize = 22.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("اسم المنتج") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), label = { Text("التصنيف") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(price, { price = it }, Modifier.fillMaxWidth(), label = { Text("السعر بالليرة السورية") })
        Spacer(Modifier.height(10.dp))
        OutlinedButton({ imagePicker.launch("image/*") }, Modifier.fillMaxWidth()) { Text(if (selectedImageUri == null) "📷 اختيار صورة المنتج" else "✅ تم اختيار صورة المنتج") }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (name.isBlank()) { message = "اكتب اسم المنتج."; return@Button }
                val priceValue = price.replace(",", ".").toDoubleOrNull()
                if (priceValue == null) { message = "أدخل سعرًا صحيحًا."; return@Button }
                loading = true; message = "جاري الحفظ..."
                Thread {
                    try {
                        var imageUrl = ""
                        selectedImageUri?.let { uri ->
                            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw Exception("تعذر قراءة الصورة.")
                            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                            imageUrl = uploadProductImage(accessToken, bytes, mimeType)
                        }
                        val id = editingProductId
                        if (id == null) { addProduct(accessToken, name.trim(), category.trim(), priceValue, imageUrl); message = "تمت إضافة المنتج بنجاح ✅" }
                        else { updateAdminProduct(accessToken, id, name.trim(), category.trim(), priceValue, imageUrl); message = "تم تعديل المنتج بنجاح ✅" }
                        products = loadProducts(); name = ""; category = ""; price = ""; selectedImageUri = null; editingProductId = null
                    } catch (e: Exception) { message = e.message ?: "حدث خطأ أثناء الحفظ." } finally { loading = false }
                }.start()
            }, Modifier.weight(1f), enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = AdminGold, contentColor = AdminBlack)) { Text(if (loading) "جاري..." else if (editingProductId == null) "إضافة المنتج" else "حفظ التعديل") }
            if (editingProductId != null) OutlinedButton({ editingProductId = null; name = ""; category = ""; price = ""; selectedImageUri = null; message = "" }, Modifier.weight(1f)) { Text("إلغاء") }
        }
        Spacer(Modifier.height(10.dp))
        if (message.isNotEmpty()) Text(message, color = AdminCream)
        Spacer(Modifier.height(16.dp))
        Text("المنتجات الحالية", color = AdminGold, fontSize = 22.sp)
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(products, key = { it.id }) { product ->
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(product.name, color = AdminCream, fontSize = 18.sp)
                    Text("${product.category} • ${formatPrice(product.price)}", color = AdminGold)
                    if (product.imageUrl.isNotEmpty()) Text("📷 توجد صورة للمنتج", color = AdminCream, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ editingProductId = product.id; name = product.name; category = product.category; price = product.price.toString(); selectedImageUri = null; message = "يمكنك تعديل بيانات المنتج الآن." }, Modifier.weight(1f)) { Text("تعديل") }
                        OutlinedButton({ Thread { try { deleteProduct(accessToken, product.id); products = loadProducts(); message = "تم حذف المنتج بنجاح ✅" } catch (e: Exception) { message = e.message ?: "تعذر حذف المنتج." } }.start() }, Modifier.weight(1f)) { Text("حذف", color = Color.Red) }
                    }
                    HorizontalDivider(Modifier.padding(top = 8.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("الطلبات", color = AdminGold, fontSize = 22.sp)
            OutlinedButton({ refreshOrders() }, enabled = !loadingOrders) { Text(if (loadingOrders) "جاري..." else "تحديث") }
        }
        Spacer(Modifier.height(6.dp))
        LazyColumn(Modifier.weight(1f)) { items(orders, key = { it.id }) { order -> AdminOrderCard(order, accessToken) { refreshOrders() } } }
    }
}

@Composable
private fun AdminOrderCard(order: AdminOrder, accessToken: String, onUpdated: () -> Unit) {
    var expanded by remember(order.id) { mutableStateOf(false) }
    var orderItems by remember(order.id) { mutableStateOf<List<AdminOrderItem>>(emptyList()) }
    var loadingItems by remember(order.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("طلب ${order.orderNumber}", color = AdminGold, fontSize = 18.sp)
        Text("العميل: ${order.customerName}", color = AdminCream)
        Text("الهاتف: ${order.customerPhone}", color = AdminCream)
        if (order.deliveryAddress.isNotBlank()) Text("العنوان: ${order.deliveryAddress}", color = AdminCream)
        Text("النوع: ${order.fulfillmentType}", color = AdminCream)
        Text("المجموع: ${formatPrice(order.totalAmount)}", color = AdminGold)
        Text("الحالة: ${adminStatusText(order.status)}", color = AdminCream)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({
                expanded = !expanded
                if (expanded && orderItems.isEmpty()) { loadingItems = true; Thread { try { orderItems = loadOrderItems(accessToken, order.id) } catch (_: Exception) {} finally { loadingItems = false } }.start() }
            }, Modifier.weight(1f)) { Text(if (expanded) "إخفاء التفاصيل" else "تفاصيل الطلب") }
            OutlinedButton({ Thread { try { updateOrderStatusAsync(accessToken, order.id, "preparing"); onUpdated() } catch (_: Exception) {} }.start() }, Modifier.weight(1f)) { Text("تحضير") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ Thread { try { updateOrderStatusAsync(accessToken, order.id, "ready"); onUpdated() } catch (_: Exception) {} }.start() }, Modifier.weight(1f)) { Text("جاهز") }
            OutlinedButton({ Thread { try { updateOrderStatusAsync(accessToken, order.id, "completed"); onUpdated() } catch (_: Exception) {} }.start() }, Modifier.weight(1f)) { Text("مكتمل") }
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            if (loadingItems) Text("جاري تحميل التفاصيل...", color = AdminCream)
            else if (orderItems.isEmpty()) Text("لا توجد تفاصيل للطلب.", color = AdminCream)
            else orderItems.forEach { item -> Text("المنتج #${item.productId} × ${item.quantity} — ${formatPrice(item.subtotal)}", color = AdminCream, modifier = Modifier.padding(vertical = 2.dp)) }
        }
        HorizontalDivider(Modifier.padding(top = 10.dp))
    }
}

private fun loadAdminOrders(accessToken: String): List<AdminOrder> {
    val connection = URL("$ADMIN_SUPABASE_URL/rest/v1/orders?select=id,order_number,customer_name,customer_phone,delivery_address,fulfillment_type,total_amount,status&order=id.desc").openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "GET"
        connection.setRequestProperty("apikey", ADMIN_SUPABASE_KEY)
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        val code = connection.responseCode
        if (code !in 200..299) throw Exception("HTTP $code: ${connection.errorStream?.bufferedReader()?.readText() ?: "تعذر تحميل الطلبات."}")
        val json = JSONArray(connection.inputStream.bufferedReader().readText())
        val result = mutableListOf<AdminOrder>()
        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)
            result.add(AdminOrder(item.getLong("id"), item.optString("order_number", ""), item.optString("customer_name", ""), item.optString("customer_phone", ""), item.optString("delivery_address", ""), item.optString("fulfillment_type", ""), item.optDouble("total_amount", 0.0), item.optString("status", "new")))
        }
        return result
    } finally { connection.disconnect() }
}

private fun loadOrderItems(accessToken: String, orderId: Long): List<AdminOrderItem> {
    val connection = URL("$ADMIN_SUPABASE_URL/rest/v1/order_items?select=id,order_id,product_id,quantity,unit_price,subtotal&order_id=eq.$orderId&order=id.asc").openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "GET"
        connection.setRequestProperty("apikey", ADMIN_SUPABASE_KEY)
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        val code = connection.responseCode
        if (code !in 200..299) throw Exception("HTTP $code: ${connection.errorStream?.bufferedReader()?.readText() ?: "تعذر تحميل تفاصيل الطلب."}")
        val json = JSONArray(connection.inputStream.bufferedReader().readText())
        val result = mutableListOf<AdminOrderItem>()
        for (i in 0 until json.length()) {
            val item = json.getJSONObject(i)
            result.add(AdminOrderItem(item.getLong("id"), item.getLong("order_id"), item.optInt("product_id", 0), item.optInt("quantity", 0), item.optDouble("unit_price", 0.0), item.optDouble("subtotal", 0.0)))
        }
        return result
    } finally { connection.disconnect() }
}

private fun updateAdminProduct(accessToken: String, id: Int, name: String, category: String, price: Double, imageUrl: String) {
    val connection = URL("$ADMIN_SUPABASE_URL/rest/v1/products?id=eq.$id").openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "PATCH"; connection.doOutput = true
        connection.setRequestProperty("apikey", ADMIN_SUPABASE_KEY)
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Prefer", "return=minimal")
        val body = JSONObject().apply { put("name", name); put("category", category); put("price", price); put("description", name); if (imageUrl.isNotBlank()) put("image_url", imageUrl) }.toString()
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        if (code !in 200..299) throw Exception("HTTP $code: ${connection.errorStream?.bufferedReader()?.readText() ?: "تعذر تعديل المنتج."}")
    } finally { connection.disconnect() }
}

private fun updateOrderStatusAsync(accessToken: String, orderId: Long, status: String) {
    val connection = URL("$ADMIN_SUPABASE_URL/rest/v1/orders?id=eq.$orderId").openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "PATCH"; connection.doOutput = true
        connection.setRequestProperty("apikey", ADMIN_SUPABASE_KEY)
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Prefer", "return=minimal")
        connection.outputStream.use { it.write(JSONObject().put("status", status).toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        if (code !in 200..299) throw Exception("HTTP $code: ${connection.errorStream?.bufferedReader()?.readText() ?: "تعذر تحديث حالة الطلب."}")
    } finally { connection.disconnect() }
}

private fun adminStatusText(status: String): String = when (status.lowercase()) {
    "new" -> "جديد 🆕"
    "preparing" -> "قيد التحضير 👨‍🍳"
    "ready" -> "جاهز ✅"
    "completed" -> "مكتمل 🎉"
    "cancelled" -> "ملغى ❌"
    else -> status
}
