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

private const val ADMIN_SUPABASE_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val ADMIN_SUPABASE_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

private val AdminGold = Color(0xFFD4AF37)
private val AdminBlack = Color(0xFF050505)
private val AdminCream = Color(0xFFF5F0E5)
private val AdminPanel = Color(0xFF111111)

private enum class AdminSection {
    HOME,
    PRODUCTS,
    ORDERS,
    SALES,
    TOP_PRODUCTS
}

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
    val id: Long,
    val orderId: Long,
    val productId: Int,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double
)

data class AdminSalesStats(
    val today: Double = 0.0,
    val week: Double = 0.0,
    val month: Double = 0.0,
    val todayOrders: Int = 0,
    val weekOrders: Int = 0,
    val monthOrders: Int = 0
)

data class AdminTopProduct(
    val productId: Int,
    val name: String,
    val quantity: Int,
    val revenue: Double
)

@Composable
fun AdminScreen(
    accessToken: String,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var section by remember { mutableStateOf(AdminSection.HOME) }
    var menuOpen by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var editingProductId by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var orders by remember { mutableStateOf<List<AdminOrder>>(emptyList()) }
    var salesStats by remember { mutableStateOf(AdminSalesStats()) }
    var topProducts by remember { mutableStateOf<List<AdminTopProduct>>(emptyList()) }
    var loadingDashboard by remember { mutableStateOf(false) }

    val imagePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            selectedImageUri = uri
            message = if (uri != null) "تم اختيار الصورة ✅" else ""
        }

    fun refreshProducts() {
        Thread {
            try {
                products = loadProducts()
            } catch (e: Exception) {
                message = e.message ?: "تعذر تحميل المنتجات."
            }
        }.start()
    }

    fun refreshOrders() {
        Thread {
            try {
                orders = loadAdminOrders(accessToken)
            } catch (e: Exception) {
                message = e.message ?: "تعذر تحميل الطلبات."
            }
        }.start()
    }

    fun refreshDashboard() {
        loadingDashboard = true
        Thread {
            try {
                val currentProducts = loadProducts()
                val currentOrders = loadAdminOrders(accessToken)
                val completedOrders =
                    currentOrders.filter {
                        it.status.equals("completed", ignoreCase = true)
                    }

                products = currentProducts
                orders = currentOrders
                salesStats = calculateSalesStats(accessToken)
                topProducts = calculateTopProducts(
                    accessToken = accessToken,
                    completedOrders = completedOrders,
                    products = currentProducts
                )
            } catch (e: Exception) {
                message = e.message ?: "تعذر تحديث لوحة التحكم."
            } finally {
                loadingDashboard = false
            }
        }.start()
    }

    LaunchedEffect(Unit) {
        refreshDashboard()
    }

    fun openSection(target: AdminSection) {
        section = target
        menuOpen = false

        when (target) {
            AdminSection.HOME,
            AdminSection.SALES,
            AdminSection.TOP_PRODUCTS -> refreshDashboard()
            AdminSection.PRODUCTS -> refreshProducts()
            AdminSection.ORDERS -> refreshOrders()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminBlack)
    ) {
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                AdminSidebar(
                    section = section,
                    onSectionSelected = ::openSection,
                    onLogout = onLogout,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(220.dp)
                )

                VerticalDivider(color = AdminGold.copy(alpha = 0.35f))

                AdminContent(
                    section = section,
                    products = products,
                    orders = orders,
                    salesStats = salesStats,
                    topProducts = topProducts,
                    loadingDashboard = loadingDashboard,
                    message = message,
                    name = name,
                    category = category,
                    price = price,
                    selectedImageUri = selectedImageUri,
                    editingProductId = editingProductId,
                    loading = loading,
                    accessToken = accessToken,
                    context = context,
                    onNameChange = { name = it },
                    onCategoryChange = { category = it },
                    onPriceChange = { price = it },
                    onPickImage = { imagePicker.launch("image/*") },
                    onEditProduct = {
                        editingProductId = it.id
                        name = it.name
                        category = it.category
                        price = it.price.toString()
                        selectedImageUri = null
                        message = "يمكنك تعديل بيانات المنتج الآن."
                        section = AdminSection.PRODUCTS
                    },
                    onDeleteProduct = { product ->
                        Thread {
                            try {
                                deleteProduct(
                                    accessToken = accessToken,
                                    id = product.id
                                )
                                products = loadProducts()
                                message = "تم حذف المنتج بنجاح ✅"
                            } catch (e: Exception) {
                                message = e.message ?: "تعذر حذف المنتج."
                            }
                        }.start()
                    },
                    onSaveProduct = {
                        saveProduct(
                            context = context,
                            accessToken = accessToken,
                            name = name,
                            category = category,
                            price = price,
                            selectedImageUri = selectedImageUri,
                            editingProductId = editingProductId,
                            onLoading = { loading = it },
                            onMessage = { message = it },
                            onProductsLoaded = { products = it },
                            onClear = {
                                name = ""
                                category = ""
                                price = ""
                                selectedImageUri = null
                                editingProductId = null
                            }
                        )
                    },
                    onCancelEdit = {
                        editingProductId = null
                        name = ""
                        category = ""
                        price = ""
                        selectedImageUri = null
                        message = ""
                    },
                    onRefresh = { refreshDashboard() },
                    onOrderStatus = { orderId, status ->
                        Thread {
                            try {
                                updateOrderStatusAsync(
                                    accessToken = accessToken,
                                    orderId = orderId,
                                    status = status
                                )
                                orders = loadAdminOrders(accessToken)
                                salesStats = calculateSalesStats(accessToken)
                                message = "تم تحديث حالة الطلب ✅"
                            } catch (e: Exception) {
                                message = e.message ?: "تعذر تحديث حالة الطلب."
                            }
                        }.start()
                    }
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                AdminTopBar(
                    title = adminSectionTitle(section),
                    menuOpen = menuOpen,
                    onMenuClick = { menuOpen = !menuOpen },
                    onLogout = onLogout
                )

                if (menuOpen) {
                    AdminHorizontalMenu(
                        section = section,
                        onSectionSelected = ::openSection
                    )
                }

                AdminContent(
                    section = section,
                    products = products,
                    orders = orders,
                    salesStats = salesStats,
                    topProducts = topProducts,
                    loadingDashboard = loadingDashboard,
                    message = message,
                    name = name,
                    category = category,
                    price = price,
                    selectedImageUri = selectedImageUri,
                    editingProductId = editingProductId,
                    loading = loading,
                    accessToken = accessToken,
                    context = context,
                    onNameChange = { name = it },
                    onCategoryChange = { category = it },
                    onPriceChange = { price = it },
                    onPickImage = { imagePicker.launch("image/*") },
                    onEditProduct = {
                        editingProductId = it.id
                        name = it.name
                        category = it.category
                        price = it.price.toString()
                        selectedImageUri = null
                        message = "يمكنك تعديل بيانات المنتج الآن."
                        section = AdminSection.PRODUCTS
                    },
                    onDeleteProduct = { product ->
                        Thread {
                            try {
                                deleteProduct(accessToken, product.id)
                                products = loadProducts()
                                message = "تم حذف المنتج بنجاح ✅"
                            } catch (e: Exception) {
                                message = e.message ?: "تعذر حذف المنتج."
                            }
                        }.start()
                    },
                    onSaveProduct = {
                        saveProduct(
                            context = context,
                            accessToken = accessToken,
                            name = name,
                            category = category,
                            price = price,
                            selectedImageUri = selectedImageUri,
                            editingProductId = editingProductId,
                            onLoading = { loading = it },
                            onMessage = { message = it },
                            onProductsLoaded = { products = it },
                            onClear = {
                                name = ""
                                category = ""
                                price = ""
                                selectedImageUri = null
                                editingProductId = null
                            }
                        )
                    },
                    onCancelEdit = {
                        editingProductId = null
                        name = ""
                        category = ""
                        price = ""
                        selectedImageUri = null
                        message = ""
                    },
                    onRefresh = { refreshDashboard() },
                    onOrderStatus = { orderId, status ->
                        Thread {
                            try {
                                updateOrderStatusAsync(accessToken, orderId, status)
                                orders = loadAdminOrders(accessToken)
                                salesStats = calculateSalesStats(accessToken)
                                message = "تم تحديث حالة الطلب ✅"
                            } catch (e: Exception) {
                                message = e.message ?: "تعذر تحديث حالة الطلب."
                            }
                        }.start()
                    }
                )
            }
        }
    }
}

@Composable
private fun AdminSidebar(
    section: AdminSection,
    onSectionSelected: (AdminSection) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(AdminPanel)
            .padding(12.dp)
    ) {
        Text(
            text = "الملكي 👑",
            color = AdminGold,
            fontSize = 25.sp,
            modifier = Modifier.padding(8.dp)
        )

        Text(
            text = "لوحة المالك",
            color = AdminCream,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        AdminMenuItem("⌂", "الرئيسية", section == AdminSection.HOME) {
            onSectionSelected(AdminSection.HOME)
        }
        AdminMenuItem("▣", "تعديل المنتجات", section == AdminSection.PRODUCTS) {
            onSectionSelected(AdminSection.PRODUCTS)
        }
        AdminMenuItem("▤", "الطلبات", section == AdminSection.ORDERS) {
            onSectionSelected(AdminSection.ORDERS)
        }
        AdminMenuItem("◈", "المبيعات", section == AdminSection.SALES) {
            onSectionSelected(AdminSection.SALES)
        }
        AdminMenuItem("★", "الأكثر طلبًا", section == AdminSection.TOP_PRODUCTS) {
            onSectionSelected(AdminSection.TOP_PRODUCTS)
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تسجيل الخروج")
        }
    }
}

@Composable
private fun AdminMenuItem(
    icon: String,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected) AdminGold else Color.Transparent
    val textColor = if (selected) AdminBlack else AdminCream

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = textColor
        )
    ) {
        Text("$icon  $title")
    }
}

@Composable
private fun AdminTopBar(
    title: String,
    menuOpen: Boolean,
    onMenuClick: () -> Unit,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AdminPanel)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onMenuClick) {
            Text(
                text = if (menuOpen) "×" else "☰",
                color = AdminGold,
                fontSize = 27.sp
            )
        }

        Text(
            text = title,
            color = AdminGold,
            fontSize = 21.sp,
            modifier = Modifier.weight(1f)
        )

        TextButton(onClick = onLogout) {
            Text("خروج", color = AdminGold)
        }
    }
}

@Composable
private fun AdminHorizontalMenu(
    section: AdminSection,
    onSectionSelected: (AdminSection) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(AdminPanel)
            .heightIn(max = 280.dp)
            .padding(horizontal = 8.dp)
    ) {
        item {
            AdminMenuItem("⌂", "الرئيسية", section == AdminSection.HOME) {
                onSectionSelected(AdminSection.HOME)
            }
        }
        item {
            AdminMenuItem("▣", "تعديل المنتجات", section == AdminSection.PRODUCTS) {
                onSectionSelected(AdminSection.PRODUCTS)
            }
        }
        item {
            AdminMenuItem("▤", "الطلبات", section == AdminSection.ORDERS) {
                onSectionSelected(AdminSection.ORDERS)
            }
        }
        item {
            AdminMenuItem("◈", "المبيعات", section == AdminSection.SALES) {
                onSectionSelected(AdminSection.SALES)
            }
        }
        item {
            AdminMenuItem("★", "الأكثر طلبًا", section == AdminSection.TOP_PRODUCTS) {
                onSectionSelected(AdminSection.TOP_PRODUCTS)
            }
        }
    }
}

@Composable
private fun AdminContent(
    section: AdminSection,
    products: List<Product>,
    orders: List<AdminOrder>,
    salesStats: AdminSalesStats,
    topProducts: List<AdminTopProduct>,
    loadingDashboard: Boolean,
    message: String,
    name: String,
    category: String,
    price: String,
    selectedImageUri: Uri?,
    editingProductId: Int?,
    loading: Boolean,
    accessToken: String,
    context: android.content.Context,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onSaveProduct: () -> Unit,
    onCancelEdit: () -> Unit,
    onRefresh: () -> Unit,
    onOrderStatus: (Long, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (section) {
            AdminSection.HOME -> {
                item {
                    DashboardHeader(
                        
