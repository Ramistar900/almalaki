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

private val AdminGold = Color(0xFFD4AF37)
private val AdminBlack = Color(0xFF050505)
private val AdminCream = Color(0xFFF5F0E5)

private const val ADMIN_SUPABASE_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val ADMIN_SUPABASE_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

data class AdminOrder(
    val id: Long,
    val orderNumber: String,
    val customerName: String,
    val customerPhone: String,
    val deliveryAddress: String,
    val fulfillmentType: String,
    val totalAmount: Double,
    val status: String
)

data class AdminOrderItem(
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val itemTotal: Double
)

@Composable
fun AdminScreen(
    accessToken: String,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    var selectedImageUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var editingProductId by remember {
        mutableStateOf<Int?>(null)
    }

    var message by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var products by remember {
        mutableStateOf<List<Product>>(emptyList())
    }

    var orders by remember {
        mutableStateOf<List<AdminOrder>>(emptyList())
    }

    var expandedOrderId by remember {
        mutableStateOf<Long?>(null)
    }

    var orderItems by remember {
        mutableStateOf<Map<Long, List<AdminOrderItem>>>(emptyMap())
    }

    var ordersLoading by remember {
        mutableStateOf(false)
    }

    val imagePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->

            selectedImageUri = uri

            message =
                if (uri != null)
                    "تم اختيار الصورة ✅"
                else
                    ""
        }

    fun refreshAll() {

        Thread {

            try {

                val loadedProducts =
                    loadProducts()

                val loadedOrders =
                    loadAdminOrders(accessToken)

                products = loadedProducts
                orders = loadedOrders

                message = "تم تحديث البيانات ✅"

            } catch (e: Exception) {

                message =
                    e.message
                        ?: "تعذر تحديث البيانات."

            }

        }.start()
    }

    LaunchedEffect(Unit) {

        refreshAll()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminBlack)
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text = "لوحة المالك 👑",
                color = AdminGold,
                fontSize = 26.sp
            )

            TextButton(
                onClick = onLogout
            ) {

                Text(
                    text = "خروج",
                    color = AdminGold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Button(
            onClick = {
                refreshAll()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AdminGold,
                contentColor = AdminBlack
            )
        ) {

            Text("تحديث الطلبات والمنتجات 🔄")
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text =
                if (editingProductId == null)
                    "إضافة منتج"
                else
                    "تعديل المنتج",
            color = AdminGold,
            fontSize = 22.sp
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("اسم المنتج")
            }
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = category,
            onValueChange = {
                category = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("التصنيف")
            }
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = price,
            onValueChange = {
                price = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("السعر بالليرة السورية")
            }
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedButton(
            onClick = {
                imagePicker.launch("image/*")
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                if (selectedImageUri == null)
                    "📷 اختيار صورة المنتج"
                else
                    "✅ تم اختيار الصورة"
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = {

                if (name.isBlank()) {

                    message =
                        "اكتب اسم المنتج."

                    return@Button
                }

                val priceValue =
                    price
                        .replace(",", ".")
                        .toDoubleOrNull()

                if (priceValue == null) {

                    message =
                        "أدخل سعرًا صحيحًا."

                    return@Button
                }

                loading = true

                message =
                    if (editingProductId == null)
                        "جاري إضافة المنتج..."
                    else
                        "جاري تعديل المنتج..."

                Thread {

                    try {

                        var imageUrl = ""

                        val uri =
                            selectedImageUri

                        if (uri != null) {

                            val bytes =
                                context
                                    .contentResolver
                                    .openInputStream(uri)
                                    ?.use {
                                        it.readBytes()
                                    }
                                    ?: throw Exception(
                                        "تعذر قراءة الصورة."
                                    )

                            val mimeType =
                                context
                                    .contentResolver
                                    .getType(uri)
                                    ?: "image/jpeg"

                            imageUrl =
                                uploadProductImage(
                                    accessToken =
                                        accessToken,
                                    bytes = bytes,
                                    mimeType = mimeType
                                )
                        }

                        val currentId =
                            editingProductId

                        if (currentId == null) {

                            addProduct(
                                accessToken =
                                    accessToken,
                                name =
                                    name.trim(),
                                category =
                                    category.trim(),
                                price =
                                    priceValue,
                                imageUrl =
                                    imageUrl
                            )

                            message =
                                "تمت إضافة المنتج بنجاح ✅"

                        } else {

                            updateAdminProduct(
                                accessToken =
                                    accessToken,
                                id =
                                    currentId,
                                name =
                                    name.trim(),
                                category =
                                    category.trim(),
                                price =
                                    priceValue,
                                imageUrl =
                                    imageUrl
                            )

                            message =
                                "تم تعديل المنتج بنجاح ✅"
                        }

                        products =
                            loadProducts()

                        name = ""
                        category = ""
                        price = ""
                        selectedImageUri = null
                        editingProductId = null

                    } catch (e: Exception) {

                        message =
                            e.message
                                ?: "حدث خطأ."

                    } finally {

                        loading = false
                    }

                }.start()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(
                containerColor = AdminGold,
                contentColor = AdminBlack
            )
        ) {

            Text(
                if (loading)
                    "جاري التنفيذ..."
                else if (editingProductId == null)
                    "إضافة المنتج"
                else
                    "حفظ التعديل"
            )
        }

        if (editingProductId != null) {

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            TextButton(
                onClick = {

                    editingProductId = null
                    name = ""
                    category = ""
                    price = ""
                    selectedImageUri = null
                    message = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    "إلغاء التعديل",
                    color = AdminCream
                )
            }
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        if (message.isNotEmpty()) {

            Text(
                text = message,
                color = AdminCream
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "الطلبات الواردة 📦",
            color = AdminGold,
            fontSize = 22.sp
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        if (orders.isEmpty()) {

            Text(
                text = "لا توجد طلبات حاليًا.",
                color = AdminCream
            )

        } else {

            orders.forEach { order ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 5.dp
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            Color(0xFF151515)
                    )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(12.dp)
                    ) {

                        Text(
                            text =
                                "طلب ${order.orderNumber}",
                            color =
                                AdminGold,
                            fontSize =
                                18.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                "العميل: ${order.customerName}",
                            color =
                                AdminCream
                        )

                        Text(
                            text =
                                "الهاتف: ${order.customerPhone}",
                            color =
                                AdminCream
                        )

                        Text(
                            text =
                                "النوع: ${order.fulfillmentType}",
                            color =
                                AdminCream
                        )

                        if (
                            order.deliveryAddress
                                .isNotBlank()
                        ) {

                            Text(
                                text =
                                    "العنوان: ${order.deliveryAddress}",
                                color =
                                    AdminCream
                            )
                        }

                        Text(
                            text =
                                "المجموع: ${
                                    formatPrice(
                                        order.totalAmount
                                    )
                                }",
                            color =
                                AdminGold
                        )

                        Text(
                            text =
                                "الحالة: ${
                                    adminStatusText(
                                        order.status
                                    )
                                }",
                            color =
                                AdminCream
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(6.dp)
                        ) {

                            Button(
                                onClick = {

                                    updateOrderStatusAsync(
                                        accessToken =
                                            accessToken,
                                        orderId =
                                            order.id,
                                        status =
                                            "preparing"
                                    ) {

                                        orders =
                                            orders.map {

                                                if (
                                                    it.id ==
                                                    order.id
                                                ) {
                                                    it.copy(
                                                        status =
                                                            "preparing"
                                                    )
                                                } else {
                                                    it
                                                }
                                            }

                                        message =
                                            "تم تحويل الطلب إلى قيد التحضير ✅"
                                    }
                                },
                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                Text(
                                    "تحضير",
                                    fontSize =
                                        11.sp
                                )
                            }

                            Button(
                                onClick = {

                                    updateOrderStatusAsync(
                                        accessToken =
                                            accessToken,
                                        orderId =
                                            order.id,
                                        status =
                                            "ready"
                                    ) {

                                        orders =
                                            orders.map {

                                                if (
                                                    it.id ==
                                                    order.id
                                                ) {
                                                    it.copy(
                                                        status =
                                                            "ready"
                                                    )
                                                } else {
                                                    it
                                                }
                                            }

                                        message =
                                            "الطلب أصبح جاهزًا ✅"
                                    }
                                },
                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                Text(
                                    "جاهز",
                                    fontSize =
                                        11.sp
                                )
                            }

                            Button(
                                onClick = {

                                    updateOrderStatusAsync(
                                        accessToken =
                                            accessToken,
                                        orderId =
                                            order.id,
                                        status =
                                            "completed"
                                    ) {

                                        orders =
                                            orders.map {

                                                if (
                                                    it.id ==
                                                    order.id
                                                ) {
                                                    it.copy(
                                                        status =
                                                            "completed"
                                                    )
                                                } else {
                                                    it
                                                }
                                            }

                                        message =
                                            "تم إكمال الطلب ✅"
                                    }
                                },
                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                Text(
                                    "تم",
                                    fontSize =
                                        11.sp
                                )
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )

                        OutlinedButton(
                            onClick = {

                                if (
                                    expandedOrderId ==
                                    order.id
                                ) {

                                    expandedOrderId =
                                        null

                                } else {

                                    expandedOrderId =
                                        order.id

                                    if (
                                        !orderItems.containsKey(
                                            order.id
                                        )
                                    ) {

                                        Thread {

                                            try {

                                                val items =
                                                    loadOrderItems(
                                                        accessToken =
                                                            accessToken,
                                                        orderId =
                                                            order.id
                                                    )

                                                orderItems =
                                                    orderItems +
                                                            (
                                                                order.id to
                                                                        items
                                                                )

                                            } catch (
                                                e: Exception
                                            ) {

                                                message =
                                                    e.message
                                                        ?: "تعذر تحميل تفاصيل الطلب."
                                            }

                                        }.start()
                                    }
                                }
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Text(
                                if (
                                    expandedOrderId ==
                                    order.id
                                )
                                    "إخفاء المنتجات"
                                else
                                    "عرض المنتجات"
                            )
                        }

                        if (
                            expandedOrderId ==
                            order.id
                        ) {

                            val itemsForOrder =
                                orderItems[
                                    order.id
                                ]

                            if (
                                itemsForOrder == null
                            ) {

                                Text(
                                    text =
                                        "جاري تحميل المنتجات...",
                                    color =
                                        AdminCream
                                )

                            } else {

                                itemsForOrder.forEach {
                                    item ->

                                    Text(
                                        text =
                                            "${item.productName} × ${item.quantity} — ${
                                                formatPrice(
                                                    item.itemTotal
                                                )
                                            }",
                                        color =
                                            AdminCream,
                                        modifier =
                                            Modifier.padding(
                                                vertical = 3.dp
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "المنتجات الحالية",
            color = AdminGold,
            fontSize = 22.sp
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        LazyColumn {

            items(products) { product ->

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 8.dp
                            )
                ) {

                    Text(
                        text = product.name,
                        color = AdminCream,
                        fontSize = 18.sp
                    )

                    Text(
                        text =
                            "${product.category} • ${
                                formatPrice(
                                    product.price
                                )
                            }",
                        color = AdminGold
                    )

                    if (
                        product.imageUrl.isNotEmpty()
                    ) {

                        Text(
                            text =
                                "📷 توجد صورة للمنتج",
                            color =
                                AdminCream,
                            fontSize =
                                13.sp
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        OutlinedButton(
                            onClick = {

                                editingProductId =
                                    product.id

                                name =
                                    product.name

                                category =
                                    product.category

                                price =
                                    product.price
                                        .toString()

                                selectedImageUri =
                                    null

                                message =
                                    "يمكنك تعديل بيانات المنتج الآن."
                            },
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text("تعديل")
                        }

                        OutlinedButton(
                            onClick = {

                                Thread {

                                    try {

                                        deleteProduct(
                                            accessToken =
                                                accessToken,
                                            id =
                                                product.id
                                        )

                                        products =
                                            loadProducts()

                                        message =
                                            "تم حذف المنتج بنجاح ✅"

                                    } catch (
                                        e: Exception
                                    ) {

                                        message =
                                            e.message
                                                ?: "تعذر حذف المنتج."
                                    }

                                }.start()
                            },
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(
                                "حذف",
                                color =
                                    Color.Red
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier =
                            Modifier.padding(
                                top = 8.dp
                            )
                    )
                }
            }
        }
    }
}

private fun loadAdminOrders(
    accessToken: String
): List<AdminOrder> {

    val url = URL(
        "$ADMIN_SUPABASE_URL/rest/v1/orders" +
                "?select=id,order_number,customer_name," +
                "customer_phone,delivery_address," +
                "fulfillment_type,total_amount,status" +
                "&order=id.desc"
    )

    val connection =
        url.openConnection() as HttpURLConnection

    try {

        connection.requestMethod = "GET"

        connection.setRequestProperty(
            "apikey",
            ADMIN_SUPABASE_KEY
        )

        connection.setRequestProperty(
            "Authorization",
            "Bearer $accessToken"
        )

        val code =
            connection.responseCode

        if (code !in 200..299) {

            val error =
                connection.errorStream
                    ?.bufferedReader()
                    ?.readText()
                    ?: "تعذر تحميل الطلبات."

            throw Exception(
                "HTTP $code: $error"
            )
        }

        val response =
            connection.inputStream
                .bufferedReader()
                .readText()

        val json =
            JSONArray(response)

        val result =
            mutableListOf<AdminOrder>()

        for (i in 0 until json.length()) {

            val item =
                json.getJSONObject(i)

            result.add(
                AdminOrder(
                    id =
                        item.getLong("
