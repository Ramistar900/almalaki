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
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext

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
 * تم تنظيف الطبقة العلوية القديمة.
 *
 * الموجود الآن:
 *
 * 🔴 مباشر
 * 📰 شريط الأخبار السفلي
 * 🕐 الوقت والتاريخ السفلي
 * 👑 الشعار الملكي الحقيقي بطبقتيه في الأسفل
 *
 * لا يوجد:
 *
 * ❌ RC علوي
 * ❌ مربع علوي
 * ❌ شعار Royal Coffee علوي
 * ❌ وقت وتاريخ علوي
 * ❌ زر خروج شكلي
 *
 * نظام الأحجام Responsive موجود.
 *
 * تم الآن ربط حجم الشعار مع
 * RoyalTVLayoutSettings المركزي.
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
     * نقرأ إعدادات التخطيط مرة واحدة داخل الشاشة.
     *
     * لا نعيد اختراع نظام جديد للحجم.
     * المصدر المركزي الآن هو:
     *
     * RoyalTVLayoutSettings.kt
     *
     * ========================================================
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
         *
         * الهاتف والتابلت والشاشات الكبيرة
         * تتعامل مع النظام الحالي بدون تغيير
         * بقية التطبيق.
         */
        val widthDp = maxWidth.value

        val tvScale =
            (widthDp / 1280f)
                .coerceIn(0.72f, 2.40f)

        /*
         * ====================================================
         * LIVE TEXT SIZE
         * ====================================================
         */
        val liveTextSize =
            (24f * tvScale).sp

        /*
         * ====================================================
         * TICKER TEXT SIZE
         * ====================================================
         */
        val tickerTextSize =
            (25f * tvScale).sp

        /*
         * ====================================================
         * RESPONSIVE LOGO SIZE
         * ====================================================
         *
         * الحجم الأساسي Responsive
         * يتم حسابه أولًا حسب الشاشة.
         *
         * ثم نطبق عليه الحجم اليدوي المحفوظ:
         *
         * 60% → 160%
         *
         * ====================================================
         */
        val bottomLogoSize =
            RoyalTVResponsiveLogo.size(
                scale = tvScale,
                sizePercent =
                    layoutSettings.logoSizePercent
            )

        /*
         * ====================================================
         * CONTENT LAYER
         * ====================================================
         *
         * هذه المساحة مخصصة للمحتوى الحقيقي لاحقًا:
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
         * 🔴 مباشر يبقى كما طلب المستخدم.
         *
         * في هذه المرحلة يبقى في أعلى اليمين.
         *
         * الحجم والموقع اليدوي لمؤشر مباشر
         * سيكونان في خطوة مستقلة.
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
         * BOTTOM IDENTITY AREA
         * ====================================================
         *
         * المنطقة السفلية تحتوي:
         *
         * 📰 الشريط الإخباري
         * 👑 الشعار الحقيقي
         * 🕐 الوقت والتاريخ
         *
         * لا يوجد أي عنصر علوي قديم هنا.
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
                 * لا نعيد رسم الشعار.
                 * لا نستبدله بـ RC نصي.
                 *
                 * الحركة الأصلية محفوظة:
                 *
                 * 👑 التاج + الدرع ثابتان
                 * ✨ لمعة متحركة
                 * RC دوران 3D حول المحور العمودي
                 *
                 * التغيير الوحيد هنا:
                 *
                 * الحجم أصبح يقرأ من
                 * RoyalTVLayoutSettings.
                 */
                if (showLogo) {
                    RoyalAppLogo(
                        modifier = Modifier
                            .size(bottomLogoSize)
                    )
                }

                /*
                 * =================================================
                 * CLOCK
                 * =================================================
                 *
                 * يعتمد على RoyalDateTime.kt الموجود
                 * في المشروع.
                 *
                 * لا نكرر منطق الوقت والتاريخ هنا.
                 */
                if (showClock) {
                    RoyalTVClock(
                        scale = tvScale
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
         * لا يوجد المستطيل الإضافي القديم:
         *
         * "جودة فاخرة أهلاً بكم في Royal TV"
         *
         * لأنه تم حذفه حسب التصميم الجديد.
         *
         * الحجم في هذه المرحلة يبقى بالنظام الحالي.
         * سيتم ربط حجم الشريط في خطوة مستقلة.
         */
        if (showTicker) {
            RoyalTVTicker(
                text = tickerText,
                textSize = tickerTextSize,
                scale = tvScale,
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
 * مؤشر البث الحقيقي.
 *
 * 🔴 النقطة الحمراء متحركة.
 *
 * النص قابل للتغيير من إعدادات الهوية الحالية.
 *
 * الموقع والحجم اليدوي سيتم ربطهما لاحقًا
 * دون حذف هذه الوظيفة.
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
 * الوقت والتاريخ في الأسفل.
 *
 * يعتمد على RoyalDateTime.kt.
 */
@Composable
private fun RoyalTVClock(
    scale: Float
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
            style = TextStyle(
                color = RoyalTVGoldLight,
                fontSize = (22f * scale).sp,
                fontWeight = FontWeight.Bold,
                lineHeight = (27f * scale).sp
            )
        )
    }
}

/**
 * ============================================================
 * TICKER
 * ============================================================
 *
 * شريط الأخبار الرئيسي.
 *
 * يتحرك من اليمين إلى اليسار.
 *
 * يتم تكرار النص لضمان استمرار الحركة.
 *
 * مهما كان طول النص،
 * يبقى داخل الشريط ولا يكسر التخطيط.
 */
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
