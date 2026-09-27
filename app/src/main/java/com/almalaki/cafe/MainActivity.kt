package com.almalaki.cafe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

private val Black = Color(0xFF0B0B0B)
private val Gold = Color(0xFFD4AF37)
private val Cream = Color(0xFFF5F0E5)

data class Product(
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

                val url = URL(
                    "https://duvxxskgdmgrtaleedqu.supabase.co/rest/v1/products?select=name,category,price"
                )

                val connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"

                connection.setRequestProperty(
                    "apikey",
                    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"
                )

                connection.setRequestProperty(
                    "Authorization",
                    "Bearer sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"
                )

                val response =
                    connection.inputStream
                        .bufferedReader()
                        .readText()

                val json = JSONArray(response)

                val result = mutableListOf<Product>()

                for (i in 0 until json.length()) {

                    val item = json.getJSONObject(i)

                    result.add(
                        Product(
                            name = item.getString("name"),
                            category = item.getString("category"),
                            price = item.getDouble("price")
                        )
                    )
                }

                products = result

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

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold,
            background = Black,
            surface = Color(0xFF171717)
        )
    ) {

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
                        text = "الملكي",
                        color = Gold,
                        fontSize = 34.sp
                    )

                    Text(
                        text = "قهوة • مشروبات • عصائر • حلويات",
                        color = Cream
                    )

                    Spacer(
                        Modifier.height(20.dp)
                    )
                }

                item {

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
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
                            text = "جاري تحميل المنتجات...",
                            color = Cream
                        )
                    }

                } else {

                    items(filtered) { product ->

                        Card(
                            modifier =
                                Modifier.fillMaxWidth(),

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        Color(0xFF191919)
                                )
                        ) {

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                            ) {

                                Column(
                                    modifier =
                                        Modifier.weight(1f)
                                ) {

                                    Text(
                                        text =
                                            product.name,
                                        color = Cream,
                                        fontSize = 19.sp
                                    )

                                    Text(
                                        text =
                                            product.category,
                                        color =
                                            Color.LightGray
                                    )
                                }

                                Text(
                                    text =
                                        "${"%.2f".format(product.price)} $",
                                    color = Gold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
