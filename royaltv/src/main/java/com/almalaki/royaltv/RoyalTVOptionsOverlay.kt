package com.almalaki.royaltv

import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

private const val HOME_SCREEN_BROADCAST_TITLE =
"بث محتوى الشاشة الرئيسية"

private data class RoyalOption(
val icon: String,
val title: String,
val description: String
)

private val royalOptions = listOf(
RoyalOption(
icon = "📶",
title = "البلوتوث",
description = "اكتشاف الأجهزة القريبة وإدارة الاتصال"
),
RoyalOption(
icon = "📡",
title = "Wi-Fi و Wi-Fi Direct",
description = "الاتصال المحلي والمباشر بين الأجهزة"
),
RoyalOption(
icon = "🔐",
title = "أذونات الاتصال",
description = "متابعة أذونات البلوتوث والشبكة"
),
RoyalOption(
icon = "📺",
title = "إدارة الشاشات",
description = "إعداد الأجهزة وأسماء الشاشات"
),
RoyalOption(
icon = "🔄",
title = "الاتصال التلقائي",
description = "إعدادات إعادة الاتصال عند الانقطاع"
),
RoyalOption(
icon = "🎛️",
title = "إعدادات القناة",
description = "خيارات العرض والتحكم بالقناة"
),
RoyalOption(
icon = "💾",
title = "الحفظ والاستعادة",
description = "خيارات حفظ إعدادات ROYAL TV"
),
RoyalOption(
    icon = "📺",
    title = HOME_SCREEN_BROADCAST_TITLE,
    description = "مشاركة الشاشة بعد موافقة Android"
),
RoyalOption(
    icon = "📤",
    title = "مشاركة التطبيق",
    description = "شارك رابط تنزيل ROYAL TV الرسمي"
)
)

@Composable
fun RoyalTVOptionsOverlay(
modifier: Modifier = Modifier,
onRequestHomeScreenBroadcast: () -> Unit = {},
onRequestExit: () -> Unit = {}
) {
var menuOpen by remember {
mutableStateOf(false)
}
val context = LocalContext.current

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
        ) + fadeIn(
            animationSpec = tween(240)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(260)
        ) + fadeOut(
            animationSpec = tween(180)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = OptionsBlack,
                    shape = RoundedCornerShape(
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
                .padding(14.dp)
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
                    .heightOneDp()
                    .background(
                        OptionsGold.copy(alpha = 0.65f)
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(top = 10.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                royalOptions.forEach { option ->
                    val isSelected = selectedOption == option

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(12.dp)
                            )
                            .background(
                                if (isSelected) {
                                    OptionsGold.copy(alpha = 0.14f)
                                } else {
                                    OptionsCard
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) {
                                    OptionsGold
                                } else {
                                    OptionsGold.copy(alpha = 0.24f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedOption = option

                                if (
    option.title == HOME_SCREEN_BROADCAST_TITLE
) {
    menuOpen = false
    onRequestHomeScreenBroadcast()
} else if (option.title == "مشاركة التطبيق") {
    menuOpen = false

    val downloadUrl =
        "https://github.com/Ramistar900/almalaki/releases/download/royaltv-v1.3/royaltv-release.apk"

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_SUBJECT,
            "تنزيل ROYAL TV"
        )
        putExtra(
            Intent.EXTRA_TEXT,
            "حمّل تطبيق ROYAL TV من الرابط الرسمي:\n$downloadUrl"
        )
    }

    context.startActivity(
        Intent.createChooser(
            shareIntent,
            "مشاركة ROYAL TV"
        )
    )
                                }
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
            }

            Text(
                text = selectedOption?.let { option ->
                    "تم اختيار: ${option.title}\n" +
                        if (
                            option.title ==
                            HOME_SCREEN_BROADCAST_TITLE
                        ) {
                            "تم طلب فتح إذن مشاركة الشاشة."
                        } else {
                            "سيتم ربط الوظيفة الفعلية ضمن مراحل الاتصال."
                        }
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
                width = 1.5.dp,
                color = OptionsGold,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable {
                scope.launch {
                    buttonScale.animateTo(
                        targetValue = 0.88f,
                        animationSpec = tween(75)
                    )
                    buttonScale.animateTo(
                        targetValue = 1.08f,
                        animationSpec = tween(110)
                    )
                    buttonScale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(130)
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

    // زر خروج مستقل عن قائمة الخيارات.
    Row(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(OptionsBlack)
            .border(
                width = 1.5.dp,
                color = OptionsGold,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable {
                onRequestExit()
            }
            .padding(
                horizontal = 12.dp,
                vertical = 9.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "⏻",
            color = OptionsGold,
            fontSize = 20.sp
        )
        Text(
            text = "خروج",
            color = OptionsGold,
            fontSize = 12.sp
        )
    }
}

}

private fun Modifier.heightOneDp(): Modifier {
return this.size(
width = 1.dp,
height = 1.dp
)
}
