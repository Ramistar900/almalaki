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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

@Composable
fun CustomerScreen(
    darkMode: Boolean,
    onTheme: () -> Unit,
    onOwner: () -> Unit
) {
    val context = LocalContext.current
val interfaceSettings = remember {
    loadRoyalInterfaceSettings(context)
}
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
var linkedRestaurants by remember(context) {
    mutableStateOf(loadRoyalLinkedRestaurants(context))
}
var showCustomerAccount by remember {
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
var showCustomerOrders by remember {
    mutableStateOf(false)
}

if (showCustomerSettings) {
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
        }
    )
    return
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
    /*
     * البحث والتصنيف
     */
    var searchQuery by remember {
        mutableStateOf("")
    }

    var selectedCategory by remember {
        mutableStateOf("الكل")
    }

    LaunchedEffect(activeRestaurant.id, activeRestaurant.joinCode) {
        loading = true
        message = ""
        Thread {
            try {
                val loadedProducts = if (
                    activeRestaurant.id == ROYAL_DEFAULT_RESTAURANT_ID
                ) {
                    loadProducts()
                } else {
                    loadProductsForRestaurantCode(activeRestaurant.joinCode)
                }
                val loadedSections = try {
                    if (activeRestaurant.id == ROYAL_DEFAULT_RESTAURANT_ID) {
                        loadCustomerSections()
                    } else {
                        loadCustomerSectionsForRestaurantCode(activeRestaurant.joinCode)
                    }
                } catch (_: Exception) {
                    emptyList()
                }
                products = loadedProducts
                customerSections = loadedSections
                cart = emptyMap()
            } catch (e: Exception) {
                products = emptyList()
                customerSections = emptyList()
                message = e.message ?: "تعذر تحميل منتجات المطعم."
            } finally {
                loading = false
            }
        }.start()
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
        remember(visibleProducts) {
            visibleProducts
                .map { it.category.trim() }
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
        
        
        RoyalCustomerInterface(
            isDarkMode = darkMode,
            newsText = interfaceSettings.newsText,
            showCartBanner = totalItems > 0,
            cartCount = totalItems,
            onCartClick = {
                showCart = true
            },
            onOrdersClick = {
                customerOrderHistory = loadRoyalCustomerOrders(context)
                showCustomerOrders = true
            },
            onAccountClick = {
                customerProfile = loadRoyalCustomerProfile(context)
                showCustomerAccount = true
            },
            onOwnerLogin = onOwner,
            onSettings = {
                showCustomerSettings = true
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
        when {

            loading -> {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    RoyalPremiumCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Gold)

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )

                            Text(
                                text = "جاري تحميل القائمة...",
                                color = GoldLight,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            message.isNotEmpty() -> {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    RoyalPremiumCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            RoyalPremiumBadge(text = "تنبيه")

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )

                            Text(
                                text = message,
                                color = Color.Red,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            filteredProducts.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    RoyalPremiumCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            RoyalPremiumBadge(text = "لا توجد نتائج")

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )

                            Text(
                                text = if (searchQuery.trim().isNotEmpty()) {
                                    "لم نجد منتجًا يطابق بحثك."
                                } else {
                                    "لا توجد منتجات في القائمة."
                                },
                                color = textColor.copy(alpha = 0.75f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            else -> {
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
                                    Text(
                                        text = category.ifBlank {
                                            "منتجات أخرى"
                                        },
                                        color = Gold,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(
                                            vertical = 4.dp
                                        )
                                    )

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
                                Text(
                                    text = "🛒",
                                    fontSize = 17.sp
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
                    type ->

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
val invoiceItemsSnapshot =
    products.mapNotNull { product ->
        val quantity = cart[product.id] ?: 0

        if (quantity > 0) {
            RoyalInvoiceItem(
                name = product.name,
                quantity = quantity,
                unitPrice = product.price
            )
        } else {
            null
        }
    }

val invoiceTotalSnapshot = totalAmount
                        orderLoading = true
                        orderMessage = ""

                        Thread {

                            try {

                                val result =
                                    createOrder(
                                        name,
                                        phone,
                                        address,
                                        type,
                                        totalAmount,
                                        products,
                                        cart,
                                        restaurantId = selectedRestaurantId
                                    )

                                invoiceOrderNumber = result.orderNumber
                                invoiceCustomerName = name
                                invoiceCustomerPhone = phone
                                invoiceFulfillmentType = type
                                invoiceDeliveryAddress = address
                                invoiceItems = invoiceItemsSnapshot
                                invoiceTotalAmount = invoiceTotalSnapshot

                                val savedProfile = RoyalCustomerProfile(
                                    name = name,
                                    phone = phone,
                                    address = address,
                                    fulfillmentType = type
                                )
                                saveRoyalCustomerProfile(context, savedProfile)
                                customerProfile = savedProfile

                                saveRoyalCustomerOrder(
                                    context,
                                    RoyalCustomerOrderRecord(
                                        orderNumber = result.orderNumber,
                                        customerName = name,
                                        customerPhone = phone,
                                        address = address,
                                        fulfillmentType = type,
                                        totalAmount = invoiceTotalSnapshot,
                                        items = invoiceItemsSnapshot
                                    )
                                )
                                customerOrderHistory = loadRoyalCustomerOrders(context)

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
    RoyalCustomerAccountDialog(
        isDarkMode = darkMode,
        profile = customerProfile,
        onDismiss = {
            showCustomerAccount = false
        },
        onSave = { profile ->
            saveRoyalCustomerProfile(context, profile)
            customerProfile = profile
            showCustomerAccount = false
        },
        onManageRestaurants = {
            showCustomerAccount = false
            linkedRestaurants = loadRoyalLinkedRestaurants(context)
            activeRestaurant = loadRoyalActiveRestaurant(context)
            restaurantLinkMessage = ""
            showRestaurantLinks = true
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
                        restaurantLinkMessage = "تم الربط بنجاح."
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
        },
        onDismiss = {
            if (!restaurantLinkLoading) showRestaurantLinks = false
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
            RoyalCategoryGlyph(
                category = category,
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

/** أيقونات مصممة بالرسم لتبقى متناسقة مع المرجع ولا تتطلب ملفات إضافية. */
@Composable
private fun RoyalCategoryGlyph(
    category: String,
    modifier: Modifier = Modifier
) {
    val normalized = category.trim().lowercase()
    val glyphColor = Color(0xFF765317)
    val kind = when {
        normalized == "الكل" || normalized.contains("جميع") ||
            normalized.contains("all") || normalized.contains("قسم") -> "all"
        normalized.contains("طعام") || normalized.contains("الطعام") ||
            normalized.contains("أكل") || normalized.contains("food") -> "food"
        normalized.contains("مشروب") || normalized.contains("قهوة") ||
            normalized.contains("مشروبات") || normalized.contains("coffee") ||
            normalized.contains("drink") -> "drink"
        normalized.contains("حلو") || normalized.contains("كيك") ||
            normalized.contains("حلويات") || normalized.contains("dessert") ||
            normalized.contains("cake") -> "dessert"
        else -> "all"
    }

    Canvas(modifier = modifier) {
        val unit = size.minDimension / 48f
        fun point(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(x * unit, y * unit)
        val strokeWidth = 2.8f * unit
        val stroke = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)

        when (kind) {
            "all" -> {
                listOf(
                    7f to 7f, 26f to 7f, 7f to 26f, 26f to 26f
                ).forEach { (x, y) ->
                    drawRoundRect(
                        color = glyphColor,
                        topLeft = point(x, y),
                        size = androidx.compose.ui.geometry.Size(13f * unit, 13f * unit),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.2f * unit),
                        style = stroke
                    )
                }
            }
            "food" -> {
                // شوكة
                drawLine(glyphColor, point(11f, 6f), point(11f, 18f), strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(glyphColor, point(6f, 6f), point(6f, 13f), strokeWidth)
                drawLine(glyphColor, point(11f, 6f), point(11f, 13f), strokeWidth)
                drawLine(glyphColor, point(16f, 6f), point(16f, 13f), strokeWidth)
                drawLine(glyphColor, point(6f, 13f), point(16f, 13f), strokeWidth)
                drawLine(glyphColor, point(11f, 18f), point(11f, 42f), strokeWidth)
                // ملعقة
                drawOval(glyphColor, point(29f, 5f), androidx.compose.ui.geometry.Size(10f * unit, 14f * unit), style = stroke)
                drawLine(glyphColor, point(34f, 19f), point(34f, 42f), strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
            "drink" -> {
                val cup = Path().apply {
                    moveTo(10f * unit, 18f * unit)
                    lineTo(38f * unit, 18f * unit)
                    lineTo(33f * unit, 41f * unit)
                    quadraticTo(24f * unit, 44f * unit, 15f * unit, 41f * unit)
                    close()
                }
                drawPath(cup, glyphColor, style = stroke)
                drawLine(glyphColor, point(9f, 17f), point(39f, 17f), strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(glyphColor, point(29f, 16f), point(34f, 5f), strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(glyphColor, point(19f, 27f), point(29f, 27f), strokeWidth)
                drawLine(glyphColor, point(18f, 33f), point(28f, 33f), strokeWidth)
            }
            "dessert" -> {
                // طبقات الكعكة والصحن
                drawLine(glyphColor, point(5f, 41f), point(43f, 41f), strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                val cake = Path().apply {
                    moveTo(10f * unit, 25f * unit)
                    lineTo(38f * unit, 25f * unit)
                    lineTo(38f * unit, 39f * unit)
                    lineTo(10f * unit, 39f * unit)
                    close()
                }
                drawPath(cake, glyphColor, style = stroke)
                val icing = Path().apply {
                    moveTo(10f * unit, 25f * unit)
                    quadraticTo(14f * unit, 19f * unit, 19f * unit, 24f * unit)
                    quadraticTo(24f * unit, 18f * unit, 28f * unit, 24f * unit)
                    quadraticTo(34f * unit, 19f * unit, 38f * unit, 25f * unit)
                }
                drawPath(icing, glyphColor, style = stroke)
                drawLine(glyphColor, point(18f, 29f), point(18f, 35f), strokeWidth)
                drawLine(glyphColor, point(29f, 29f), point(29f, 35f), strokeWidth)
                drawLine(glyphColor, point(24f, 5f), point(24f, 16f), strokeWidth)
                drawCircle(glyphColor, 3f * unit, point(24f, 5f), style = stroke)
                drawLine(glyphColor, point(18f, 16f), point(30f, 16f), strokeWidth)
            }
        }
    }
}
