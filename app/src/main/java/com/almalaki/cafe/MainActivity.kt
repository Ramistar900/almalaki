package com.almalaki.cafe

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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

           
