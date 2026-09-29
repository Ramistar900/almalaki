package com.almalaki.cafe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray

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

fun archiveReasonText(reason: String, status: String): String {
    return when {
        reason.equals("completed", true) ||
                status.equals("completed", true) ->
            "مكتمل / مسلّم"

        reason.equals("cancelled", true) ||
                status.equals("cancelled", true) ->
            "ملغى"

        reason.equals("deleted", true) ->
            "محذوف"

        else ->
            "مؤرشف"
    }
}

@Composable
fun ArchiveScreen(
    archivedOrders: List<AdminArchivedOrder>
) {
    var filter by remember {
        mutableStateOf("all")
    }

    val filtered = when (filter) {
        "completed" ->
            archivedOrders.filter {
                it.archiveReason.equals("completed", true) ||
                        it.status.equals("completed", true)
            }

        "cancelled" ->
            archivedOrders.filter {
                it.archiveReason.equals("cancelled", true) ||
                        it.status.equals("cancelled", true)
            }

        "deleted" ->
            archivedOrders.filter {
                it.archiveReason.equals("deleted", true)
            }

        else ->
            archivedOrders
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        SectionTitle("📦 أرشيف الطلبات")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            ArchiveFilterButton(
                title = "الكل",
                selected = filter == "all",
                onClick = { filter = "all" },
                modifier = Modifier.weight(1f)
            )

            ArchiveFilterButton(
                title = "مكتمل",
                selected = filter == "completed",
                onClick = { filter = "completed" },
                modifier = Modifier.weight(1f)
            )

            ArchiveFilterButton(
                title = "ملغى",
                selected = filter == "cancelled",
                onClick = { filter = "cancelled" },
                modifier = Modifier.weight(1f)
            )

            ArchiveFilterButton(
                title = "محذوف",
                selected = filter == "deleted",
                onClick = { filter = "deleted" },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filtered.isEmpty()) {

            Text(
                "لا توجد طلبات في هذا القسم.",
                color = AdminCream,
                modifier = Modifier.padding(vertical = 12.dp)
            )

        } else {

            filtered.forEach { order ->

                ArchivedOrderCard(order)

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }
        }
    }
}

@Composable
fun ArchiveFilterButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor =
                if (selected) AdminGold
                else AdminPanel,
            contentColor =
                if (selected) AdminBlack
                else AdminCream
        )
    ) {
        Text(
            title,
            fontSize = 12.sp
        )
    }
}

@Composable
fun ArchivedOrderCard(
    order: AdminArchivedOrder
) {
    var expanded by remember(order.archiveId) {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AdminPanel
        )
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Text(
                "طلب ${order.orderNumber}",
                color = AdminGold,
                fontSize = 19.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "الحالة: ${
                    archiveReasonText(
                        order.archiveReason,
                        order.status
                    )
                }",
                color = AdminCream
            )

            Text(
                "العميل: ${order.customerName}",
                color = AdminCream
            )

            Text(
                "الهاتف: ${order.customerPhone}",
                color = AdminCream
            )

            if (order.deliveryAddress.isNotBlank()) {
                Text(
                    "العنوان: ${order.deliveryAddress}",
                    color = AdminCream
                )
            }

            Text(
                "النوع: ${order.fulfillmentType}",
                color = AdminCream
            )

            Text(
                "المبلغ: ${formatPrice(order.totalAmount)}",
                color = AdminGold,
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "تاريخ الطلب: ${formatArchiveDate(order.originalCreatedAt)}",
                color = AdminCream,
                fontSize = 12.sp
            )

            Text(
                "تاريخ الأرشفة: ${formatArchiveDate(order.archivedAt)}",
                color = AdminCream,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(7.dp))

            OutlinedButton(
                onClick = {
                    expanded = !expanded
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (expanded)
                        "إخفاء المنتجات"
                    else
                        "عرض المنتجات"
                )
            }

            if (expanded) {

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                val itemsJson =
                    try {
                        JSONArray(order.orderItemsJson)
                    } catch (_: Exception) {
                        JSONArray()
                    }

                if (itemsJson.length() == 0) {

                    Text(
                        "لا توجد تفاصيل محفوظة للمنتجات.",
                        color = AdminCream,
                        fontSize = 13.sp
                    )

                } else {

                    for (i in 0 until itemsJson.length()) {

                        val item =
                            itemsJson.optJSONObject(i)

                        if (item != null) {

                            val productName =
                                item.optString(
                                    "product_name",
                                    "منتج #${item.optInt("product_id", 0)}"
                                )

                            val quantity =
                                item.optInt(
                                    "quantity",
                                    0
                                )

                            val itemTotal =
                                item.optDouble(
                                    "item_total",
                                    0.0
                                )

                            Text(
                                "$productName × $quantity — ${
                                    formatPrice(itemTotal)
                                }",
                                color = AdminCream,
                                modifier = Modifier.padding(
                                    vertical = 2.dp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatArchiveDate(value: String): String {

    if (value.isBlank()) {
        return "-"
    }

    return try {

        val date = parseSupabaseDate(value)

        if (date != null) {

            val formatter =
                java.text.SimpleDateFormat(
                    "yyyy/MM/dd HH:mm",
                    java.util.Locale.getDefault()
                )

            formatter.format(date)

        } else {
            value
        }

    } catch (_: Exception) {
        value
    }
}
