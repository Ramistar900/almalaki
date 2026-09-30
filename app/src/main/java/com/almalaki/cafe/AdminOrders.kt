package com.almalaki.cafe

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

    var loadingItems by remember(order.id) {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AdminPanel)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                "طلب ${order.orderNumber}",
                color = AdminGold,
                fontSize = 18.sp
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
                "المجموع: ${formatPrice(order.totalAmount)}",
                color = AdminGold
            )

            Text(
                "الحالة: ${adminStatusText(order.status)}",
                color = AdminCream
            )

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

                            Thread {
                                try {
                                    orderItems = loadOrderItems(
                                        accessToken,
                                        order.id
                                    )
                                } catch (_: Exception) {
                                } finally {
                                    loadingItems = false
                                }
                            }.start()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (expanded) "إخفاء"
                        else "التفاصيل"
                    )
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
                ) {
                    Text("جاهز")
                }

                OutlinedButton(
                    onClick = {
                        RoyalSoundManager.playClick()
                        onStatus(order.id, "completed")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("مكتمل")
                }
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
                        Text(
                            "إلغاء الطلب",
                            color = Color.Red
                        )
                    }
                }

                if (order.status.lowercase() == "cancelled") {
                    OutlinedButton(
                        onClick = {
                            RoyalSoundManager
