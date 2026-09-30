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
    var archivedOrders by remember { mutableStateOf<List<AdminArchivedOrder>>(emptyList()) }
    var salesStats by remember { mutableStateOf(AdminSalesStats()) }
    var topProducts by remember { mutableStateOf<List<AdminTopProduct>>(emptyList()) }
    var loadingDashboard by remember { mutableStateOf(false) }
    var loadingArchive by remember { mutableStateOf(false) }
    var accountEmail by remember { mutableStateOf("") }
    var loadingAccount by remember { mutableStateOf(false) }

    // 🔔 إشعار الطلب الجديد
    var showNewOrderDialog by remember { mutableStateOf(false) }
    var newOrderNumber by remember { mutableStateOf("") }

    // الطلبات التي تم التعرف عليها مسبقًا
    var knownOrderIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // لمنع إشعار الطلبات الموجودة أصلًا عند أول تحميل
    var ordersNotificationInitialized by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(message) {
        if (message.isNotEmpty()) {
            kotlinx.coroutines.delay(4000)
            message = ""
        }
    }

    val imagePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            selectedImageUri = uri
            message = if (uri != null) "تم اختيار الصورة ✅" else ""
        }

    fun handleLoadedOrders(
        loadedOrders: List<AdminOrder>,
        notifyNewOrder: Boolean
    ) {
        if (!ordersNotificationInitialized) {
            knownOrderIds = loadedOrders.map { it.id }.toSet()
            ordersNotificationInitialized = true
            orders = loadedOrders
            return
        }

        val newOrders =
            loadedOrders.filter {
                it.id !in knownOrderIds
            }

        orders = loadedOrders
        knownOrderIds = loadedOrders.map { it.id }.toSet()

        if (notifyNewOrder && newOrders.isNotEmpty()) {
            val latestOrder = newOrders.maxByOrNull { it.id }

            if (latestOrder != null) {
                newOrderNumber = latestOrder.orderNumber
                showNewOrderDialog = true
                AppSounds.newOrder(context)
            }
        }
    }

    fun refreshProducts() {
        Thread {
            try {
                products = loadProducts()
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    message = e.message ?: "تعذر تحميل المنتجات."
                }
            }
        }.start()
    }

    fun refreshOrders() {
        Thread {
            try {
                val loadedOrders = loadAdminOrders(accessToken)

                Handler(Looper.getMainLooper()).post {
                    handleLoadedOrders(
                        loadedOrders = loadedOrders,
                        notifyNewOrder = true
                    )
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    message = e.message ?: "تعذر تحميل الطلبات."
                }
            }
        }.start()
    }

    fun refreshArchive() {
        loadingArchive = true
        Thread {
            try {
                val loadedArchive = loadArchivedOrders(accessToken)

                Handler(Looper.getMainLooper()).post {
                    archivedOrders = loadedArchive
                    loadingArchive = false
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    message = e.message ?: "تعذر تحميل الأرشيف."
                    loadingArchive = false
                }
            }
        }.start()
    }

    fun refreshAccount() {
        loadingAccount = true
        Thread {
            try {
                val email = loadCurrentAccountEmail(accessToken)

                Handler(Looper.getMainLooper()).post {
                    accountEmail = email
                    loadingAccount = false
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    message = e.message ?: "تعذر تحميل بيانات الحساب."
                    loadingAccount = false
                }
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
                    loadingAccount = false
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    message = e.message ?: "تعذر تغيير البريد الإلكتروني."
                    loadingAccount = false
                }
            }
        }.start()
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        loadingAccount = true
        Thread {
            try {
                updateAccountPassword(
                    accessToken = accessToken,
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    confirmPassword = confirmPassword
                )

                Handler(Looper.getMainLooper()).post {
                    AppSounds.passwordChanged(context)
                    message = "تم تغيير كلمة المرور بنجاح ✓"
                    loadingAccount = false
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    message = e.message ?: "تعذر تغيير كلمة المرور."
                    loadingAccount = false
                }
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

                Handler(Looper.getMainLooper()).post {
                    products = currentProducts

                    handleLoadedOrders(
                        loadedOrders = currentOrders,
                        notifyNewOrder = false
                    )

                    salesStats = calculateSalesStats(accessToken)

                    topProducts = calculateTopProducts(
                        accessToken = accessToken,
                        completedOrders = completedOrders,
                        products = currentProducts
                    )

                    loadingDashboard = false
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    message = e.message ?: "تعذر تحديث لوحة التحكم."
                    loadingDashboard = false
                }
            }
        }.start()
    }

    LaunchedEffect(Unit) {
        refreshDashboard()
        refreshArchive()
        refreshAccount()
    }

    // 🔔 فحص الطلبات الجديدة تلقائيًا كل 10 ثوانٍ
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(10000)

            try {
                val loadedOrders =
                    loadAdminOrders(accessToken)

                handleLoadedOrders(
                    loadedOrders = loadedOrders,
                    notifyNewOrder = true
                )
            } catch (_: Exception) {
                // لا نعرض خطأ كل 10 ثوانٍ للمالك
            }
        }
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

            AdminSection.ARCHIVE -> refreshArchive()

            AdminSection.ACCOUNT_SETTINGS -> refreshAccount()
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

                VerticalDivider(
                    color = AdminGold.copy(alpha = 0.35f)
                )

                AdminContent(
                    section = section,
                    products = products,
                    orders = orders,
                    archivedOrders = archivedOrders,
                    loadingArchive = loadingArchive,
                    salesStats = salesStats,
                    topProducts = topProducts,
                    loadingDashboard = loadingDashboard,
                    accountEmail = accountEmail,
                    loadingAccount = loadingAccount,
                    onRefreshAccount = ::refreshAccount,
                    onChangeEmail = ::changeEmail,
                    onChangePassword = ::changePassword,
                    message = message,
                    name = name,
                    category = category,
                    price = price,
                    selectedImageUri = selectedImageUri,
                    editingProductId = editingProductId,
                    loading = loading,
                    accessToken = accessToken,
                    context = context,

                    onNameChange = {
                        name = it
                    },

                    onCategoryChange = {
                        category = it
                    },

                    onPriceChange = {
                        price = it
                    },

                    onPickImage = {
                        imagePicker.launch("image/*")
                    },

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

                                val updatedProducts =
                                    loadProducts()

                                Handler(Looper.getMainLooper()).post {
                                    products = updatedProducts
                                    message = "تم حذف المنتج بنجاح ✅"
                                }
                            } catch (e: Exception) {
                                Handler(Looper.getMainLooper()).post {
                                    message =
                                        e.message
                                            ?: "تعذر حذف المنتج."
                                }
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
                            onLoading = {
                                loading = it
                            },
                            onMessage = {
                                message = it
                            },
                            onProductsLoaded = {
                                products = it
                            },
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

                    onRefresh = {
                        refreshDashboard()
                    },

                    onRefreshArchive = {
                        refreshArchive()
                    },

                    onOrderStatus = { orderId, status ->
                        Thread {
                            try {
                                updateOrderStatusAsync(
                                    accessToken = accessToken,
                                    orderId = orderId,
                                    status = status
                                )

                                val updatedOrders =
                                    loadAdminOrders(accessToken)

                                val updatedSales =
                                    calculateSalesStats(accessToken)

                                Handler(Looper.getMainLooper()).post {
                                    orders = updatedOrders
                                    salesStats = updatedSales
                                    message = "تم تحديث حالة الطلب ✅"
                                }
                            } catch (e: Exception) {
                                Handler(Looper.getMainLooper()).post {
                                    message =
                                        e.message
                                            ?: "تعذر تحديث حالة الطلب."
                                }
                            }
                        }.start()
                    },

                    onDeleteOrder = { orderId ->
                        Thread {
                            try {
                                deleteCancelledOrder(
                                    accessToken = accessToken,
                                    orderId = orderId
                                )

                                val updatedOrders =
                                    loadAdminOrders(accessToken)

                                Handler(Looper.getMainLooper()).post {
                                    orders = updatedOrders
                                    knownOrderIds =
                                        updatedOrders.map {
                                            it.id
                                        }.toSet()

                                    message =
                                        "تم حذف الطلب الملغى ✅"
                                }
                            } catch (e: Exception) {
                                Handler(Looper.getMainLooper()).post {
                                    message =
                                        e.message
                                            ?: "تعذر حذف الطلب."
                                }
                            }
                        }.start()
                    }
                )
            }
        } else {

            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                AdminTopBar(
    title = adminSectionTitle(section),
    menuOpen = menuOpen,
    onMenuClick = {
        AppSounds.buttonClick(context)
        menuOpen = !menuOpen
    },
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
    archivedOrders = archivedOrders,
    loadingArchive = loadingArchive,
    salesStats = salesStats,
    topProducts = topProducts,
    loadingDashboard = loadingDashboard,
    accountEmail = accountEmail,
    loadingAccount = loadingAccount,
    onRefreshAccount = ::refreshAccount,
    onChangeEmail = ::changeEmail,
    onChangePassword = ::changePassword,
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
    onRefreshArchive = { refreshArchive() },
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
    },
    onDeleteOrder = { orderId ->
        Thread {
            try {
                deleteCancelledOrder(
                    accessToken = accessToken,
                    orderId = orderId
                )
                orders = loadAdminOrders(accessToken)
                knownOrderIds = orders.map { it.id }.toSet()
                message = "تم حذف الطلب الملغى ✅"
            } catch (e: Exception) {
                message = e.message ?: "تعذر حذف الطلب."
            }
        }.start()
    }
)
        }
    }

    // 🪟 نافذة إشعار الطلب الجديد للمالك فقط
    if (showNewOrderDialog) {
        AlertDialog(
            onDismissRequest = {
                showNewOrderDialog = false
            },
            title = {
                Text(
                    text = "🔔 طلب جديد",
                    color = AdminGold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "وصل طلب جديد إلى الكافيه.",
                        color = Color.White
                    )

                    Text(
                        text = "رقم الطلب: $newOrderNumber",
                        color = AdminGold,
                        fontSize = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        AppSounds.buttonClick(context)
                        showNewOrderDialog = false
                        openSection(AdminSection.ORDERS)
                    }
                ) {
                    Text(
                        text = "عرض الطلب",
                        color = AdminGold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        AppSounds.buttonClick(context)
                        showNewOrderDialog = false
                    }
                ) {
                    Text(
                        text = "إغلاق",
                        color = Color.LightGray
                    )
                }
            },
            containerColor = AdminBlack
        )
    }
    } 
