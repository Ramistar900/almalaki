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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/*
 * ============================================================
 * Royal TV
 * المرحلة الأولى — واجهة العرض الأساسية
 * ============================================================
 *
 * هذه الشاشة مصممة كواجهة DISPLAY حقيقية للتلفزيون.
 *
 * الطبقات مستقلة:
 *
 * 1. Content Layer
 * 2. Logo Layer
 * 3. Live Layer
 * 4. Clock Layer
 * 5. Ticker Layer
 *
 * لاحقًا سنضيف داخل Content Layer:
 * - فيديو
 * - YouTube
 * - صور
 * - بوسترات
 * - عرض شرائح
 * - إعلانات
 * - الطلبات الجاهزة
 *
 * مهم:
 * لا يتم هنا إنشاء نظام طلبات أو YouTube جديد.
 * سنربط الأنظمة الموجودة في مراحل لاحقة.
 */

/* ============================================================
 * الألوان
 * ============================================================ */

private val RoyalTVBlack = Color(0xFF030303)
private val RoyalTVGold = Color(0xFFD4AF37)
private val RoyalTVGoldLight = Color(0xFFFFE9A3)
private val RoyalTVCream = Color(0xFFF5F0E5)
private val RoyalTVRed = Color(0xFFE53935)

/* ============================================================
 * الشاشة الرئيسية
 * ============================================================ */

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
         * Responsive Scale
         * ====================================================
         *
         * المرجع:
         * 1280px
         *
         * الهدف:
         * نفس التصميم يعمل على:
         * - TV
         * - Google TV
         * - Android TV Box
         * - رسيفر Android
         * - شاشات كبيرة
         */

        val widthDp = maxWidth.value

        val tvScale =
            (widthDp / 1280f)
                .coerceIn(0.72f, 2.40f)

        /*
         * ====================================================
         * المقاسات
         * ====================================================
         */

        val logoSize =
            (118f * tvScale).dp

        val liveTextSize =
            (24f * tvScale).sp

        val tickerTextSize =
            (25f * tvScale).sp

        /*
         * ====================================================
         * CONTENT LAYER
         * ====================================================
         *
         * هذه المساحة محجوزة للمحتوى الحقيقي.
         *
         * لاحقًا يمكن وضع:
         *
         * VideoPlayer
         * YouTube
         * Image
         * Poster
         * Slideshow
         * Advertisement
         *
         * بدون إعادة بناء باقي الشاشة.
         */

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            /*
             * خلفية محتوى بسيطة.
             *
             * لا نضع أي عناصر داخلها الآن.
             */
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RoyalTVBlack)
            )
        }

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
                        start = (34f * tvScale).dp,
                        top = (28f * tvScale).dp
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
                        end = (34f * tvScale).dp,
                        top = (32f * tvScale).dp
                    )
            )
        }

        /*
         * ====================================================
         * CLOCK LAYER
         * ====================================================
         *
         * نستخدم RoyalDateTime.kt الموجود فعليًا
         * في المشروع.
         *
         * لا نكرر منطق الساعة هنا.
         */

        if (showClock) {
            RoyalTVClock(
                scale = tvScale,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = (34f * tvScale).dp,
                        bottom = (28f * tvScale).dp
                    )
            )
        }

        /*
         * ====================================================
         * TICKER LAYER
         * ====================================================
         *
         * مستقل عن Content Layer.
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
                        start = (34f * tvScale).dp,
                        end = (300f * tvScale).dp,
                        bottom = (28f * tvScale).dp
                    )
            )
        }
    }
}

/* ============================================================
 * شعار Royal TV
 * ============================================================
 *
 * دوران 3D حول المحور العمودي.
 *
 * ملاحظة:
 * نستخدم مؤقتًا RC لأن ملف PNG الخاص بـ Royal TV
 * لم يتم تثبيته في drawable حتى الآن.
 *
 * عندما يصبح:
 *
 * res/drawable/royaltv.png
 *
 * متوفرًا، سنستبدل محتوى الشعار فقط.
 * ============================================================ */

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

                        /*
                         * أمامي — 3 ثوانٍ.
                         */
                        durationMillis = 9000

                        0f at 0
                        0f at 3000

                        /*
                         * دوران.
                         */
                        180f at 5000
                        360f at 6000

                        /*
                         * أمامي — 3 ثوانٍ.
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

                rotationY = rotationY

                /*
                 * cameraDistance ثابت حتى لا يتغير
                 * حجم الشعار أثناء الحركة.
                 */
                cameraDistance = 16f * 100f
            }
            .clip(
                RoundedCornerShape(
                    (size.value * 0.22f).dp
                )
            )
            .background(
                Color.Black.copy(alpha = 0.82f),
                RoundedCornerShape(
                    (size.value * 0.22f).dp
                )
            )
            .border(
                width =
                    (1.8f * (size.value / 118f)).dp,
                color = RoyalTVGold,
                shape =
                    RoundedCornerShape(
                        (size.value * 0.22f).dp
                    )
            ),
        contentAlignment = Alignment.Center
    ) {

        /*
         * لمعان بسيط على الشعار المؤقت.
         */

        Text(
            text = "RC",
            color = RoyalTVGoldLight,
            fontSize =
                (43f * (size.value / 118f)).sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}

/* ============================================================
 * LIVE INDICATOR
 * ============================================================ */

@Composable
private fun RoyalTVLiveIndicator(
    text: String,
    textSize: TextUnit,
    scale: Float,
    modifier: Modifier = Modifier
) {

    val transition =
        rememberInfiniteTransition(
            label = "royal_tv_live"
        )

    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 1100,
                        easing = LinearEasing
                    ),
                repeatMode = RepeatMode.Reverse
            ),
        label = "royal_tv_live_alpha"
    )

    Row(
        modifier = modifier
            .border(
                width = (1.6f * scale).dp,
                color = RoyalTVGold.copy(
                    alpha = 0.9f
                ),
                shape =
                    RoundedCornerShape(
                        (15f * scale).dp
                    )
            )
            .background(
                Color.Black.copy(alpha = 0.62f),
                RoundedCornerShape(
                    (15f * scale).dp
                )
            )
            .padding(
                horizontal = (19f * scale).dp,
                vertical = (8f * scale).dp
            ),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.Center
    ) {

        Box(
            modifier = Modifier
                .size(
                    (10f * scale).dp
                )
                .background(
                    RoyalTVRed.copy(alpha = alpha),
                    RoundedCornerShape(50)
                )
        )

        Spacer(
            modifier = Modifier.width(
                (9f * scale).dp
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

/* ============================================================
 * CLOCK
 * ============================================================
 *
 * مرتبط مباشرة بـ RoyalDateTime.kt
 *
 * الشكل:
 *
 * 6:56 ص
 * الأربعاء 6/6/2025
 *
 * ويتم التحديث كل ثانية بواسطة الملف الموجود.
 * ============================================================ */

@Composable
private fun RoyalTVClock(
    scale: Float,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .border(
                width = (1.5f * scale).dp,
                color = RoyalTVGold.copy(alpha = 0.85f),
                shape =
                    RoundedCornerShape(
                        (14f * scale).dp
                    )
            )
            .background(
                Color.Black.copy(alpha = 0.68f),
                RoundedCornerShape(
                    (14f * scale).dp
                )
            )
            .padding(
                horizontal = (18f * scale).dp,
                vertical = (9f * scale).dp
            )
    ) {

        RoyalDateTime(
            language = "ar",
            style = TextStyle(
                color = RoyalTVGoldLight,
                fontSize = (22f * scale).sp,
                fontWeight = FontWeight.Bold,
                lineHeight = (27f * scale).sp
            )
        )
    }
}

/* ============================================================
 * TICKER
 * ============================================================ */

@Composable
private fun RoyalTVTicker(
    text: String,
    textSize: TextUnit,
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

    /*
     * الحركة من اليمين إلى اليسار.
     */

    val tickerX by transition.animateFloat(
        initialValue = 0f,
        targetValue = -1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 16000,
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
                (62f * scale).dp
            )
            .clip(
                RoundedCornerShape(
                    (17f * scale).dp
                )
            )
            .border(
                width = (1.7f * scale).dp,
                color = RoyalTVGold,
                shape =
                    RoundedCornerShape(
                        (17f * scale).dp
                    )
            )
            .background(
                Color.Black.copy(alpha = 0.68f),
                RoundedCornerShape(
                    (17f * scale).dp
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
                    textWidthPx = it.size.width
                }
            )

            Spacer(
                modifier = Modifier.width(
                    (150f * scale).dp
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
