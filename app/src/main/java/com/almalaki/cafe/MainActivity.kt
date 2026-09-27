package com.almalaki.cafe

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Gold = Color(0xFFD4AF37)
private val Black = Color(0xFF050505)
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
        colorScheme = darkColorScheme(
            primary = Gold,
            background = Black,
            surface = Black,
            onBackground = Cream,
            onSurface = Cream
        )
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

    LaunchedEffect(Unit) {

        Thread {

            try {

                products = loadProducts()

            } catch (e: Exception) {

                message =
                    e.message
                        ?: "تعذر تحميل القائمة."

            } finally {

                loading = false
            }

        }.start()
    }

    val background =
        if (darkMode)
            Black
        else
            Color(0xFFF7F2E8)

    val textColor =
        if (darkMode)
            Cream
        else
            Color(0xFF222222)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(
                start = 18.dp,
                end = 18.dp,
                top = 25.dp
            )
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column {

                Text(
                    text = "Royal",
                    color = Gold,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Cursive
                )

                Text(
                    text = "قائمة Royal",
                    color = Gold,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold
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

                    fontSize = 22.sp,
                    color = Gold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "طعمٌ يستحق التجربة",
            color = textColor,
            fontSize = 16.sp
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        HorizontalDivider(
            color = Gold.copy(alpha = 0.45f)
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        if (loading) {

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment =
                    Alignment.Center
            ) {

                CircularProgressIndicator(
                    color = Gold
                )
            }

        } else if (message.isNotEmpty()) {

            Text(
                text = message,
                color = Color.Red,
                modifier = Modifier.padding(8.dp)
            )

        } else if (products.isEmpty()) {

            Text(
                text = "لا توجد منتجات حاليًا.",
                color = textColor,
                fontSize = 17.sp
            )

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                items(products) { product ->

                    ProductRow(
                        product = product,
                        textColor = textColor
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedButton(
            onClick = onOwner,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = "دخول المالك",
                color = Gold
            )
        }
    }
}

@Composable
fun ProductRow(
    product: Product,
    textColor: Color
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        colors = CardDefaults.cardColors(
            containerColor =
                Color(0xFF101010)
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = product.name,
                    color = textColor,
                    fontSize = 19.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )

                if (product.category.isNotBlank()) {

                    Text(
                        text = product.category,
                        color = Gold,
                        fontSize = 14.sp
                    )
                }
            }

            Text(
                text = formatPrice(product.price),
                color = Gold,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
