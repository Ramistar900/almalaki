package com.almalaki.cafe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AdminGold = Color(0xFFD4AF37)
private val AdminBlack = Color(0xFF050505)
private val AdminCream = Color(0xFFF5F0E5)

@Composable
fun AdminScreen(
    accessToken: String,
    onLogout: () -> Unit
) {

    var name by remember {
        mutableStateOf("")
    }

    var category by remember {
        mutableStateOf("")
    }

    var price by remember {
        mutableStateOf("")
    }

    var imageUrl by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminBlack)
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = "لوحة المالك 👑",
                color = AdminGold,
                fontSize = 26.sp
            )

            TextButton(
                onClick = onLogout
            ) {

                Text(
                    text = "خروج",
                    color = AdminGold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "إضافة منتج",
            color = AdminGold,
            fontSize = 22.sp
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    "اسم المنتج",
                    color = AdminGold
                )
            }
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = category,
            onValueChange = {
                category = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    "التصنيف",
                    color = AdminGold
                )
            }
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = price,
            onValueChange = {
                price = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    "السعر بالليرة السورية",
                    color = AdminGold
                )
            }
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = imageUrl,
            onValueChange = {
                imageUrl = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    "رابط صورة المنتج",
                    color = AdminGold
                )
            }
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = {

                // سيتم ربط زر الإضافة بقاعدة البيانات
                // في الخطوة التالية.

            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AdminGold,
                contentColor = AdminBlack
            )
        ) {

            Text(
                "إضافة المنتج"
            )
        }

        Spacer(
            modifier = Modifier.height(25.dp)
        )

        Text(
            text = "المنتجات الحالية",
            color = AdminGold,
            fontSize = 22.sp
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        LazyColumn {

            item {

                Text(
                    text = "سيتم عرض المنتجات هنا",
                    color = AdminCream
                )
            }
        }
    }
}
