package com.almalaki.cafe

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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

    var section by remember {
        mutableStateOf(AdminSection.HOME)
    }

    var menuOpen by remember {
        mutableStateOf(false)
    }

    var name by remember {
        mutableStateOf("")
    }

    var category by remember {
        mutableStateOf("")
    }

    var price by remember {
        mutableStateOf("")
    }

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

    var archivedOrders by remember {
        mutableStateOf<List<AdminArchivedOrder>>(emptyList())
    }

    var salesStats by remember {
        mutableStateOf(AdminSalesStats())
    }

    var topProducts by remember {
        mutableStateOf<List<AdminTopProduct>>(emptyList())
    }

    var loadingDashboard by remember {
        mutableStateOf(false)
    }

    var loadingArchive by remember {
        mutableStateOf(false)
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri

        message =
            if (uri != null) {
                "تم اختيار الصورة ✅"
            } else {
                ""
            }
    }

    fun refreshProducts() {
        Thread {
            try {
                val loaded = loadProducts()
                products = loaded
            } catch (e: Exception) {
                message =
                    e.message ?: "تعذر تحميل المنتجات."
            }
        }.start()
    }

    fun refreshOrders() {
        Thread {
            try {
                val loaded =
                    loadAdminOrders(accessToken)

                orders =
                    loaded.filter {
                        !it.status.equals(
                            "completed",
                            ignoreCase = true
                        ) &&
                        !it.status.equals(
                            "cancelled",
                            ignoreCase = true
                        )
                    }
            } catch (e: Exception) {
                message =
                    e.message ?: "تعذر تحميل الطلبات."
            }
        }.start()
    }

    fun refreshArchive() {
        loadingArchive = true

        Thread {
            try {
                val loaded =
                    loadArchivedOrders(accessToken)

                archivedOrders = loaded

            } catch (e: Exception) {
                message =
                    e.message ?: "تعذر تحميل الأرشيف."

            } finally {
                loadingArchive = false
            }
        }.start()
    }

    fun refreshDashboard() {
        loadingDashboard = true

        Thread {
            try {
                val currentProducts =
                    loadProducts()

                val currentOrders =
                    loadAdminOrders(accessToken)

                val activeOrders =
                    currentOrders.filter {
                        !it.status.equals(
                            "completed",
                            ignoreCase = true
                        ) &&
                        !it.status.equals(
                            "cancelled",
                            ignoreCase = true
                        )
                    }

                val completedOrders =
                    currentOrders.filter {
                        it.status.equals(
                            "completed",
                            ignoreCase = true
                        )
                    }

                val stats =
                    calculateSalesStats(accessToken)

                val top =
                    calculateTopProducts(
                        accessToken = accessToken,
                        completedOrders = completedOrders,
                        products = currentProducts
                    )

                products = currentProducts
                orders = activeOrders
                salesStats = stats
                topProducts = top

            } catch (e: Exception) {
                message =
                    e.message ?: "تعذر تحديث لوحة التحكم."

            } finally {
                loadingDashboard = false
            }
        }.start()
    }

    fun clearProductForm() {
        name = ""
        category = ""
        price = ""
        selectedImageUri = null
        editingProductId = null
    }

    fun openSection(target: AdminSection) {
        section = target
        menuOpen = false

        when (target) {
            AdminSection.HOME -> {
                refreshDashboard()
            }

            AdminSection.PRODUCTS -> {
                refreshProducts()
            }

            AdminSection.ORDERS -> {
                refreshOrders()
            }

            AdminSection.ARCHIVE -> {
                refreshArchive()
            }

            AdminSection.SALES -> {
                refreshDashboard()
            }

            AdminSection.TOP_PRODUCTS -> {
                refreshDashboard()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshDashboard()
        refreshArchive()
    }

    fun saveCurrentProduct() {
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
                clearProductForm()
            }
        )
    }

    fun editProduct(product: Product) {
        editingProductId = product.id
        name = product.name
        category = product.category
        price = product.price.toString()
        selectedImageUri = null
        section = AdminSection.PRODUCTS
        menuOpen = false
        message = "يمكنك تعديل بيانات المنتج الآن."
    }

    fun removeProduct(product: Product) {
        Thread {
            try {
                deleteProduct(
                    accessToken,
                    product.id
                )

                products = loadProducts()

                message =
                    "تم حذف المنتج بنجاح ✅"

            } catch (e: Exception) {
                message =
                    e.message ?: "تعذر حذف المنتج."
            }
        }.start()
    }

    fun changeOrderStatus(
        orderId: Long,
        status: String
    ) {
        Thread {
            try {
                updateOrderStatusAsync(
                    accessToken,
                    orderId,
                    status
                )

                val currentOrders =
                    loadAdminOrders(accessToken)

                orders =
                    currentOrders.filter {
                        !it.status.equals(
                            "completed",
                            ignoreCase = true
                        ) &&
                        !it.status.equals(
                            "cancelled",
                            ignoreCase = true
                        )
                    }

                salesStats =
                    calculateSalesStats(accessToken)

                val completed =
                    currentOrders.filter {
                        it.status.equals(
                            "completed",
                            ignoreCase = true
                        )
                    }

                topProducts =
                    calculateTopProducts(
                        accessToken,
                        completed,
                        products
                    )

                refreshArchive()

                message =
                    "تم تحديث حالة الطلب ✅"

            } catch (e: Exception) {
                message =
                    e.message ?: "تعذر تحديث حالة الطلب."
            }
        }.start()
    }

        fun removeCancelledOrder(
        orderId: Long
    ) {
        Thread {
            try {
                deleteCancelledOrder(
                    accessToken,
                    orderId
                )

                val currentOrders =
                    loadAdminOrders(accessToken)

                orders =
                    currentOrders.filter {
                        !it.status.equals(
                            "completed",
                            ignoreCase = true
                        ) &&
                        !it.status.equals(
                            "cancelled",
                            ignoreCase = true
                        )
                    }

                refreshArchive()

                message = "تم حذف الطلب وأرشفته ✅"

            } catch (e: Exception) {
                message = e.message ?: "تعذر حذف الطلب."
            }
        }.start()
        }
                        } catch (e: Exception) {
                message = e.message ?: "تعذر حذف الطلب."
            }
        }.start()
    }
}
