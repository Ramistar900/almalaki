package com.almalaki.cafe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class RoyalSalesRecord(
    val id: Long,
    val soldAt: String,
    val orderNumber: String,
    val orderId: Long,
    val customerName: String,
    val customerPhone: String,
    val fulfillmentType: String,
    val deliveryAddress: String,
    val totalAmount: Double,
    val items: List<RoyalSalesItem>
)

data class RoyalSalesItem(
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val itemTotal: Double
)

private val RoyalSalesDateFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH)
        .withZone(ZoneId.of("Asia/Damascus"))

private fun royalSalesDisplayDate(value: String): String {
    return try {
        RoyalSalesDateFormatter.format(Instant.parse(value))
    } catch (_: Exception) {
        value.replace("T", " ").take(16)
    }
}

private fun royalSalesGet(
    accessToken: String,
    table: String,
    select: String,
    offset: Int,
    limit: Int = 1000
): String {
    val encodedSelect = URLEncoder.encode(select, "UTF-8")
    val url = URL(
        "$ADMIN_SUPABASE_URL/rest/v1/$table" +
            "?select=$encodedSelect&order=id.asc&limit=$limit&offset=$offset"
    )
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.setRequestProperty("apikey", ADMIN_SUPABASE_KEY)
    connection.setRequestProperty("Authorization", "Bearer $accessToken")
    connection.setRequestProperty("Accept", "application/json")

    val code = connection.responseCode
    val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
        .bufferedReader().use { it.readText() }

    if (code !in 200..299) {
        throw IllegalStateException(body.ifBlank { "تعذر تحميل سجلات المبيعات." })
    }
    return body
}

private fun loadRoyalSalesRecords(accessToken: String): List<RoyalSalesRecord> {
    val records = mutableListOf<RoyalSalesRecord>()
    var offset = 0

    while (true) {
        val body = royalSalesGet(
            accessToken,
            "sales_records",
            "id,sold_at,order_number,order_id,customer_name,customer_phone,fulfillment_type,delivery_address,total_amount",
            offset
        )
        val array = JSONArray(body)
        if (array.length() == 0) break
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            records.add(
                RoyalSalesRecord(
                    id = obj.optLong("id"),
                    soldAt = obj.optString("sold_at"),
                    orderNumber = obj.optString("order_number"),
                    orderId = obj.optLong("order_id"),
                    customerName = obj.optString("customer_name"),
                    customerPhone = obj.optString("customer_phone"),
                    fulfillmentType = obj.optString("fulfillment_type"),
                    deliveryAddress = obj.optString("delivery_address"),
                    totalAmount = obj.optDouble("total_amount", 0.0),
                    items = emptyList()
                )
            )
        }
        if (array.length() < 1000) break
        offset += 1000
    }

    val itemsByRecord = mutableMapOf<Long, MutableList<RoyalSalesItem>>()
    offset = 0
    while (true) {
        val body = royalSalesGet(
            accessToken,
            "sales_record_items",
            "sales_record_id,product_id,product_name,quantity,unit_price,item_total",
            offset
        )
        val array = JSONArray(body)
        if (array.length() == 0) break
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val recordId = obj.optLong("sales_record_id")
            itemsByRecord.getOrPut(recordId) { mutableListOf() }.add(
                RoyalSalesItem(
                    productId = obj.optLong("product_id"),
                    productName = obj.optString("product_name"),
                    quantity = obj.optInt("quantity"),
                    unitPrice = obj.optDouble("unit_price", 0.0),
                    itemTotal = obj.optDouble("item_total", 0.0)
                )
            )
        }
        if (array.length() < 1000) break
        offset += 1000
    }

    return records.map { it.copy(items = itemsByRecord[it.id].orEmpty()) }.reversed()
}

@Composable
fun RoyalSalesRecordsScreen(
    accessToken: String,
    modifier: Modifier = Modifier
) {
    var records by remember { mutableStateOf<List<RoyalSalesRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var dateFrom by remember { mutableStateOf("") }
    var dateTo by remember { mutableStateOf("") }
    var productFilter by remember { mutableStateOf("") }
    var minTotal by remember { mutableStateOf("") }
    var maxTotal by remember { mutableStateOf("") }
    var minQuantity by remember { mutableStateOf("") }
    var maxQuantity by remember { mutableStateOf("") }

    fun refresh() {
        loading = true
        error = ""
        Thread {
            try {
                records = loadRoyalSalesRecords(accessToken)
            } catch (e: Exception) {
                error = e.message ?: "تعذر تحميل سجلات المبيعات."
            } finally {
                loading = false
            }
        }.start()
    }

    LaunchedEffect(Unit) { refresh() }

    val filtered = records.filter { record ->
        val q = search.trim()
        val pq = productFilter.trim()
        val date = record.soldAt.take(10)
        val quantity = record.items.sumOf { it.quantity }
        val min = minTotal.toDoubleOrNull()
        val max = maxTotal.toDoubleOrNull()
        val minQ = minQuantity.toIntOrNull()
        val maxQ = maxQuantity.toIntOrNull()

        (q.isBlank() || record.orderNumber.contains(q, true) ||
            record.customerName.contains(q, true) || record.customerPhone.contains(q, true) ||
            record.items.any { it.productName.contains(q, true) }) &&
            (dateFrom.isBlank() || date >= dateFrom.trim()) &&
            (dateTo.isBlank() || date <= dateTo.trim()) &&
            (pq.isBlank() || record.items.any { it.productName.contains(pq, true) }) &&
            (min == null || record.totalAmount >= min) &&
            (max == null || record.totalAmount <= max) &&
            (minQ == null || quantity >= minQ) &&
            (maxQ == null || quantity <= maxQ)
    }

    val totalRevenue = filtered.sumOf { it.totalAmount }
    val totalOrders = filtered.size
    val totalQuantity = filtered.sumOf { it.items.sumOf { item -> item.quantity } }
    val averageOrder = if (totalOrders == 0) 0.0 else totalRevenue / totalOrders
    val damascusZone = ZoneId.of("Asia/Damascus")
val today = LocalDate.now(damascusZone)

val todayRecords = records.filter { record ->
    try {
        Instant.parse(record.soldAt)
            .atZone(damascusZone)
            .toLocalDate() == today
    } catch (_: Exception) {
        false
    }
}

val todayRevenue = todayRecords.sumOf { it.totalAmount }
val todayOrders = todayRecords.size
val todayQuantity = todayRecords.sumOf {
    it.items.sumOf { item -> item.quantity }
}
val todayAverageOrder =
    if (todayOrders == 0) 0.0 else todayRevenue / todayOrders
val weekStart = today.minusDays(6)

val weekRecords = records.filter { record ->
    try {
        val recordDate = Instant.parse(record.soldAt)
            .atZone(damascusZone)
            .toLocalDate()

        !recordDate.isBefore(weekStart) &&
            !recordDate.isAfter(today)
    } catch (_: Exception) {
        false
    }
}

val weekRevenue = weekRecords.sumOf { it.totalAmount }
val weekOrders = weekRecords.size
val weekQuantity = weekRecords.sumOf {
    it.items.sumOf { item -> item.quantity }
}
val weekAverageOrder =
    if (weekOrders == 0) 0.0 else weekRevenue / weekOrders


val monthStart = today.minusDays(29)

val monthRecords = records.filter { record ->
    try {
        val recordDate = Instant.parse(record.soldAt)
            .atZone(damascusZone)
            .toLocalDate()

        !recordDate.isBefore(monthStart) &&
            !recordDate.isAfter(today)
    } catch (_: Exception) {
        false
    }
}

val monthRevenue = monthRecords.sumOf { it.totalAmount }
val monthOrders = monthRecords.size
val monthQuantity = monthRecords.sumOf {
    it.items.sumOf { item -> item.quantity }
}
val monthAverageOrder =
    if (monthOrders == 0) 0.0 else monthRevenue / monthOrders


val yearStart = LocalDate.of(today.year, 1, 1)

val yearRecords = records.filter { record ->
    try {
        val recordDate = Instant.parse(record.soldAt)
            .atZone(damascusZone)
            .toLocalDate()

        !recordDate.isBefore(yearStart) &&
            !recordDate.isAfter(today)
    } catch (_: Exception) {
        false
    }
}

val yearRevenue = yearRecords.sumOf { it.totalAmount }
val yearOrders = yearRecords.size
val yearQuantity = yearRecords.sumOf {
    it.items.sumOf { item -> item.quantity }
}
val yearAverageOrder =
    if (yearOrders == 0) 0.0 else yearRevenue / yearOrders
    Column(
        modifier = modifier.fillMaxSize().background(AdminBlack).padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("سجلات المبيعات 📊", color = AdminGold, fontSize = 24.sp)
                Text("بحث وتصفية وتقارير المبيعات المكتملة", color = AdminCream, fontSize = 13.sp)
            }
            OutlinedButton(onClick = { refresh() }, enabled = !loading) {
                Text(if (loading) "جاري..." else "تحديث")
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AdminPanel)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("بحث") },
                            placeholder = { Text("رقم الطلب أو العميل أو المنتج...") }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = dateFrom,
                                onValueChange = { dateFrom = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("من تاريخ") },
                                placeholder = { Text("2026-10-01") }
                            )
                            OutlinedTextField(
                                value = dateTo,
                                onValueChange = { dateTo = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("إلى تاريخ") },
                                placeholder = { Text("2026-10-31") }
                            )
                        }
                        OutlinedTextField(
                            value = productFilter,
                            onValueChange = { productFilter = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("تصفية حسب المنتج") }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = minTotal,
                                onValueChange = { minTotal = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("أقل إجمالي") }
                            )
                            OutlinedTextField(
                                value = maxTotal,
                                onValueChange = { maxTotal = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("أعلى إجمالي") }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = minQuantity,
                                onValueChange = { minQuantity = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("أقل كمية") }
                            )
                            OutlinedTextField(
                                value = maxQuantity,
                                onValueChange = { maxQuantity = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("أعلى كمية") }
                            )
                        }
                        Button(
                            onClick = {
                                search = ""; dateFrom = ""; dateTo = ""; productFilter = ""
                                minTotal = ""; maxTotal = ""; minQuantity = ""; maxQuantity = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AdminGold,
                                contentColor = AdminBlack
                            )
                        ) { Text("مسح جميع الفلاتر") }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AdminPanel)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("التقرير الحالي", color = AdminGold, fontSize = 20.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("إجمالي المبيعات: ${formatPrice(totalRevenue)}", color = AdminCream)
                        Text("عدد عمليات البيع: $totalOrders", color = AdminCream)
                        Text("إجمالي الكميات المباعة: $totalQuantity", color = AdminCream)
                        Text("متوسط قيمة الطلب: ${formatPrice(averageOrder)}", color = AdminCream)
                    }
                }
            }
                        item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AdminPanel)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "تقرير اليوم 📅",
                            color = AdminGold,
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "مبيعات اليوم: ${formatPrice(todayRevenue)}",
                            color = AdminCream
                        )
                        Text(
                            "عدد الطلبات اليوم: $todayOrders",
                            color = AdminCream
                        )
                        Text(
                            "الكميات المباعة اليوم: $todayQuantity",
                            color = AdminCream
                        )
                        Text(
                            "متوسط قيمة الطلب: ${formatPrice(todayAverageOrder)}",
                            color = AdminCream
                        )
                    }
                }
                        }
                        item {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AdminPanel)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "تقرير آخر 7 أيام 📊",
                color = AdminGold,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "مبيعات آخر 7 أيام: ${formatPrice(weekRevenue)}",
                color = AdminCream
            )
            Text(
                "عدد الطلبات: $weekOrders",
                color = AdminCream
            )
            Text(
                "الكميات المباعة: $weekQuantity",
                color = AdminCream
            )
            Text(
                "متوسط قيمة الطلب: ${formatPrice(weekAverageOrder)}",
                color = AdminCream
            )
        }
    }
                        }
                        item {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AdminPanel)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "تقرير آخر 30 يومًا 📆",
                color = AdminGold,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "مبيعات آخر 30 يومًا: ${formatPrice(monthRevenue)}",
                color = AdminCream
            )
            Text(
                "عدد الطلبات: $monthOrders",
                color = AdminCream
            )
            Text(
                "الكميات المباعة: $monthQuantity",
                color = AdminCream
            )
            Text(
                "متوسط قيمة الطلب: ${formatPrice(monthAverageOrder)}",
                color = AdminCream
            )
        }
    }
                        }
                        item {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AdminPanel)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "تقرير السنة ${today.year} 📆",
                color = AdminGold,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "مبيعات السنة: ${formatPrice(yearRevenue)}",
                color = AdminCream
            )
            Text(
                "عدد الطلبات: $yearOrders",
                color = AdminCream
            )
            Text(
                "الكميات المباعة: $yearQuantity",
                color = AdminCream
            )
            Text(
                "متوسط قيمة الطلب: ${formatPrice(yearAverageOrder)}",
                color = AdminCream
            )
        }
    }
                        }

            if (loading) item { Text("جاري تحميل سجلات المبيعات...", color = AdminCream) }
            if (error.isNotBlank()) item { Text(error, color = Color(0xFFFF7777)) }
            if (!loading && error.isBlank() && filtered.isEmpty()) {
                item { Text("لا توجد نتائج مطابقة للفلاتر الحالية.", color = AdminCream) }
            }

            items(filtered, key = { it.id }) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AdminPanel)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("طلب ${record.orderNumber}", color = AdminGold, fontSize = 18.sp)
                        Text(royalSalesDisplayDate(record.soldAt), color = AdminCream, fontSize = 12.sp)
                        Text("العميل: ${record.customerName.ifBlank { "غير محدد" }}", color = AdminCream)
                        Text("الإجمالي: ${formatPrice(record.totalAmount)}", color = AdminGold)
                        if (record.items.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            record.items.forEach { item ->
                                Text(
                                    "${item.productName} × ${item.quantity} = ${formatPrice(item.itemTotal)}",
                                    color = AdminCream,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
