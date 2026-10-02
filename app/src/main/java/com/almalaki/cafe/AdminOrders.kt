package com.almalaki.cafe

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AdminOrderCard(
    order: AdminOrder,
    accessToken: String,
    onStatus: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    openDetails: Boolean = false
) {
    var expanded by remember(order.id) { mutableStateOf(false) }

LaunchedEffect(openDetails) {
    if (openDetails) {
        expanded = true
    }
}

var orderItems by remember(order.id) {
    mutableStateOf<List<AdminOrderItem>>(emptyList())
}

var loadingItems by remember(order.id) { mutableStateOf(false) }
    var detailsError by remember(order.id) { mutableStateOf("") }
var showInvoice by remember(order.id) { mutableStateOf(false) }
    Card(
    modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
        containerColor = Color(0xFF111111)
    ),
    border = BorderStroke(
        width = 1.dp,
        color = Color(0xFFD4AF37).copy(alpha = 0.35f)
    )
) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {
    RoyalCustomerStarIcon()

    Spacer(modifier = Modifier.width(10.dp))

    Text(
        text = order.customerName,
        color = AdminGold,
        fontSize = 18.sp
    )
            }
                        if (expanded) {
                Text("الهاتف: ${order.customerPhone}", color = AdminCream)

                if (order.deliveryAddress.isNotBlank()) {
                    Text("العنوان: ${order.deliveryAddress}", color = AdminCream)
                }

                Text("النوع: ${order.fulfillmentType}", color = AdminCream)
                Text("المجموع: ${formatPrice(order.totalAmount)}", color = AdminGold)
                Text("الحالة: ${adminStatusText(order.status)}", color = AdminCream)
            }

            Spacer(modifier = Modifier.height(7.dp))
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
        Text(if (expanded) "إغلاق" else "فتح")
    }

    if (order.status.lowercase() == "cancelled") {
        OutlinedButton(
            onClick = {
                RoyalSoundManager.playClick()
                onDelete(order.id)
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(
                "حذف",
                color = Color.Red
            )
        }
    }
}

            if (expanded) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
@Composable
fun RoyalCustomerStarIcon(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "royal_star_shine")

    val shineProgress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2400,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shine_progress"
    )

    Canvas(
        modifier = modifier.size(52.dp)
    ) {
        val center = Offset(
            size.width / 2f,
            size.height / 2f
        )

        val outerRadius = size.minDimension * 0.44f
        val innerRadius = outerRadius * 0.45f

        val starPath = Path()

        for (i in 0 until 10) {
            val angle = Math.toRadians(
                -90.0 + i * 36.0
            )

            val radius =
                if (i % 2 == 0) outerRadius
                else innerRadius

            val point = Offset(
                x = center.x + cos(angle).toFloat() * radius,
                y = center.y + sin(angle).toFloat() * radius
            )

            if (i == 0) {
                starPath.moveTo(point.x, point.y)
            } else {
                starPath.lineTo(point.x, point.y)
            }
        }

        starPath.close()

        // إطار النجمة الأسود المفرغ
        drawPath(
            path = starPath,
            color = Color(0xFF050505),
            style = Stroke(
                width = 5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // اللمعان الأبيض المتحرك حول النجمة
        val shineX = size.width * shineProgress

        drawPath(
            path = starPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    Color.White.copy(alpha = 0.95f),
                    Color.White,
                    Color.Transparent,
                    Color.Transparent
                ),
                start = Offset(
                    shineX - size.width * 0.35f,
                    0f
                ),
                end = Offset(
                    shineX + size.width * 0.35f,
                    size.height
                )
            ),
            style = Stroke(
                width = 3.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // رأس الشخص بالذهبي
        val headRadius = size.minDimension * 0.09f

        val headCenter = Offset(
            center.x,
            center.y - size.minDimension * 0.10f
        )

        drawCircle(
            color = Color(0xFFD4AF37),
            radius = headRadius,
            center = headCenter
        )

        // جسم الشخص بالذهبي
        val bodyPath = Path().apply {
            moveTo(
                center.x - size.minDimension * 0.17f,
                center.y + size.minDimension * 0.18f
            )

            quadraticBezierTo(
                center.x - size.minDimension * 0.14f,
                center.y - size.minDimension * 0.01f,
                center.x,
                center.y - size.minDimension * 0.01f
            )

            quadraticBezierTo(
                center.x + size.minDimension * 0.14f,
                center.y - size.minDimension * 0.01f,
                center.x + size.minDimension * 0.17f,
                center.y + size.minDimension * 0.18f
            )

            close()
        }

        drawPath(
            path = bodyPath,
            color = Color(0xFFD4AF37)
        )
    }
}
