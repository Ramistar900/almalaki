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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

@Composable
fun CustomerScreen(
    darkMode: Boolean,
    onTheme: () -> Unit,
    onOwner: () -> Unit
) {
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

        /*
         * الشعار الملكي
         */
        RoyalLogo()

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        /*
         * رأس الصفحة الملكي
         */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "قائمة Royal",
                    color = Gold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = "طعمٌ يستحق التجربة",
                    color = textColor.copy(alpha = 0.75f),
                    fontSize = 13.sp
                )
            }

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                RoyalDateTime(
                    language = "ar",
                    style = androidx.compose.ui.text.TextStyle(
                        color = Gold,
                        fontSize = 11.sp
                    )
                )

                Text(
                    text =
                        if (darkMode) {
                            "☀"
                        } else {
                            "🌙"
                        },
                    color = Gold,
                    fontSize = 23.sp,
                    modifier =
                        Modifier.padding(
                            start = 8.dp,
                            end = 4.dp
                        )
                )
            }
        }

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
                            products,
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

        /*
         * دخول المالك
         */
        OutlinedButton(
            onClick = onOwner,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),

            shape =
                androidx.compose.foundation.shape
                    .RoundedCornerShape(14.dp)
        ) {

            Text(
                text = "👑 دخول المالك",
                color = Gold,
                fontWeight =
                    FontWeight.Medium
            )
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

                                orderSuccessNumber =
                                    result.orderNumber

                                cart = emptyMap()

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
}
