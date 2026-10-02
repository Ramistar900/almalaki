package com.almalaki.cafe

import android.content.res.Configuration
import android.net.Uri
import android.os.Handler
import android.os.Looper
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

data class AdminOrderItem(
val id: Long,
val orderId: Long,
val productId: Int,
val productName: String,
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
val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

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
var archivedOrders by remember { mutableStateOf<List<AdminArchivedOrder>>(emptyList()) }
var salesStats by remember { mutableStateOf(AdminSalesStats()) }
var topProducts by remember { mutableStateOf<List<AdminTopProduct>>(emptyList()) }
var loadingDashboard by remember { mutableStateOf(false) }
var loadingArchive by remember { mutableStateOf(false) }
var accountEmail by remember { mutableStateOf("") }
var loadingAccount by remember { mutableStateOf(false) }
var orderAlertVisible by remember { mutableStateOf(false) }
var orderAlertInitialized by remember { mutableStateOf(false) }
var lastKnownOrderIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
LaunchedEffect(message) {
    if (message.isNotEmpty()) {
        kotlinx.coroutines.delay(4000)
        message = ""
    }
}

val imagePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
) { uri ->
    selectedImageUri = uri
    message = if (uri != null) "تم اختيار الصورة ✅" else ""
}

fun refreshProducts() {
    Thread {
        try { products = loadProducts() }
        catch (e: Exception) { message = e.message ?: "تعذر تحميل المنتجات." }
    }.start()
}

fun refreshOrders() {
    Thread {
        try { orders = loadAdminOrders(accessToken) }
        catch (e: Exception) { message = e.message ?: "تعذر تحميل الطلبات." }
    }.start()
}

fun refreshArchive() {
    loadingArchive = true
    Thread {
        try { archivedOrders = loadArchivedOrders(accessToken) }
        catch (e: Exception) { message = e.message ?: "تعذر تحميل الأرشيف." }
        finally { loadingArchive = false }
    }.start()
}

fun refreshAccount() {
    loadingAccount = true
    Thread {
        try {
            val email = loadCurrentAccountEmail(accessToken)
            Handler(Looper.getMainLooper()).post { accountEmail = email }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post { message = e.message ?: "تعذر تحميل بيانات الحساب." }
        } finally {
            Handler(Looper.getMainLooper()).post { loadingAccount = false }
        }
    }.start()
}

fun changeEmail(newEmail: String) {
    loadingAccount = true
    Thread {
        try {
            updateAccountEmail(accessToken, newEmail)
            Handler(Looper.getMainLooper()).post {
                accountEmail = newEmail.trim()
                message = "تم تغيير البريد الإلكتروني بنجاح ✓"
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post { message = e.message ?: "تعذر تغيير البريد الإلكتروني." }
        } finally {
            Handler(Looper.getMainLooper()).post { loadingAccount = false }
        }
    }.start()
}

fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
    loadingAccount = true
    Thread {
        try {
            updateAccountPassword(accessToken, currentPassword, newPassword, confirmPassword)
            Handler(Looper.getMainLooper()).post {
                AppSounds.passwordChanged(context)
                message = "تم تغيير كلمة المرور بنجاح ✓"
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post { message = e.message ?: "تعذر تغيير كلمة المرور." }
        } finally {
            Handler(Looper.getMainLooper()).post { loadingAccount = false }
        }
    }.start()
}

fun refreshDashboard() {
    loadingDashboard = true
    Thread {
        try {
            val currentProducts = loadProducts()
            val currentOrders = loadAdminOrders(accessToken)
            val completedOrders = currentOrders.filter { it.status.equals("completed", ignoreCase = true) }
            products = currentProducts
            orders = currentOrders
            salesStats = calculateSalesStats(accessToken)
            topProducts = calculateTopProducts(accessToken, completedOrders, currentProducts)
        } catch (e: Exception) {
            message = e.message ?: "تعذر تحديث لوحة التحكم."
        } finally { loadingDashboard = false }
    }.start()
}
LaunchedEffect(Unit) {
    refreshDashboard()
    refreshArchive()
    refreshAccount()
}

LaunchedEffect(accessToken) {
    while (true) {
        try {
            val currentOrders = loadAdminOrders(accessToken)
            val currentIds = currentOrders.map { it.id }.toSet()

            if (!orderAlertInitialized) {
                lastKnownOrderIds = currentIds
                orderAlertInitialized = true
            } else {
                val newOrders = currentOrders.filter { order ->
                    order.id !in lastKnownOrderIds &&
                        order.status.equals("new", ignoreCase = true)
                }

                if (newOrders.isNotEmpty()) {
                    orderAlertVisible = true
                    startRoyalOrderAlert(context)
                }

                lastKnownOrderIds = currentIds
            }

            orders = currentOrders
        } catch (_: Exception) {
        }

        kotlinx.coroutines.delay(5000L)
    }
}

fun openSection(target: AdminSection) {
    section = target
    menuOpen = false
    when (target) {
        AdminSection.HOME, AdminSection.SALES, AdminSection.TOP_PRODUCTS -> refreshDashboard()
        AdminSection.PRODUCTS -> refreshProducts()
        AdminSection.ORDERS -> refreshOrders()
        AdminSection.ARCHIVE -> refreshArchive()
        AdminSection.ACCOUNT_SETTINGS -> refreshAccount()
    }
}

Column(modifier = Modifier.fillMaxSize().background(AdminBlack)) {
    if (isLandscape) {
    Column(modifier = Modifier.fillMaxSize()) {

        if (orderAlertVisible) {
            RoyalOrderAlertBanner(
                visible = true,
                onStop = {
                    stopRoyalOrderAlert()
                    orderAlertVisible = false
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AdminSidebar(section = section, onSectionSelected = ::openSection, onLogout = onLogout,
                modifier = Modifier.fillMaxHeight().width(220.dp))
            VerticalDivider(color = AdminGold.copy(alpha = 0.35f))
            AdminContent(
                section = section, products = products, orders = orders, archivedOrders = archivedOrders,
                loadingArchive = loadingArchive, salesStats = salesStats, topProducts = topProducts,
                loadingDashboard = loadingDashboard, accountEmail = accountEmail, loadingAccount = loadingAccount,
                onRefreshAccount = ::refreshAccount, onChangeEmail = ::changeEmail, onChangePassword = ::changePassword,
                message = message, name = name, category = category, price = price,
                selectedImageUri = selectedImageUri, editingProductId = editingProductId, loading = loading,
                accessToken = accessToken, context = context, onNameChange = { name = it },
                onCategoryChange = { category = it }, onPriceChange = { price = it },
                onPickImage = { imagePicker.launch("image/*") },
                onEditProduct = {
                    editingProductId = it.id; name = it.name; category = it.category; price = it.price.toString()
                    selectedImageUri = null; message = "يمكنك تعديل بيانات المنتج الآن."; section = AdminSection.PRODUCTS
                },
                onDeleteProduct = { product -> Thread {
                    try { deleteProduct(accessToken, product.id); products = loadProducts(); message = "تم حذف المنتج بنجاح ✅" }
                    catch (e: Exception) { message = e.message ?: "تعذر حذف المنتج." }
                }.start() },
                onSaveProduct = { saveProduct(context, accessToken, name, category, price, selectedImageUri, editingProductId,
                    { loading = it }, { message = it }, { products = it }, {
                        name = ""; category = ""; price = ""; selectedImageUri = null; editingProductId = null
                    }) },
                onCancelEdit = { editingProductId = null; name = ""; category = ""; price = ""; selectedImageUri = null; message = "" },
                onRefresh = { refreshDashboard() }, onRefreshArchive = { refreshArchive() },
                onOrderStatus = { orderId, status -> Thread {
                    try { updateOrderStatusAsync(accessToken, orderId, status); orders = loadAdminOrders(accessToken); salesStats = calculateSalesStats(accessToken); message = "تم تحديث حالة الطلب ✅" }
                    catch (e: Exception) { message = e.message ?: "تعذر تحديث حالة الطلب." }
                }.start() },
                onDeleteOrder = { orderId -> Thread {
                    try { deleteCancelledOrder(accessToken, orderId); orders = loadAdminOrders(accessToken); message = "تم حذف الطلب الملغى ✅" }
                    catch (e: Exception) { message = e.message ?: "تعذر حذف الطلب." }
                }.start() }
            )
        }
    }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            AdminTopBar(title = adminSectionTitle(section), menuOpen = menuOpen,
                onMenuClick = { menuOpen = !menuOpen }, onLogout = onLogout)
            if (menuOpen) AdminHorizontalMenu(section = section, onSectionSelected = ::openSection)
            if (orderAlertVisible) {
    RoyalOrderAlertBanner(
        visible = true,
        onStop = {
            stopRoyalOrderAlert()
            orderAlertVisible = false
        }
    )
            }
            AdminContent(
                section = section, products = products, orders = orders, archivedOrders = archivedOrders,
                loadingArchive = loadingArchive, salesStats = salesStats, topProducts = topProducts,
                loadingDashboard = loadingDashboard, accountEmail = accountEmail, loadingAccount = loadingAccount,
                onRefreshAccount = ::refreshAccount, onChangeEmail = ::changeEmail, onChangePassword = ::changePassword,
                message = message, name = name, category = category, price = price,
                selectedImageUri = selectedImageUri, editingProductId = editingProductId, loading = loading,
                accessToken = accessToken, context = context, onNameChange = { name = it },
                onCategoryChange = { category = it }, onPriceChange = { price = it },
                onPickImage = { imagePicker.launch("image/*") },
                onEditProduct = {
                    editingProductId = it.id; name = it.name; category = it.category; price = it.price.toString()
                    selectedImageUri = null; message = "يمكنك تعديل بيانات المنتج الآن."; section = AdminSection.PRODUCTS
                },
                onDeleteProduct = { product -> Thread {
                    try { deleteProduct(accessToken, product.id); products = loadProducts(); message = "تم حذف المنتج بنجاح ✅" }
                    catch (e: Exception) { message = e.message ?: "تعذر حذف المنتج." }
                }.start() },
                onSaveProduct = { saveProduct(context, accessToken, name, category, price, selectedImageUri, editingProductId,
                    { loading = it }, { message = it }, { products = it }, {
                        name = ""; category = ""; price = ""; selectedImageUri = null; editingProductId = null
                    }) },
                onCancelEdit = { editingProductId = null; name = ""; category = ""; price = ""; selectedImageUri = null; message = "" },
                onRefresh = { refreshDashboard() }, onRefreshArchive = { refreshArchive() },
                onOrderStatus = { orderId, status -> Thread {
                    try { updateOrderStatusAsync(accessToken, orderId, status); orders = loadAdminOrders(accessToken); salesStats = calculateSalesStats(accessToken); message = "تم تحديث حالة الطلب ✅" }
                    catch (e: Exception) { message = e.message ?: "تعذر تحديث حالة الطلب." }
                }.start() },
                onDeleteOrder = { orderId -> Thread {
                    try { deleteCancelledOrder(accessToken, orderId); orders = loadAdminOrders(accessToken); message = "تم حذف الطلب الملغى ✅" }
                    catch (e: Exception) { message = e.message ?: "تعذر حذف الطلب." }
                }.start() }
            )
        }
    }
}

}
