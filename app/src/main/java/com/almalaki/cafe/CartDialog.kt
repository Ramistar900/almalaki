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

    /*
     * الحركة الذهبية
     */
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

    /*
     * ألوان Royal Premium
     */
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

    /*
     * خلفية النافذة
     */
    val dialogBackground =
        Brush.verticalGradient(
            colors =
                listOf(
                    royalPurpleDark,
                    royalBlack2,
                    royalBlack
                )
        )

    /*
     * إطار ذهبي متحرك
     */
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
                        shape =
                            RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 2.dp,
                        brush = animatedBorder,
                        shape =
                            RoundedCornerShape(24.dp)
                    )
                    .padding(14.dp)
        ) {

            Column(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                /*
                 * التاج والعنوان
                 */
                Column(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "♛",
                        color = goldLight,
                        fontSize = 31.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(2.dp)
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        verticalAlignment =
                            Alignment.CenterVertically,

                        horizontalArrangement =
                            Arrangement.Center
                    ) {

                        Text(
                            text = "━━━━",
                            color =
                                gold.copy(
                                    alpha = 0.55f
                                ),
                            fontSize = 9.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.size(6.dp)
                        )

                        Text(
                            text = "🛒 سلة المشتريات",
                            color = gold,
                            fontSize = 22.sp,
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        Spacer(
                            modifier =
                                Modifier.size(6.dp)
                        )

                        Text(
                            text = "━━━━",
                            color =
                                gold.copy(
                                    alpha = 0.55f
                                ),
                            fontSize = 9.sp
                        )
                    }

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

                Spacer(
                    modifier
