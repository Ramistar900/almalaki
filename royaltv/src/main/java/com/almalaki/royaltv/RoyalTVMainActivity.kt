
package com.almalaki.royaltv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val RoyalBlack = Color(0xFF050505)
private val RoyalGold = Color(0xFFD4AF37)
private val RoyalCream = Color(0xFFF5F0E5)
private val RoyalMutedGold = Color(0xFFB8A66A)
private val RoyalRed = Color(0xFFE53935)
private val RoyalCard = Color(0xFF111111)

class RoyalTVMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        setContent {
            MaterialTheme {
                RoyalTVChannelScreen()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoyalTVChannelScreen() {

    var currentTime by remember { mutableStateOf("--:--:--") }
    var currentDate by remember { mutableStateOf("----/--/--") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()

            currentTime = SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
            ).format(now)

            currentDate = SimpleDateFormat(
                "yyyy/MM/dd",
                Locale.getDefault()
            ).format(now)

            delay(1000L)
        }
    }

    val liveTransition = rememberInfiniteTransition(
        label = "royal_live"
    )

    val liveAlpha by liveTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "royal_live_alpha"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(RoyalBlack)
    ) {

        val scale = minOf(
            maxWidth.value / 1280f,
            maxHeight.value / 720f
        ).coerceIn(0.5f, 2.2f)

        val sidePadding = (28f * scale).dp
        val bottomPadding = (22f * scale).dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = sidePadding,
                    end = sidePadding,
                    top = (20f * scale).dp,
                    bottom = bottomPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "الملكي",
                color = RoyalGold,
                fontSize = (64f * scale).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height((8f * scale).dp))

            Text(
                text = "ROYAL TV",
                color = RoyalCream,
                fontSize = (34f * scale).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height((14f * scale).dp))

            Text(
                text = "هويتكم على شاشة الملكي",
                color = RoyalMutedGold,
                fontSize = (22f * scale).sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.weight(1f))

            // الشريط الإخباري المتحرك
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape((12f * scale).dp))
                    .background(RoyalCard)
                    .border(
                        width = (1.2f * scale).dp,
                        color = RoyalGold,
                        shape = RoundedCornerShape((12f * scale).dp)
                    )
                    .padding(
                        horizontal = (16f * scale).dp,
                        vertical = (10f * scale).dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "الملكي",
                    color = RoyalGold,
                    fontSize = (17f * scale).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.width((12f * scale).dp))

                Box(
                    modifier = Modifier
                        .width((1.5f * scale).dp)
                        .height((24f * scale).dp)
                        .background(RoyalMutedGold)
                )

                Spacer(modifier = Modifier.width((12f * scale).dp))

                Text(
                    text = "مرحبًا بكم في ROYAL TV  •  قريباً جداً •  أهلاً بكم في الملكي  •  ",
                    modifier = Modifier
                        .weight(1f)
                        .basicMarquee(),
                    color = RoyalCream,
                    fontSize = (14f * scale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }

            Spacer(modifier = Modifier.height((12f * scale).dp))

            // الهوية السفلية للقناة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // الشعار النصي المؤقت
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الملكي",
                        color = RoyalGold,
                        fontSize = (23f * scale).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.width((8f * scale).dp))

                    Text(
                        text = "ROYAL TV",
                        color = RoyalCream,
                        fontSize = (12f * scale).sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                // الوقت والتاريخ
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentTime,
                        color = RoyalGold,
                        fontSize = (17f * scale).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(
                        modifier = Modifier.height((2f * scale).dp)
                    )

                    Text(
                        text = currentDate,
                        color = RoyalCream,
                        fontSize = (11f * scale).sp,
                        maxLines = 1
                    )
                }

                // مؤشر البث المباشر
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(RoyalCard)
                        .border(
                            width = (1f * scale).dp,
                            color = RoyalRed,
                            shape = CircleShape
                        )
                        .padding(
                            horizontal = (12f * scale).dp,
                            vertical = (7f * scale).dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size((9f * scale).dp)
                            .alpha(liveAlpha)
                            .background(
                                color = RoyalRed,
                                shape = CircleShape
                            )
                    )

                    Spacer(
                        modifier = Modifier.width((7f * scale).dp)
                    )

                    Text(
                        text = "مباشر",
                        color = RoyalCream,
                        fontSize = (13f * scale).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
