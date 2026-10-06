package com.almalaki.cafe

import androidx.compose.ui.platform.LocalConfiguration
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val RESET_SCHEME = "com.almalaki"
private const val RESET_HOST = "reset-password"

private const val SUPABASE_AUTH_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val SUPABASE_AUTH_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

class MainActivity : ComponentActivity() {

    private var recoveryTokenState = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RoyalFontManager.initialize(this)
        recoveryTokenState.value = extractRecoveryToken(intent)

        setContent {
            RoyalCoffeeApp(
                recoveryToken = recoveryTokenState.value,
                onRecoveryFinished = {
                    recoveryTokenState.value = ""
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val token = extractRecoveryToken(intent)
        if (token.isNotBlank()) {
            recoveryTokenState.value = token
        }
    }

    private fun extractRecoveryToken(intent: Intent?): String {
        val uri = intent?.data ?: return ""

        if (uri.scheme != RESET_SCHEME || uri.host != RESET_HOST) {
            return ""
        }

        val fragment = uri.fragment ?: return ""

        return fragment
            .split("&")
            .mapNotNull { part ->
                val index = part.indexOf("=")
                if (index <= 0) null
                else {
                    val key = Uri.decode(part.substring(0, index))
                    val value = Uri.decode(part.substring(index + 1))
                    key to value
                }
            }
            .toMap()["access_token"]
            .orEmpty()
    }
}

@Composable
fun RoyalCoffeeApp(
    recoveryToken: String = "",
    onRecoveryFinished: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("royal_settings", Context.MODE_PRIVATE)
    }

    var darkMode by remember {
        mutableStateOf(prefs.getBoolean("dark_mode", true))
    }
    var screen by remember { mutableStateOf("customer") }
    var token by remember { mutableStateOf("") }
    val selectedArabicFont by
    RoyalFontManager.arabicFont.collectAsState()

val selectedEnglishFont by
    RoyalFontManager.englishFont.collectAsState()

val configuration = LocalConfiguration.current

val appLanguage =
    configuration.locales
        .get(0)
        ?.language
        ?.lowercase()
        .orEmpty()

val activeFontFamily: FontFamily =
    if (appLanguage.startsWith("ar")) {
        RoyalFontManager
            .getFont(selectedArabicFont)
            .family
    } else {
        RoyalFontManager
            .getFont(selectedEnglishFont)
            .family
    }

val royalTypography =
    Typography().copy(
        displayLarge = Typography().displayLarge.copy(fontFamily = activeFontFamily),
        displayMedium = Typography().displayMedium.copy(fontFamily = activeFontFamily),
        displaySmall = Typography().displaySmall.copy(fontFamily = activeFontFamily),
        headlineLarge = Typography().headlineLarge.copy(fontFamily = activeFontFamily),
        headlineMedium = Typography().headlineMedium.copy(fontFamily = activeFontFamily),
        headlineSmall = Typography().headlineSmall.copy(fontFamily = activeFontFamily),
        titleLarge = Typography().titleLarge.copy(fontFamily = activeFontFamily),
        titleMedium = Typography().titleMedium.copy(fontFamily = activeFontFamily),
        titleSmall = Typography().titleSmall.copy(fontFamily = activeFontFamily),
        bodyLarge = Typography().bodyLarge.copy(fontFamily = activeFontFamily),
        bodyMedium = Typography().bodyMedium.copy(fontFamily = activeFontFamily),
        bodySmall = Typography().bodySmall.copy(fontFamily = activeFontFamily),
        labelLarge = Typography().labelLarge.copy(fontFamily = activeFontFamily),
        labelMedium = Typography().labelMedium.copy(fontFamily = activeFontFamily),
        labelSmall = Typography().labelSmall.copy(fontFamily = activeFontFamily)
    )

    LaunchedEffect(recoveryToken) {
        if (recoveryToken.isNotBlank()) {
            screen = "reset"
        }
    }

    MaterialTheme(
    colorScheme = if (darkMode) darkColorScheme(
        primary = Gold,
        background = Black,
        surface = Black,
        onBackground = Cream,
        onSurface = Cream
    ) else lightColorScheme(
        primary = GoldDark,
        background = Color(0xFFF7F2E8),
        surface = Color(0xFFF7F2E8),
        onBackground = Color(0xFF222222),
        onSurface = Color(0xFF222222)
    ),
    typography = royalTypography
) {
        when (screen) {
            "customer" -> CustomerScreen(
                darkMode = darkMode,
                onTheme = {
                    darkMode = !darkMode
                    prefs.edit().putBoolean("dark_mode", darkMode).apply()
                },
                onOwner = { screen = "login" }
            )

            "login" -> LoginScreen(
                onBack = { screen = "customer" },
                onSuccess = { newToken ->
                    token = newToken
                    screen = "admin"
                }
            )

            "admin" -> AdminScreen(
                accessToken = token,
                onLogout = {
                    token = ""
                    screen = "customer"
                }
            )

            "reset" -> PasswordResetScreen(
                recoveryToken = recoveryToken,
                onFinished = {
                    onRecoveryFinished()
                    screen = "login"
                }
            )
        }
    }
}

fun updatePasswordWithRecoveryToken(
    recoveryToken: String,
    newPassword: String
) {
    val connection =
        URL("$SUPABASE_AUTH_URL/auth/v1/user")
            .openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "PUT"
        connection.doOutput = true
        connection.setRequestProperty("apikey", SUPABASE_AUTH_KEY)
        connection.setRequestProperty(
            "Authorization",
            "Bearer $recoveryToken"
        )
        connection.setRequestProperty(
            "Content-Type",
            "application/json"
        )

        val body = JSONObject()
            .put("password", newPassword)
            .toString()

        connection.outputStream.use {
            it.write(body.toByteArray(Charsets.UTF_8))
        }

        val code = connection.responseCode

        val response =
            if (code in 200..299) {
                connection.inputStream
                    ?.bufferedReader()
                    ?.readText()
                    .orEmpty()
            } else {
                connection.errorStream
                    ?.bufferedReader()
                    ?.readText()
                    .orEmpty()
            }

        if (code !in 200..299) {
            val errorMessage = try {
                val json = JSONObject(response)
                json.optString(
                    "msg",
                    json.optString(
                        "message",
                        "تعذر تغيير كلمة المرور."
                    )
                )
            } catch (_: Exception) {
                "تعذر تغيير كلمة المرور. HTTP $code"
            }

            throw Exception(errorMessage)
        }
    } finally {
        connection.disconnect()
    }
}
