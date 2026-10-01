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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    /*
     * Royal animated shine
     */
    val infiniteTransition =
        rememberInfiniteTransition(
            label = "cart_royal_shine"
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
            label = "cart_shine_position"
        )

    val royalBlack =
        Color(0xFF050505)

    val royalBlack2 =
        Color(0xFF0B090D)

    val royalPurple =
        Color(0xFF21142F)

    val royalPurpleDark =
        Color(0xFF120B19)

    val royalBronze =
        Color(0xFF6E4D18)

    val goldDark =
        Color(0xFF8C6B16)

    val gold =
        Color(0xFFD4AF37)

    val goldLight =
        Color(0xFFFFE9A3)

    val dialogBackground =
        Brush.verticalGradient(
            colors =
                listOf(
                    royalPurpleDark,
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

    AlertDialog(
        onDismissRequest = onClose,

        containerColor = Color.Transparent,

        title = {

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors =
                                    listOf(
                                        royalPurple,
                                        royalBlack2
                                    )
                            ),
                            shape =
                                RoundedCornerShape(18.dp)
                        )
                        .border(
                            width = 1.dp,
                            brush = animatedBorder,
                            shape =
                                RoundedCornerShape(18.dp)
                        )
                        .padding(
                            horizontal = 12.dp,
                            vertical = 10.dp
                        )
            ) {

                Column(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "♛",
                        color = goldLight,
                        fontSize = 28.sp
                    )

                    Text(
                        text = "سلة المشتريات",
                        color = gold,
                        fontSize = 22.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )

                    Text(
                        text = "اختياراتك الملكية",
                        color =
                            goldLight.copy(
                                alpha = 0.68f
                            ),
                        fontSize = 11.sp
                    )
                }
            }
        },

        text = {

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            brush = dialogBackground,
                            shape =
                                RoundedCornerShape(18.dp)
                        )
                        .border(
                            width = 1.dp,
                            brush = animatedBorder,
                            shape =
                                RoundedCornerShape(18.dp)
                        )
                        .padding(12.dp)
            ) {

                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    if (selectedProducts.isEmpty()) {

                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                Text(
                                    text = "🛒",
                                    fontSize = 35.sp
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(8.dp)
                                )

                                Text(
                                    text = "السلة فارغة.",
                                    color = Cream,
                                    fontSize = 15.sp
                                )
                            }
                        }

                    } else {

                        Column(
                            Modifier.heightIn(
                                max = 380.dp
                            )
                        ) {

                            selectedProducts.forEach { product ->

                                val quantity =
                                    cart[product.id] ?: 0

                                val itemTotal =
                                    product.price * quantity

                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                vertical = 4.dp
                                            )
                                            .background(
                                                color =
                                                    Color.Black.copy(
                                                        alpha = 0.28f
                                                    ),
                                                shape =
                                                    RoundedCornerShape(
                                                        14.dp
                                                    )
                                            )
                                            .border(
                                                width = 1.dp,
                                                color =
                                                    gold.copy(
                                                        alpha = 0.20f
                                                    ),
                                                shape =
                                                    RoundedCornerShape(
                                                        14.dp
                                                    )
                                            )
                                            .padding(10.dp)
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
                                                text =
                                                    product.name,
                                                color = Cream,
                                                fontSize = 15.sp,
                                                fontWeight =
                                                    FontWeight.SemiBold
                                            )

                                            Spacer(
                                                modifier =
                                                    Modifier.height(
                                                        2.dp
                                                    )
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
                                                        alpha = 0.82f
                                                    ),
                                                fontSize = 12.sp
                                            )

                                            Spacer(
                                                modifier =
                                                    Modifier.height(
                                                        2.dp
                                                    )
                                            )

                                            Text(
                                                text =
                                                    formatPrice(
                                                        itemTotal
                                                    ),
                                                color =
                                                    goldLight,
                                                fontSize = 14.sp,
                                                fontWeight =
                                                    FontWeight.Bold
                                            )
                                        }

                                        Row(
                                            verticalAlignment =
                                                Alignment.CenterVertically,
                                            horizontalArrangement =
                                                Arrangement.spacedBy(
                                                    3.dp
                                                )
                                        ) {

                                            CartQuantityButton(
                                                text = "−",
                                                onClick = {
                                                    onDecrease(
                                                        product.id
                                                    )
                                                }
                                            )

                                            Box(
                                                modifier =
                                                    Modifier
                                                        .size(36.dp)
                                                        .background(
                                                            color =
                                                                gold.copy(
                                                                    alpha = 0.10f
                                                                ),
                                                            shape =
                                                                RoundedCornerShape(
                                                                    10.dp
                                                                )
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color =
                                                                gold.copy(
                                                                    alpha = 0.35f
                                                                ),
                                                            shape =
                                                                RoundedCornerShape(
                                                                    10.dp
                                                                )
                                                        ),
                                                contentAlignment =
                                                    Alignment.Center
                                            ) {

                                                Text(
                                                    text =
                                                        quantity.toString(),
                                                    color =
                                                        goldLight,
                                                    fontSize = 15.sp,
                                                    fontWeight =
                                                        FontWeight.Bold
                                                )
                                            }

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
                                }
                            }
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    /*
                     * Total
                     */
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    color =
                                        gold.copy(
                                            alpha = 0.08f
                                        ),
                                    shape =
                                        RoundedCornerShape(
                                            14.dp
                                        )
                                )
                                .border(
                                    width = 1.dp,
                                    color =
                                        gold.copy(
                                            alpha = 0.32f
                                        ),
                                    shape =
                                        RoundedCornerShape(
                                            14.dp
                                        )
                                )
                                .padding(12.dp)
                    ) {

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text = "المبلغ المطلوب",
                                color = Gold,
                                fontSize = 15.sp,
                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            Text(
                                text =
                                    formatPrice(
                                        totalAmount
                                    ),
                                color = goldLight,
                                fontSize = 21.sp,
                                fontWe
