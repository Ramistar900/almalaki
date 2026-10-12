package com.almalaki.royaltv

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset

private val BroadcastGold = Color(0xFFD4AF37)
private val BroadcastCream = Color(0xFFF9F295)
private val BroadcastBlack = Color(0xFF111111)

/**
 * هوية ROYAL TV العائمة.
 *
 * تتضمن:
 * - الشعار الملكي المتحرك.
 * - التاريخ والوقت.
 * - الشريط الإخباري المتحرك.
 *
 * هذه الواجهة تخص وحدة ROYAL TV فقط.
 */
@Composable
fun RoyalTVBroadcastIdentity(
    newsText: String = RoyalTVTickerManager.getText(),
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        /*
         * مقياس تلقائي حسب عرض الشاشة بوحدة dp.
         * 360dp هو العرض المرجعي للهاتف.
         */
        val scale = (maxWidth.value / 360f)
            .coerceIn(0.85f, 2.6f)

        val logoSize = (48f * scale).dp
        val dateSize = (9f * scale).sp
        val timeSize = (13f * scale).sp
        val tickerTextSize = (12f * scale).sp
        val tickerHeight = (22f * scale).dp

        var now by remember {
            mutableStateOf(Date())
        }

        LaunchedEffect(Unit) {
            while (true) {
                now = Date()
                delay(1000L)
            }
        }

        /*
         * يطابق تنسيق التاريخ في الشريط السفلي الأصلي.
         * يستخدم المنطقة الزمنية الافتراضية للجهاز.
         */
        val dateText = remember(now) {
            SimpleDateFormat(
                "EEEE d/M/yyyy",
                Locale("ar")
            ).format(now)
        }

        val timeText = remember(now) {
            SimpleDateFormat(
                "HH:mm",
                Locale.US
            ).format(now)
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(
                    horizontal = (8f * scale).dp,
                    vertical = (5f * scale).dp
                ),
            verticalAlignment = Alignment.Bottom
        ) {
            /*
             * الشعار الأساسي المتحرك.
             * يستخدم طبقتَي PNG داخل وحدة ROYAL TV.
             */
            Box(
                modifier = Modifier.size(logoSize),
                contentAlignment = Alignment.Center
            ) {
                RoyalLayeredLogoAnimation(
                    crownShieldRes =
                        R.drawable.royal_crest_layer,
                    rcRes =
                        R.drawable.royal_rc_layer,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = "ROYAL TV"
                )
            }

            Spacer(
                Modifier.width((5f * scale).dp)
            )

            /*
             * التاريخ والوقت.
             */
            Column(
                modifier = Modifier.width(
                    (82f * scale).dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = dateText,
                    color = BroadcastGold,
                    fontSize = dateSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )

                Spacer(
                    Modifier.height((2f * scale).dp)
                )

                Box(
                    modifier = Modifier
                        .width((68f * scale).dp)
                        .height((25f * scale).dp)
                        .clip(
                            RoundedCornerShape(
                                (7f * scale).dp
                            )
                        )
                        .border(
                            width = (1f * scale).dp,
                            color = BroadcastGold,
                            shape = RoundedCornerShape(
                                (7f * scale).dp
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    /*
                     * عرض الوقت بالأرقام اللاتينية
                     * مع المحافظة على تنسيق HH:mm.
                     */
                    val parts = timeText.split(":")

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.Center
                    ) {
                        Text(
                            text = parts.getOrElse(0) { "00" },
                            color = Color.White,
                            fontSize = timeSize,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = ":",
                            color = BroadcastGold,
                            fontSize = timeSize,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = parts.getOrElse(1) { "00" },
                            color = Color.White,
                            fontSize = timeSize,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(
                Modifier.width((6f * scale).dp)
            )

            /*
             * الشريط الإخباري الأصلي:
             * مستطيل أسود رفيع، إطار ذهبي،
             * حركة من اليمين إلى اليسار.
             */
            RoyalTVOriginalStyleTicker(
                text = newsText,
                scale = scale,
                height = tickerHeight,
                textSize = tickerTextSize,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RoyalTVOriginalStyleTicker(
    text: String,
    scale: Float,
    height: androidx.compose.ui.unit.Dp,
    textSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    var containerWidth by remember {
        mutableIntStateOf(0)
    }

    var contentWidth by remember(text) {
        mutableIntStateOf(0)
    }

    val ready = containerWidth > 0 && contentWidth > 0

    androidx.compose.runtime.key(text, ready) {
        val transition = rememberInfiniteTransition(
            label = "royal_tv_original_ticker"
        )

        val offset by transition.animateFloat(
            initialValue = containerWidth.toFloat(),
            targetValue = -contentWidth.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 25_000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "royal_tv_ticker_offset"
        )

        Box(
            modifier = modifier
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(BroadcastBlack)
                .border(
                    width = (1f * scale).dp,
                    color = BroadcastGold,
                    shape = RoundedCornerShape(50)
                )
                .clipToBounds()
                .onSizeChanged {
                    containerWidth = it.width
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .onSizeChanged {
                        contentWidth = it.width
                    }
                    .offset {
                        IntOffset(
                            x = if (ready) {
                                offset.roundToInt()
                            } else {
                                containerWidth
                            },
                            y = 0
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                val items = text.split("&")

                items.forEachIndexed { index, item ->
                    Text(
                        text = item.trim(),
                        color = BroadcastCream,
                        fontSize = textSize,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )

                    /*
                     * فاصل ثابت لتخفيف الحمل الرسومي.
                     * الشعار المتحرك الرئيسي يبقى في الأسفل.
                     */
                    if (index < items.lastIndex) {
                        Spacer(
                            Modifier.width((10f * scale).dp)
                        )

                        Image(
                            painter = painterResource(
                                R.drawable.royal_crest_layer
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(
                                (18f * scale).dp
                            ),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(
                            Modifier.width((10f * scale).dp)
                        )
                    }
                }
            }
        }
    }
}
