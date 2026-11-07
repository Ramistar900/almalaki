package com.almalaki.cafe

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.animateFloat
import androidx.compose.runtime.infiniteRepeatable
import androidx.compose.runtime.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.ShoppingCart

@Composable
fun CustomerScreen(
    darkMode: Boolean,
    onTheme: () -> Unit,
    onOwner: () -> Unit,
    onDeveloper: () -> Unit
) {
    val context = LocalContext.current
    val customerScope = rememberCoroutineScope()
    var customerSyncGeneration by remember { mutableStateOf(0) }
    var customerIdentityReady by remember { mutableStateOf(false) }
    var syncedCustomerUserId by remember { mutableStateOf<String?>(null) }
var showCustomerSettings by remember {
    mutableStateOf(false)
}
var customerProfile by remember(context) {
    mutableStateOf(loadRoyalCustomerProfile(context))
}
var customerOrderHistory by remember(context) {
    mutableStateOf(loadRoyalCustomerOrders(context))
}
var activeRestaurant by remember(context) {
    mutableStateOf(loadRoyalActiveRestaurant(context))
}
var brandingRevision by remember(activeRestaurant.id) {
    mutableStateOf(0)
}
var lastRemoteBranding by remember(activeRestaurant.id) {
    mutableStateOf<RoyalRestaurantBranding?>(null)
}
var interfaceSettings by remember(activeRestaurant.id, customerIdentityReady) {
    mutableStateOf(
        if (customerIdentityReady) loadRoyalInterfaceSettings(context, activeRestaurant.id)
        else RoyalInterfaceSettings()
    )
}
var linkedRestaurants by remember(context) {
    mutableStateOf(loadRoyalLinkedRestaurants(context))
}
var showCustomerAccount by remember {
    mutableStateOf(false)
}
var showCustomerProfileEditor by remember {
    mutableStateOf(false)
}
var showRestaurantLinks by remember {
    mutableStateOf(false)
}
var restaurantLinkLoading by remember {
    mutableStateOf(false)
}
var restaurantLinkMessage by remember {
    mutableStateOf("")
}
var restaurantLinkCode by remember { mutableStateOf("") }
var showRestaurantLinkSuccess by remember { mutableStateOf(false) }

fun linkRestaurantFromHome() {
    val code = restaurantLinkCode.trim()
    if (code.isBlank() || restaurantLinkLoading) return
    restaurantLinkLoading = true
    restaurantLinkMessage = ""
    customerScope.launch {
        try {
            val linked = withContext(Dispatchers.IO) {
                linkRoyalRestaurant(context, code)
            }
            linkedRestaurants = loadRoyalLinkedRestaurants(context)
            selectedCategory = "الكل"
            cart = emptyMap()
            activeRestaurant = linked
            restaurantLinkCode = ""
            showRestaurantLinkSuccess = true
        } catch (error: Exception) {
            restaurantLinkMessage = error.message ?: "تعذر ربط المطعم."
        } finally {
            restaurantLinkLoading = false
        }
    }
}
var showCustomerOrders by remember {
    mutableStateOf(false)
}
var showCustomerReservation by remember {
    mutableStateOf(false)
}

fun syncCustomerAccount() {
    customerSyncGeneration += 1
    val syncGeneration = customerSyncGeneration

    customerScope.launch {
        val currentUserId = RoyalSupabaseAuth.currentUserId()
        if (syncGeneration != customerSyncGeneration) return@launch
        if (currentUserId != syncedCustomerUserId) customerIdentityReady = false

        if (currentUserId == null) {
            clearRoyalCustomerLocalAccount(context)
            customerProfile = RoyalCustomerProfile()
            customerOrderHistory = emptyList()
            saveRoyalLinkedRestaurants(context, emptyList())
            linkedRestaurants = loadRoyalLinkedRestaurants(context)
            val defaultRestaurant = RoyalLinkedRestaurant(ROYAL_DEFAULT_RESTAURANT_ID, "الملكي")
            selectRoyalRestaurant(context, defaultRestaurant)
            activeRestaurant = defaultRestaurant
            syncedCustomerUserId = null
            customerIdentityReady = true
            return@launch
        }

        prepareRoyalCustomerLocalAccount(context, currentUserId)
        if (
            syncGeneration != customerSyncGeneration ||
            RoyalSupabaseAuth.currentUserId() != currentUserId
        ) return@launch

        customerProfile = loadRoyalCustomerProfile(context)
        customerOrderHistory = loadRoyalCustomerOrders(context)
        linkedRestaurants = loadRoyalLinkedRestaurants(context)
        activeRestaurant = loadRoyalActiveRestaurant(context)
        syncedCustomerUserId = currentUserId
        customerIdentityReady = true

        // Device registration is best-effort; it must never block account data sync.
        withContext(Dispatchers.IO) {
            runCatching { RoyalCustomerCloud.registerCurrentDevice(context) }
        }
        if (
            syncGeneration != customerSyncGeneration ||
            RoyalSupabaseAuth.currentUserId() != currentUserId
        ) return@launch

        try {
            val snapshot = withContext(Dispatchers.IO) {
                RoyalCustomerCloud.loadSnapshot()
            }
            // A delayed response from a previous account must never overwrite
            // the local cache or UI after a different account signs in.
            if (
                syncGeneration != customerSyncGeneration ||
                RoyalSupabaseAuth.currentUserId() != currentUserId
            ) return@launch

            snapshot.profile?.let {
                saveRoyalCustomerProfile(context, it)
                customerProfile = it
            }
            saveRoyalLinkedRestaurants(context, snapshot.restaurants)
            linkedRestaurants = loadRoyalLinkedRestaurants(context)
            activeRestaurant = loadRoyalActiveRestaurant(context)
            customerOrderHistory = snapshot.orders
        } catch (_: Exception) {
            // Keep the local copy available if the network is temporarily unavailable.
        }
    }
}

LaunchedEffect(Unit) {
    syncCustomerAccount()
    RoyalSupabaseAuth.sessionStatusEvents().collect {
        // Also sync on sign-out/expired sessions so local account data is cleared.
        syncCustomerAccount()
    }
}

LaunchedEffect(activeRestaurant.id, customerIdentityReady) {
    if (!customerIdentityReady) return@LaunchedEffect
    if (activeRestaurant.id.isBlank()) return@LaunchedEffect
    suspend fun refreshBranding() {
        try {
            val remoteBranding = withContext(Dispatchers.IO) {
                loadAndCacheRoyalRestaurantBranding(context, activeRestaurant.id)
            }
            if (remoteBranding != null && remoteBranding != lastRemoteBranding) {
                lastRemoteBranding = remoteBranding
                brandingRevision += 1
            }
            interfaceSettings = loadRoyalInterfaceSettings(context, activeRestaurant.id)
        } catch (_: Exception) {
            // Keep the current tenant-scoped cache when the network is unavailable.
        }
    }
    refreshBranding()
    while (true) {
        delay(60_000L)
        refreshBranding()
    }
}

DisposableEffect(activeRestaurant.id, customerIdentityReady) {
    if (customerIdentityReady && activeRestaurant.id.isNotBlank()) {
        startRoyalRestaurantBrandingRealtime(activeRestaurant.id) {
            customerScope.launch {
                try {
                    val remoteBranding = withContext(Dispatchers.IO) {
                        loadAndCacheRoyalRestaurantBranding(context, activeRestaurant.id)
                    }
                    if (remoteBranding != null && remoteBranding != lastRemoteBranding) {
                        lastRemoteBranding = remoteBranding
                        brandingRevision += 1
                    }
                    interfaceSettings = loadRoyalInterfaceSettings(context, activeRestaurant.id)
                } catch (_: Exception) {
                    // Periodic refresh remains as a fallback.
                }
            }
        }
    }
    onDispose { stopRoyalRestaurantBrandingRealtime() }
}

    var products by remember {
        mutableStateOf<List<Product>>(emptyList())
    }
    var customerSections by remember {
    mutableStateOf<List<CustomerSection>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var message by remember {
        mutableStateOf("")
    }

    var cart by remember {
        mutableStateOf<Map<Int, Int>>(emptyMap())
    }

    var showCart by remember {
        mutableStateOf(false)
    }

    var showCheckout by remember {
        mutableStateOf(false)
    }

    var orderLoading by remember {
        mutableStateOf(false)
    }

    var orderMessage by remember {
        mutableStateOf("")
    }

    var orderSuccessNumber by remember {
        mutableStateOf("")
    }
var showInvoice by remember {
    mutableStateOf(false)
}

var invoiceOrderNumber by remember {
    mutableStateOf("")
}

var invoiceCustomerName by remember {
    mutableStateOf("")
}

var invoiceCustomerPhone by remember {
    mutableStateOf("")
}

var invoiceFulfillmentType by remember {
    mutableStateOf("")
}

var invoiceDeliveryAddress by remember {
    mutableStateOf("")
}

var invoiceItems by remember {
    mutableStateOf<List<RoyalInvoiceItem>>(emptyList())
}

var invoiceTotalAmount by remember {
    mutableStateOf(0.0)
}

LaunchedEffect(customerIdentityReady) {
    if (!customerIdentityReady) {
        showCustomerSettings = false
        showCustomerAccount = false
        showCustomerProfileEditor = false
        showRestaurantLinks = false
        showCustomerOrders = false
        showCustomerReservation = false
        showCart = false
        showCheckout = false
        showInvoice = false
        cart = emptyMap()
        products = emptyList()
        customerSections = emptyList()
        loading = true
    }
}

LaunchedEffect(syncedCustomerUserId) {
    // Clear transient customer-only UI whenever the authenticated account changes,
    // even if the previous and next accounts selected the same restaurant.
    showCustomerSettings = false
    showCustomerAccount = false
    showCustomerProfileEditor = false
    showRestaurantLinks = false
    showCustomerOrders = false
    showCustomerReservation = false
    showCart = false
    showCheckout = false
    showInvoice = false
    cart = emptyMap()
    orderMessage = ""
    orderSuccessNumber = ""
}
    /*
     * البحث والتصنيف
     */
    var searchQuery by remember {
        mutableStateOf("")
    }

    var selectedCategory by remember {
        mutableStateOf("الكل")
    }

    LaunchedEffect(activeRestaurant.id, activeRestaurant.joinCode, customerIdentityReady) {
        // Do not load a previously selected tenant until the current account
        // has been identified and its local cache has been prepared.
        if (!customerIdentityReady) {
            loading = true
            products = emptyList()
            customerSections = emptyList()
            cart = emptyMap()
            return@LaunchedEffect
        }
        // Clear the previous tenant's content immediately; never keep its menu
        // or cart visible while the next restaurant is loading.
        loading = true
        message = ""
        products = emptyList()
        customerSections = emptyList()
        cart = emptyMap()
        selectedCategory = "الكل"
        showCart = false
        showCheckout = false
        try {
            val (loadedProducts, loadedSections) = withContext(Dispatchers.IO) {
                val productsForRestaurant = if (
                    activeRestaurant.id == ROYAL_DEFAULT_RESTAURANT_ID
                ) {
                    loadProducts()
                } else {
                    loadProductsForRestaurantCode(activeRestaurant.joinCode)
                }
                val sectionsForRestaurant = try {
                    if (activeRestaurant.id == ROYAL_DEFAULT_RESTAURANT_ID) {
                        loadCustomerSections()
                    } else {
                        loadCustomerSectionsForRestaurantCode(activeRestaurant.joinCode)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    emptyList()
                }
                productsForRestaurant to sectionsForRestaurant
            }

            // LaunchedEffect cancels the previous load when the active restaurant changes.
            // Publish results only if this effect is still the active, non-cancelled request.
            products = loadedProducts
            customerSections = loadedSections
            cart = emptyMap()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            products = emptyList()
            customerSections = emptyList()
            message = e.message ?: "تعذر تحميل منتجات المطعم."
        } finally {
            loading = false
        }
    }

    val background =
        if (darkMode) {
            Black
        } else {
            Color(0xFFF7F2E8)
        }

    val textColor =
        if (darkMode) {
            Cream
        } else {
            Color(0xFF222222)
        }

    val totalAmount =
        products.sumOf { product ->
            product.price * (cart[product.id] ?: 0)
        }

    val totalItems =
        cart.values.sum()

    
    /*
     * تحديد التصنيفات الظاهرة بحسب إعدادات المالك
     */
    val sectionCategoryNames =
        customerSections
            .flatMap { it.categoryNames }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    val enabledSectionCategoryNames =
        customerSections
            .filter { it.enabled }
            .flatMap { it.categoryNames }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    /*
     * إخفاء منتجات الأقسام المعطلة عن العميل
     */
    val visibleProducts =
        products.filter { product ->
            val category = product.category.trim()

            category !in sectionCategoryNames ||
                category in enabledSectionCategoryNames
        }


    /*
     * التصنيفات المتاحة للعميل
     */
    val categories =
        remember(visibleProducts, customerSections) {
            // اعرض التصنيفات المفعّلة من إعدادات المطعم حتى لو لم توجد منتجات بعد.
            val configuredCategories = customerSections
                .filter { it.enabled }
                .flatMap { it.categoryNames }
            val productCategories = visibleProducts.map { it.category }
            (configuredCategories + productCategories)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        }
        

    /*
     * المنتجات بعد تطبيق البحث والإخفاء
     */
    val filteredProducts =
    visibleProducts.filter { product ->
        val query = searchQuery.trim()

        val matchesSearch =
            query.isEmpty() ||
                product.name.contains(
                    query,
                    ignoreCase = true
                ) ||
                product.category.contains(
                    query,
                    ignoreCase = true
                )

        val matchesCategory =
            selectedCategory == "الكل" ||
                product.category.trim() == selectedCategory

        matchesSearch && matchesCategory
    }
       if (showCustomerSettings && customerIdentityReady) {
    RoyalCustomerSettings(
        isDarkMode = darkMode,
        onBack = {
            showCustomerSettings = false
        },
        onAccountClick = {
            customerProfile = loadRoyalCustomerProfile(context)
            showCustomerAccount = true
        },
        onOrdersClick = {
            customerOrderHistory = loadRoyalCustomerOrders(context)
            showCustomerOrders = true
        },
        onThemeToggle = onTheme
    )
    return
       } 
        
        RoyalCustomerInterface(
            brandingRevision = brandingRevision,
            restaurantId = if (customerIdentityReady) activeRestaurant.id else "",
            isDarkMode = darkMode,
            newsText = interfaceSettings.newsText,
            showCartBanner = totalItems > 0,
            cartCount = totalItems,
            onCartClick = {
                if (customerIdentityReady) showCart = true
            },
            onOrdersClick = {
                if (customerIdentityReady) {
                    customerOrderHistory = loadRoyalCustomerOrders(context)
                    showCustomerOrders = true
                }
            },
            onAccountClick = {
                if (customerIdentityReady) {
                    customerProfile = loadRoyalCustomerProfile(context)
                    showCustomerAccount = true
                }
            },
            onReservationClick = {
                if (customerIdentityReady) {
                    if (RoyalSupabaseAuth.currentUserId() == null) {
                        showCustomerAccount = true
                    } else if (activeRestaurant.id == ROYAL_DEFAULT_RESTAURANT_ID) {
                        customerProfile = loadRoyalCustomerProfile(context)
                        showCustomerProfileEditor = true
                    } else {
                        showCustomerReservation = true
                    }
                }
            },
            onDeveloperLogin = onDeveloper,
            onOwnerLogin = onOwner,
            onSettings = {
                if (customerIdentityReady) showCustomerSettings = true
            },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it }
        ) {        
    Column(
    Modifier
        .fillMaxSize()
        .padding(
            start = 14.dp,
            end = 14.dp
        )
) {



        /*
         * عرض المنتجات مجمّعة حسب التصنيف
         */
        // تبقى مساحة المنتجات ضمن نفس الواجهة دائمًا؛ عند فراغ القائمة لا نظهر بطاقة تنبيه.
                BoxWithConstraints(
                    Modifier.weight(1f)
                ) {
                    // ارتفاع تقريبي مطابق لبنر الاسم، لتبقى التصنيفات تحته.
                    val titleBannerHeight = (maxWidth / 2.6f) + 6.dp
                    val columns = when {
                        maxWidth < 600.dp -> 3
                        maxWidth < 900.dp -> 4
                        maxWidth < 1400.dp -> 5
                        else -> 6
                    }
                    // احسب أبعاد كبسولات التصنيفات ضمن نطاق BoxWithConstraints.
                    val compactCategories = maxWidth < 480.dp
                    val categoryPillWidth = if (compactCategories) {
                        ((maxWidth - 36.dp) / 4f).coerceIn(78.dp, 118.dp)
                    } else {
                        ((maxWidth - 40.dp) / 4.15f).coerceIn(150.dp, 320.dp)
                    }

                    val groupedProducts = filteredProducts
                        .groupBy { it.category.trim() }
                        .toList()

                    if (activeRestaurant.id == ROYAL_DEFAULT_RESTAURANT_ID) {
                        // Direct restaurant linking stays in the existing empty product area.
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    top = titleBannerHeight + 62.dp,
                                    bottom = 12.dp,
                                    start = 18.dp,
                                    end = 18.dp
                                ),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                value = restaurantLinkCode,
                                onValueChange = { restaurantLinkCode = it.uppercase() },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !restaurantLinkLoading,
                                label = { Text("رمز المطعم") },
                                placeholder = { Text("ROYAL-…") }
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = ::linkRestaurantFromHome,
                                enabled = restaurantLinkCode.isNotBlank() && !restaurantLinkLoading,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Gold,
                                    contentColor = Black
                                )
                            ) {
                                if (restaurantLinkLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Black,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("ربط المطعم", fontWeight = FontWeight.Bold)
                                }
                            }
                            if (restaurantLinkMessage.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = restaurantLinkMessage,
                                    color = Color(0xFFFF7777),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "يرجى الربط",
                                color = Gold,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "أدخل رمز الربط الذي يعطيك إياه مالك المطعم لعرض منتجاته وأسعاره.",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                color = textColor.copy(alpha = 0.82f),
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                         top = titleBannerHeight + 62.dp,
                         bottom = 120.dp
                     ),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        groupedProducts.forEach { (category, categoryProducts) ->

                            item(
                                key = "category_$category",
                                span = {
                                    androidx.compose.foundation.lazy.grid.GridItemSpan(
                                        maxLineSpan
                                    )
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RoyalAnimatedIcon(
                                            iconRes = royalFoodIconResource(category),
                                            contentDescription = category,
                                            modifier = Modifier.size(34.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = category.ifBlank { "منتجات أخرى" },
                                            color = Gold,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }

                                    HorizontalDivider(
                                        color = Gold.copy(alpha = 0.45f),
                                        thickness = 1.dp
                                    )
                                }
                            }

                            items(
                                categoryProducts,
                                key = { it.id }
                            ) { product ->
                                ProductCard(
                                    product = product,
                                    quantity = cart[product.id] ?: 0,
                                    onAdd = {
                                        val old = cart[product.id] ?: 0
                                        cart = cart + (product.id to old + 1)
                                    },
                                    onRemove = {
                                        val old = cart[product.id] ?: 0
                                        cart = if (old <= 1) {
                                            cart - product.id
                                        } else {
                                            cart + (product.id to old - 1)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // التصنيفات ثابتة فوق الشبكة؛ تمر البطاقات خلفها عند التمرير.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(top = titleBannerHeight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 2.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val categoryItems = (listOf("الكل") + categories.filterNot { it == "الكل" }).distinct()
                            categoryItems.forEach { category ->
                                RoyalCategoryPill(
                                    category = category,
                                    selected = selectedCategory == category,
                                    width = categoryPillWidth,
                                    compact = compactCategories,
                                    onClick = { selectedCategory = category }
                                )
                            }
                        }
                        HorizontalDivider(
                            color = Gold.copy(alpha = 0.35f),
                            thickness = 1.dp
                        )
                    }
                    }
                }
        
        
        /*
         * =================================================
         * بطاقة السلة — ترتيب داخل البنر الأصلي
         * =================================================
         */
        if (totalItems > 0) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .height(88.dp)
                    .clip(
                        androidx.compose.foundation.shape
                            .RoundedCornerShape(16.dp)
                    )
            ) {

                // الحفاظ على صورة البنر الأصلية
                Image(
                    painter = painterResource(
                        id = R.drawable.royal_cart_banner
                    ),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // المحتوى فوق الصورة، دون حذفها
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(alpha = 0.30f)
                        )
                        .padding(
                            horizontal = 12.dp,
                            vertical = 5.dp
                        )
                ) {

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // الأعلى: السلة داخل إطار ذهبي وعدد المنتجات
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .border(
                                        width = 1.5.dp,
                                        color = Gold,
                                        shape = androidx.compose.foundation.shape
                                            .RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
    imageVector = Icons.Filled.ShoppingCart,
    contentDescription = null,
    tint = Gold,
    modifier = Modifier.size(18.dp)
)
                            }

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Text(
                                text = "$totalItems منتجات",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // الأسفل: المبلغ داخل إطار وزر أنحف
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {

                                Text(
                                    text = "المبلغ المطلوب",
                                    color = GoldLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Box(
                                    modifier = Modifier
                                        .border(
                                            width = 1.5.dp,
                                            color = Gold,
                                            shape = androidx.compose.foundation.shape
                                                .RoundedCornerShape(8.dp)
                                        )
                                        .padding(
                                            horizontal = 8.dp,
                                            vertical = 1.dp
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = formatPrice(totalAmount),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    showCart = true
                                },
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(
                                    horizontal = 10.dp,
                                    vertical = 0.dp
                                ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Gold,
                                    contentColor = Black
                                ),
                                shape = androidx.compose.foundation.shape
                                    .RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = " عرض المشتريات ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
        

    /*
     * نافذة السلة
     */
    if (showCart) {

        CartDialog(
            products = products,
            cart = cart,
            totalAmount = totalAmount,

            onClose = {
                showCart = false
            },

            onCheckout = {
                showCart = false
                orderMessage = ""
                orderSuccessNumber = ""
                showCheckout = true
            },

            onIncrease = { id ->

                val old =
                    cart[id] ?: 0

                cart =
                    cart +
                        (
                            id
                                to old + 1
                        )
            },

            onDecrease = { id ->

                val old =
                    cart[id] ?: 0

                cart =
                    if (old <= 1) {
                        cart - id
                    } else {
                        cart +
                            (
                                id
                                    to old - 1
                            )
                    }
            }
        )
    }

    /*
     * نافذة تأكيد الطلب
     */
    if (showCheckout) {

        CheckoutDialog(
            totalAmount = totalAmount,

            loading = orderLoading,

            message = orderMessage,

            successOrderNumber =
                orderSuccessNumber,

            onClose = {
                if (!orderLoading) {
                    showCheckout = false
                }
            },

            onConfirm = {
                    name,
                    phone,
                    address,
                    type,
                    paymentMethod ->

                when {

                    orderLoading -> Unit

       name.trim().isEmpty() -> {
                        orderMessage =
                            "اكتب اسمك."
                    }

                    type == "توصيل إلى المنزل" &&
                        address.trim().isEmpty() -> {

                        orderMessage =
                            "اكتب عنوان التوصيل."
                    }

                    else -> {
val selectedRestaurantId = activeRestaurant.id
val selectedRestaurantCode = activeRestaurant.joinCode
    .ifBlank { ROYAL_DEFAULT_RESTAURANT_CODE }

// Persist one key per exact customer/store/cart attempt. The same key is reused
// after timeouts, but another signed-in customer receives a different key.
val checkoutPrefs = context.getSharedPreferences(
    "royal_checkout_idempotency",
    android.content.Context.MODE_PRIVATE
)
val checkoutSignature = org.json.JSONObject()
    .put("customer_user_id", RoyalSupabaseAuth.currentUserId().orEmpty())
    .put("name", name.trim())
    .put("phone", phone.trim())
    .put("address", address.trim())
    .put("type", type)
    .put("restaurant", selectedRestaurantCode.trim().uppercase())
    .put("cart", org.json.JSONArray().also { items ->
        cart.filterValues { it > 0 }.toSortedMap().forEach { (id, quantity) ->
            items.put(
                org.json.JSONObject()
                    .put("product_id", id)
                    .put("quantity", quantity)
            )
        }
    })
    .toString()

val storedSignature = checkoutPrefs.getString("active_checkout_signature", null)
val existingKey = checkoutPrefs.getString("active_checkout_key", null)
val idempotencyKey = if (storedSignature == checkoutSignature && !existingKey.isNullOrBlank()) {
    existingKey
} else {
    UUID.randomUUID().toString().also { newKey ->
        val saved = checkoutPrefs.edit()
            .putString("active_checkout_key", newKey)
            .putString("active_checkout_signature", checkoutSignature)
            .commit()
        if (!saved) {
            orderMessage = "تعذر حفظ محاولة الطلب بأمان. أعد المحاولة."
            return@CheckoutDialog
        }
    }
}
                        orderLoading = true
                        orderMessage = ""

                        Thread {

                            try {

                                val result =
                                    createOrder(
                                        customerName = name,
                                        customerPhone = phone,
                                        deliveryAddress = address,
                                        fulfillmentType = type,
                                        totalAmount = totalAmount,
                                        products = products,
                                        cart = cart,
                                        restaurantId = selectedRestaurantId,
                                        restaurantCode = selectedRestaurantCode,
                                        paymentMethod = paymentMethod,
                                        idempotencyKey = idempotencyKey
                                    )

                                invoiceOrderNumber = result.orderNumber
                                invoiceCustomerName = name
                                invoiceCustomerPhone = phone
                                invoiceFulfillmentType = type
                                invoiceDeliveryAddress = address
                                invoiceItems = result.items
                                invoiceTotalAmount = result.totalAmount

                                val savedProfile = RoyalCustomerProfile(
                                    name = name,
                                    phone = phone,
                                    address = address,
                                    fulfillmentType = type
                                )
                                saveRoyalCustomerProfile(context, savedProfile)
                                if (RoyalSupabaseAuth.currentUserId() != null) {
                                    runCatching { RoyalCustomerCloud.saveProfile(savedProfile) }
                                }
                                customerProfile = savedProfile

                                val orderSaved = saveRoyalCustomerOrder(
                                    context,
                                    RoyalCustomerOrderRecord(
                                        orderNumber = result.orderNumber,
                                        customerName = name,
                                        customerPhone = phone,
                                        address = address,
                                        fulfillmentType = type,
                                        totalAmount = result.totalAmount,
                                        items = result.items
                                    )
                                )
                                if (!orderSaved) {
                                    throw Exception(
                                        "تم تسجيل الطلب على الخادم، لكن تعذر حفظ نسخة محلية آمنة. أعد المحاولة من النافذة نفسها؛ لن يُنشأ طلب مكرر."
                                    )
                                }
                                customerOrderHistory = loadRoyalCustomerOrders(context)

                                // Keep the key until the server response and local order are both saved.
                                checkoutPrefs.edit()
                                    .remove("active_checkout_key")
                                    .remove("active_checkout_signature")
                                    .commit()

                                orderSuccessNumber = result.orderNumber
                                cart = emptyMap()

                                showCheckout = false
                                showInvoice = true

                            } catch (e: Exception) {

                                orderMessage =
                                    e.message
                                        ?: "حدث خطأ أثناء إرسال الطلب."

                            } finally {

                                orderLoading = false
                            }

                        }.start()
                    }
                }
            }
        )
    }

if (showInvoice) {
    RoyalProfessionalInvoice(
        orderNumber = invoiceOrderNumber,
        customerName = invoiceCustomerName,
        customerPhone = invoiceCustomerPhone,
        fulfillmentType = invoiceFulfillmentType,
        deliveryAddress = invoiceDeliveryAddress,
        items = invoiceItems,
        totalAmount = invoiceTotalAmount,
        onClose = {
            showInvoice = false
        }
    )
}


if (showCustomerAccount) {
    RoyalCustomerAccountPortal(
        isDarkMode = darkMode,
        onDismiss = { showCustomerAccount = false },
        onCustomerDetails = {
            customerProfile = loadRoyalCustomerProfile(context)
            showCustomerAccount = false
            showCustomerProfileEditor = true
        },
        onAuthChanged = { syncCustomerAccount() }
    )
}

if (showCustomerProfileEditor) {
    RoyalCustomerAccountDialog(
        isDarkMode = darkMode,
        profile = customerProfile,
        onDismiss = { showCustomerProfileEditor = false },
        onSave = { profile ->
            saveRoyalCustomerProfile(context, profile)
            customerProfile = profile
            showCustomerProfileEditor = false
            if (RoyalSupabaseAuth.currentUserId() != null) {
                customerScope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            RoyalCustomerCloud.saveProfile(profile)
                        }
                    }
                }
            }
        },
        onManageRestaurants = {
            showCustomerProfileEditor = false
            linkedRestaurants = loadRoyalLinkedRestaurants(context)
            activeRestaurant = loadRoyalActiveRestaurant(context)
            restaurantLinkMessage = ""
            showRestaurantLinks = true
        }
    )
}

if (showRestaurantLinkSuccess) {
    val linkPulse by rememberInfiniteTransition(label = "restaurant-link-success")
        .animateFloat(
            initialValue = 1f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 850),
                repeatMode = RepeatMode.Reverse
            ),
            label = "restaurant-link-success-pulse"
        )
    AlertDialog(
        onDismissRequest = { showRestaurantLinkSuccess = false },
        containerColor = if (darkMode) Color(0xFF111111) else Color.White,
        title = {
            Text(
                "تم ربط المطعم بنجاح",
                color = Gold,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.royal_purchase_started),
                    contentDescription = "أيقونة نجاح ربط المطعم",
                    modifier = Modifier
                        .size(96.dp)
                        .graphicsLayer(scaleX = linkPulse, scaleY = linkPulse)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "تم الاتصال بـ ${activeRestaurant.name}، وستظهر منتجاته في الواجهة.",
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { showRestaurantLinkSuccess = false }) {
                Text("متابعة", color = Gold)
            }
        }
    )
}

if (showRestaurantLinks) {
    RoyalRestaurantLinksDialog(
        isDarkMode = darkMode,
        activeRestaurant = activeRestaurant,
        restaurants = linkedRestaurants,
        loading = restaurantLinkLoading,
        message = restaurantLinkMessage,
        onLink = { code ->
            if (!restaurantLinkLoading) {
                restaurantLinkLoading = true
                restaurantLinkMessage = ""
                Thread {
                    try {
                        val linked = linkRoyalRestaurant(context, code)
                        linkedRestaurants = loadRoyalLinkedRestaurants(context)
                        selectedCategory = "الكل"
                        activeRestaurant = linked
                        restaurantLinkMessage = ""
                        showRestaurantLinks = false
                    } catch (e: Exception) {
                        restaurantLinkMessage = e.message ?: "تعذر ربط المطعم."
                    } finally {
                        restaurantLinkLoading = false
                    }
                }.start()
            }
        },
        onSelect = { restaurant ->
            selectRoyalRestaurant(context, restaurant)
            selectedCategory = "الكل"
            cart = emptyMap()
            activeRestaurant = restaurant
            restaurantLinkMessage = ""
            showRestaurantLinks = false
        },
        onDismiss = {
            if (!restaurantLinkLoading) showRestaurantLinks = false
        }
    )
}

if (showCustomerReservation) {
    CustomerReservationDialog(
        restaurantId = activeRestaurant.id,
        profile = customerProfile,
        onDismiss = { showCustomerReservation = false },
        onSubmitted = {
            showCustomerReservation = false
            orderMessage = "تم إرسال طلب الحجز، بانتظار تأكيد المطعم."
        }
    )
}

if (showCustomerOrders) {
    RoyalCustomerOrdersDialog(
        isDarkMode = darkMode,
        orders = customerOrderHistory,
        onDismiss = {
            showCustomerOrders = false
        }
    )
}
}
        }
}


/**
 * كبسولة تصنيف بتصميم Royal: قاعدة كريمية بإطار ذهبي،
 * اسم التصنيف داخل كبسولة خضراء داكنة وأيقونة ذهبية يسارًا.
 */
@Composable
private fun RoyalCategoryPill(
    category: String,
    selected: Boolean,
    width: androidx.compose.ui.unit.Dp,
    compact: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .width(width)
            .height(if (compact) 42.dp else 56.dp)
            .clip(shape)
            .background(Color(0xFFFFF5DF))
            .border(
                width = if (selected) 2.dp else 1.5.dp,
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(Color(0xFFFFE9A3), Color(0xFFB68A31), Color(0xFFFFD96A))
                ),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(if (compact) 3.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 25.dp else 38.dp),
            contentAlignment = Alignment.Center
        ) {
            RoyalAnimatedIcon(
                iconRes = royalFoodIconResource(category),
                contentDescription = category,
                modifier = Modifier.size(if (compact) 21.dp else 32.dp)
            )
        }
        Spacer(Modifier.width(if (compact) 2.dp else 4.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(if (compact) 32.dp else 44.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (selected) Color(0xFF063B2B) else Color(0xFF082E24)
                )
                .border(
                    1.dp,
                    Color(0xFFD4AF56),
                    RoundedCornerShape(50)
                )
                .padding(horizontal = if (compact) 2.dp else 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category,
                color = Color(0xFFFFE6A0),
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 9.sp else 14.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
