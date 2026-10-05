package com.almalaki.cafe

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/*
 * ============================================================
 * Royal TV
 * ============================================================
 *
 * أساس مستقل لشاشة Royal TV.
 *
 * الطبقات مستقلة:
 *
 * 1. Content Layer
 * 2. Logo Layer
 * 3. Live Layer
 * 4. Clock Layer
 * 5. Ticker Layer
 *
 * لاحقًا سنضيف:
 * - الفيديو
 * - YouTube
 * - الصور
 * - البوسترات
 * - عرض الشرائح
 * - الإعلانات
 * - الجدولة
 * - الطلبات الجاهزة
 * - التحكم من الهاتف والتابلت
 * - التحكم المحلي من التلفزيون
 *
 * مهم:
 * إعدادات Royal TV ستكون مستقلة عن إعدادات العميل.
 */

private val RoyalTVBlack = Color(0xFF030303)
private val RoyalTVGold = Color(0xFFD4AF37)
private val RoyalTVGoldLight = Color(0xFFFFE9A3)
private val RoyalTVCream = Color(0xFFF5F0E5)
private val RoyalTVRed = Color(0xFFE53935)

/*
 * الشاشة الرئيسية لـ Royal TV
 */
@Composable
fun RoyalTVScreen(
    modifier: Modifier = Modifier,
    tickerText: String =
        "Royal Coffee • جودة فاخرة • أهلاً بكم في Royal TV",
    liveText: String = "مباشر",
    showLogo: Boolean = true,
    showTicker: Boolean = true,
    showClock: Boolean = true,
    showLive: Boolean = true
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(RoyalTVBlack)
    ) {

        /*
         * ====================================================
         * Responsive TV Scale
         * ====================================================
         *
         * لا نستخدم حجمًا ثابتًا لكل الشاشات.
         *
         * كلما كبرت شاشة Royal TV:
         * - يكبر الشعار
         * - يكبر مباشر
         * - يكبر الشريط
         * - يكبر الوقت
         */
        val density = LocalDensity.current

        val referenceWidth = 1280f

        val widthPx = with(density) {
            maxWidth.toPx()
        }

        val tvScale =
            (widthPx / referenceWidth)
                .coerceIn(0.72f, 2.40f)

        val logoSize =
            (86f * tvScale).dp

        val liveTextSize =
            (22f * tvScale).sp

        val tickerTextSize =
            (24f * tvScale).sp

        val clockTextSize =
            (22f * tvScale).sp

        /*
         * ====================================================
         * CONTENT LAYER
         * ====================================================
         *
         * حاليًا فارغة.
         *
         * لاحقًا ستكون هنا:
         * فيديو / صورة / YouTube / بوستر / إعلان...
         */
        Box(
            modifier = Modifier.fillMaxSize()
        )

        /*
         * ====================================================
         * LOGO LAYER
         * ====================================================
         */
        if (showLogo) {

            RoyalTVAnimatedLogo(
                size = logoSize,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = (28f * tvScale).dp,
                        top = (24f * tvScale).dp
                    )
            )
        }

        /*
         * ====================================================
         * LIVE LAYER
         * ====================================================
         */
        if (showLive) {

            RoyalTVLiveIndicator(
                text = liveText,
                textSize = liveTextSize,
                scale = tvScale,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        end = (28f * tvScale).dp,
                        top = (28f * tvScale).dp
                    )
            )
        }

        /*
         * ====================================================
         * CLOCK LAYER
         * ====================================================
         *
         * مؤقتًا نعرض Royal TV.
         *
         * سيتم ربط RoyalDateTime.kt لاحقًا.
         */
        if (showClock) {

            RoyalTVClockPlaceholder(
                textSize = clockTextSize,
                scale = tvScale,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = (28f * tvScale).dp,
                        bottom = (24f * tvScale).dp
                    )
            )
        }

        /*
         * ====================================================
         * TICKER LAYER
         * ====================================================
         *
         * مستقل عن الفيديو والشعار.
         */
        if (showTicker) {

            RoyalTVTicker(
                text = tickerText,
                textSize = tickerTextSize,
                scale = tvScale,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        start = (28f * tvScale).dp,
                        end = (230f * tvScale).dp,
                        bottom = (24f * tvScale).dp
                    )
            )
        }
    }
}

/*
 * ============================================================
 * Royal TV Logo
 * ============================================================
 *
 * حركة حول المحور العمودي rotationY.
 *
 * ليست دورانًا مسطحًا.
 */
@Composable
private fun RoyalTVAnimatedLogo(
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {

    val transition =
        rememberInfiniteTransition(
            label = "royal_tv_logo_rotation"
        )

    val rotationY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes {

                        durationMillis = 9000

                        /*
                         * واجهة أمامية لمدة 3 ثوانٍ.
                         */
                        0f at 0
                        0f at 3000

                        /*
                         * الدوران.
                         */
                        180f at 5000
                        360f at 6000

                        /*
                         * توقف أمامي.
                         */
                        360f at 9000
                    },
                repeatMode = RepeatMode.Restart
            ),
        label = "royal_tv_logo_rotation_y"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {

                this.rotationY = rotationY

                /*
                 * زيادة المسافة حتى يظهر تأثير 3D بشكل صحيح.
                 */
                cameraDistance =
                    14f * density
            }
            .clip(
                RoundedCornerShape(
                    size * 0.24f
                )
            )
            .background(
                Color.Black.copy(alpha = 0.72f),
                RoundedCornerShape(
                    size * 0.24f
                )
            )
            .border(
                width =
                    (1.5f * (size.value / 86f)).dp,
                color = RoyalTVGold,
                shape =
                    RoundedCornerShape(
                        size * 0.24f
                    )
            ),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = "RC",
            color = RoyalTVGoldLight,
            fontSize =
                (34f * (size.value / 86f)).sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

/*
 * ============================================================
 * LIVE INDICATOR
 * ============================================================
 */
@Composable
private fun RoyalTVLiveIndicator(
    text: String,
    textSize: androidx.compose.ui.unit.TextUnit,
    scale: Float,
    modifier: Modifier = Modifier
) {

    val transition =
        rememberInfiniteTransition(
            label = "royal_tv_live"
        )

    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 1200,
                        easing = LinearEasing
                    ),
                repeatMode = RepeatMode.Reverse
            ),
        label = "royal_tv_live_alpha"
    )

    Row(
        modifier = modifier
            .border(
                width = (1.5f * scale).dp,
                color = RoyalTVGold.copy(
                    alpha = 0.82f
                ),
                shape =
                    RoundedCornerShape(
                        (14f * scale).dp
                    )
            )
            .background(
                Color.Black.copy(alpha = 0.42f),
                RoundedCornerShape(
                    (14f * scale).dp
                )
            )
            .padding(
                horizontal = (16f * scale).dp,
                vertical = (7f * scale).dp
            ),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.Center
    ) {

        Box(
            modifier = Modifier
                .size(
                    (9f * scale).dp
                )
                .background(
                    RoyalTVRed.copy(
                        alpha = alpha
                    ),
                    RoundedCornerShape(50)
                )
        )

        Spacer(
            modifier = Modifier.width(
                (8f * scale).dp
            )
        )

        Text(
            text = text,
            color = RoyalTVCream,
            fontSize = textSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/*
 * ============================================================
 * CLOCK PLACEHOLDER
 * ============================================================
 *
 * لن نعدل RoyalDateTime.kt الآن.
 */
@Composable
private fun RoyalTVClockPlaceholder(
    textSize: androidx.compose.ui.unit.TextUnit,
    scale: Float,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier
            .border(
                width = (1.3f * scale).dp,
                color = RoyalTVGold.copy(
                    alpha = 0.72f
                ),
                shape =
                    RoundedCornerShape(
                        (12f * scale).dp
                    )
            )
            .background(
                Color.Black.copy(alpha = 0.44f),
                RoundedCornerShape(
                    (12f * scale).dp
                )
            )
            .padding(
                horizontal = (14f * scale).dp,
                vertical = (6f * scale).dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = "Royal TV",
            color = RoyalTVGoldLight,
            fontSize = textSize,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/*
 * ============================================================
 * TICKER
 * ============================================================
 *
 * شريط بعرض الشاشة تقريبًا.
 *
 * الحركة مستقلة عن طبقة الفيديو.
 */
@Composable
private fun RoyalTVTicker(
    text: String,
    textSize: androidx.compose.ui.unit.TextUnit,
    scale: Float,
    modifier: Modifier = Modifier
) {

    var textWidthPx by remember(text) {
        mutableIntStateOf(0)
    }

    val transition =
        rememberInfiniteTransition(
            label = "royal_tv_ticker"
        )

    val tickerX by transition.animateFloat(
        initialValue = 0f,
        targetValue = -1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 15000,
                        easing = LinearEasing
                    ),
                repeatMode = RepeatMode.Restart
            ),
        label = "royal_tv_ticker_progress"
    )

    val travelPx =
        if (textWidthPx > 0) {
            textWidthPx.toFloat()
        } else {
            1f
        }

    val offsetPx =
        tickerX * travelPx

    Box(
        modifier = modifier
            .height(
                (56f * scale).dp
            )
            .clip(
                RoundedCornerShape(
                    (16f * scale).dp
                )
            )
            .border(
                width = (1.6f * scale).dp,
                color = RoyalTVGold,
                shape =
                    RoundedCornerShape(
                        (16f * scale).dp
                    )
            )
            .background(
                Color.Black.copy(alpha = 0.48f),
                RoundedCornerShape(
                    (16f * scale).dp
                )
            ),
        contentAlignment =
            Alignment.CenterStart
    ) {

        Row(
            modifier = Modifier
                .offset {
                    IntOffset(
                        offsetPx.roundToInt(),
                        0
                    )
                }
                .wrapContentWidth(
                    unbounded = true
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = text,
                color = RoyalTVCream,
                fontSize = textSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                onTextLayout = {
                    textWidthPx =
                        it.size.width
                }
            )

            Spacer(
                modifier = Modifier.width(
                    (120f * scale).dp
                )
            )

            Text(
                text = text,
                color = RoyalTVCream,
                fontSize = textSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}
