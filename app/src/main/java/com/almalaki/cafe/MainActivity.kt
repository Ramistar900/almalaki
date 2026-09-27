package com.almalaki.cafe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

private val Black = Color(0xFF0B0B0B)
private val Gold = Color(0xFFD4AF37)
private val Cream = Color(0xFFF5F0E5)

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
            AlmalakiApp()
        }
    }
}

@Composable
fun AlmalakiApp() {

    var screen by remember {
        mutableStateOf("customer")
    }

    var accessToken by remember {
        mutableStateOf("")
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold,
            background = Black,
            surface = Color(0xFF171717)
        )
    ) {

        when (screen) {

            "customer" -> CustomerScreen(
                onOwnerLogin = {
                    screen = "login"
                }
            )

            "login" -> LoginScreen(
                onBack = {
                    screen = "customer"
                },
                onLoginSuccess = { token ->
                    accessToken = token
                    screen = "admin"
                }
            )

            "admin" -> AdminScreen(
                accessToken = accessToken,
                onLogout = {
                    accessToken = ""
                    screen = "customer"
                }
            )
        }
    }
}

@Composable
fun CustomerScreen(
    onOwnerLogin: () -> Unit
) {

    var products by remember {
        mutableStateOf<List<Product>>(emptyList())
    }

    var category by remember {
        mutableStateOf("الكل")
    }

    var loading by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {

        Thread {

            try {

                products = loadProducts()

            } catch (e: Exception) {

                e.printStackTrace()

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
        containerColor = Black
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            item {

                Text(
                    text = "Royal Coffee",
                    color = Gold,
                    fontSize = 36.sp
                )

                Text(
                    text = "طعمٌ يستحق التجربة",
                    color = Cream,
                    fontSize = 18.sp
                )

                Spacer(
                    Modifier.height(16.dp)
                )

                OutlinedButton(
                    onClick = onOwnerLogin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("دخول المالك")
                }

                Spacer(
                    Modifier.height(10.dp)
                )
            }

            item {

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {

                    listOf(
                        "الكل",
                        "قهوة",
                        "بارد",
                        "عصائر",
                        "حلويات"
                    ).forEach {

                        FilterChip(
                            selected = category == it,
                            onClick = {
                                category = it
                            },
                            label = {
                                Text(it)
                            }
                        )
                    }
                }
            }

            if (loading) {

                item {

                    Text(
                        "جاري تحميل المنتجات...",
                        color = Cream
                    )
                }

            } else {

                items(filtered) { product ->

                    ProductCard(product)
                }
            }
        }
    }
}

@Composable
fun ProductCard(product: Product) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF191919)
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    product.name,
                    color = Cream,
                    fontSize = 19.sp
                )

                Text(
                    product.category,
                    color = Color.LightGray
                )
            }

            Text(
                "${"%.2f".format(product.price)} $",
                color = Gold,
                fontSize = 17.sp
            )
        }
    }
}

@Composable
fun LoginScreen(
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            "دخول المالك",
            color = Gold,
            fontSize = 32.sp
        )

        Spacer(
            Modifier.height(24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            label = {
                Text("البريد الإلكتروني")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            label = {
                Text("كلمة المرور")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(18.dp)
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
                            result.getString("access_token")

                        val userId =
                            result
                                .getJSONObject("user")
                                .getString("id")

                        if (userId == OWNER_ID) {

                            onLoginSuccess(token)

                        } else {

                            message =
                                "هذا الحساب ليس حساب المالك."
                        }

                    } catch (e: Exception) {

                        message =
                            "البريد أو كلمة المرور غير صحيحة."

                    }

                    loading = false

                }.start()

            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                if (loading)
                    "جاري الدخول..."
                else
                    "دخول"
            )
        }

        Spacer(
            Modifier.height(12.dp)
        )

        TextButton(
            onClick = onBack
        ) {
            Text("العودة")
        }

        if (message.isNotEmpty()) {

            Spacer(
                Modifier.height(12.dp)
            )

            Text(
                message,
                color = Color.Red
            )
        }
    }
}

@Composable
fun AdminScreen(
    accessToken: String,
    onLogout: () -> Unit
) {

    var products by remember {
        mutableStateOf<List<Product>>(emptyList())
    }

    var name by remember {
        mutableStateOf("")
    }

    var category by remember {
        mutableStateOf("")
    }

    var price by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf("")
    }

    fun refresh() {

        Thread {

            try {

                products = loadProducts()

            } catch (e: Exception) {

                message = "تعذر تحميل المنتجات."

            }

        }.start()
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                "لوحة المالك 👑",
                color = Gold,
                fontSize = 28.sp
            )

            TextButton(
                onClick = onLogout
            ) {
                Text("خروج")
            }
        }

        Spacer(
            Modifier.height(12.dp)
        )

        Text(
            "إضافة منتج",
            color = Cream,
            fontSize = 20.sp
        )

        Spacer(
            Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
            },
            label = {
                Text("اسم المنتج")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = category,
            onValueChange = {
                category = it
            },
            label = {
                Text("التصنيف")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = price,
            onValueChange = {
                price = it
            },
            label = {
                Text("السعر")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(10.dp)
        )

        Button(
            onClick = {

                val priceValue =
                    price.toDoubleOrNull()

                if (
                    name.isBlank() ||
                    category.isBlank() ||
                    priceValue == null
                ) {

                    message =
                        "أدخل الاسم والتصنيف والسعر."

                    return@Button
                }

                Thread {

                    try {

                        addProduct(
                            accessToken,
                            name,
                            category,
                            priceValue
                        )

                        name = ""
                        category = ""
                        price = ""

                        products = loadProducts()

                        message =
                            "تمت إضافة المنتج."

                    } catch (e: Exception) {

                        message =
                            "حدث خطأ أثناء الإضافة."
                    }

                }.start()

            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("إضافة المنتج")
        }

        if (message.isNotEmpty()) {

            Text(
                message,
                color = Gold
            )
        }

        Spacer(
            Modifier.height(18.dp)
        )

        Text(
            "المنتجات الحالية",
            color = Cream,
            fontSize = 20.sp
        )

        Spacer(
            Modifier.height(8.dp)
        )

        LazyColumn(
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            items(products) { product ->

                AdminProductCard(
                    product = product,
                    accessToken = accessToken,
                    onChanged = {
                        refresh()
                    }
                )
            }
        }
    }
}

@Composable
fun AdminProductCard(
    product: Product,
    accessToken: String,
    onChanged: () -> Unit
) {

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF191919)
        )
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Text(
                product.name,
                color = Cream,
                fontSize = 18.sp
            )

            Text(
                "${product.category} — ${"%.2f".format(product.price)} $",
                color = Gold
            )

            Spacer(
                Modifier.height(8.dp)
            )

            Button(
                onClick = {

                    Thread {

                        try {

                            deleteProduct(
                                accessToken,
                                product.id
                            )

                            onChanged()

                        } catch (e: Exception) {

                            e.printStackTrace()
                        }

                    }.start()
                }
            ) {

                Text("حذف")
            }
        }
    }
}

fun loadProducts(): List<Product> {

    val url = URL(
        "$SUPABASE_URL/rest/v1/products?select=id,name,category,price&order=id.asc"
    )

    val connection =
        url.openConnection() as HttpURLConnection

    connection.requestMethod = "GET"

    connection.setRequestProperty(
        "apikey",
        SUPABASE_KEY
    )

    connection.setRequestProperty(
        "Authorization",
        "Bearer $SUPABASE_KEY"
    )

    val response =
        connection.inputStream
            .bufferedReader()
            .readText()

    val json = JSONArray(response)

    val result =
        mutableListOf<Product>()

    for (i in 0 until json.length()) {

        val item =
            json.getJSONObject(i)

        result.add(
            Product(
                id = item.getInt("id"),
                name = item.getString("name"),
                category = item.getString("category"),
                price = item.getDouble("price")
            )
        )
    }

    return result
}

fun loginWithSupabase(
    email: String,
    password: String
): JSONObject {

    val url = URL(
        "$SUPABASE_URL/auth/v1/token?grant_type=password"
    )

    val connection =
        url.openConnection() as HttpURLConnection

    connection.requestMethod = "POST"
    connection.doOutput = true

    connection.setRequestProperty(
        "apikey",
        SUPABASE_KEY
    )

    connection.setRequestProperty(
        "Content-Type",
        "application/json"
    )

    val body =
        JSONObject()
            .put("email", email)
            .put("password", password)
            .toString()

    connection.outputStream.use {
        it.write(body.toByteArray())
    }

    val response =
        connection.inputStream
            .bufferedReader()
            .readText()

    return JSONObject(response)
}

fun addProduct(
    accessToken: String,
    name: String,
    category: String,
    price: Double
) {

    val url =
        URL("$SUPABASE_URL/rest/v1/products")

    val connection =
        url.openConnection() as HttpURLConnection

    connection.requestMethod = "POST"
    connection.doOutput = true

    connection.setRequestProperty(
        "apikey",
        SUPABASE_KEY
    )

    connection.setRequestProperty(
        "Authorization",
        "Bearer $accessToken"
    )

    connection.setRequestProperty(
        "Content-Type",
        "application/json"
    )

    val body =
        JSONObject()
            .put("name", name)
            .put("category", category)
            .put("price", price)
            .toString()

    connection.outputStream.use {
        it.write(body.toByteArray())
    }

    connection.inputStream.close()
}

fun deleteProduct(
    accessToken: String,
    id: Int
) {

    val url =
        URL(
            "$SUPABASE_URL/rest/v1/products?id=eq.$id"
        )

    val connection =
        url.openConnection() as HttpURLConnection

    connection.requestMethod = "DELETE"

    connection.setRequestProperty(
        "apikey",
        SUPABASE_KEY
    )

    connection.setRequestProperty(
        "Authorization",
        "Bearer $accessToken"
    )

    connection.inputStream.close()
}
