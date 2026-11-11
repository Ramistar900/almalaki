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
import androidx.activity.compose.BackHandler
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
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
val context = LocalContext.current
val configuration = LocalConfiguration.current
val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
val useWideAdminLayout = isLandscape && configuration.screenWidthDp >= 840
    var ownerRestaurantId by remember { mutableStateOf("") }
    var ownerRestaurants by remember { mutableStateOf<List<OwnerRestaurant>>(emptyList()) }
    var restaurantMenuExpanded by remember { mutableStateOf(false) }
    var isDeveloperAccount by remember { mutableStateOf<Boolean?>(null) }
    var interfaceSettings by remember {
        mutableStateOf(RoyalInterfaceSettings())
    }

var section by remember { mutableStateOf(AdminSection.HOME) }
var menuOpen by remember { mutableStateOf(false) }
val sectionHistory = remember { mutableStateListOf<AdminSection>() }
BackHandler {
    if (menuOpen) {
        menuOpen = false
    } else if (sectionHistory.isNotEmpty()) {
        section = sectionHistory.removeAt(sectionHistory.lastIndex)
    } else if (section != AdminSection.HOME) {
        section = AdminSection.HOME
    } else {
        onBack()
    }
}

var name by remember { mutableStateOf("") }
var category by remember { mutableStateOf("") }
var price by remember { mutableStateOf("") }
var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
var editingProductId by remember { mutableStateOf<Int?>(null) }
var message by remember { mutableStateOf("") }

LaunchedEffect(accessToken) {
    try {
        val loadedRestaurants = kotlinx.coroutines.withContext(
            kotlinx.coroutines.Dispatchers.IO
        ) {
            loadOwnerRestaurants(accessToken)
        }
        ownerRestaurants = loadedRestaurants
        if (ownerRestaurantId !in loadedRestaurants.map { it.id }) {
            ownerRestaurantId = loadedRestaurants.first().id
        }
        isDeveloperAccount = try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                isRoyalDeveloperAccount(accessToken)
            }
        } catch (_: Exception) {
            null
        }
    } catch (error: Exception) {
        ownerRestaurants = emptyList()
        ownerRestaurantId = ""
        message = error.message ?: "تعذر تحميل المطاعم المرتبطة بحساب المالك."
    }
}
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
var highlightedOrderId by remember { mutableStateOf<Long?>(null) }
var orderAlertMessage by remember { mutableStateOf("🔔 يوجد طلب جديد") }
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
    val requestedRestaurantId = ownerRestaurantId
    if (requestedRestaurantId.isBlank()) return

    Thread {
        try {
            val loadedProducts = loadProducts(accessToken, requestedRestaurantId)
            Handler(Looper.getMainLooper()).post {
                if (ownerRestaurantId == requestedRestaurantId) {
                    products = loadedProducts
                }
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                if (ownerRestaurantId == requestedRestaurantId) {
                    message = e.message ?: "تعذر تحميل المنتجات."
                }
            }
        }
    }.start()
}

fun refreshOrders() {
    val requestedRestaurantId = ownerRestaurantId
    if (requestedRestaurantId.isBlank()) return

    Thread {
        try {
            val loadedOrders = loadAdminOrders(accessToken, requestedRestaurantId)
            Handler(Looper.getMainLooper()).post {
                if (ownerRestaurantId == requestedRestaurantId) {
                    orders = loadedOrders
                }
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                if (ownerRestaurantId == requestedRestaurantId) {
                    message = e.message ?: "تعذر تحميل الطلبات."
                }
            }
        }
    }.start()
}

fun refreshArchive() {
    val requestedRestaurantId = ownerRestaurantId
    if (requestedRestaurantId.isBlank()) return
    loadingArchive = true

    Thread {
        try {
            val loadedArchive = loadArchivedOrders(accessToken, requestedRestaurantId)
            Handler(Looper.getMainLooper()).post {
                if (ownerRestaurantId == requestedRestaurantId) {
                    archivedOrders = loadedArchive
                    loadingArchive = false
                }
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                if (ownerRestaurantId == requestedRestaurantId) {
                    message = e.message ?: "تعذر تحميل الأرشيف."
                    loadingArchive = false
                }
            }
        }
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
    val requestedRestaurantId = ownerRestaurantId
    if (requestedRestaurantId.isBlank()) return
    loadingDashboard = true

    Thread {
        try {
            val currentProducts = loadProducts(accessToken, requestedRestaurantId)
            val currentOrders = loadAdminOrders(accessToken, requestedRestaurantId)
            val completedOrders = currentOrders.filter { it.status.equals("completed", ignoreCase = true) }
            val currentSalesStats = calculateSalesStats(accessToken, requestedRestaurantId)
            val currentTopProducts = calculateTopProducts(
                accessToken,
                completedOrders,
                currentProducts,
                requestedRestaurantId
            )
            Handler(Looper.getMainLooper()).post {
                if (ownerRestaurantId == requestedRestaurantId) {
                    products = currentProducts
                    orders = currentOrders
                    salesStats = currentSalesStats
                    topProducts = currentTopProducts
                }
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                if (ownerRestaurantId == requestedRestaurantId) {
                    message = e.message ?: "تعذر تحديث لوحة التحكم."
                }
            }
        } finally {
            Handler(Looper.getMainLooper()).post {
                if (ownerRestaurantId == requestedRestaurantId) {
                    loadingDashboard = false
                }
            }
        }
    }.start()
}
LaunchedEffect(Unit) {
    refreshAccount()
}
LaunchedEffect(ownerRestaurantId) {
    if (ownerRestaurantId.isBlank()) return@LaunchedEffect
    stopRoyalOrderAlert()
    orderAlertVisible = false
    highlightedOrderId = null
    orderAlertMessage = "🔔 يوجد طلب جديد"
    migrateLegacyRoyalInterfaceSettings(context, ownerRestaurantId)
    RoyalTextLogoSettings.migrateLegacy(context, ownerRestaurantId)
    interfaceSettings = loadRoyalInterfaceSettings(context, ownerRestaurantId)
    loading = false
    loadingDashboard = false
    loadingArchive = false
    products = emptyList()
    orders = emptyList()
    archivedOrders = emptyList()
    salesStats = AdminSalesStats()
    topProducts = emptyList()
    editingProductId = null
    name = ""
    category = ""
    price = ""
    selectedImageUri = null
    refreshDashboard()
    refreshArchive()
}

LaunchedEffect(accessToken, ownerRestaurantId) {
    if (ownerRestaurantId.isBlank()) return@LaunchedEffect
    orderAlertInitialized = false
    lastKnownOrderIds = emptySet()
    while (true) {
        try {
            val currentOrders = kotlinx.coroutines.withContext(
    kotlinx.coroutines.Dispatchers.IO
) {
    loadAdminOrders(accessToken, ownerRestaurantId)
            }

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
    val alertOrder = newOrders.first()

    val alertItems = kotlinx.coroutines.withContext(
        kotlinx.coroutines.Dispatchers.IO
    ) {
        loadOrderItems(
            accessToken = accessToken,
            orderId = alertOrder.id,
            restaurantId = ownerRestaurantId
        )
    }

    val isDelivery =
        alertOrder.fulfillmentType.contains("توصيل", ignoreCase = true)

    val itemsText = alertItems.joinToString("\n") { item ->
        "${item.productName} × ${item.quantity}"
    }

    orderAlertMessage = buildString {
        appendLine(
            if (isDelivery) {
                "🔔 طلب توصيل جديد"
            } else {
                "🔔 طلب جديد"
            }
        )

        appendLine(alertOrder.customerName)

        if (itemsText.isNotBlank()) {
            appendLine(itemsText)
        }

        if (isDelivery && alertOrder.deliveryAddress.isNotBlank()) {
            appendLine("🚚 ملاحظة توصيل:")
            appendLine(alertOrder.deliveryAddress)
        }

        append("💰 المجموع: ${alertOrder.totalAmount} ل.س")
        
    }

    highlightedOrderId = alertOrder.id
    orderAlertVisible = true
    startRoyalOrderAlert(context)
                }
                                lastKnownOrderIds = currentIds
            }

            orders = currentOrders

        } catch (e: Exception) {
            message = e.message ?: "تعذر فحص الطلبات الجديدة."
        }

        kotlinx.coroutines.delay(5000L)
    }
}
DisposableEffect(accessToken, ownerRestaurantId) {
    if (ownerRestaurantId.isNotBlank()) {
        startRoyalOrderRealtime(
            accessToken = accessToken,
            restaurantId = ownerRestaurantId,
            context = context
        ) {
            orderAlertVisible = true
            startRoyalOrderAlert(context)
        }
    }

    onDispose {
        stopRoyalOrderRealtime()
    }
}
fun openSection(target: AdminSection) {
    if (target != section) {
        sectionHistory.add(section)
    }

    section = target
    menuOpen = false
    when (target) {
        AdminSection.HOME, AdminSection.SALES, AdminSection.TOP_PRODUCTS -> refreshDashboard()
        AdminSection.PRODUCTS -> refreshProducts()
        AdminSection.ORDERS -> refreshOrders()
        AdminSection.RESERVATIONS -> Unit
        AdminSection.ARCHIVE -> refreshArchive()
        AdminSection.ACCOUNT_SETTINGS -> refreshAccount()
        AdminSection.INTERFACE_SETTINGS -> Unit
        AdminSection.TV_SETTINGS -> Unit
        AdminSection.ROYAL_TV -> Unit
        AdminSection.ROYAL_TV_CONTROL -> Unit
        AdminSection.ROYAL_TV_IDENTITY -> Unit
    }
}
if (section == AdminSection.ROYAL_TV) {
    RoyalTVScreen(
        modifier = Modifier.fillMaxSize()
    )
    return
}

Column(
    modifier = Modifier
        .fillMaxSize()
        .background(AdminBlack)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Royal3DAnimatedLogo(size = 38.dp, restaurantId = ownerRestaurantId)

        if (isDeveloperAccount != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isDeveloperAccount == true) "المطور الملكي" else "مالك المطعم",
                color = AdminGold,
                fontSize = 12.sp
            )
        }

        if (ownerRestaurants.size > 1) {
            Box {
                TextButton(onClick = { restaurantMenuExpanded = true }, colors = ButtonDefaults.textButtonColors(contentColor = AdminGold)) {
                    Text(ownerRestaurants.firstOrNull { it.id == ownerRestaurantId }?.name ?: "اختر المطعم")
                    Text("⌄")
                }
                DropdownMenu(expanded = restaurantMenuExpanded, onDismissRequest = { restaurantMenuExpanded = false }) {
                    ownerRestaurants.forEach { restaurant ->
                        DropdownMenuItem(
                            text = { Text(restaurant.name) },
                            onClick = {
                                restaurantMenuExpanded = false
                                if (ownerRestaurantId != restaurant.id) {
                                    ownerRestaurantId = restaurant.id
                                    message = "تم اختيار المطعم"
                                }
                            }
                        )
                    }
                }
            }
        } else if (ownerRestaurants.isNotEmpty()) {
            Text(text = ownerRestaurants.first().name, color = AdminGold, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.weight(1f))

        RoyalShinyLogo(restaurantId = ownerRestaurantId)
    }
    if (useWideAdminLayout) {
    Column(
    modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
) {

        if (orderAlertVisible) {
    RoyalOrderAlertBanner(
        visible = true,
        message = orderAlertMessage,
        onStop = {
            stopRoyalOrderAlert()
            orderAlertVisible = false
        },
        onOpen = {
            section = AdminSection.ORDERS
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
                isDeveloperAccount = isDeveloperAccount,
                onRefreshAccount = ::refreshAccount, onChangeEmail = ::changeEmail, onChangePassword = ::changePassword,
                message = message, name = name, category = category, price = price,
                selectedImageUri = selectedImageUri, editingProductId = editingProductId, loading = loading,
                accessToken = accessToken, context = context, restaurantId = ownerRestaurantId, onNameChange = { name = it },
                onCategoryChange = { category = it }, onPriceChange = { price = it },
                onPickImage = { imagePicker.launch("image/*") },
                onEditProduct = {
                    editingProductId = it.id; name = it.name; category = it.category; price = it.price.toString()
                    selectedImageUri = null; message = "يمكنك تعديل بيانات المنتج الآن."; section = AdminSection.PRODUCTS
                },
                onDeleteProduct = { product ->
                    val requestedRestaurantId = ownerRestaurantId
                    if (requestedRestaurantId.isNotBlank()) Thread {
                        try {
                            deleteProduct(accessToken, product.id, requestedRestaurantId)
                            val loadedProducts = loadProducts(accessToken, requestedRestaurantId)
                            Handler(Looper.getMainLooper()).post {
                                if (ownerRestaurantId == requestedRestaurantId) {
                                    products = loadedProducts
                                    message = "تم حذف المنتج بنجاح ✅"
                                }
                            }
                        } catch (e: Exception) {
                            Handler(Looper.getMainLooper()).post {
                                if (ownerRestaurantId == requestedRestaurantId) {
                                    message = e.message ?: "تعذر حذف المنتج."
                                }
                            }
                        }
                    }.start()
                },
                onSaveProduct = {
                    val requestedRestaurantId = ownerRestaurantId
                    if (requestedRestaurantId.isNotBlank()) {
                        saveProduct(
                            context,
                            accessToken,
                            name,
                            category,
                            price,
                            selectedImageUri,
                            editingProductId,
                            requestedRestaurantId,
                            { value ->
                                Handler(Looper.getMainLooper()).post {
                                    if (ownerRestaurantId == requestedRestaurantId) loading = value
                                }
                            },
                            { value ->
                                Handler(Looper.getMainLooper()).post {
                                    if (ownerRestaurantId == requestedRestaurantId) message = value
                                }
                            },
                            { value ->
                                Handler(Looper.getMainLooper()).post {
                                    if (ownerRestaurantId == requestedRestaurantId) products = value
                                }
                            },
                            {
                                Handler(Looper.getMainLooper()).post {
                                    if (ownerRestaurantId == requestedRestaurantId) {
                                        name = ""
                                        category = ""
                                        price = ""
                                        selectedImageUri = null
                                        editingProductId = null
                                    }
                                }
                            }
                        )
                    }
                },
                onCancelEdit = { editingProductId = null; name = ""; category = ""; price = ""; selectedImageUri = null; message = "" },
                onRefresh = { refreshDashboard() }, onRefreshArchive = { refreshArchive() },
                onOrderStatus = { orderId, status ->
                    val requestedRestaurantId = ownerRestaurantId
                    if (requestedRestaurantId.isNotBlank()) Thread {
                        try {
                            updateOrderStatusAsync(accessToken, orderId, status, requestedRestaurantId)
                            val loadedOrders = loadAdminOrders(accessToken, requestedRestaurantId)
                            val loadedStats = calculateSalesStats(accessToken, requestedRestaurantId)
                            Handler(Looper.getMainLooper()).post {
                                if (ownerRestaurantId == requestedRestaurantId) {
                                    orders = loadedOrders
                                    salesStats = loadedStats
                                    message = "تم تحديث حالة الطلب ✅"
                                }
                            }
                        } catch (e: Exception) {
                            Handler(Looper.getMainLooper()).post {
                                if (ownerRestaurantId == requestedRestaurantId) {
                                    message = e.message ?: "تعذر تحديث حالة الطلب."
                                }
                            }
                        }
                    }.start()
                },
                onDeleteOrder = { orderId ->
                    val requestedRestaurantId = ownerRestaurantId
                    if (requestedRestaurantId.isNotBlank()) Thread {
                        try {
                            deleteCancelledOrder(accessToken, orderId, requestedRestaurantId)
                            val loadedOrders = loadAdminOrders(accessToken, requestedRestaurantId)
                            Handler(Looper.getMainLooper()).post {
                                if (ownerRestaurantId == requestedRestaurantId) {
                                    orders = loadedOrders
                                    message = "تم حذف الطلب الملغى ✅"
                                }
                            }
                        } catch (e: Exception) {
                            Handler(Looper.getMainLooper()).post {
                                if (ownerRestaurantId == requestedRestaurantId) {
                                    message = e.message ?: "تعذر حذف الطلب."
                                }
                            }
                        }
                    }.start()
                }
            )
                }

             if (section != AdminSection.ROYAL_TV) {
             RoyalBottomBar(newsText = interfaceSettings.newsText, restaurantId = ownerRestaurantId)
        }
    }
    } else {
        Column(
    modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
) {
            AdminTopBar(title = adminSectionTitle(section), menuOpen = menuOpen,
                onMenuClick = { menuOpen = !menuOpen }, onLogout = onLogout)
            if (menuOpen) AdminHorizontalMenu(section = section, onSectionSelected = ::openSection)
            if (orderAlertVisible) {
    RoyalOrderAlertBanner(
        visible = true,
        message = orderAlertMessage,
        onStop = {
            stopRoyalOrderAlert()
            orderAlertVisible = false
        },
        onOpen = {
            section = AdminSection.ORDERS
            stopRoyalOrderAlert()
            orderAlertVisible = false
        }
    )
            }
                        Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
            AdminContent(
                section = section, products = products, orders = orders, archivedOrders = archivedOrders,
                loadingArchive = loadingArchive, salesStats = salesStats, topProducts = topProducts,
                loadingDashboard = loadingDashboard, accountEmail = accountEmail, loadingAccount = loadingAccount,
                isDeveloperAccount = isDeveloperAccount,
                onRefreshAccount = ::refreshAccount, onChangeEmail = ::changeEmail, onChangePassword = ::changePassword,
                message = message, name = name, category = category, price = price,
                selectedImageUri = selectedImageUri, editingProductId = editingProductId, loading = loading,
                accessToken = accessToken, context = context, restaurantId = ownerRestaurantId, onNameChange = { name = it },
                onCategoryChange = { category = it }, onPriceChange = { price = it },
                onPickImage = { imagePicker.launch("image/*") },
                onEditProduct = {
                    editingProductId = it.id; name = it.name; category = it.category; price = it.price.toString()
                    selectedImageUri = null; message = "يمكنك تعديل بيانات المنتج الآن."; section = AdminSection.PRODUCTS
                },
                onDeleteProduct = { product ->
                    val requestedRestaurantId = ownerRestaurantId
                    if (requestedRestaurantId.isNotBlank()) Thread {
                        try {
                            deleteProduct(accessToken, product.id, requestedRestaurantId)
                            val loadedProducts = loadProducts(accessToken, requestedRestaurantId)
                            Handler(Looper.getMainLooper()).post {
                                if (ownerRestaurantId == requestedRestaurantId) {
                                    products = loadedProducts
                                    message = "تم حذف المنتج بنجاح ✅"
                                }
                            }
                        } catch (e: Exception) {
                            Handler(Looper.getMainLooper()).post {
                                if (ownerRestaurantId == requestedRestaurantId) {
                                    message = e.message ?: "تعذر حذف المنتج."
                                }
                            }
                        }
                    }.start()
                },
                onSaveProduct = {
                    val requestedRestaurantId = ownerRestaurantId
                    if (requestedRestaurantId.isNotBlank()) {
                        saveProduct(
                            context,
                            accessToken,
                            name,
                            category,
                            price,
                            selectedImageUri,
                            editingProductId,
                            requestedRestaurantId,
                            { value ->
                                Handler(Looper.getMainLooper()).post {
                                    if (ownerRestaurantId == requestedRestaurantId) loading = value
                                }
                            },
                            { value ->
                                Handler(Looper.getMainLooper()).post {
                                    if (ownerRestaurantId == requestedRestaurantId) message = value
                                }
                            },
                            { value ->
                                Handler(Looper.getMainLooper()).post {
                                    if (ownerRestaurantId == requestedRestaurantId) products = value
                                }
                            },
                            {
                                Handler(Looper.getMainLooper()).post {
                                    if (ownerRestaurantId == requestedRestaurantId) {
                                        name = ""
                                        category = ""
                                        price = ""
                                        selectedImageUri = null
                                        editingProductId = null
                                    }
                                }
                            }
                        )
                    }
                },
                onCancelEdit = { editingProductId = null; name = ""; category = ""; price = ""; selectedImageUri = null; message = "" },
                onRefresh = { refreshDashboard() }, onRefreshArchive = { refreshArchive() },
                onOrderStatus = { orderId, status ->
                    val requestedRestaurantId = ownerRestaurantId
                    if (requestedRestaurantId.isNotBlank()) Thread {
                        try {
                            updateOrderStatusAsync(accessToken, orderId, status, requestedRestaurantId)
                            val loadedOrders = loadAdminOrders(accessToken, requestedRestaurantId)
                            val loadedStats = calculateSalesStats(accessToken, requestedRestaurantId)
                            Handler(Looper.getMainLooper()).post {
                                if (ownerRestaurantId == requestedRestaurantId) {
                                    orders = loadedOrders
                                    salesStats = loadedStats
                                    message = "تم تحديث حالة الطلب ✅"
                                }
                            }
                        } catch (e: Exception) {
                            Handler(Looper.getMainLooper()).post {
                                if (ownerRestaurantId == requestedRestaurantId) {
                                    message = e.message ?: "تعذر تحديث حالة الطلب."
                                }
                            }
                        }
                    }.start()
                },
                onDeleteOrder = { orderId ->
                    val requestedRestaurantId = ownerRestaurantId
                    if (requestedRestaurantId.isNotBlank()) Thread {
                        try {
                            deleteCancelledOrder(accessToken, orderId, requestedRestaurantId)
                            val loadedOrders = loadAdminOrders(accessToken, requestedRestaurantId)
                            Handler(Looper.getMainLooper()).post {
                                if (ownerRestaurantId == requestedRestaurantId) {
                                    orders = loadedOrders
                                    message = "تم حذف الطلب الملغى ✅"
                                }
                            }
                        } catch (e: Exception) {
                            Handler(Looper.getMainLooper()).post {
                                if (ownerRestaurantId == requestedRestaurantId) {
                                    message = e.message ?: "تعذر حذف الطلب."
                                }
                            }
                        }
                    }.start()
                },
                highlightedOrderId = highlightedOrderId
            )
                        }
            if (section != AdminSection.ROYAL_TV) {
            RoyalBottomBar(newsText = interfaceSettings.newsText, restaurantId = ownerRestaurantId)
            }
        }
    }
}

}
