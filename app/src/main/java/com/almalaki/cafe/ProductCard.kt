package com.almalaki.cafe

import android.view.KeyEvent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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

    // حركة اللمعان الذهبي حول البطاقة كاملة
    val infiniteTransition =
        rememberInfiniteTransition(label = "royal_card_border")

    val shinePosition by infiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2600,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "royal_border_shine"
    )

    val royalBlack = Color(0xFF050505)
    val royalBlack2 = Color(0xFF0B090D)
    val royalPurple = Color(0xFF21142F)
    val royalPurpleDark = Color(0xFF120B19)
    val royalBronze = Color(0xFF6E4D18)

    val goldDark = Color(0xFF8C6B16)
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFFE9A3)

    val productGreen = Color(0xFF39E875)
    val currencyGreen = Color(0xFF39E875)
    val priceBlack = Color(0xFF111111)

    val cardBackground = Brush.verticalGradient(
        colors = listOf(
            royalPurpleDark,
            royalBlack2,
            royalBlack
        )
    )

    // هذا التدرج مخصص للإطار الخارجي، وليس إطار الصورة.
    val animatedBorder = Brush.linearGradient(
        colors = listOf(
            goldDark,
            gold,
            goldLight,
            Color.White,
            goldLight,
            royalBronze,
            goldDark
        ),
        start = androidx.compose.ui.geometry.Offset(
            x = shinePosition * 700f,
            y = shinePosition * 250f
        ),
        end = androidx.compose.ui.geometry.Offset(
            x = shinePosition * 700f + 500f,
            y = shinePosition * 250f + 350f
        )
    )

    val innerShine = Brush.horizontalGradient(
        colors = listOf(
            Color.Transparent,
            gold.copy(alpha = 0.035f),
            Color.Transparent
        ),
        startX = shinePosition * 500f,
        endX = shinePosition * 500f + 400f
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
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        // الإطار الذهبي الخارجي يطابق حدود البطاقة
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = cardBackground,
                    shape = RoundedCornerShape(13.dp)
                )
                .border(
                    width = if (isFocused) 2.dp else 1.5.dp,
                    brush = if (isFocused) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White,
                                goldLight,
                                gold,
                                Color.White
                            )
                        )
                    } else {
                        animatedBorder
                    },
                    shape = RoundedCornerShape(13.dp)
                )
                .padding(5.dp)
        ) {
            // خلفية البطاقة الملكية
            Image(
                painter = painterResource(
                    id = R.drawable.royal_product_card_bg
                ),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(9.dp)),
                contentScale = ContentScale.Crop
            )

            // طبقة داكنة لتحسين وضوح المحتوى
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.Black.copy(alpha = 0.34f),
                        shape = RoundedCornerShape(9.dp)
                    )
            )

            // لمعة خفيفة داخل البطاقة
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = innerShine,
                        shape = RoundedCornerShape(9.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 3.dp,
                        vertical = 3.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // صورة المنتج، من دون تاج أو زخارف
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.35f)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF18111F),
                                    Color(0xFF080808)
                                )
                            )
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
                            Text(
                                text = product.name,
                                color = goldLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // الاسم والسعر يساراً، والشراء وأزرار الكمية يميناً
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 7.dp,
                            bottom = 2.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // الطرف الأيسر: اسم المنتج والسعر تحته
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = product.name,
                            color = productGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF5EEDC))
                                .padding(
                                    horizontal = 5.dp,
                                    vertical = 4.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "%,.0f".format(product.price),
                                color = priceBlack,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )

                            Text(
                                text = "ل.س",
                                color = currencyGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    // الطرف الأيمن: زر شراء وتحته ناقص والكمية وزائد
                    Column(
                        modifier = Modifier.weight(0.92f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Button(
                            onClick = {
                                AppSounds.productClick(context)
                                onAdd()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(29.dp)
                                .defaultMinSize(
                                    minWidth = 0.dp,
                                    minHeight = 0.dp
                                ),
                            contentPadding = PaddingValues(
                                horizontal = 4.dp,
                                vertical = 0.dp
                            ),
                            shape = RoundedCornerShape(7.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = gold,
                                contentColor = Color(0xFF111111)
                            )
                        ) {
                            Text(
                                text = "شراء",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SmallCartButton(
                                text = "−",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    AppSounds.productClick(context)
                                    onRemove()
                                }
                            )

                            Text(
                                text = quantity.toString(),
                                modifier = Modifier.weight(0.8f),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )

                            SmallCartButton(
                                text = "+",
                                modifier = Modifier.weight(1f),
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
}

@Composable
fun SmallCartButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(28.dp)
            .defaultMinSize(
                minWidth = 0.dp,
                minHeight = 0.dp
            ),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(7.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFD4AF37),
            contentColor = Color(0xFF111111)
        )
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
