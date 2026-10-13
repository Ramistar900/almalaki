package com.almalaki.royaltv

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val BroadcastGold = Color(0xFFD4AF37)
private val BroadcastCream = Color(0xFFF9F295)

/* فيروزي داكن ممزوج بالأسود */
private val BroadcastTickerBackground = Color(0xFF153536)

private val BroadcastTimeColor = Color.White
private val BroadcastLiveRed = Color(0xFFFF3030)

/**
 * هوية البث الخاصة بوحدة ROYAL TV فقط.
 *
 * - الشعار الملكي بطبقاته الأصلية.
 * - ساعة فقط دون تاريخ.
 * - خلفية الساعة شفافة.
 * - شريط فيروزي داكن بإطار ذهبي ثابت.
 * - حركة النص الإخباري فقط.
 * - مؤشر مباشر مستقل.
 */
@Composable
fun RoyalTVBroadcastIdentity(
    newsText: String = RoyalTVTickerManager.getText(),
    modifier: Modifier = Modifier
) {
        val context = LocalContext.current

    LaunchedEffect(context) {
        RoyalTVBroadcastSettings.initialize(context)
    }

    val tickerEnabled by
        RoyalTVBroadcastSettings.tickerEnabled.collectAsState()
        
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
    
        val scale = (maxWidth.value / 360f)
            .coerceIn(0.85f, 2.6f)

        val logoSize = (48f * scale).dp
        val timeTextSize = (13f * scale).sp
        val tickerTextSize = (12f * scale).sp
        val tickerHeight = (24f * scale).dp
        val liveTextSize = (10f * scale).sp

        var now by remember {
            mutableStateOf(Date())
        }

        LaunchedEffect(Unit) {
            while (true) {
                now = Date()
                delay(1000L)
            }
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                (6f * scale).dp
            )
        ) {
            /* الشعار الملكي الأصلي */
            Box(
                modifier = Modifier.size(logoSize),
                contentAlignment = Alignment.Center
            ) {
                RoyalLayeredLogoAnimation(
                    crownShieldRes = R.drawable.royal_crest_layer,
                    rcRes = R.drawable.royal_rc_layer,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = "ROYAL TV"
                )
            }

            /* الساعة فقط: خلفية شفافة وإطار ذهبي */
            Box(
                modifier = Modifier
                    .width((68f * scale).dp)
                    .height((29f * scale).dp)
                    .clip(
                        RoundedCornerShape((8f * scale).dp)
                    )
                    .border(
                        width = (1f * scale).dp,
                        color = BroadcastGold,
                        shape = RoundedCornerShape(
                            (8f * scale).dp
                        )
                    )
                    .padding(horizontal = (3f * scale).dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = timeText,
                    color = BroadcastTimeColor,
                    fontSize = timeTextSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }

                        /* الشريط الإخباري المتحرك */
            if (tickerEnabled) {
                RoyalTVOriginalStyleTicker(
                    text = newsText,
                    scale = scale,
                    height = tickerHeight,
                    textSize = tickerTextSize,
                    modifier = Modifier.weight(1f)
                )
            }

            /* مؤشر مباشر مستقل عن الشريط */
            RoyalTVLiveIndicator(
                scale = scale,
                textSize = liveTextSize
            )
        }
    }
}

@Composable
private fun RoyalTVLiveIndicator(
    scale: Float,
    textSize: androidx.compose.ui.unit.TextUnit
) {
    val transition = rememberInfiniteTransition(
        label = "royal_tv_live_indicator"
    )

    val dotAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 700,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "royal_tv_live_dot_alpha"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(
                width = (1f * scale).dp,
                color = BroadcastGold,
                shape = RoundedCornerShape(50)
            )
            .padding(
                horizontal = (7f * scale).dp,
                vertical = (5f * scale).dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            (4f * scale).dp
        )
    ) {
        Box(
            modifier = Modifier
                .size((7f * scale).dp)
                .background(
                    BroadcastLiveRed.copy(alpha = dotAlpha),
                    RoundedCornerShape(50)
                )
        )

        Text(
            text = "مباشر",
            color = Color.White,
            fontSize = textSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
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
            label = "royal_tv_ticker"
        )

        val offset by transition.animateFloat(
            initialValue = containerWidth.toFloat(),
            targetValue = -contentWidth.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 25000,
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
                .background(BroadcastTickerBackground)
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

                    if (index < items.lastIndex) {
                        Spacer(
                            Modifier.width((10f * scale).dp)
                        )

                        /* فاصل ثابت بلا حركة لمعان */
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
