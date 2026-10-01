package com.almalaki.cafe

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdminOrderCard(
    order: AdminOrder,
    accessToken: String,
    onStatus: (Long, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var expanded by remember(order.id) { mutableStateOf(false) }
    var orderItems by remember(order.id) {
        mutableStateOf<List<AdminOrderItem>>(emptyList())
    }
    var loadingItems by remember(order.id) { mutableStateOf(false) }
    var detailsError by remember(order.id) { mutableStateOf("") }
var showInvoice by remember(order.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AdminPanel)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("طلب ${order.orderNumber}", color = AdminGold, fontSize = 18.sp)
            Text("العميل: ${order.customerName}", color = AdminCream)
            Text("الهاتف: ${order.customerPhone}", color = AdminCream)

            if (order.deliveryAddress.isNotBlank()) {
                Text("العنوان: ${order.deliveryAddress}", color = AdminCream)
            }

            Text("النوع: ${order.fulfillmentType}", color = AdminCream)
            Text("المجموع: ${formatPrice(order.totalAmount)}", color = AdminGold)
            Text("الحالة: ${adminStatusText(order.status)}", color = AdminCream)

            Spacer(modifier = Modifier.height(7.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        RoyalSoundManager.playClick()
                        expanded = !expanded

                        if (expanded && orderItems.isEmpty()) {
                            loadingItems = true
                            detailsError = ""

                            Thread {
                                try {
                                    val loadedItems = loadOrderItems(
                                        accessToken = accessToken,
                                        orderId = order.id
                                    )

                                    Handler(Looper.getMainLooper()).post {
                                        orderItems = loadedItems
                                        loadingItems = false
                                    }
                                } catch (e: Exception) {
                                    Handler(Looper.getMainLooper()).post {
                                        detailsError = e.message
                                            ?: "تعذر تحميل تفاصيل الطلب."
                                        loadingItems = false
                                    }
                                }
                            }.start()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (expanded) "إخفاء" else "التفاصيل")
                }

                OutlinedButton(
                    onClick = {
                        RoyalSoundManager.playClick()
                        onStatus(order.id, "preparing")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("تحضير")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        RoyalSoundManager.playClick()
                        onStatus(order.id, "ready")
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("جاهز") }

                OutlinedButton(
                    onClick = {
                        RoyalSoundManager.playClick()
                        onStatus(order.id, "completed")
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("مكتمل") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (
                    order.status.lowercase() != "completed" &&
                    order.status.lowercase() != "cancelled"
                ) {
                    OutlinedButton(
                        onClick = {
                            RoyalSoundManager.playClick()
                            onStatus(order.id, "cancelled")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء الطلب", color = Color.Red)
                    }
                }

                if (order.status.lowercase() == "cancelled") {
                    OutlinedButton(
                        onClick = {
                            RoyalSoundManager.playClick()
                            onDelete(order.id)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("حذف الطلب", color = Color.Red)
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))

                if (loadingItems) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = AdminGold
                    )
                } else if (detailsError.isNotBlank()) {
                    Text(detailsError, color = Color.Red, fontSize = 14.sp)
                } else if (orderItems.isEmpty()) {
                    Text("لا توجد تفاصيل للطلب", color = AdminCream)
                } else {
                    Text(
                        "تفاصيل الطلب",
                        color = AdminGold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    orderItems.forEach { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = AdminBlack
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    item.productName,
                                    color = AdminCream,
                                    fontSize = 16.sp
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    "الكمية: ${item.quantity}",
                                    color = AdminCream,
                                    fontSize = 14.sp
                                )

                                Text(
                                    "سعر القطعة: ${formatPrice(item.unitPrice)}",
                                    color = AdminGold,
                                    fontSize = 14.sp
                                )

                                Text(
                                    "إجمالي المنتج: ${formatPrice(item.subtotal)}",
                                    color = AdminGold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
           
            Spacer(modifier = Modifier.height(8.dp))

OutlinedButton(
    onClick = {
        RoyalSoundManager.playClick()
        showInvoice = true
    },
    modifier = Modifier.fillMaxWidth()
) {
    Text("🧾 فتح الفاتورة")
}
                    if (showInvoice) {
                        RoyalProfessionalInvoice(
                            orderNumber = order.orderNumber,
                            customerName = order.customerName,
                            customerPhone = order.customerPhone,
                            fulfillmentType = order.fulfillmentType,
                            deliveryAddress = order.deliveryAddress,
                            items = orderItems.map {
                                RoyalInvoiceItem(
                                    name = it.productName,
                                    quantity = it.quantity,
                                    unitPrice = it.unitPrice
                                )
                            },
                            totalAmount = order.totalAmount,
                                                        onClose = {
                                showInvoice = false
                            },
                            isOwner = true
                        )
                            }
                        
                    }
        }
    }

}
