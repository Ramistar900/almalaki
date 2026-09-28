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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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
                        ?: "تعذر تحميل القائمة."
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

        Spacer(
            modifier = Modifier.height(4.dp)
        )

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
                    fontWeight =
                        FontWeight.SemiBold
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
                        if (darkMode)
                            "☀"
                        else
                            "🌙",
                    color = Gold,
                    fontSize = 23.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        HorizontalDivider(
            color = Gold.copy(
                alpha = 0.45f
            )
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

                            maxWidth < 600.dp ->
                                2

                            maxWidth < 900.dp ->
                                3

                            maxWidth < 1400.dp ->
                                4

                            else ->
                                5
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
                                    cart[product.id]
                                        ?: 0,

                                onAdd = {

                                    val old =
                                        cart[product.id]
                                            ?: 0

                                    cart =
                                        cart +
                                            (
                                                product.id to
                                                    (old + 1)
                                            )
                                },

                                onRemove = {

                                    val old =
                                        cart[product.id]
                                            ?: 0

                                    cart =
                                        if (old <= 1) {

                                            cart -
                                                product.id

                                        } else {

                                            cart +
                                                (
                                                    product.id to
                                                        (old - 1)
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
                    .padding(
                        bottom = 8.dp
                    ),

                shape =
                    RoundedCornerShape(16.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            CardBlack
                    ),

                border =
                    BorderStroke(
                        1.dp,
                        Gold
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
                            text =
                                "المبلغ المطلوب",

                            color = Gold,
                            fontSize = 14.sp,

                            fontWeight =
                                FontWeight.SemiBold
                        )

                        Text(
                            text =
                                formatPrice(
                                    totalAmount
                                ),

                            color = GoldLight,
                            fontSize = 19.sp,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            showCart = true
                        },

                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        Gold,

                                    contentColor =
                                        Black
                                )
                    ) {

                        Text(
                            text =
                                "عرض السلة 🛒",

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onOwner,

            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    bottom = 4.dp
                ),

            border =
                BorderStroke(
                    1.dp,
                    Gold
                )
        ) {

            Text(
                text =
                    "👑 دخول المالك",

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
                            id to
                                (old + 1)
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
                                id to
                                    (old - 1)
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

            successOrderNumber =
                orderSuccessNumber,

            onClose = {

                if (!orderLoading) {
                    showCheckout = false
                }
            },

            onConfirm = { name, phone, address, type ->

                if (orderLoading) {

                    return@CheckoutDialog
                }

                if (name.trim().isEmpty()) {

                    orderMessage =
                        "اكتب اسمك."

                    return@CheckoutDialog
                }

                if (phone.trim().isEmpty()) {

                    orderMessage =
                        "اكتب رقم الهاتف."

                    return@CheckoutDialog
                }

                if (
                    type == "توصيل إلى المنزل" &&
                    address.trim().isEmpty()
                ) {

                    orderMessage =
                        "اكتب عنوان التوصيل."

                    return@CheckoutDialog
                }

                orderLoading = true
                orderMessage = ""

                Thread {

                    try {

                        val result =
                            createOrder(

                                customerName =
                                    name,

                                customerPhone =
                                    phone,

                                deliveryAddress =
                                    address,

                                fulfillmentType =
                                    type,

                                totalAmount =
                                    totalAmount,

                                products =
                                    products,

                                cart =
                                    cart
                            )

                        orderSuccessNumber =
                            result.orderNumber

                        cart =
                            emptyMap()

                    } catch (e: Exception) {

                        orderMessage =
                            e.message
                                ?: "حدث خطأ أثناء إرسال الطلب."

                    } finally {

                        orderLoading = false
                    }

                }.start()
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
                Offset(
                    x = shinePosition * 500f,
                    y = 0f
                ),

            end =
                Offset(
                    x = shinePosition * 500f + 500f,
                    y = 0f
                )
        )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = 2.dp
            ),

        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text = "Royal Coffee",

            style =
                TextStyle(
                    brush = brush
                ),

            fontSize = 39.sp,

            fontFamily =
                FontFamily.Cursive,

            fontWeight =
                FontWeight.Bold
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
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    CardBlack
            ),

        border =
            BorderStroke(
                1.5.dp,
                Gold.copy(
                    alpha = 0.75f
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .clip(
                        RoundedCornerShape(13.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = Gold,
                        shape =
                            RoundedCornerShape(13.dp)
                    )
                    .padding(3.dp)
            ) {

                if (
                    product.imageUrl.isNotBlank()
                ) {

                    RemoteProductImage(
                        url =
                            product.imageUrl
                    )

                } else {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color(0xFF1C1C1C)
                            ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "Royal",
                            color = Gold,
                            fontSize = 24.sp,

                            fontFamily =
                                FontFamily.Cursive
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )

            Text(
                text = product.name,

                color = Cream,
                fontSize = 17.sp,

                fontWeight =
                    FontWeight.SemiBold
            )

            if (
                product.category.isNotBlank()
            ) {

                Text(
                    text =
                        product.category,

                    color = Gold,
                    fontSize = 12.sp
                )
            }

            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )

            Text(
                text =
                    formatPrice(
                        product.price
                    ),

                color = GoldLight,
                fontSize = 15.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            if (quantity == 0) {

                Button(
                    onClick = onAdd,

                    modifier =
                        Modifier.fillMaxWidth(),

                    colors =
                        ButtonDefaults
                            .buttonColors(
                                containerColor =
                                    Gold,

                                contentColor =
                                    Black
                            )
                ) {

                    Text(
                        text =
                            "إضافة إلى السلة 🛒",

                        fontWeight =
                            FontWeight.Bold
                    )
                }

            } else {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically,

                    horizontalArrangement =
                        Arrangement.SpaceEvenly
                ) {

                    SmallCartButton(
                        text = "−",
                        onClick = onRemove
                    )

                    Text(
                        text =
                            quantity.toString(),

                        color = GoldLight,
                        fontSize = 19.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    SmallCartButton(
                        text = "+",
                        onClick = onAdd
                    )
                }
            }
        }
    }
}

@Composable
fun SmallCartButton(
    text: String,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,

        modifier =
            Modifier.size(42.dp),

        contentPadding =
            PaddingValues(0.dp),

        colors =
            ButtonDefaults.buttonColors(
                containerColor =
                    Gold,

                contentColor =
                    Black
            )
    ) {

        Text(
            text = text,
            fontSize = 23.sp,

            fontWeight =
                FontWeight.Bold
        )
    }
}

@Composable
fun CartDialog(
    products: List<Product>,
    cart: Map<Int, Int>,
    totalAmount: Double,

    onClose: () -> Unit,
    onCheckout: () -> Unit,

    onIncrease: (Int) -> Unit,
    onDecrease: (Int) -> Unit
) {

    val selectedProducts =
        products.filter {

            (cart[it.id] ?: 0) > 0
        }

    AlertDialog(

        onDismissRequest =
            onClose,

        containerColor =
            CardBlack,

        title = {

            Text(
                text =
                    "🛒 سلة المشتريات",

                color = Gold,
                fontSize = 24.sp,

                fontWeight =
                    FontWeight.Bold
            )
        },

        text = {

            Column(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                if (
                    selectedProducts.isEmpty()
                ) {

                    Text(
                        text =
                            "السلة فارغة.",

                        color = Cream
                    )

                } else {

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    max = 380.dp
                                )
                    ) {

                        selectedProducts
                            .forEach { product ->

                                val quantity =
                                    cart[product.id]
                                        ?: 0

                                val itemTotal =
                                    product.price *
                                        quantity

                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                vertical = 7.dp
                                            ),

                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Column(
                                        modifier =
                                            Modifier.weight(
                                                1f
                                            )
                                    ) {

                                        Text(
                                            text =
                                                product.name,

                                            color =
                                                Cream,

                                            fontSize =
                                                15.sp,

                                            fontWeight =
                                                FontWeight
                                                    .SemiBold
                                        )

                                        Text(
                                            text =
                                                "$quantity × ${
                                                    formatPrice(
                                                        product.price
                                                    )
                                                }",

                                            color =
                                                Gold,

                                            fontSize =
                                                12.sp
                                        )

                                        Text(
                                            text =
                                                formatPrice(
                                                    itemTotal
                                                ),

                                            color =
                                                GoldLight,

                                            fontSize =
                                                13.sp
                                        )
                                    }

                                    Row(
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {

                                        TextButton(
                                            onClick = {

                                                onDecrease(
                                                    product.id
                                                )
                                            }
                                        ) {

                                            Text(
                                                text =
                                                    "−",

                                                color =
                                                    Gold,

                                                fontSize =
                                                    22.sp
                                            )
                                        }

                                        Text(
                                            text =
                                                quantity.toString(),

                                            color =
                                                Cream,

                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        TextButton(
                                            onClick = {

                                                onIncrease(
                                                    product.id
                                                )
                                            }
                                        ) {

                                            Text(
                                                text =
                                                    "+",

                                                color =
                                                    Gold,

                                                fontSize =
                                                    22.sp
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    color =
                                        Gold.copy(
                                            alpha = 0.18f
                                        )
                                )
                            }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Text(
                    text =
                        "المبلغ المطلوب",

                    color = Gold,
                    fontSize = 16.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    text =
                        formatPrice(
                            totalAmount
                        ),

                    color = GoldLight,
                    fontSize = 23.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick = onClose
            ) {

                Text(
                    text = "متابعة التسوق",
                    color = Gold
                )
            }
        },

        confirmButton = {

            Button(
                onClick = onCheckout,

                enabled =
                    selectedProducts.isNotEmpty(),

                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                Gold,

                            contentColor =
                                Black
                        )
            ) {

                Text(
                    text =
                        "متابعة إلى تأكيد الطلب"
                )
            }
        }
    )
}

@Composable
fun CheckoutDialog(
    totalAmount: Double,
    loading: Boolean,
    message: String,
    successOrderNumber: String,

    onClose: () -> Unit,

    onConfirm:
        (
            String,
            String,
            String,
            String
        ) -> Unit
) {

    var customerName by remember {
        mutableStateOf("")
    }

    var customerPhone by remember {
        mutableStateOf("")
    }

    var deliveryAddress by remember {
        mutableStateOf("")
    }

    var fulfillmentType by remember {
        mutableStateOf("داخل المحل")
    }

    if (successOrderNumber.isNotEmpty()) {

        AlertDialog(

            onDismissRequest = onClose,

            containerColor =
                CardBlack,

            title = {

                Text(
                    text =
                        "تم تأكيد الطلب ✅",

                    color = Gold,
                    fontSize = 24.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            },

            text = {

                Column {

                    Text(
                        text =
                            "تم إرسال طلبك بنجاح.",

                        color = Cream,
                        fontSize = 16.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(
                        text =
                            "رقم الطلب",

                        color = Gold,
                        fontSize = 15.sp
                    )

                    Text(
                        text =
                            successOrderNumber,

                        color = GoldLight,
                        fontSize = 22.sp,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },

            confirmButton = {

                Button(
                    onClick = onClose,

                    colors =
                        ButtonDefaults
                            .buttonColors(
                                containerColor =
                                    Gold,

                                contentColor =
                                    Black
                            )
                ) {

                    Text(
                        text = "إغلاق"
                    )
                }
            }
        )

        return
    }

    AlertDialog(

        onDismissRequest = {

            if (!loading) {
                onClose()
            }
        },

        containerColor =
            CardBlack,

        title = {

            Text(
                text =
                    "تأكيد الطلب",

                color = Gold,
                fontSize = 24.sp,

                fontWeight =
                    FontWeight.Bold
            )
        },

        text = {

            Column {

                OutlinedTextField(
                    value = customerName,

                    onValueChange = {
                        customerName = it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {
                        Text(
                            "الاسم"
                        )
                    },

                    singleLine = true
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = customerPhone,

                    onValueChange = {
                        customerPhone = it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {
                        Text(
                            "رقم الهاتف"
                        )
                    },

                    singleLine = true
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Text(
                    text =
                        "طريقة استلام الطلب",

                    color = Gold,
                    fontSize = 15.sp,

                    fontWeight =
                        FontWeight.SemiBold
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceEvenly
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        RadioButton(
                            selected =
                                fulfillmentType ==
                                    "داخل المحل",

                            onClick = {
                                fulfillmentType =
                                    "داخل المحل"
                            },

                            enabled =
                                !loading
                        )

                        Text(
                            text =
                                "داخل المحل",

                            color = Cream
                        )
                    }

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        RadioButton(
                            selected =
                                fulfillmentType ==
                                    "توصيل إلى المنزل",

                            onClick = {
                                fulfillmentType =
                                    "توصيل إلى المنزل"
                            },

                            enabled =
                                !loading
                        )

                        Text(
                            text =
                                "توصيل للمنزل",

                            color = Cream
                        )
                    }
                }

                if (
                    fulfillmentType ==
                        "توصيل إلى المنزل"
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    OutlinedTextField(
                        value =
                            deliveryAddress,

                        onValueChange = {
                            deliveryAddress = it
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        label = {
                            Text(
                                "عنوان التوصيل"
                            )
                        },

                        minLines = 2
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Text(
                    text =
                        "المبلغ المطلوب: ${
                            formatPrice(
                                totalAmount
                            )
                        }",

                    color = GoldLight,
                    fontSize = 17.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                if (message.isNotEmpty()) {

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text = message,
                        color = Color.Red
                    )
                }
            }
        },

        dismissButton = {

            TextButton(
                onClick = onClose,
                enabled = !loading
            ) {

                Text(
                    text = "إلغاء",
                    color = Gold
                )
            }
        },

        confirmButton = {

            Button(
                onClick = {

                    onConfirm(
                        customerName,
                        customerPhone,
                        deliveryAddress,
                        fulfillmentType
                    )
                },

                enabled = !loading,

                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                Gold,

                            contentColor =
                                Black
                        )
            ) {

                Text(
                    text =
                        if (loading)
                            "جاري إرسال الطلب..."
                        else
                            "تأكيد الطلب"
                )
            }
        }
    )
}

@Composable
fun RemoteProductImage(
    url: String
) {

    var bitmap by remember(url) {
        mutableStateOf<
            android.graphics.Bitmap?
            >(null)
    }

    LaunchedEffect(url) {

        Thread {

            try {

                val stream =
                    URL(url).openStream()

                val loaded =
                    BitmapFactory
                        .decodeStream(stream)

                stream.close()

                bitmap = loaded

            } catch (_: Exception) {

                bitmap = null
            }

        }.start()
    }

    if (bitmap != null) {

        Image(

            bitmap =
                bitmap!!.asImageBitmap(),

            contentDescription =
                "صورة المنتج",

            modifier =
                Modifier.fillMaxSize(),

            contentScale =
                ContentScale.Crop
        )

    } else {

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Color(0xFF1C1C1C)
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text =
                    "جاري تحميل الصورة...",

                color = Gold,
                fontSize = 13.sp
            )
        }
    }
}
