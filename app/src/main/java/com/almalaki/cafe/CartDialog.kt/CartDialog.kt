package com.almalaki.cafe

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CartDialog(
    products: List<Product>,
    cart: Map<Int, Int>,
    totalAmount: Double,
    onClose: () -> Unit,
    onCheckout: () -> Unit,
    onIncrease: (Int) -> Unit,
    onDecrease: (Int) -> Unit
) {
    val selectedProducts =
        products.filter { (cart[it.id] ?: 0) > 0 }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = CardBlack,

        title = {
            Text(
                "🛒 سلة المشتريات",
                color = Gold,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        },

        text = {
            Column {
                if (selectedProducts.isEmpty()) {
                    Text(
                        "السلة فارغة.",
                        color = Cream
                    )
                } else {
                    Column(
                        Modifier.heightIn(max = 380.dp)
                    ) {
                        selectedProducts.forEach { product ->

                            val quantity =
                                cart[product.id] ?: 0

                            val itemTotal =
                                product.price * quantity

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 7.dp),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Column(
                                    Modifier.weight(1f)
                                ) {
                                    Text(
                                        product.name,
                                        color = Cream,
                                        fontSize = 15.sp
                                    )

                                    Text(
                                        "$quantity × ${
                                            formatPrice(product.price)
                                        }",
                                        color = Gold,
                                        fontSize = 12.sp
                                    )

                                    Text(
                                        formatPrice(itemTotal),
                                        color = GoldLight,
                                        fontSize = 13.sp
                                    )
                                }

                                Row(
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            onDecrease(product.id)
                                        }
                                    ) {
                                        Text(
                                            "−",
                                            color = Gold,
                                            fontSize = 22.sp
                                        )
                                    }

                                    Text(
                                        quantity.toString(),
                                        color = Cream,
                                        fontWeight = FontWeight.Bold
                                    )

                                    TextButton(
                                        onClick = {
                                            onIncrease(product.id)
                                        }
                                    ) {
                                        Text(
                                            "+",
                                            color = Gold,
                                            fontSize = 22.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(
                    Modifier.height(10.dp)
                )

                Text(
                    "المبلغ المطلوب",
                    color = Gold,
                    fontSize = 16.sp
                )

                Text(
                    formatPrice(totalAmount),
                    color = GoldLight,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },

        dismissButton = {
            TextButton(
                onClick = onClose
            ) {
                Text(
                    "متابعة التسوق",
                    color = Gold
                )
            }
        },

        confirmButton = {
            Button(
                onClick = onCheckout,
                enabled = selectedProducts.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = Black
                )
            ) {
                Text("متابعة إلى تأكيد الطلب")
            }
        }
    )
}
