package com.almalaki.cafe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    val products = listOf(
        Product("إسبريسو", "قهوة", 2.5),
        Product("كابتشينو", "قهوة", 3.5),
        Product("لاتيه", "قهوة", 3.5),
        Product("آيس كوفي", "بارد", 4.0),
        Product("موهيتو", "بارد", 4.0),
        Product("عصير برتقال", "عصائر", 3.0),
        Product("تشيز كيك", "حلويات", 4.5),
        Product("براونيز", "حلويات", 4.0)
    )

    var category by remember { mutableStateOf("الكل") }
    var cart by remember { mutableStateOf(listOf<Product>()) }

    val filtered =
        if (category == "الكل")
            products
        else
            products.filter { it.category == category }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold,
            background = Black,
            surface = Color(0xFF171717)
        )
    ) {

        Scaffold(
            containerColor = Black,
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF111111)
                ) {
                    Text(
                        text = "السلة: ${cart.size}",
                        color = Gold,
                        modifier = Modifier.padding(16.dp)
                    )

                    Spacer(Modifier.weight(1f))

                    Text(
                        text = "الملكي",
                        color = Cream,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        ) { padding ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                item {
                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "الملكي",
                        color = Gold,
                        fontSize = 34.sp
                    )

                    Text(
                        text = "قهوة • مشروبات • عصائر • حلويات",
                        color = Cream
                    )

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "اطلب ما تحب",
                        color = Cream,
                        fontSize = 23.sp
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
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
                                onClick = { category = it },
                                label = { Text(it) }
                            )
                        }
                    }
                }

                items(filtered) { product ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                cart = cart + product
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF191919)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = product.name,
                                    color = Cream,
                                    fontSize = 19.sp
                                )

                                Text(
                                    text = product.category,
                                    color = Color.LightGray
                                )
                            }

                            Text(
                                text = "${"%.2f".format(product.price)} $",
                                color = Gold
                            )
                        }
                    }
                }

                item {

                    if (cart.isNotEmpty()) {

                        Button(
                            onClick = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Gold,
                                contentColor = Black
                            )
                        ) {

                            Text(
                                text = "متابعة الطلب • ${
                                    "%.2f".format(
                                        cart.sumOf { it.price }
                                    )
                                } $"
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
