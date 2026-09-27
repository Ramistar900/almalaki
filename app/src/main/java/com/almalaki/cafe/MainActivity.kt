package com.almalaki.cafe

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    val context = androidx.compose.ui.platform.LocalContext.current

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (darkMode)
                    Black
                else
                    Color(0xFFF7F2E8)
            )
            .padding(20.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(35.dp)
        )

        Text(
            text = "Royal Coffee",
            color = Gold,
            fontSize = 38.sp
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
                    Color.DarkGray,
            fontSize = 17.sp
        )

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        OutlinedButton(
            onClick = onTheme,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                if (darkMode)
                    "☀ الوضع الفاتح"
                else
                    "🌙 الوضع الداكن"
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedButton(
            onClick = onOwner,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                "دخول المالك",
                color = Gold
            )
        }

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        Text(
            text = "المنتجات",
            color = Gold,
            fontSize = 24.sp
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text = "سيتم عرض قائمة المنتجات هنا",
            color =
                if (darkMode)
                    Cream
                else
                    Color.DarkGray
        )
    }
}
