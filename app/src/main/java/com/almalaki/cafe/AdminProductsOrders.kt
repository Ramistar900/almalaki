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
fun ProductEditor(
    name: String,
    category: String,
    price: String,
    selectedImageUri: Uri?,
    editingProductId: Int?,
    loading: Boolean,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onSaveProduct: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Column {
        SectionTitle(
            if (editingProductId == null)
                "إضافة منتج"
            else
                "تعديل المنتج"
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("اسم المنتج") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = category,
            onValueChange = onCategoryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("التصنيف") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = price,
            onValueChange = onPriceChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("السعر بالليرة السورية") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onPickImage,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (selectedImageUri == null)
                    "📷 اختيار صورة المنتج"
                else
                    "✅ تم اختيار صورة المنتج"
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSaveProduct,
                enabled = !loading,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdminGold,
                    contentColor = AdminBlack
                )
            ) {
                Text(
                    if (loading) "جاري..."
                    else if (editingProductId == null) "إضافة المنتج"
                    else "حفظ التعديل"
                )
            }

            if (editingProductId != null) {
                OutlinedButton(
                    onClick = onCancelEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إلغاء")
                }
            }
        }
    }
}

@Composable
fun ProductRow(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AdminPanel)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                product.name,
                color = AdminCream,
                fontSize = 18.sp
            )
            Text(
                "${product.category} • ${formatPrice(product.price)}",
                color = AdminGold
            )
            if (product.imageUrl.isNotEmpty()) {
                Text(
                    "📷 توجد صورة للمنتج",
                    color = AdminCream,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("تعديل")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("حذف", color = Color.Red)
                }
            }
        }
    }
}

@Composable
fun AdminOrderCard(
    order: AdminOrder,
    accessToken: String,
    onStatus: (Long, String) -> Unit
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
                        onStatus(order.id, "ready")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("جاهز")
                }
                OutlinedButton(
                    onClick = {
                        onStatus(order.id, "completed")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("مكتمل")
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

