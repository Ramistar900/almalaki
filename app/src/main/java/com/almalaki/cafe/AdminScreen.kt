package com.almalaki.cafe

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp


data class AdminOrder(
    val id: Long,
    val orderNumber: String,
    val customerName: String,
    val customerPhone: String,
    val deliveryAddress: String,
    val fulfillmentType: String,
    val totalAmount: Double,
    val status: String,
    val createdAt: String = ""
)

data class AdminOrderItem(val id: Long, val orderId: Long, val productId: Int, val quantity: Int, val unitPrice: Double, val subtotal: Double)

data class AdminSalesStats(val today: Double = 0.0, val week: Double = 0.0, val month: Double = 0.0, val todayOrders: Int = 0, val weekOrders: Int = 0, val monthOrders: Int = 0)

data class AdminTopProduct(val productId: Int, val name: String, val quantity: Int, val revenue: Double)

@Composable
fun AdminScreen(accessToken: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
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
    var dashboardLoading by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
        message = if (uri != null) "تم اختيار الصورة ✅" else ""
    }

    fun refreshProducts() = Thread { try { products = loadProducts() } catch (e: Exception) { message = e.message ?: "تعذر تحميل المنتجات." } }.start()
    fun refreshOrders() = Thread { try { orders = loadAdminOrders(accessToken) } catch (e: Exception) { message = e.message ?: "تعذر تحميل الطلبات." } }.start()
    fun refreshDashboard() {
        dashboardLoading = true
        Thread {
            try {
                val p = loadProducts(); val o = loadAdminOrders(accessToken)
                products = p; orders = o
                salesStats = calculateSalesStats(accessToken)
                topProducts = calculateTopProducts(accessToken, o.filter { it.status.equals("completed", true) }, p)
            } catch (e: Exception) { message = e.message ?: "تعذر تحديث لوحة التحكم." }
            finally { dashboardLoading = false }
        }.start()
    }
    fun clearForm() { name = ""; category = ""; price = ""; selectedImageUri = null; editingProductId = null }
    fun openSection(target: AdminSection) {
        section = target; menuOpen = false
        when (target) {
            AdminSection.PRODUCTS -> refreshProducts()
            AdminSection.ORDERS -> refreshOrders()
            else -> refreshDashboard()
        }
    }
    fun save() = saveProduct(context, accessToken, name, category, price, selectedImageUri, editingProductId,
        { loading = it }, { message = it }, { products = it }, { clearForm() })

    LaunchedEffect(Unit) { refreshDashboard() }

    fun content() {
        AdminContent(
            section = section, products = products, orders = orders, salesStats = salesStats, topProducts = topProducts,
            loadingDashboard = dashboardLoading, message = message, name = name, category = category, price = price,
            selectedImageUri = selectedImageUri, editingProductId = editingProductId, loading = loading,
            accessToken = accessToken, context = context,
            onNameChange = { name = it }, onCategoryChange = { category = it }, onPriceChange = { price = it },
            onPickImage = { picker.launch("image/*") },
            onEditProduct = { p -> editingProductId = p.id; name = p.name; category = p.category; price = p.price.toString(); selectedImageUri = null; section = AdminSection.PRODUCTS },
            onDeleteProduct = { p -> Thread { try { deleteProduct(accessToken, p.id); products = loadProducts(); message = "تم حذف المنتج بنجاح ✅" } catch (e: Exception) { message = e.message ?: "تعذر حذف المنتج." } }.start() },
            onSaveProduct = { save() }, onCancelEdit = { clearForm() }, onRefresh = { refreshDashboard() },
            onOrderStatus = { id, status -> Thread { try { updateOrderStatusAsync(accessToken, id, status); orders = loadAdminOrders(accessToken); salesStats = calculateSalesStats(accessToken); message = "تم تحديث حالة الطلب ✅" } catch (e: Exception) { message = e.message ?: "تعذر تحديث حالة الطلب." } }.start() },
            onDeleteOrder = { id -> Thread { try { deleteCancelledOrder(accessToken, id); orders = loadAdminOrders(accessToken); message = "تم حذف الطلب الملغى ✅" } catch (e: Exception) { message = e.message ?: "تعذر حذف الطلب." } }.start() }
        )
    }

    Column(Modifier.fillMaxSize().background(AdminBlack)) {
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                AdminSidebar(section, ::openSection, onLogout, Modifier.fillMaxHeight().width(220.dp))
                VerticalDivider()
                content()
            }
        } else {
            AdminTopBar(adminSectionTitle(section), menuOpen, { menuOpen = !menuOpen }, onLogout)
            if (menuOpen) AdminHorizontalMenu(section, ::openSection)
            Box(Modifier.fillMaxSize()) { content() }
        }
    }
}
