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
import androidx.compose.ui.draw.paint
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
    var isFocused by remember { mutableStateOf(false) }

    val transition = rememberInfiniteTransition(label = "royal_border")

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

    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFFE9A3)
    val dark = Color(0xFF101010)
    val productGreen = Color(0xFF006400)
    val priceText = Color(0xFFF2EBDD)

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF806018),
            gold,
            goldLight,
            gold,
            Color(0xFF806018)
        ),
        start = androidx.compose.ui.geometry.Offset(
            shine * 500f,
            shine * 200f
        ),
        end = androidx.compose.ui.geometry.Offset(
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
            containerColor = dark
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .paint(
    painter = painterResource(
        id = R.drawable.royal_product_card_bg
    ),
    sizeToIntrinsics = false,
    contentScale = ContentScale.Crop
)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x66191919),
                            Color(0x99090909)
                        )
                    )
                )
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    brush = if (isFocused) {
                        Brush.linearGradient(
                            listOf(
                                Color.White,
                                goldLight,
                                gold
                            )
                        )
                    } else {
                        borderBrush
                    },
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // صورة المنتج: بلا إطار داخلي أو زوايا مزخرفة.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .background(Color(0xFF181818)),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUrl.isNotBlank()) {
                    RemoteProductImage(product.imageUrl)
                } else {
                    Text(
                        text = product.name,
                        color = goldLight,
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
                        top = 5.dp,
                        bottom = 1.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = product.name,
                        color = productGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "%,.0f".format(product.price),
                            color = priceText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        Text(
                            text = "ل.س",
                            color = productGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(0.9f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // كلمة شراء نص فقط؛ لا زر ولا وظيفة ضغط.
                    Text(
                        text = "شراء",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(27.dp),
                        color = gold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )

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
                            fontSize = 11.sp,
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

@Composable
fun SmallCartButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(25.dp)
            .aspectRatio(1f)
            .defaultMinSize(
                minWidth = 0.dp,
                minHeight = 0.dp
            ),
        contentPadding = PaddingValues(0.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFD4AF37),
            contentColor = Color(0xFF17130A)
        )
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
