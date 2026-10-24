package com.almalaki.cafe

import android.view.KeyEvent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

    var isFocused by remember {
        mutableStateOf(false)
    }

    // اللمعان الملكي المتحرك
    val infiniteTransition =
        rememberInfiniteTransition(
            label = "royal_shine"
        )

    val shinePosition by
        infiniteTransition.animateFloat(
            initialValue = -1.2f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2600,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "shine_position"
        )

    // ألوان الملكي الأصلية
    val royalBlack = Color(0xFF050505)
    val royalBlack2 = Color(0xFF0B090D)
    val royalPurple = Color(0xFF21142F)
    val royalPurpleDark = Color(0xFF120B19)
    val royalBronze = Color(0xFF6E4D18)
    val goldDark = Color(0xFF8C6B16)
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFFE9A3)

    val cardBackground = Brush.verticalGradient(
        colors = listOf(
            royalPurpleDark,
            royalBlack2,
            royalBlack
        )
    )

    val animatedBorder = Brush.linearGradient(
        colors = listOf(
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

    val innerShine = Brush.horizontalGradient(
        colors = listOf(
            Color.Transparent,
            gold.copy(alpha = 0.05f),
            Color.Transparent,
            Color.Transparent
        ),
        startX = shinePosition * 700f,
        endX = shinePosition * 700f + 500f
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .onKeyEvent { event ->
                val nativeEvent = event.nativeKeyEvent

                if (
                    nativeEvent.action == KeyEvent.ACTION_UP &&
                    (
                        nativeEvent.keyCode == KeyEvent.KEYCODE_ENTER ||
                        nativeEvent.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = cardBackground,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = if (isFocused) 3.dp else 2.dp,
                    brush = if (isFocused) {
                        Brush.linearGradient(
                            colors = listOf(
                                goldLight,
                                gold,
                                Color.White.copy(alpha = 0.95f),
                                goldLight
                            )
                        )
                    } else {
                        animatedBorder
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(4.dp)
        ) {
            // اللمعة الداخلية
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = innerShine,
                        shape = RoundedCornerShape(13.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 2.dp,
                        vertical = 1.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // التاج الملكي
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(19.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "━━━━",
                        color = gold.copy(alpha = 0.55f),
                        fontSize = 7.sp
                    )

                    Spacer(
                        modifier = Modifier.size(3.dp)
                    )

                    Text(
                        text = "♛",
                        color = goldLight,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )

                    Spacer(
                        modifier = Modifier.size(3.dp)
                    )

                    Text(
                        text = "━━━━",
                        color = gold.copy(alpha = 0.55f),
                        fontSize = 7.sp
                    )
                }

                // صورة المنتج بإطار ذهبي
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(66.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF18111F),
                                    Color(0xFF080808)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    goldLight.copy(alpha = 0.8f),
                                    gold.copy(alpha = 0.25f),
                                    goldDark.copy(alpha = 0.8f)
                                )
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(2.dp)
                ) {
                    if (product.imageUrl.isNotBlank()) {
                        RemoteProductImage(product.imageUrl)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            royalPurple,
                                            royalBlack
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "♛",
                                    color = gold,
                                    fontSize = 18.sp
                                )

                                Text(
                                    text = "Royal",
                                    color = goldLight,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Cursive,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                // اسم المنتج
                Text(
                    text = product.name,
                    color = Cream,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                // تصنيف المنتج
                if (product.category.isNotBlank()) {
                    Text(
                        text = product.category,
                        color = goldLight.copy(alpha = 0.72f),
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                // السعر
                Text(
                    text = formatPrice(product.price),
                    color = goldLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                // السلة: الإضافة أو أزرار الكمية
                if (quantity == 0) {
                    Button(
                        onClick = {
                            AppSounds.productClick(context)
                            onAdd()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .defaultMinSize(
                                minWidth = 0.dp,
                                minHeight = 0.dp
                            ),
                        contentPadding =
                            androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 2.dp,
                                vertical = 0.dp
                            ),
                        shape = RoundedCornerShape(9.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = gold,
                            contentColor = Black
                        )
                    ) {
                        Text(
                            text = "＋ سلة",
                            fontSize = 10.sp,
                            maxLines = 1,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallCartButton(
                            text = "−",
                            onClick = {
                                AppSounds.buttonClick(context)
                                onRemove()
                            }
                        )

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    color = gold.copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = gold.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = quantity.toString(),
                                color = goldLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        SmallCartButton(
                            text = "+",
                            onClick = {
                                AppSounds.productClick(context)
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
        modifier = Modifier
            .size(30.dp)
            .defaultMinSize(
                minWidth = 0.dp,
                minHeight = 0.dp
            ),
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(0.dp),
        shape = RoundedCornerShape(9.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Gold,
            contentColor = Black
        )
    ) {
        Text(
            text = text,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
