package com.almalaki.cafe

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

private val Gold = Color(0xFFD4AF37)
private val GoldLight = Color(0xFFFFE9A3)
private val GoldDark = Color(0xFF8C6B16)
private val Black = Color(0xFF050505)
private val CardBlack = Color(0xFF111111)
private val Cream = Color(0xFFF5F0E5)

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
        )
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

@Composable
fun PasswordResetScreen(
    recoveryToken: String,
    onFinished: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var success by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CardBlack)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "الملكي",
                    color = Gold,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    if (success) "تم تغيير كلمة المرور"
                    else "استعادة كلمة المرور",
                    color = GoldLight,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                if (success) {
                    Text(
                        "تم تغيير كلمة المرور بنجاح ✅",
                        color = Cream,
                        fontSize = 17.sp
                    )

                    Button(
                        onClick = onFinished,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold,
                            contentColor = Black
                        )
                    ) {
                        Text("العودة إلى تسجيل الدخول")
                    }
                } else {
                    Text(
                        "أدخل كلمة المرور الجديدة ثم أكدها.",
                        color = Cream,
                        fontSize = 15.sp
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            message = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("كلمة المرور الجديدة") },
                        singleLine = true,
                        enabled = !loading,
                        visualTransformation =
                            androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            message = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("تأكيد كلمة المرور") },
                        singleLine = true,
                        enabled = !loading,
                        visualTransformation =
                            androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )

                    if (message.isNotBlank()) {
                        Text(message, color = Color.Red, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            when {
                                newPassword.length < 6 ->
                                    message =
                                        "كلمة المرور يجب أن تكون 6 أحرف أو أكثر."

                                newPassword != confirmPassword ->
                                    message =
                                        "كلمتا المرور غير متطابقتين."

                                recoveryToken.isBlank() ->
                                    message =
                                        "انتهت صلاحية رابط الاستعادة. اطلب رابطًا جديدًا."

                                else -> {
                                    loading = true
                                    message = ""

                                    Thread {
                                        try {
                                            updatePasswordWithRecoveryToken(
                                                recoveryToken,
                                                newPassword
                                            )

                                            Handler(Looper.getMainLooper()).post {
                                                loading = false
                                                success = true
                                            }
                                        } catch (e: Exception) {
                                            Handler(Looper.getMainLooper()).post {
                                                loading = false
                                                message =
                                                    e.message
                                                        ?: "تعذر تغيير كلمة المرور."
                                            }
                                        }
                                    }.start()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold,
                            contentColor = Black
                        )
                    ) {
                        Text(
                            if (loading)
                                "جاري تغيير كلمة المرور..."
                            else
                                "تغيير كلمة المرور 🔐"
                        )
                    }
                }
            }
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

@Composable
fun CustomerScreen(darkMode: Boolean, onTheme: () -> Unit, onOwner: () -> Unit) {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }
    var cart by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var showCart by remember { mutableStateOf(false) }
    var showCheckout by remember { mutableStateOf(false) }
    var orderLoading by remember { mutableStateOf(false) }
    var orderMessage by remember { mutableStateOf("") }
    var orderSuccessNumber by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        Thread {
            try { products = loadProducts() }
            catch (e: Exception) { message = e.message ?: "تعذر تحميل المنتجات." }
            loading = false
        }.start()
    }

    val background = if (darkMode) Black else Color(0xFFF7F2E8)
    val textColor = if (darkMode) Cream else Color(0xFF222222)
    val totalAmount = products.sumOf { product -> product.price * (cart[product.id] ?: 0) }
    val totalItems = cart.values.sum()

    Column(Modifier.fillMaxSize().background(background).padding(start = 14.dp, end = 14.dp, top = 18.dp)) {
        RoyalLogo()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("قائمة Royal", color = Gold, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                Text("طعمٌ يستحق التجربة", color = textColor, fontSize = 14.sp)
            }
            TextButton(onClick = onTheme) { Text(if (darkMode) "☀" else "🌙", color = Gold, fontSize = 23.sp) }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Gold.copy(alpha = 0.45f))
        Spacer(Modifier.height(8.dp))

        when {
            loading -> Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) { CircularProgressIndicator(color = Gold) }
            message.isNotEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) { Text(message, color = Color.Red) }
            else -> BoxWithConstraints(Modifier.weight(1f)) {
                val columns = when { maxWidth < 600.dp -> 2; maxWidth < 900.dp -> 3; maxWidth < 1400.dp -> 4; else -> 5 }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductCard(
                            product = product, quantity = cart[product.id] ?: 0,
                            onAdd = { val old = cart[product.id] ?: 0; cart = cart + (product.id to old + 1) },
                            onRemove = {
                                val old = cart[product.id] ?: 0
                                cart = if (old <= 1) cart - product.id else cart + (product.id to old - 1)
                            }
                        )
                    }
                }
            }
        }

        if (totalItems > 0) {
            Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBlack)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🛒 $totalItems منتجات", color = Cream, fontSize = 14.sp)
                        Text("المبلغ المطلوب", color = Gold, fontSize = 14.sp)
                        Text(formatPrice(totalAmount), color = GoldLight, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { showCart = true }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Black)) { Text("عرض السلة 🛒") }
                }
            }
        }
        OutlinedButton(onClick = onOwner, Modifier.fillMaxWidth().padding(bottom = 4.dp)) { Text("👑 دخول المالك", color = Gold) }
    }

    if (showCart) CartDialog(
        products = products, cart = cart, totalAmount = totalAmount,
        onClose = { showCart = false },
        onCheckout = { showCart = false; orderMessage = ""; orderSuccessNumber = ""; showCheckout = true },
        onIncrease = { id -> val old = cart[id] ?: 0; cart = cart + (id to old + 1) },
        onDecrease = { id -> val old = cart[id] ?: 0; cart = if (old <= 1) cart - id else cart + (id to old - 1) }
    )

    if (showCheckout) CheckoutDialog(
        totalAmount = totalAmount, loading = orderLoading, message = orderMessage,
        successOrderNumber = orderSuccessNumber,
        onClose = { if (!orderLoading) showCheckout = false },
        onConfirm = { name, phone, address, type ->
            when {
                orderLoading -> Unit
                name.trim().isEmpty() -> orderMessage = "اكتب اسمك."
                type == "توصيل إلى المنزل" && address.trim().isEmpty() -> orderMessage = "اكتب عنوان التوصيل."
                else -> {
                    orderLoading = true; orderMessage = ""
                    Thread {
                        try {
                            val result = createOrder(name, phone, address, type, totalAmount, products, cart)
                            orderSuccessNumber = result.orderNumber
                            cart = emptyMap()
                        } catch (e: Exception) { orderMessage = e.message ?: "حدث خطأ أثناء إرسال الطلب." }
                        finally { orderLoading = false }
                    }.start()
                }
            }
        }
    )
}

@Composable
fun RoyalLogo() {
    val transitio
