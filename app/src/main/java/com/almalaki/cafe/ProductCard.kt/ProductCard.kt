package com.almalaki.cafe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProductCard(
    product: Product,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBlack
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .border(
                        2.dp,
                        Gold,
                        RoundedCornerShape(13.dp)
                    )
                    .padding(3.dp)
            ) {
                if (product.imageUrl.isNotBlank()) {
                    RemoteProductImage(product.imageUrl)
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1C1C1C)),
                        Alignment.Center
                    ) {
                        Text(
                            "Royal",
                            color = Gold,
                            fontSize = 24.sp,
                            fontFamily = FontFamily.Cursive
                        )
                    }
                }
            }

            Spacer(Modifier.height(7.dp))

            Text(
                product.name,
                color = Cream,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (product.category.isNotBlank()) {
                Text(
                    product.category,
                    color = Gold,
                    fontSize = 12.sp
                )
            }

            Text(
                formatPrice(product.price),
                color = GoldLight,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            if (quantity == 0) {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = Black
                    )
                ) {
                    Text("إضافة إلى السلة 🛒")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallCartButton("−", onRemove)

                    Text(
                        quantity.toString(),
                        color = GoldLight,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    SmallCartButton("+", onAdd)
                }
            }
        }
    }
}

@Composable
fun SmallCartButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(42.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Gold,
            contentColor = Black
        )
    ) {
        Text(
            text,
            fontSize = 23.sp,
            font
