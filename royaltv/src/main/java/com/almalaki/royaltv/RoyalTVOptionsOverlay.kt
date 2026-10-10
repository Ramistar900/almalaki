
package com.almalaki.royaltv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val OptionsGold = Color(0xFFD4AF37)
private val OptionsBlack = Color(0xFF080C12)
private val OptionsCard = Color(0xFF171B23)
private val OptionsText = Color(0xFFDCE5EE)

private data class RoyalOption(
    val icon: String,
    val title: String,
    val description: String
)

private val royalOptions = listOf(
    RoyalOption(
        "📶",
        "البلوتوث",
        "اكتشاف الأجهزة القريبة وإدارة الاتصال"
    ),
    RoyalOption(
        "📡",
        "Wi-Fi و Wi-Fi Direct",
        "الاتصال المحلي والمباشر بين الأجهزة"
    ),
    RoyalOption(
        "🔐",
        "أذونات الاتصال",
        "متابعة أذونات البلوتوث والشبكة"
    ),
    RoyalOption(
        "📺",
        "إدارة الشاشات",
        "إعداد الأجهزة وأسماء الشاشات"
    ),
    RoyalOption(
        "🔄",
        "الاتصال التلقائي",
        "إعدادات إعادة الاتصال عند الانقطاع"
    ),
    RoyalOption(
        "🎛️",
        "إعدادات القناة",
        "خيارات العرض والتحكم بالقناة"
    ),
    RoyalOption(
        "💾",
        "الحفظ والاستعادة",
        "خيارات حفظ إعدادات ROYAL TV"
    )
)

@Composable
fun RoyalTVOptionsOverlay(
    modifier: Modifier = Modifier
) {
    var menuOpen by remember {
        mutableStateOf(false)
    }

    var selectedOption by remember {
        mutableStateOf<RoyalOption?>(null)
    }

    val rotation by animateFloatAsState(
        targetValue = if (menuOpen) 180f else 0f,
        animationSpec = tween(320),
        label = "royal_options_rotation"
    )

    val buttonScale = remember {
        Animatable(1f)
    }

    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (menuOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(alpha = 0.58f)
                    )
                    .clickable {
                        menuOpen = false
                    }
            )
        }

        AnimatedVisibility(
            visible = menuOpen,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.86f)
                .widthIn(max = 340.dp),
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(320)
            ) + fadeIn(tween(240)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(260)
            ) + fadeOut(tween(180))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        OptionsBlack,
                        RoundedCornerShape(
                            topStart = 18.dp,
                            bottomStart = 18.dp
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = OptionsGold.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            bottomStart = 18.dp
                        )
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "ROYAL TV",
                            color = OptionsGold,
                            fontSize = 21.sp
                        )

                        Text(
                            text = "خيارات القناة والاتصال",
                            color = OptionsText,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = "✕",
                        color = OptionsGold,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                menuOpen = false
                            }
                            .padding(10.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(
                            width = 1.dp,
                            height = 1.dp
                        )
                        .background(
                            OptionsGold.copy(alpha = 0.65f)
                        )
                )

                royalOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selectedOption == option) {
                                    OptionsGold.copy(alpha = 0.14f)
                                } else {
                                    OptionsCard
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (selectedOption == option) {
                                    OptionsGold
                                } else {
                                    OptionsGold.copy(alpha = 0.24f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedOption = option
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = option.icon,
                            fontSize = 23.sp
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = option.title,
                                color = OptionsGold,
                                fontSize = 14.sp
                            )

                            Text(
                                text = option.description,
                                color = OptionsText.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = "‹",
                            color = OptionsGold,
                            fontSize = 24.sp
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = selectedOption?.let {
                        "تم اختيار: ${it.title}\n" +
                            "سيتم ربط الوظيفة الفعلية ضمن مراحل الاتصال."
                    } ?: "اختر قسمًا لعرض خياراته.",
                    color = OptionsText,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(OptionsCard)
                        .padding(12.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .graphicsLayer {
                    scaleX = buttonScale.value
                    scaleY = buttonScale.value
                }
                .clip(RoundedCornerShape(14.dp))
                .background(OptionsBlack)
                .border(
                    1.5.dp,
                    OptionsGold,
                    RoundedCornerShape(14.dp)
                )
                .clickable {
                    scope.launch {
                        buttonScale.animateTo(
                            0.88f,
                            tween(75)
                        )
                        buttonScale.animateTo(
                            1.08f,
                            tween(110)
                        )
                        buttonScale.animateTo(
                            1f,
                            tween(130)
                        )
                    }

                    menuOpen = !menuOpen
                }
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "⚙",
                color = OptionsGold,
                fontSize = 22.sp,
                modifier = Modifier.graphicsLayer {
                    rotationZ = rotation
                }
            )

            Text(
                text = "خيارات",
                color = OptionsGold,
                fontSize = 12.sp
            )
        }
    }
}
