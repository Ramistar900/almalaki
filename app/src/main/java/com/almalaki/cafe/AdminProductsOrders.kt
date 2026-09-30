package com.almalaki.cafe

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AdminPanel)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "طلب ${order.orderNumber}",
                color = AdminGold,
                fontSize = 18.sp
            )
            Text("العميل: ${order.customerName}", color = AdminCream)
            Text("الهاتف: ${order.customerPhone}", color = AdminCream)
            if (order.deliveryAddress.isNotBlank()) {
                Text("العنوان: ${order.deliveryAddress}", color = AdminCream)
            }
            Text("النوع: ${order.fulfillmentType}", color = AdminCream)
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
                if (order.status.lowercase() != "completed" &&
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
                Spacer(modifier = Modifier.height(8.dp))
                if (loadingItems) {
                    Text("جاري تحميل التفاصيل...", color = AdminCream)
                } else if (orderItems.isEmpty()) {
                    Text("لا توجد تفاصيل للطلب.", color = AdminCream)
                } else {
                    orderItems.forEach { item ->
                        Text(
                            "المنتج #${item.productId} × ${item.quantity} — " +
                                    formatPrice(item.subtotal),
                            color = AdminCream,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopProductRow(product: AdminTopProduct) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AdminPanel)
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${product.quantity}×",
                color = AdminGold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, color = AdminCream, fontSize = 17.sp)
                Text(
                    "مبيعات: ${formatPrice(product.revenue)}",
                    color = AdminGold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

