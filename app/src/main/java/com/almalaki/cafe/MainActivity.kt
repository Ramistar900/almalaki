package com.almalaki.cafe

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.URL

private val Gold = Color(0xFFD4AF37)
private val GoldLight = Color(0xFFFFE9A3)
private val GoldDark = Color(0xFF8C6B16)
private val Black = Color(0xFF050505)
private val CardBlack = Color(0xFF111111)
private val Cream = Color(0xFFF5F0E5)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RoyalCoffeeApp()
        }
    }
}

@Composable
fun RoyalCoffeeApp() {

    val context = LocalContext.current

    val prefs = remember {
        context.getSharedPreferences(
            "royal_settings",
            Context.MODE_PRIVATE
        )
    }

    var darkMode by remember {
        mutableStateOf(
            prefs.getBoolean("dark_mode", true)
        )
    }

    var screen by remember {
        mutableStateOf("customer")
    }

    var token by remember {
        mutableStateOf("")
    }

    MaterialTheme(
        colorScheme =
            if (darkMode) {
                darkColorScheme(
                    primary = Gold,
                    background = Black,
                    surface = Black,
                    onBackground = Cream,
                    onSurface = Cream
                )
            } else {
                lightColorScheme(
                    primary = GoldDark,
                    background = Color(0xFFF7F2E8),
                    surface = Color(0xFFF7F2E8),
                    onBackground = Color(0xFF222222),
                    onSurface = Color(0xFF222222)
                )
            }
    ) {

        when (screen) {

            "customer" -> {
                CustomerScreen(
                    darkMode = darkMode,
                    onTheme = {
                        darkMode = !darkMode

                        prefs.edit()
                            .putBoolean(
                                "dark_mode",
                                darkMode
                            )
                            .apply()
                    },
                    onOwner = {
                        screen = "login"
                    }
                )
            }

            "login" -> {
                LoginScreen(
                    onBack = {
                        screen = "customer"
                    },
                    onSuccess = { newToken ->
                        token = newToken
                        screen = "admin"
                    }
                )
            }

            "admin" -> {
                AdminScreen(
                    accessToken = token,
                    onLogout = {
                        token = ""
                        screen = "customer"
                    }
                )
            }
        }
    }
}

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
                    e.message
                        ?: "تعذر تحميل المنتجات."
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

            val quantity =
                cart[product.id] ?: 0

            product.price * quantity
        }

    val totalItems =
        cart.values.sum()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(
                start = 14.dp,
                end = 14.dp,
                top = 18.dp
            )
    ) {

        RoyalLogo()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column {

                Text(
                    text = "قائمة Royal",
                    color = Gold,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "طعمٌ يستحق التجربة",
                    color = textColor,
                    fontSize = 14.sp
                )
            }

            TextButton(
                onClick = onTheme
            ) {

                Text(
                    text =
                        if (darkMode) "☀"
                        else "🌙",

                    color = Gold,
                    fontSize = 23.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        HorizontalDivider(
            color = Gold.copy(alpha = 0.45f)
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        when {

            loading -> {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator(
                        color = Gold
                    )
                }
            }

            message.isNotEmpty() -> {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text = message,
                        color = Color.Red
                    )
                }
            }

            else -> {

                BoxWithConstraints(
                    modifier = Modifier.weight(1f)
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
                            items = products,
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
                                                product.id to
                                                    old + 1
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
                                                    product.id to
                                                        old - 1
                                                )
                                        }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (totalItems > 0) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),

                shape =
                    RoundedCornerShape(16.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor = CardBlack
                    )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 14.dp,
                            vertical = 10.dp
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically,

                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Column {

                        Text(
                            text =
                                "🛒 $totalItems منتجات",

                            color = Cream,
                            fontSize = 14.sp
                        )

                        Text(
                            text = "المبلغ المطلوب",
                            color = Gold,
                            fontSize = 14.sp
                        )

                        Text(
                            text =
                                formatPrice(totalAmount),

                            color = GoldLight,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
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
                            )
                    ) {

                        Text(
                            text = "عرض السلة 🛒"
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onOwner,

            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {

            Text(
                text = "👑 دخول المالك",
                color = Gold
            )
        }
    }

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
                            id to old + 1
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
                                id to old - 1
                            )
                    }
            }
        )
    }

    if (showCheckout) {

        CheckoutDialog(
            totalAmount = totalAmount,
            loading = orderLoading,
            message = orderMessage,
            successOrderNumber = orderSuccessNumber,

            onClose = {
                if (!orderLoading) {
                    showCheckout = false
                }
            },

            onConfirm = { name, phone, address, type ->

                when {

                    orderLoading -> {
                        // لا نفعل شيئًا
                    }

                    name.trim().isEmpty() -> {
                        orderMessage = "اكتب اسمك."
                    }

                    phone.trim().isEmpty() -> {
                        orderMessage = "اكتب رقم الهاتف."
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
                                        customerName = name,
                                        customerPhone = phone,
                                        deliveryAddress = address,
                                        fulfillmentType = type,
                                        totalAmount = totalAmount,
                                        products = products,
                                        cart = cart
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

@Composable
fun RoyalLogo() {

    val transition =
        rememberInfiniteTransition(
            label = "royal_shine"
        )

    val shinePosition by
        transition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,

            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 2600,
                            easing = LinearEasing
                        ),
                    repeatMode =
                        RepeatMode.Restart
                ),

            label = "shine_position"
        )

    val brush =
        Brush.linearGradient(

            colors =
                listOf(
                    GoldDark,
                    Gold,
                    GoldLight,
                    Color.White,
                    GoldLight,
                    Gold,
                    GoldDark
                ),

            start =
                androidx.compose.ui.geometry.Offset(
                    x = shinePosition * 500f,
                    y = 0f
                ),

            end =
                androidx.compose.ui.geometry.Offset(
                    x = shinePosition * 500f + 500f,
                    y = 0f
                )
        )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),

        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text = "Royal Coffee",

            color = Gold,

            fontSize = 39.sp,

            fontFamily = FontFamily.Cursive,

            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {

    Card(
        modifier
