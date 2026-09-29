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
    var salesStats by remember { mutableStateOf(AdminSalesStats()) }
    var topProducts by remember { mutableStateOf<List<AdminTopProduct>>(emptyList()) }
    var loadingDashboard by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
        message = if (uri != null) "تم اختيار الصورة ✅" else ""
    }

    fun refreshProducts() {
        Thread {
            try {
                val result = loadProducts()
                products = result
            } catch (e: Exception) {
                message = e.message ?: "تعذر تحميل المنتجات."
            }
        }.start()
    }

    fun refreshOrders() {
        Thread {
            try {
                val result = loadAdminOrders(accessToken)
                orders = result
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
                val completedOrders = currentOrders.filter {
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
            AdminSection.HOME,
            AdminSection.SALES,
            AdminSection.TOP_PRODUCTS -> refreshDashboard()
            AdminSection.PRODUCTS -> refreshProducts()
            AdminSection.ORDERS -> refreshOrders()
        }
    }

    LaunchedEffect(Unit) {
        refreshDashboard()
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
            onLoading = { loading = it },
            onMessage = { message = it },
            onProductsLoaded = { products = it },
            onClear = { clearProductForm() }
        )
    }

    fun handleOrderStatus(orderId: Long, status: String) {
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

    fun handleDeleteOrder(orderId: Long) {
        Thread {
            try {
                deleteCancelledOrder(accessToken, orderId)
                orders = loadAdminOrders(accessToken)
                message = "تم حذف الطلب الملغى ✅"
            } catch (e: Exception) {
                message = e.message ?: "تعذر حذف الطلب."
            }
        }.start()
    }

    fun handleDeleteProduct(product: Product) {
        Thread {
            try {
                deleteProduct(accessToken, product.id)
                products = loadProducts()
                message = "تم حذف المنتج بنجاح ✅"
            } catch (e: Exception) {
                message = e.message ?: "تعذر حذف المنتج."
            }
        }.start()
    }

    fun handleEditProduct(product: Product) {
        editingProductId = product.id
        name = product.name
        category = product.category
        price = product.price.toString()
        selectedImageUri = null
        section = AdminSection.PRODUCTS
        menuOpen = false
        message = "يمكنك تعديل بيانات المنتج الآن."
    }

    AdminContentArea(
        isLandscape = isLandscape,
        section = section,
        menuOpen = menuOpen,
        onMenuClick = { menuOpen = !menuOpen },
        onSectionSelected = ::openSection,
        onLogout = onLogout,
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
        onEditProduct = ::handleEditProduct,
        onDeleteProduct = ::handleDeleteProduct,
        onSaveProduct = ::saveCurrentProduct,
        onCancelEdit = { clearProductForm(); message = "" },
        onRefresh = ::refreshDashboard,
        onOrderStatus = ::handleOrderStatus,
        onDeleteOrder = ::handleDeleteOrder
    )
}

@Composable
private fun AdminContentArea(
    isLandscape: Boolean,
    section: AdminSection,
    menuOpen: Boolean,
    onMenuClick: () -> Unit,
    onSectionSelected: (AdminSection) -> Unit,
    onLogout: () -> Unit,
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
    onOrderStatus: (Long, String) -> Unit,
    onDeleteOrder: (Long) -> Unit
) {
    val content = @Composable {
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
            onNameChange = onNameChange,
            onCategoryChange = onCategoryChange,
            onPriceChange = onPriceChange,
            onPickImage = onPickImage,
            onEditProduct = onEditProduct,
            onDeleteProduct = onDeleteProduct,
            onSaveProduct = onSaveProduct,
            onCancelEdit = onCancelEdit,
            onRefresh = onRefresh,
            onOrderStatus = onOrderStatus,
            onDeleteOrder = onDeleteOrder
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(AdminBlack)
    ) {
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                AdminSidebar(
                    section = section,
                    onSectionSelected = onSectionSelected,
                    onLogout = onLogout,
                    modifier = Modifier.fillMaxHeight().width(220.dp)
                )
                VerticalDivider(color = AdminGold.copy(alpha = 0.35f))
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    content()
                }
            }
        } else {
            AdminTopBar(
                title = adminSectionTitle(section),
                menuOpen = menuOpen,
                onMenuClick = onMenuClick,
                onLogout = onLogout
            )
            if (menuOpen) {
                AdminHorizontalMenu(
                    section = section,
                    onSectionSelected = onSectionSelected
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                content()
            }
        }
    }
}
