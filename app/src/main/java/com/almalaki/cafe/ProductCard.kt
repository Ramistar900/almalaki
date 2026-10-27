package com.almalaki.cafe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
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
    var isFocused by remember { mutableStateOf(false) }

    val transition =
        rememberInfiniteTransition(label = "royal_border")

    val shine by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "royal_shine"
    )

    // ألوان البطاقة الملكية
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFFE9A3)
    val cream = Color(0xFFF7F0DF)
    val darkGreen = Color(0xFF063D24)
    val productGreen = Color(0xFF006400)
    val priceText = Color(0xFF173D2A)

    // إطار أخضر ملكي متحرك بلمعة ذهبية
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF032817),
            Color(0xFF0A6B3B),
            Color(0xFFB8E2C7),
            gold,
            Color(0xFF176A3D),
            Color(0xFF032817)
        ),
        start = Offset(
            shine * 500f,
            shine * 200f
        ),
        end = Offset(
            shine * 500f + 400f,
            shine * 200f + 250f
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .onKeyEvent { event ->
                val key = event.nativeKeyEvent

                if (
                    key.action == KeyEvent.ACTION_UP &&
                    (
                        key.keyCode == KeyEvent.KEYCODE_ENTER ||
                        key.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
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
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = cream
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cream)
                .border(
                    width = 2.dp,
                    brush = if (isFocused) {
                        Brush.linearGradient(
                            listOf(
                                Color.White,
                                goldLight,
                                Color(0xFF0A6B3B),
                                gold
                            )
                        )
                    } else {
                        borderBrush
                    },
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // مساحة صورة أوضح مع زوايا ناعمة، مع إبقاء مصدر الصورة كما هو.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFFE9DFC8)),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUrl.isNotBlank()) {
                    RemoteProductImage(product.imageUrl)
                } else {
                    Text(
                        text = product.name,
                        color = darkGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 7.dp,
                        bottom = 2.dp
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(5.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                // اسم المنتج والسعر
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement =
                        Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = product.name,
                        color = productGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "%,.0f".format(product.price),
                            color = priceText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )

                        Text(
                            text = "ل.س",
                            color = productGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                // الشراء وأزرار الكمية
                Column(
                    modifier = Modifier.weight(0.9f),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.spacedBy(3.dp)
                ) {

                    // كلمة شراء حمراء لامعة
                    Text(
                        text = "شراء",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(27.dp),
                        color = Color(0xFFFF1717),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0xFFFF5A36),
                                offset = Offset(0f, 0f),
                                blurRadius = 9f
                            )
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(3.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        // زر الإنقاص داخل دائرة خضراء بإطار ذهبي
                        SmallCartButton(
                            text = "−",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                AppSounds.productClick(context)
                                onRemove()
                            }
                        )

                        // العدد بلون واضح على الخلفية العاجية
                        Text(
                            text = quantity.toString(),
                            modifier = Modifier.weight(0.8f),
                            color = darkGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )

                        // زر الإضافة داخل دائرة خضراء بإطار ذهبي
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

@Composable
fun SmallCartButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glintTransition =
        rememberInfiniteTransition(label = "cart_button_glint")

    // ومضة سريعة، ثم انتظار خمس ثوانٍ قبل تكرارها.
    val glint by glintTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.keyframes {
                durationMillis = 5450
                1f at 0
                1f at 5000
                -1f at 5001
                1f at 5450
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "cart_button_glint_position"
    )

    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFFE9A3)

    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0xFF032817))
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF0A8),
                        gold,
                        Color(0xFF8A6415),
                        goldLight
                    )
                ),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // لمعة بلورية تمر فوق الدائرة.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.55f),
                            goldLight.copy(alpha = 0.85f),
                            Color.Transparent
                        ),
                        start = Offset(glint * 42f, -8f),
                        end = Offset(glint * 42f + 18f, 40f)
                    )
                )
        )

        Text(
            text = text,
            color = goldLight,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(
                    color = goldLight,
                    offset = Offset(0f, 0f),
                    blurRadius = 2f
                )
            )
        )
    }
}



private fun darkGreenButtonColor(): Color =
    Color(0xFF032817)
