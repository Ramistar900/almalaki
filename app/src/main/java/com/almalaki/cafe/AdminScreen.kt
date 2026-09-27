package com.almalaki.cafe

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    var selectedImageUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var message by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var products by remember {
        mutableStateOf<List<Product>>(emptyList())
    }

    val imagePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            selectedImageUri = uri
            message =
                if (uri != null)
                    "تم اختيار الصورة ✅"
                else
                    ""
        }

    LaunchedEffect(Unit) {

        Thread {

            try {
                val result = loadProducts()

                products = result

            } catch (e: Exception) {

                message =
                    e.message ?: "تعذر تحميل المنتجات."
            }

        }.start()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminBlack)
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
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
                Text("اسم المنتج")
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
                Text("التصنيف")
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
                Text("السعر بالليرة السورية")
            }
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedButton(
            onClick = {
                imagePicker.launch("image/*")
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                if (selectedImageUri == null)
                    "📷 اختيار صورة المنتج"
                else
                    "✅ تم اختيار صورة المنتج"
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = {

                if (name.isBlank()) {
                    message = "اكتب اسم المنتج."
                    return@Button
                }

                val priceValue =
                    price.replace(",", ".").toDoubleOrNull()

                if (priceValue == null) {
                    message = "أدخل سعرًا صحيحًا."
                    return@Button
                }

                loading = true
                message = "جاري إضافة المنتج..."

                Thread {

                    try {

                        var imageUrl = ""

                        val uri = selectedImageUri

                        if (uri != null) {

                            val bytes =
                                context.contentResolver
                                    .openInputStream(uri)
                                    ?.use {
                                        it.readBytes()
                                    }
                                    ?: throw Exception(
                                        "تعذر قراءة الصورة."
                                    )

                            val mimeType =
                                context.contentResolver
                                    .getType(uri)
                                    ?: "image/jpeg"

                            imageUrl =
                                uploadProductImage(
                                    accessToken = accessToken,
                                    bytes = bytes,
                                    mimeType = mimeType
                                )
                        }

                        addProduct(
                            accessToken = accessToken,
                            name = name.trim(),
                            category = category.trim(),
                            price = priceValue,
                            imageUrl = imageUrl
                        )

                        products =
                            loadProducts()

                        name = ""
                        category = ""
                        price = ""
                        selectedImageUri = null

                        message =
                            "تمت إضافة المنتج بنجاح ✅"

                    } catch (e: Exception) {

                        message =
                            e.message
                                ?: "حدث خطأ أثناء الإضافة."

                    } finally {

                        loading = false
                    }

                }.start()

            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(
                containerColor = AdminGold,
                contentColor = AdminBlack
            )
        ) {

            Text(
                if (loading)
                    "جاري الإضافة..."
                else
                    "إضافة المنتج"
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        if (message.isNotEmpty()) {

            Text(
                text = message,
                color = AdminCream
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
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

            items(products) { product ->

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {

                    Text(
                        text = product.name,
                        color = AdminCream,
                        fontSize = 18.sp
                    )

                    Text(
                        text =
                            "${product.category} • " +
                                    formatPrice(product.price),
                        color = AdminGold
                    )

                    if (product.imageUrl.isNotEmpty()) {

                        Text(
                            text = "📷 توجد صورة للمنتج",
                            color = AdminCream,
                            fontSize = 13.sp
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(
                            top = 8.dp
                        )
                    )
                }
            }
        }
    }
}
