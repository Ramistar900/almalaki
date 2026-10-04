package com.almalaki.cafe

import androidx.compose.ui.input.key.onKeyEvent
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    /*
     * حالة تركيز Android TV
     */
    var isFocused by remember {
        mutableStateOf(false)
    }

    /*
     * حركة اللمعة الملكية
     */
    val infiniteTransition =
        rememberInfiniteTransition(
            label = "royal_shine"
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
            label = "shine_position"
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
     * خلفية البطاقة
     */
    val cardBackground =
        Brush.verticalGradient(
            colors =
                listOf(
                    royalPurpleDark,
                    royalBlack2,
                    royalBlack
                )
        )

    /*
     * لمعان متحرك للإطار
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
                y = 500f
            )
        )

    /*
     * لمعان داخلي خفيف
     */
    val innerShine =
        Brush.horizontalGradient(
            colors =
                listOf(
                    Color.Transparent,
                    gold.copy(alpha = 0.05f),
                    Color.Transparent,
                    Color.Transparent
                ),
            startX = shinePosition * 700f,
            endX = shinePosition * 700f + 500f
        )

    Card(
        modifier =
    Modifier
        .fillMaxWidth()
        .onFocusChanged {
            isFocused = it.isFocused
        }
        .onKeyEvent { event ->

    val nativeEvent = event.nativeKeyEvent

    if (
        nativeEvent.action == android.view.KeyEvent.ACTION_UP &&
        (
            nativeEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER ||
            nativeEvent.keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER
        )
    ) {
        AppSounds.productClick(context)
        onAdd()
        true
    } else {
        false
    }
        }
        .focusable(),

        shape =
            RoundedCornerShape(20.dp),

        colors =
            CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
    ) {

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        brush = cardBackground,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width =
                            if (isFocused) {
                                3.dp
                            } else {
                                2.dp
                            },
                        brush =
                            if (isFocused) {
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            goldLight,
                                            gold,
                                            Color.White.copy(
                                                alpha = 0.95f
                                            ),
                                            goldLight
                                        )
                                )
                            } else {
                                animatedBorder
                            },
                        shape =
                            RoundedCornerShape(20.dp)
                    )
                    .padding(7.dp)
        ) {

            /*
             * لمعان داخلي متحرك
             */
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            brush = innerShine,
                            shape = RoundedCornerShape(18.dp)
                        )
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 4.dp,
                            end = 4.dp,
                            top = 2.dp,
                            bottom = 5.dp
                        ),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                /*
                 * التاج الملكي أعلى البطاقة
                 */
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(32.dp),

                    contentAlignment =
                        Alignment.Center
                ) {

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
                                Modifier.size(5.dp)
                        )

                        Text(
                            text = "♛",
                            color = goldLight,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )

                        Spacer(
                            modifier =
                                Modifier.size(5.dp)
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
                }

                /*
                 * مساحة صورة المنتج
                 */
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(125.dp)
                            .clip(
                                RoundedCornerShape(
                                    14.dp
                                )
                            )
                            .background(
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            Color(0xFF18111F),
                                            Color(0xFF080808)
                                        )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush =
                                    Brush.linearGradient(
                                        colors =
                                            listOf(
                                                goldLight.copy(
                                                    alpha = 0.8f
                                                ),
                                                gold.copy(
                                                    alpha = 0.25f
                                                ),
                                                goldDark.copy(
                                                    alpha = 0.8f
                                                )
                                            )
                                    ),
                                shape =
                                    RoundedCornerShape(
                                        14.dp
                                    )
                            )
                            .padding(3.dp)
                ) {

                    if (product.imageUrl.isNotBlank()) {

                        RemoteProductImage(
                            product.imageUrl
                        )

                    } else {

                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            colors =
                                                listOf(
                                                    royalPurple,
                                                    royalBlack
                                                )
                                        )
                                    ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Column(
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                Text(
                                    text = "♛",
                                    color = gold,
                                    fontSize = 26.sp
                                )

                                Text(
                                    text = "Royal",
                                    color = goldLight,
                                    fontSize = 23.sp,
                                    fontFamily =
                                        FontFamily.Cursive,
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                /*
                 * اسم المنتج
                 */
                Text(
                    text = product.name,
                    color = Cream,
                    fontSize = 17.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )

                /*
                 * التصنيف
                 */
                if (product.category.isNotBlank()) {

                    Spacer(
                        modifier =
                            Modifier.height(2.dp)
                    )

                    Text(
                        text = product.category,
                        color =
                            goldLight.copy(
                                alpha = 0.72f
                            ),
                        fontSize = 12.sp
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                /*
                 * السعر
                 */
                Text(
                    text =
                        formatPrice(
                            product.price
                        ),
                    color = goldLight,
                    fontSize = 16.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(7.dp)
                )

                /*
                 * زر إضافة المنتج
                 */
                if (quantity == 0) {

                    Button(
                        onClick = {
                            AppSounds.productClick(
                                context
                            )
                            onAdd()
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(
                                13.dp
                            ),

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = gold,
                                contentColor = Black
                            )
                    ) {

                        Text(
                            text = "إضافة إلى السلة 🛒",
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }

                } else {

                    /*
                     * أزرار الكمية
                     */
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.SpaceEvenly,

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        SmallCartButton(
                            text = "−",

                            onClick = {
                                AppSounds.buttonClick(
                                    context
                                )
                                onRemove()
                            }
                        )

                        Box(
                            modifier =
                                Modifier
                                    .size(44.dp)
                                    .background(
                                        color =
                                            gold.copy(
                                                alpha = 0.10f
                                            ),
                                        shape =
                                            RoundedCornerShape(
                                                12.dp
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
                                                12.dp
                                            )
                                    ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text =
                                    quantity.toString(),
                                color = goldLight,
                                fontSize = 19.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }

                        SmallCartButton(
                            text = "+",

                            onClick = {
                                AppSounds.productClick(
                                    context
                                )
                                onAdd()
                            }
                        )
                    }
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

        modifier =
            Modifier.size(42.dp),

        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(0.dp),

        shape =
            RoundedCornerShape(12.dp),

        colors =
            ButtonDefaults.buttonColors(
                containerColor = Gold,
                contentColor = Black
            )
    ) {

        Text(
            text = text,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
