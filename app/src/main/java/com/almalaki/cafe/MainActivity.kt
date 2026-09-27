package com.almalaki.cafe

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                    e.message ?: "تعذر تحميل القائمة."
            }

            loading = false

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
                start = 16.dp,
                end = 16.dp,
                top = 22.dp
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
                    fontSize = 46.sp,
                    fontFamily = FontFamily.Cursive,
                    fontWeight = FontWeight.Light
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
                        if (darkMode) "☀"
                        else "🌙",
                    color = Gold,
                    fontSize = 23.sp
                )
            }
        }

        Text(
            text = "طعمٌ يستحق التجربة",
            color = textColor,
            fontSize = 15.sp
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        HorizontalDivider(
            color = Gold.copy(alpha = 0.45f)
        )

        Spacer(
            modifier = Modifier.height(10.dp)
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

                Text(
                    text = message,
                    color = Color.Red
                )
            }

            else -> {

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding =
                        PaddingValues(bottom = 10.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    items(products) { product ->

                        ProductCard(
                            product = product
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onOwner,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = "👑 دخول المالك",
                color = Gold
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF111111)
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            if (product.imageUrl.isNotBlank()) {

                RemoteProductImage(
                    url = product.imageUrl
                )

            } else {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
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
                        fontFamily = FontFamily.Cursive
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = product.name,
                color = Cream,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (product.category.isNotBlank()) {

                Text(
                    text = product.category,
                    color = Gold,
                    fontSize = 13.sp
                )
            }

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = formatPrice(product.price),
                color = Gold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RemoteProductImage(
    url: String
) {

    var bitmap by remember(url) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }

    LaunchedEffect(url) {

        Thread {

            try {

                val stream =
                    URL(url).openStream()

                val loaded =
                    BitmapFactory.decodeStream(stream)

                stream.close()

                bitmap = loaded

            } catch (_: Exception) {
                bitmap = null
            }

        }.start()
    }

    if (bitmap != null) {

        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "صورة المنتج",
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentScale = ContentScale.Crop
        )

    } else {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Color(0xFF1C1C1C)
                ),
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text = "جاري تحميل الصورة...",
                color = Gold,
                fontSize = 13.sp
            )
        }
    }
}
