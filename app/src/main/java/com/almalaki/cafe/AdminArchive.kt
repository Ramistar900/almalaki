package com.almalaki.cafe

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class AdminArchivedOrder(
    val archiveId: Long,
    val originalOrderId: Long,
    val orderNumber: String,
    val customerName: String,
    val customerPhone: String,
    val deliveryAddress: String,
    val fulfillmentType: String,
    val totalAmount: Double,
    val status: String,
    val originalCreatedAt: String,
    val archivedAt: String,
    val archiveReason: String,
    val orderItemsJson: String
)

fun loadArchivedOrders(
    accessToken: String
): List<AdminArchivedOrder> {

    val response = adminRequest(
        "GET",
        "orders_archive?select=archive_id,original_order_id,order_number,customer_name,customer_phone,delivery_address,fulfillment_type,total_amount,status,original_created_at,archived_at,archive_reason,order_items&order=archived_at.desc",
        accessToken
    )

    val json = JSONArray(response)
    val result = mutableListOf<AdminArchivedOrder>()

    for (i in 0 until json.length()) {
        val item = json.getJSONObject(i)

        result.add(
            AdminArchivedOrder(
                archiveId = item.optLong("archive_id", 0),
                originalOrderId = item.optLong("original_order_id", 0),
                orderNumber = item.optString("order_number", ""),
                customerName = item.optString("customer_name", ""),
                customerPhone = item.optString("customer_phone", ""),
                deliveryAddress = item.optString("delivery_address", ""),
                fulfillmentType = item.optString("fulfillment_type", ""),
                totalAmount = item.optDouble("total_amount", 0.0),
                status = item.optString("status", ""),
                originalCreatedAt = item.optString("original_created_at", ""),
                archivedAt = item.optString("archived_at", ""),
                archiveReason = item.optString("archive_reason", ""),
                orderItemsJson = item.optJSONArray("order_items")?.toString() ?: "[]"
            )
        )
    }

    return result
}

fun archiveReasonText(reason: String): String {
    return when (reason.lowercase()) {
        "completed" -> "مكتمل"
        "cancelled" -> "ملغى"
        "deleted" -> "محذوف"
        else -> reason.ifBlank { "أرشيف" }
    }
}

@Composable
fun ArchiveScreen(
    archivedOrders: List<AdminArchivedOrder>,
    loading: Boolean,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        item {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "الأرشيف 📦",
                    color = AdminGold,
                    fontSize = 24.sp,
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !loading
                ) {
                    Text(
                        if (loading) "جاري..."
                        else "تحديث"
                    )
                }
            }
        }

        if (archivedOrders.isEmpty()) {
            item {
                Text(
                    text = if (loading)
                        "جاري تحميل الأرشيف..."
                    else
                        "لا توجد طلبات مؤرشفة.",
                    color = AdminCream
                )
            }
        } else {
            items(
                archivedOrders,
                key = { it.archiveId }
            ) { order ->
                ArchivedOrderCard(order)
            }
        }
    }
}

@Composable
fun ArchivedOrderCard(
    order: AdminArchivedOrder
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AdminPanel
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Text(
                text = "طلب ${order.orderNumber}",
                color = AdminGold,
                fontSize = 18.sp
            )

            Text(
                text = "العميل: ${order.customerName}",
                color = AdminCream
            )

            Text(
                text = "الهاتف: ${order.customerPhone}",
                color = AdminCream
            )

            if (order.deliveryAddress.isNotBlank()) {
                Text(
                    text = "العنوان: ${order.deliveryAddress}",
                    color = AdminCream
                )
            }

            Text(
                text = "النوع: ${order.fulfillmentType}",
                color = AdminCream
            )

            Text(
                text = "المبلغ: ${formatPrice(order.totalAmount)}",
                color = AdminGold
            )

            Text(
                text = "الحالة: ${adminStatusText(order.status)}",
                color = AdminCream
            )

            Text(
                text = "سبب الأرشفة: ${archiveReasonText(order.archiveReason)}",
                color = AdminCream
            )

            Text(
                text = "تاريخ الأرشفة: ${formatArchiveDate(order.archivedAt)}",
                color = AdminCream,
                fontSize = 13.sp
            )
        }
    }
}

fun formatArchiveDate(value: String): String {
    if (value.isBlank()) return ""

    return try {
        val input = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            Locale.US
        )

        val output = SimpleDateFormat(
            "yyyy/MM/dd HH:mm",
            Locale.getDefault()
        )

        input.timeZone = TimeZone.getTimeZone("UTC")

        val date: Date = input.parse(value)
            ?: return value

        output.format(date)

    } catch (_: Exception) {
        value
    }
}
