package com.almalaki.cafe

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val SUPABASE_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val SUPABASE_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

private const val OWNER_ID =
    "ab88911b-6713-4cf3-84be-5a1d9128281a"

private val Gold = Color(0xFFD4AF37)
private val BrightGold = Color(0xFFFFE8A0)
private val Black = Color(0xFF050505)
private val DarkSurface = Color(0xFF111111)
private val DarkCard = Color(0xFF181818)
private val Cream = Color(0xFFF5F0E5)
private val LightBackground = Color(0xFFF7F2E8)
private val LightCard = Color(0xFFFFFFFF)
private val DarkText = Color(0xFF171717)

data class Product(
    val id: Int,
    val name: String,
    val category: String,
    val price: Double,
    val imageBase64: String?
)

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

    val preferences = remember {
        context.getSharedPreferences(
            "royal_coffee_settings",
            Context.MODE_PRIVATE
        )
    }

    var darkMode by remember {
        mutableStateOf(
            preferences.getBoolean("dark_mode", true)
        )
    }

    var screen by remember {
        mutableStateOf("customer")
    }

    var accessToken by remember {
        mutableStateOf("")
    }

    val background =
        if (darkMode) Black else LightBackground

    val textColor =
        if (darkMode) Cream else DarkText

    MaterialTheme(
        colorScheme =
            if (darkMode) {
                darkColorScheme(
                    primary = Gold,
                    secondary = BrightGold,
                    background = background,
                    surface = DarkSurface,
                    onBackground = textColor,
                    onSurface = textColor
                )
            } else {
                lightColorScheme(
                    primary = Gold,
                    secondary = Gold,
                    background = background,
                    surface = LightCard,
                    onBackground = textColor,
                    onSurface = textColor
                )
            }
    ) {

        when (screen) {

            "customer" -> {
                CustomerScreen(
                    darkMode = darkMode,
                    onToggleTheme = {
                        darkMode = !darkMode

                        preferences.edit()
                            .putBoolean(
                                "dark_mode",
                                darkMode
                            )
                            .apply()
                    },
                    onOwnerLogin = {
                        screen = "login"
                    }
                )
            }

            "login" -> {
                LoginScreen(
                    darkMode = darkMode,
                    onBack = {
                        screen = "customer"
                    },
                    onLoginSuccess = { token ->
                        accessToken = token
                        screen = "admin"
                    }
                )
            }

            "admin" -> {
                AdminScreen(
                    darkMode = darkMode,
                    accessToken = accessToken,
                    onLogout = {
                        accessToken = ""
                        screen = "customer"
                    }
                )
            }
        }
    }
}

@Composable
fun RoyalTitle(
    darkMode: Boolean
) {

    val transition =
        rememberInfiniteTransition(
            label = "goldShine"
        )

    val shinePosition by transition.animateFloat(
        initialValue = -600f,
        targetValue = 900f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 2600,
                        easing = LinearEasing
                    ),
                repeatMode = RepeatMode.Restart
            ),
        label = "shine"
    )

    val goldBrush =
        Brush.linearGradient(
            colors = listOf(
                Gold,
                Gold,
                BrightGold,
                Color.White,
                BrightGold,
                Gold
            ),
            start = Offset(
                shinePosition,
                0f
            ),
            end = Offset(
                shinePosition + 400f,
                100f
            )
        )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Royal Coffee",
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                brush = goldBrush
            )
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        Text(
            text = "طعمٌ يستحق التجربة",
            color =
                if (darkMode)
                    Cream
                else
                    DarkText,
            fontSize = 17.sp
        )
    }
}

@Composable
fun ThemeButton(
    darkMode: Boolean,
    onClick: () -> Unit
) {

    OutlinedButton(
        onClick = onClick,
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = Gold
            ),
        border =
            BorderStroke(
                1.dp,
                Gold
            )
    ) {

        Text(
            if (darkMode)
                "☀ الوضع الفاتح"
            else
                "🌙 الوضع الداكن"
        )
    }
}

@Composable
fun CustomerScreen(
    darkMode: Boolean,
    onToggleTheme: () -> Unit,
    onOwnerLogin: () -> Unit
) {

    var products by remember {
        mutableStateOf<List<Product>>(
            emptyList()
        )
    }

    var category by remember {
        mutableStateOf("الكل")
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {

        Thread {

            try {
                products = loadProducts()
                errorMessage = ""
            } catch (e: Exception) {
                errorMessage =
                    e.message ?: "تعذر تحميل المنتجات."
            }

            loading = false

        }.start()
    }

    val filteredProducts =
        if (category == "الكل") {
            products
        } else {
            products.filter {
                it.category == category
            }
        }

    Scaffold(
        containerColor =
            if (darkMode)
                Black
            else
                LightBackground
    ) { padding ->

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            item {

                RoyalTitle(
                    darkMode = darkMode
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                ThemeButton(
                    darkMode = darkMode,
                    onClick = onToggleTheme
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedButton(
                    onClick = onOwnerLogin,
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = Gold
                        ),
                    border =
                        BorderStroke(
                            1.dp,
                            Gold
                        )
                ) {
                    Text("دخول المالك")
                }
            }

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {

                    listOf(
                        "الكل",
                        "قهوة",
                        "بارد",
                        "عصائر",
                        "حلويات"
                    ).forEach { itemCategory ->

                        FilterChip(
                            selected =
                                category == itemCategory,
                            onClick = {
                                category = itemCategory
                            },
                            label = {
                                Text(itemCategory)
                            },
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor =
                                        Gold,
                                    selectedLabelColor =
                                        Black,
                                    labelColor =
                                        if (darkMode)
                                            Cream
                                        else
                                            DarkText
                                )
                        )
                    }
                }
            }

            if (loading) {

                item {

                    Text(
                        text = "جاري تحميل المنتجات...",
                        color =
                            if (darkMode)
                                Cream
                            else
                                DarkText
                    )
                }

            } else if (errorMessage.isNotEmpty()) {

                item {

                    Text(
                        text = errorMessage,
                        color = Color.Red
                    )
                }

            } else {

                items(filteredProducts) { product ->

                    ProductCard(
                        product = product,
                        darkMode = darkMode
                    )
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    darkMode: Boolean
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Gold,
                    shape = RoundedCornerShape(14.dp)
                ),
        shape = RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (darkMode)
                        DarkCard
                    else
                        LightCard
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
        ) {

            if (!product.imageBase64.isNullOrBlank()) {

                Base64ProductImage(
                    base64 = product.imageBase64
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )
            }

            Text(
                text = product.name,
                color = Gold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

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

                Text(
                    text = product.category,
                    color =
                        if (darkMode)
                            Cream
                        else
                            DarkText
                )

                Text(
                    text = formatPrice(product.price),
                    color = Gold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun Base64ProductImage(
    base64: String
) {

    val bitmap = remember(base64) {

        try {

            val bytes =
                Base64.decode(
                    base64,
                    Base64.DEFAULT
                )

            BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size
            )

        } catch (e: Exception) {
            null
        }
    }

    if (bitmap != null) {

        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "صورة المنتج",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(190.dp),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun LoginScreen(
    darkMode: Boolean,
    onBack: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(false)
    }

    val textColor =
        if (darkMode)
            Cream
        else
            DarkText

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    if (darkMode)
                        Black
                    else
                        LightBackground
                )
                .padding(24.dp),
        verticalArrangement =
            Arrangement.Center
    ) {

        RoyalTitle(
            darkMode = darkMode
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Text(
            text = "دخول المالك",
            color = Gold,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        RoyalTextField(
            value = email,
            onValueChange = {
                email = it
            },
            label = "البريد الإلكتروني",
            textColor = textColor
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        RoyalTextField(
            value = password,
            onValueChange = {
                password = it
            },
            label = "كلمة المرور",
            textColor = textColor
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Button(
            onClick = {

                loading = true
                message = ""

                Thread {

                    try {

                        val result =
                            loginWithSupabase(
                                email,
                                password
                            )

                        val token =
                            result.getString(
                                "access_token"
                            )

                        val userId =
                            result
                                .getJSONObject("user")
                                .getString("id")

                        if (userId == OWNER_ID
