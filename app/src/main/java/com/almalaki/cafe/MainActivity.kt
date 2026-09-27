package com.almalaki.cafe

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val SUPABASE_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val SUPABASE_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

private const val OWNER_ID =
    "ab88911b-6713-4cf3-84be-5a1d9128281a"

private val Gold = Color(0xFFD4AF37)
private val BrightGold = Color(0xFFFFE9A6)
private val Black = Color(0xFF050505)
private val DarkSurface = Color(0xFF121212)
private val DarkCard = Color(0xFF181818)
private val Cream = Color(0xFFF5F0E5)
private val LightBackground = Color(0xFFF7F2E8)
private val LightCard = Color(0xFFFFFFFF)
private val DarkText = Color(0xFF171717)

data class Product(
    val id: Int,
    val name: String,
    val category: String,
    val price: Double
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

    val surfaceColor =
        if (darkMode) DarkSurface else LightCard

    val colorScheme =
        if (darkMode) {
            darkColorScheme(
                primary = Gold,
                secondary = BrightGold,
                background = background,
                surface = surfaceColor,
                onBackground = textColor,
                onSurface = textColor
            )
        } else {
            lightColorScheme(
                primary = Gold,
                secondary = Gold,
                background = background,
                surface = surfaceColor,
                onBackground = textColor,
                onSurface = textColor
            )
        }

    MaterialTheme(
        colorScheme = colorScheme
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

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "goldShine"
        )

    val position by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 2600,
                        easing = LinearEasing
                    ),
                repeatMode = RepeatMode.Restart
            ),
        label = "shinePosition"
    )

    val textBrush =
        Brush.linearGradient(
            colors = listOf(
                Gold,
                Gold,
                BrightGold,
                Color.White,
                BrightGold,
                Gold,
                Gold
            ),
            start = Offset(
                position * 500f,
                0f
            ),
            end = Offset(
                position * 500f + 500f,
                100f
            )
        )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = "Royal Coffee",
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            style = androidx.compose.ui.text.TextStyle(
                brush = textBrush
            )
        )

        Spacer(
            Modifier.height(4.dp)
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
            androidx.compose.foundation.BorderStroke(
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

    var error by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {

        Thread {

            try {

                val result =
                    loadProducts()

                products = result
                error = ""

            } catch (e: Exception) {

                error =
                    e.message
                        ?: "تعذر تحميل المنتجات."

            }

            loading = false

        }.start()
    }

    val filtered =
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
                    Modifier.height(14.dp)
                )

                ThemeButton(
                    darkMode = darkMode,
                    onClick = onToggleTheme
                )

                Spacer(
                    Modifier.height(10.dp)
                )

                OutlinedButton(
                    onClick = onOwnerLogin,
                    modifier =
                        Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = Gold
                        ),
                    border =
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Gold
                        )
                ) {

                    Text("دخول المالك")
                }

                Spacer(
                    Modifier.height(14.dp)
                )
            }

            item {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(5.dp)
                ) {

                    listOf(
                        "الكل",
                        "قهوة",
                        "بارد",
                        "عصائر",
                        "حلويات"
                    ).forEach {

                        FilterChip(
                            selected =
                                category == it,
                            onClick = {
                                category = it
                            },
                            label = {
                                Text(it)
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
                        "جاري تحميل المنتجات...",
                        color =
                            if (darkMode)
                                Cream
                            else
                                DarkText
                    )
                }

            } else if (error.isNotEmpty()) {

                item {

                    Text(
                        error,
                        color = Color.Red
                    )
                }

            } else {

                items(filtered) { product ->

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

    val cardColor =
        if (darkMode)
            DarkCard
        else
            LightCard

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    Gold,
                    RoundedCornerShape(14.dp)
                ),
        shape =
            RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = cardColor
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    product.name,
                    color = Gold,
                    fontSize = 20.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    product.category,
                    color =
                        if (
