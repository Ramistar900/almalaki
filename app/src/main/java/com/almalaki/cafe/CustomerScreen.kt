package com.almalaki.cafe

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
    var products by remember {
        mutableStateOf<List<Product>>(emptyList())
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

    LaunchedEffect(Unit) {
        Thread {
            try {
                products = loadProducts()
            } catch (e: Exception) {
                message =
                    e.message ?: "تعذر تحميل المنتجات."
            }

            loading = false
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
     * استخراج التصنيفات الموجودة فعليًا في المنتجات
     */
    val categories =
        remember(products) {
            listOf("الكل") +
                products
                    .map { it.category.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
        }

    /*
     * المنتجات بعد تطبيق البحث والتصنيف
     */
    val filteredProducts =
        products.filter { product ->

            val matchesCategory =
                selectedCategory == "الكل" ||
                    product.category.trim() == selectedCategory

            val query =
                searchQuery.trim()

            val matchesSearch =
                query.isEmpty() ||
                    product.name.contains(
                        query,
                        ignoreCase = true
                    )

            matchesCategory && matchesSearch
        }

        RoyalCustomerInterface(
    isDarkMode = darkMode,
    newsText = interfaceSettings.newsText,
    onOwnerLogin = onOwner
) {

        Column(
            Modifier
                .fillMaxSize()
                .background(background)
                .padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = 18.dp
                )
        ) {

        

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        /*
         * خط ذهبي فاخر
         */
        HorizontalDivider(
            color = Gold.copy(alpha = 0.35f),
            thickness = 1.dp
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        /*
         * عنوان قسم المنتجات
         */
        RoyalPremiumSectionTitle(
            title = "منتجاتنا",
            subtitle = "اختر ما يناسب ذوقك"
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        /*
         * البحث
         */
        ProductSearchBox(
            query = searchQuery,
            onQueryChange = {
                searchQuery = it
            }
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        /*
         * التصنيفات
         */
        ProductCategoryRow(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = {
                selectedCategory = it
            }
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        when {

            /*
             * التحميل
             */
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
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Gold
                            )

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

            /*
             * خطأ تحميل المنتجات
             */
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
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            RoyalPremiumBadge(
                                text = "تنبيه"
                            )

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

            /*
             * لا توجد نتائج
             */
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
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            RoyalPremiumBadge(
                                text = "لا توجد نتائج"
                            )

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )

                            Text(
                                text =
                                    if (searchQuery.trim().isNotEmpty()) {
                                        "لم نجد منتجًا يطابق بحثك."
                                    } else {
                                        "لا توجد منتجات في هذا التصنيف."
                                    },
                                color =
                                    textColor.copy(
                                        alpha = 0.75f
                                    ),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            /*
             * قائمة المنتجات
             */
            else -> {

                BoxWithConstraints(
                    Modifier.weight(1f)
                ) {

                    val columns =
                        when {
                            maxWidth < 600.dp -> 2
                            maxWidth < 900.dp -> 3
                            maxWidth < 1400.dp -> 4
                            else -> 5
                        }

                    LazyVerticalGrid(
                        columns =
                            GridCells.Fixed(columns),

                        modifier =
                            Modifier.fillMaxSize(),

                        contentPadding =
                            PaddingValues(
                                top = 2.dp,
                                bottom = 12.dp
                            ),

                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp),

                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {

                        items(
                            filteredProducts,
                            key = { it.id }
                        ) { product ->

                            ProductCard(
                                product = product,

                                quantity =
                                    cart[product.id] ?: 0,

                                onAdd = {
                                    val old =
                                        cart[product.id] ?: 0

                                    cart =
                                        cart +
                                            (
                                                product.id
                                                    to old + 1
                                            )
                                },

                                onRemove = {
                                    val old =
                                        cart[product.id] ?: 0

                                    cart =
                                        if (old <= 1) {
                                            cart - product.id
                                        } else {
                                            cart +
                                                (
                                                    product.id
                                                        to old - 1
                                                )
                                        }
                                }
                            )
                        }
                    }
                }
            }
        }

        /*
         * بطاقة السلة
         */
        if (totalItems > 0) {

            RoyalPremiumCard(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text = "🛒",
                                fontSize = 18.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.padding(
                                        horizontal = 3.dp
                                    )
                            )

                            Text(
                                text =
                                    "$totalItems منتجات",
                                color = Cream,
                                fontSize = 14.sp,
                                fontWeight =
                                    FontWeight.Medium
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text = "المبلغ المطلوب",
                            color =
                                GoldLight.copy(
                                    alpha = 0.72f
                                ),
                            fontSize = 12.sp
                        )

                        Spacer(
                            modifier = Modifier.height(2.dp)
                        )

                        Text(
                            text =
                                formatPrice(totalAmount),
                            color = Gold,
                            fontSize = 19.sp,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            showCart = true
                        },

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = Gold,
                                contentColor = Black
                            ),

                        shape =
                            androidx.compose.foundation.shape
                                .RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "عرض السلة 🛒",
                            fontWeight =
                                FontWeight.SemiBold
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
                                        cart
                                    )

                                invoiceOrderNumber = result.orderNumber
invoiceCustomerName = name
invoiceCustomerPhone = phone
invoiceFulfillmentType = type
invoiceDeliveryAddress = address
invoiceItems = invoiceItemsSnapshot
invoiceTotalAmount = invoiceTotalSnapshot

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
}

