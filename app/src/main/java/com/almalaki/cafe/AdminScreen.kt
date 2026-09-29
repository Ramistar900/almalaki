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
        configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE

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

    var salesStats by remember {
        mutableStateOf(AdminSalesStats())
    }

    var topProducts by remember {
        mutableStateOf<List<AdminTopProduct>>(emptyList())
    }

    var loadingDashboard by remember {
        mutableStateOf(false)
    }

    val imagePicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetContent()
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

                products = loadProducts()

            } catch (e: Exception) {

                message =
                    e.message
                        ?: "تعذر تحميل المنتجات."
            }
        }.start()
    }

    fun refreshOrders() {
        Thread {
            try {

                orders =
                    loadAdminOrders(
                        accessToken
                    )

            } catch (e: Exception) {

                message =
                    e.message
                        ?: "تعذر تحميل الطلبات."
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
                    loadAdminOrders(
                        accessToken
                    )

                val completedOrders =
                    currentOrders.filter {
                        it.status.equals(
                            "completed",
                            ignoreCase = true
                        )
                    }

                products =
                    currentProducts

                orders =
                    currentOrders

                salesStats =
                    calculateSalesStats(
                        accessToken
                    )

                topProducts =
                    calculateTopProducts(
                        accessToken =
                            accessToken,
                        completedOrders =
                            completedOrders,
                        products =
                            currentProducts
                    )

            } catch (e: Exception) {

                message =
                    e.message
                        ?: "تعذر تحديث لوحة التحكم."

            } finally {

                loadingDashboard = false
            }

        }.start()
    }

    LaunchedEffect(Unit) {
        refreshDashboard()
    }

    fun openSection(
        target: AdminSection
    ) {

        section = target
        menuOpen = false

        when (target) {

            AdminSection.HOME,
            AdminSection.SALES,
            AdminSection.TOP_PRODUCTS -> {
                refreshDashboard()
            }

            AdminSection.PRODUCTS -> {
                refreshProducts()
            }

            AdminSection.ORDERS -> {
                refreshOrders()
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    AdminBlack
                )
    ) {

        if (isLandscape) {

            Row(
                modifier =
                    Modifier.fillMaxSize()
            ) {

                AdminSidebar
