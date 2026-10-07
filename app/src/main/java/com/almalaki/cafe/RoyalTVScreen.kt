package com.almalaki.cafe

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

private val RoyalTVBlack = Color(0xFF030303)
private val RoyalTVGold = Color(0xFFD4AF37)
private val RoyalTVGoldLight = Color(0xFFFFE9A3)
private val RoyalTVCream = Color(0xFFF5F0E5)
private val RoyalTVRed = Color(0xFFE53935)

/**
 * ============================================================
 * ROYAL TV SCREEN
 * ============================================================
 *
 * شاشة القناة الأساسية.
 *
 * الموجود:
 *
 * 🔴 مباشر
 * 📰 شريط الأخبار السفلي
 * 🕐 الوقت والتاريخ السفلي
 * 👑 الشعار الملكي الحقيقي بطبقتيه في الأسفل
 *
 * تم الحفاظ على:
 *
 * - نظام Responsive
 * - الشعار الحقيقي
 * - حركة الشعار
 * - حركة RC ثلاثية الأبعاد
 * - لمعة الدرع والتاج
 * - الشريط المتحرك
 * - الوقت والتاريخ
 *
 * ============================================================
 * CENTRAL LAYOUT SYSTEM
 * ============================================================
 *
 * كل أحجام ومواقع العناصر التي تم تجهيزها
 * أصبحت تُقرأ من:
 *
 * RoyalTVLayoutSettings.kt
 *
 * ============================================================
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
    val context = LocalContext.current

    /*
     * ========================================================
     * CENTRAL LAYOUT SETTINGS
     * ========================================================
     *
     * المصدر المركزي الوحيد للتخطيط الحالي.
     *
     * لا ننشئ SharedPreferences جديدة هنا.
     */
    val layoutSettings = remember(context) {
        loadRoyalTVLayoutSettings(context)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(RoyalTVBlack)
    ) {

        /*
         * ====================================================
         * RESPONSIVE SCALE
         * ====================================================
         *
         * المرجع:
         * 1280 × 720
         */
        val widthDp = maxWidth.value

        val tvScale =
            (widthDp / 1280f)
                .coerceIn(0.72f, 2.40f)

        /*
         * ====================================================
         * MANUAL RESPONSIVE PERCENTAGES
         * ====================================================
         *
         * جميع القيم محفوظة في:
         *
         * RoyalTVLayoutSettings
         */
        val tickerSizePercent =
            clampRoyalTVSizePercent(
                layoutSettings.tickerSizePercent
            )

        val timeSizePercent =
            clampRoyalTVSizePercent(
                layoutSettings.timeSizePercent
            )

        val dateSizePercent =
            clampRoyalTVSizePercent(
                layoutSettings.dateSizePercent
            )

        val liveSizePercent =
            clampRoyalTVSizePercent(
                layoutSettings.liveSizePercent
            )

        /*
         * ====================================================
         * LIVE SIZE
         * ====================================================
         */
        val liveMultiplier =
            liveSizePercent / 100f

        val liveTextSize =
            (24f * tvScale * liveMultiplier).sp

        /*
         * ====================================================
         * TICKER SIZE
         * ====================================================
         *
         * الحجم الأساسي:
         * 25sp
         *
         * ثم Responsive
         * ثم الحجم اليدوي 60% → 160%
         */
        val tickerMultiplier =
            tickerSizePercent / 100f

        val tickerTextSize =
            (25f * tvScale * tickerMultiplier).sp

        val tickerHeight =
            (62f * tvScale * tickerMultiplier).dp

        val tickerCornerRadius =
            (17f * tvScale * tickerMultiplier).dp

        /*
         * ====================================================
         * CLOCK SIZE
         * ====================================================
         *
         * الآن الوقت والتاريخ مستقلان تماماً.
         *
         * الوقت:
         * timeSizePercent
         *
         * التاريخ:
         * dateSizePercent
         *
         * لا نستخدم maxOf() بينهما بعد الآن.
         */
        val timeMultiplier =
            timeSizePercent / 100f

        val dateMultiplier =
            dateSizePercent / 100f

        val timeTextSize =
            (22f * tvScale * timeMultiplier).sp

        val timeLineHeight =
            (27f * tvScale * timeMultiplier).sp

        val dateTextSize =
            (22f * tvScale * dateMultiplier).sp

        val dateLineHeight =
            (27f * tvScale * dateMultiplier).sp

        /*
         * ====================================================
         * RESPONSIVE LOGO SIZE
         * ====================================================
         *
         * الحجم الأساسي Responsive
         * + الحجم اليدوي المحفوظ.
         */
        val bottomLogoSize =
            RoyalTVResponsiveLogo.size(
                scale = tvScale,
                sizePercent =
                    layoutSettings.logoSizePercent
            )

        /*
         * ====================================================
         * LOGO POSITION
         * ====================================================
         *
         * الموقع يُحسب نسبةً إلى موضع الشعار الحالي.
         *
         * 0   = الوضع الحالي
         * +   = يمين / أسفل
         * -   = يسار / أعلى
         *
         * القيمة مضروبة في tvScale حتى تبقى Responsive.
         */
        val logoOffsetX =
            (layoutSettings.logoOffsetXPercent * tvScale)
                .dp

        val logoOffsetY =
            (layoutSettings.logoOffsetYPercent * tvScale)
                .dp

        /*
         * ====================================================
         * LIVE POSITION
         * ====================================================
         *
         * نفس المبدأ:
         *
         * 0 = الوضع الحالي
         * +X = يمين
         * -X = يسار
         * +Y = أسفل
         * -Y = أعلى
         */
        val liveOffsetX =
            (layoutSettings.liveOffsetXPercent * tvScale)
                .dp

        val liveOffsetY =
            (layoutSettings.liveOffsetYPercent * tvScale)
                .dp

        /*
         * ====================================================
         * CONTENT LAYER
         * ====================================================
         *
         * مساحة المحتوى الحقيقي مستقبلًا:
         *
         * YouTube
         * صور
         * فيديو
         * إعلانات
         * طلبات جاهزة
         * Playlists
         */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RoyalTVBlack)
        )

        /*
         * ====================================================
         * LIVE LAYER
         * ====================================================
         *
         * 🔴 مباشر
         *
         * الحجم والموقع أصبحا مرتبطين
         * بالنظام المركزي.
         */
        if (showLive) {
            RoyalTVLiveIndicator(
                text = liveText,
                textSize = liveTextSize,
                scale = tvScale,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(
                        x = liveOffsetX,
                        y = liveOffsetY
                    )
                    .padding(
                        end = (34f * tvScale).dp,
                        top = (32f * tvScale).dp
                    )
            )
        }

        /*
         * ====================================================
         * BOTTOM IDENTITY AREA
         * ====================================================
         *
         * 👑 الشعار
         * 🕐 الوقت والتاريخ
         *
         * الموضع الأساسي محفوظ كما هو.
         *
         * تعديل موقع الشعار لا يغيّر موضع الساعة.
         */
        if (showClock || showLogo) {

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = (34f * tvScale).dp,
                        bottom = (28f * tvScale).dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(
                        (16f * tvScale).dp
                    )
            ) {

                /*
                 * =================================================
                 * REAL ROYAL LOGO
                 * =================================================
                 *
                 * الشعار الحقيقي بطبقتيه:
                 *
                 * royal_crest_layer.png
                 * royal_rc_layer.png
                 *
                 * لا نعيد رسمه.
                 * لا نستبدله بـ RC نصي.
                 *
                 * الحركة الأصلية محفوظة بالكامل.
                 */
                if (showLogo) {
                    RoyalAppLogo(
                        modifier = Modifier
                            .size(bottomLogoSize)
                            .offset(
                                x = logoOffsetX,
                                y = logoOffsetY
                            )
                    )
                }

                /*
                 * =================================================
                 * CLOCK
                 * =================================================
                 *
                 * الوقت والتاريخ أصبحا مستقلين:
                 *
                 * - حجم الوقت مستقل.
                 * - حجم التاريخ مستقل.
                 * - showDate يتحكم بإظهار التاريخ.
                 *
                 * إذا كان showDate = false
                 * فلن يحجز التاريخ أي مساحة.
                 */
                if (showClock) {
                    RoyalTVClock(
                        scale = tvScale,
                        showDate = layoutSettings.showDate,
                        timeTextSize = timeTextSize,
                        timeLineHeight = timeLineHeight,
                        dateTextSize = dateTextSize,
                        dateLineHeight = dateLineHeight
                    )
                }
            }
        }

        /*
         * ====================================================
         * TICKER LAYER
         * ====================================================
         *
         * الشريط الإخباري الرئيسي فقط.
         *
         * مهما كان طول النص:
         *
         * لا يكبر المستطيل
         * لا يكسر الشاشة
         * لا يخرج عن الشريط
         * يستمر بالتمرير.
         */
        if (showTicker) {
            RoyalTVTicker(
                text = tickerText,
                textSize = tickerTextSize,
                scale = tvScale,
                tickerHeight = tickerHeight,
                tickerCornerRadius = tickerCornerRadius,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(
                        start = (34f * tvScale).dp,
                        end = (
                            34f +
                                118f +
                                34f
                            ).dp * tvScale,
                        bottom = (28f * tvScale).dp
                    )
            )
        }
    }
}

/**
 * ============================================================
 * LIVE INDICATOR
 * ============================================================
 *
 * مؤشر البث.
 *
 * 🔴 النقطة الحمراء متحركة.
 *
 * الموقع والحجم أصبحا مركزيين.
 */
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
                color =
                    RoyalTVGold.copy(
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
                    RoyalTVRed.copy(
                        alpha = alpha
                    ),
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

/**
 * ============================================================
 * CLOCK
 * ============================================================
 *
 * الوقت والتاريخ.
 *
 * يعتمد على RoyalDateTime.kt.
 *
 * الآن:
 *
 * - حجم الوقت مستقل.
 * - حجم التاريخ مستقل.
 * - إظهار/إخفاء التاريخ مستقل.
 */
@Composable
private fun RoyalTVClock(
    scale: Float,
    showDate: Boolean,
    timeTextSize: TextUnit,
    timeLineHeight: TextUnit,
    dateTextSize: TextUnit,
    dateLineHeight: TextUnit
) {
    Box(
        modifier = Modifier
            .border(
                width = (1.5f * scale).dp,
                color =
                    RoyalTVGold.copy(
                        alpha = 0.85f
                    ),
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
            showTime = true,
            showDate = showDate,
            timeStyle = TextStyle(
                color = RoyalTVGoldLight,
                fontSize = timeTextSize,
                fontWeight = FontWeight.Bold,
                lineHeight = timeLineHeight
            ),
            dateStyle = TextStyle(
                color = RoyalTVGoldLight,
                fontSize = dateTextSize,
                fontWeight = FontWeight.Bold,
                lineHeight = dateLineHeight
            )
        )
    }
}
/**
 * ============================================================
 * TICKER
 * ============================================================
 *
 * شريط الأخبار.
 *
 * يتحرك من اليمين إلى اليسار.
 *
 * يتم تكرار النص لضمان استمرار الحركة.
 *
 * مهما كان النص طويلًا:
 *
 * يبقى داخل الشريط.
 */
@Composable
private fun RoyalTVTicker(
    text: String,
    textSize: TextUnit,
    scale: Float,
    tickerHeight: androidx.compose.ui.unit.Dp,
    tickerCornerRadius: androidx.compose.ui.unit.Dp,
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
            .height(tickerHeight)
            .clip(
                RoundedCornerShape(
                    tickerCornerRadius
                )
            )
            .border(
                width = (1.7f * scale).dp,
                color = RoyalTVGold,
                shape =
                    RoundedCornerShape(
                        tickerCornerRadius
                    )
            )
            .background(
                Color.Black.copy(alpha = 0.68f),
                RoundedCornerShape(
                    tickerCornerRadius
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
