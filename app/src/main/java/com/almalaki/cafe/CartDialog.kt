package com.almalaki.cafe

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "royal_cart_shine"
        )

    val shinePosition by
        infiniteTransition.animateFloat(
            initialValue = -1.2f,
            targetValue = 1.2f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 2600,
                            easing = LinearEasing
                        ),
                    repeatMode = RepeatMode.Restart
                ),
            label = "royal_cart_shine_position"
        )

    val royalBlack = Color(0xFF050505)
    val royalBlack2 = Color(0xFF0B090D)
    val royalPurple = Color(0xFF21142F)
    val royalPurpleDark = Color(0xFF120B19)
    val royalBronze = Color(0xFF6E4D18)

    val goldDark = Color(0xFF8C6B16)
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFFE9A3)

    val dialogBackground =
        Brush.verticalGradient(
            colors =
                listOf(
                    royalPurpleDark,
                    royalPurple,
                    royalBlack2,
                    royalBlack
                )
        )

    val animatedBorder =
        Brush.linearGradient(
            colors =
                listOf(
                    goldDark,
                    gold,
                    goldLight,
                    Color.White.copy(alpha = 0.95f),
                    gold,
                    royalBronze,
                    goldDark
                ),
            start = androidx.compose.ui.geometry.Offset(
                x = shinePosition * 900f,
                y = 0f
            ),
            end = androidx.compose.ui.geometry.Offset(
                x = shinePosition * 900f + 500f,
                y = 650f
            )
        )

    Dialog(
        onDismissRequest = onClose,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        brush = dialogBackground,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 2.dp,
                        brush = animatedBorder,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "♛",
                        color = goldLight,
                        fontSize = 31.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "━━━━",
                            color = gold.copy(alpha = 0.55f),
                            fontSize = 9.sp
                        )

                        Spacer(
                            modifier = Modifier.size(6.dp)
                        )

                        Text(
                            text = "🛒 سلة المشتريات",
                            color = gold,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(
                            modifier = Modifier.size(6.dp)
                        )

                        Text(
                            text = "━━━━",
                            color = gold.copy(alpha = 0.55f),
                            fontSize = 9.sp
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "اختياراتك الملكية",
                        color = goldLight.copy(alpha = 0.68f),
                        fontSize = 11.sp
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                if (selectedProducts.isEmpty()) {

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    color = royalBlack2,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = gold.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🛒",
                                fontSize = 38.sp
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = "السلة فارغة",
                                color = gold,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = "أضف منتجاتك المفضلة للمتابعة",
                                color = goldLight.copy(alpha = 0.65f),
                                fontSize = 12.sp
                            )
                        }
                    }

                } else {

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp)
                    ) {
                        selectedProducts.forEach { product ->

                            val quantity =
                                cart[product.id] ?: 0

                            val itemTotal =
                                product.price * quantity

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 6.dp
                                        ),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Column(
                                    modifier =
                                        Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = product.name,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight =
                                            FontWeight.SemiBold
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.height(2.dp)
                                    )

                                    Text(
                                        text =
                                            "$quantity × ${
                                                formatPrice(
                                                    product.price
                                                )
                                            }",
                                        color =
                                            gold.copy(
                                                alpha = 0.75f
                                            ),
                                        fontSize = 11.sp
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.height(2.dp)
                                    )

                                    Text(
                                        text =
                                            formatPrice(
                                                itemTotal
                                            ),
                                        color = goldLight,
                                        fontSize = 13.sp,
                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                }

                                Row(
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    CartQuantityButton(
                                        text = "−",
                                        onClick = {
                                            onDecrease(
                                                product.id
                                            )
                                        }
                                    )

                                    Text(
                                        text =
                                            quantity.toString(),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight =
                                            FontWeight.Bold,
                                        modifier =
                                            Modifier.padding(
                                                horizontal = 8.dp
                                            )
                                    )

                                    CartQuantityButton(
                                        text = "+",
                                        onClick = {
                                            onIncrease(
                                                product.id
                                            )
                                        }
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier.height(3.dp)
                            )

                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(
                                            gold.copy(
                                                alpha = 0.12f
                                            )
                                        )
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = gold.copy(
                                    alpha = 0.08f
                                ),
                                shape =
                                    RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color =
                                    gold.copy(
                                        alpha = 0.30f
                                    ),
                                shape =
                                    RoundedCornerShape(16.dp)
                            )
                            .padding(13.dp)
                ) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {
                            Text(
                                text = "المبلغ المطلوب",
                                color =
                                    goldLight.copy(
                                        alpha = 0.75f
                                    ),
                                fontSize = 12.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(2.dp)
                            )

                            Text(
                                text = "الإجمالي",
                                color = gold,
                                fontSize = 15.sp,
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }

                        Text(
                            text =
                                formatPrice(
                                    totalAmount
                                ),
                            color = goldLight,
                            fontSize = 22.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    TextButton(
                        onClick = onClose,
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            text = "متابعة التسوق",
                            color = gold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onCheckout,
                        enabled =
                            selectedProducts.isNotEmpty(),
                        modifier =
                            Modifier.weight(1.35f),
                        shape =
                            RoundedCornerShape(13.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = gold,
                                contentColor = royalBlack,
                                disabledContainerColor =
                                    gold.copy(
                                        alpha = 0.25f
                                    ),
                                disabledContentColor =
                                    goldLight.copy(
                                        alpha = 0.45f
                                    )
                            )
                    ) {
                        Text(
                            text = "متابعة الطلب",
                            fontSize = 13.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CartQuantityButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(38.dp),
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                0.dp
            ),
        shape = RoundedCornerShape(11.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor =
                    Color(0xFFD4AF37),
                contentColor =
                    Color(0xFF050505)
            )
    ) {
        Text(
            text = text,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
