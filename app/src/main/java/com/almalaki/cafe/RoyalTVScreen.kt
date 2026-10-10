package com.almalaki.cafe

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.almalaki.cafe.royaltv.RoyalTVEditTarget
import kotlin.math.roundToInt

private val RoyalTVBlack = Color(0xFF030303)
private val RoyalTVGold = Color(0xFFD4AF37)
private val RoyalTVGoldLight = Color(0xFFFFE9A3)
private val RoyalTVCream = Color(0xFFF5F0E5)
private val RoyalTVRed = Color(0xFFE53935)

/**
 * ============================================================
 * ROYAL TV EDITOR SELECTION FRAME
 * ============================================================
 *
 * إطار ذهبي بخطوط متقطعة.
 *
 * يظهر فقط عندما يكون العنصر محدداً
 * داخل محرر Royal TV.
 *
 * لا يضيف:
 * - تعبئة
 * - ظل
 * - تغيير في العنصر
 */
private fun Modifier.royalTVSelectionFrame(
    selected: Boolean
): Modifier {

    if (!selected) {
        return this
    }

    return drawWithContent {

        drawContent()

        drawRect(
            color = RoyalTVGold,
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect =
                    PathEffect.dashPathEffect(
                        floatArrayOf(
                            10.dp.toPx(),
                            7.dp.toPx()
                        ),
                        phase = 0f
                    )
            )
        )
    }
}
private fun Modifier.royalTVEditorGestures(
    target: RoyalTVEditTarget,
    enabled: Boolean,
    onTargetSelected: ((RoyalTVEditTarget) -> Unit)?,
    onTransform: ((RoyalTVEditTarget, Offset, Float) -> Unit)?
): Modifier {
    if (!enabled) return this

    return this
        .pointerInput(target) {
            detectTapGestures {
                onTargetSelected?.invoke(target)
            }
        }
        .pointerInput(target) {
            detectTransformGestures { _, pan, zoom, _ ->
                if (pan != Offset.Zero || zoom != 1f) {
                    onTargetSelected?.invoke(target)
                    onTransform?.invoke(target, pan, zoom)
                }
            }
        }
}

/**
 * ============================================================
 * ROYAL TV SCREEN
 * ============================================================
 *
 * شاشة Royal TV الأساسية.
 *
 * العناصر:
 *
 * 🔴 مباشر
 * 📰 شريط الأخبار
 * 🕐 الوقت والتاريخ
 * 👑 الشعار
 *
 * التخطيط المركزي:
 *
 * RoyalTVLayoutSettings.kt
 *
 * يدعم:
 *
 * - Responsive
 * - حجم الشعار
 * - X/Y الشعار
 * - حجم الشريط
 * - X/Y الشريط
 * - حجم الوقت
 * - X/Y الوقت
 * - حجم التاريخ
 * - X/Y التاريخ
 * - حجم LIVE
 * - X/Y LIVE
 * - إظهار/إخفاء التاريخ
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
    showLive: Boolean = true,
    layoutSettingsOverride: RoyalTVLayoutSettings? = null,
    editorSelectedTarget: RoyalTVEditTarget? = null,
    editorSelectionEnabled: Boolean = false,
    onEditorTargetSelected: ((RoyalTVEditTarget) -> Unit)? = null,
    onEditorTransform: ((RoyalTVEditTarget, Offset, Float) -> Unit)? = null
) {

    val context = LocalContext.current

    /*
     * ========================================================
     * CENTRAL LAYOUT SETTINGS
     * ========================================================
     */
    val storedLayoutSettings =
        remember(context) {
            loadRoyalTVLayoutSettings(context)
        }

    val layoutSettings =
        layoutSettingsOverride
            ?: storedLayoutSettings

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
        
        /*
         * قراءة أبعاد مساحة العرض الفعلية.
         * حساب الحجم باستخدام العرض والارتفاع معًا.
         */
        val displayMetrics =
            rememberRoyalTVDisplayMetrics(
                widthDp = maxWidth,
                heightDp = maxHeight
            )

        val tvScale =
            RoyalTVResponsiveScale.calculate(
                displayMetrics
            )
            

        /*
         * ====================================================
         * SIZE PERCENTAGES
         * ====================================================
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
            (
                24f *
                    tvScale *
                    liveMultiplier
                ).sp

        /*
         * ====================================================
         * TICKER SIZE
         * ====================================================
         */

        val tickerMultiplier =
            tickerSizePercent / 100f

        val tickerTextSize =
            (
                25f *
                    tvScale *
                    tickerMultiplier
                ).sp

        val tickerHeight =
            (
                62f *
                    tvScale *
                    tickerMultiplier
                ).dp

        val tickerCornerRadius =
            (
                17f *
                    tvScale *
                    tickerMultiplier
                ).dp

        /*
         * ====================================================
         * CLOCK SIZE
         * ====================================================
         */

        val timeMultiplier =
            timeSizePercent / 100f

        val dateMultiplier =
            dateSizePercent / 100f

        val timeTextSize =
            (
                22f *
                    tvScale *
                    timeMultiplier
                ).sp

        val timeLineHeight =
            (
                27f *
                    tvScale *
                    timeMultiplier
                ).sp

        val dateTextSize =
            (
                22f *
                    tvScale *
                    dateMultiplier
                ).sp

        val dateLineHeight =
            (
                27f *
                    tvScale *
                    dateMultiplier
                ).sp

        /*
         * ====================================================
         * LOGO SIZE
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
         * LOGO POSITION
         * ====================================================
         */

        val logoOffsetX =
            (
                layoutSettings.logoOffsetXPercent *
                    tvScale
                ).dp

        val logoOffsetY =
            (
                layoutSettings.logoOffsetYPercent *
                    tvScale
                ).dp

        /*
         * ====================================================
         * LIVE POSITION
         * ====================================================
         */

        val liveOffsetX =
            (
                layoutSettings.liveOffsetXPercent *
                    tvScale
                ).dp

        val liveOffsetY =
            (
                layoutSettings.liveOffsetYPercent *
                    tvScale
                ).dp

        /*
         * ====================================================
         * TICKER POSITION
         * ====================================================
         */

        val tickerOffsetX =
            (
                layoutSettings.tickerOffsetXPercent *
                    tvScale
                ).dp

        val tickerOffsetY =
            (
                layoutSettings.tickerOffsetYPercent *
                    tvScale
                ).dp

        /*
         * ====================================================
         * TIME POSITION
         * ====================================================
         */

        val timeOffsetX =
            (
                layoutSettings.timeOffsetXPercent *
                    tvScale
                ).dp

        val timeOffsetY =
            (
                layoutSettings.timeOffsetYPercent *
                    tvScale
                ).dp

        /*
         * ====================================================
         * DATE POSITION
         * ====================================================
         */

        val dateOffsetX =
            (
                layoutSettings.dateOffsetXPercent *
                    tvScale
                ).dp

        val dateOffsetY =
            (
                layoutSettings.dateOffsetYPercent *
                    tvScale
                ).dp

            /*
         * ====================================================
         * CONTENT LAYER
         * ====================================================
         */

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RoyalTVBlack)
        ) {

            /*
             * ====================================================
             * LIVE
             * ====================================================
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
                        .royalTVSelectionFrame(
                            editorSelectedTarget ==
                                RoyalTVEditTarget.LIVE
                        )
                        .royalTVEditorGestures(
                            target = RoyalTVEditTarget.LIVE,
                            enabled = editorSelectionEnabled,
                            onTargetSelected = onEditorTargetSelected,
                            onTransform = onEditorTransform
                        )
                )
            }

            /*
             * ====================================================
             * BOTTOM IDENTITY AREA
             * ====================================================
             */

            if (showClock || showLogo) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = (34f * tvScale).dp,
                            bottom = (28f * tvScale).dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        (16f * tvScale).dp
                    )
                ) {

                    /*
                     * ROYAL LOGO
                     */

                    if (showLogo) {
                        RoyalAppLogo(
                            modifier = Modifier
                                .size(bottomLogoSize)
                                .offset(
                                    x = logoOffsetX,
                                    y = logoOffsetY
                                )
                                .royalTVSelectionFrame(
                                    editorSelectedTarget ==
                                        RoyalTVEditTarget.LOGO
                                )
                                .royalTVEditorGestures(
                                    target = RoyalTVEditTarget.LOGO,
                                    enabled = editorSelectionEnabled,
                                    onTargetSelected =
                                        onEditorTargetSelected,
                                    onTransform = onEditorTransform
                                )
                        )
                    }

                    /*
                     * CLOCK AND DATE
                     */

                    if (showClock) {
                        RoyalTVClock(
                            scale = tvScale,
                            showDate = layoutSettings.showDate,
                            timeTextSize = timeTextSize,
                            timeLineHeight = timeLineHeight,
                            dateTextSize = dateTextSize,
                            dateLineHeight = dateLineHeight,
                            timeOffsetX = timeOffsetX,
                            timeOffsetY = timeOffsetY,
                            dateOffsetX = dateOffsetX,
                            dateOffsetY = dateOffsetY,
                            editorSelectionEnabled =
                                editorSelectionEnabled,
                            onEditorTargetSelected =
                                onEditorTargetSelected,
                            onEditorTransform =
                                onEditorTransform,
                            modifier = Modifier.royalTVSelectionFrame(
                                editorSelectedTarget ==
                                    RoyalTVEditTarget.TIME ||
                                editorSelectedTarget ==
                                    RoyalTVEditTarget.DATE
                            )
                        )
                    }
                }
            }

            /*
             * ====================================================
             * TICKER
             * ====================================================
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
                        .offset(
                            x = tickerOffsetX,
                            y = tickerOffsetY
                        )
                        .fillMaxWidth()
                        .padding(
                            start = (34f * tvScale).dp,
                            end = (
                                34f + 118f + 34f
                            ).dp * tvScale,
                            bottom = (28f * tvScale).dp
                        )
                        .royalTVSelectionFrame(
                            editorSelectedTarget ==
                                RoyalTVEditTarget.TICKER
                        )
                        .royalTVEditorGestures(
                            target = RoyalTVEditTarget.TICKER,
                            enabled = editorSelectionEnabled,
                            onTargetSelected =
                                onEditorTargetSelected,
                            onTransform = onEditorTransform
                        )
                )
            }
        }
    }
}

/**
 * ============================================================
 * LIVE INDICATOR
 * ============================================================
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
                repeatMode =
                    RepeatMode.Reverse
            ),
        label = "royal_tv_live_alpha"
    )

    Row(
        modifier = modifier
            .border(
                width =
                    (
                        1.6f *
                            scale
                    ).dp,
                color =
                    RoyalTVGold.copy(
                        alpha = 0.9f
                    ),
                shape =
                    RoundedCornerShape(
                        (
                            15f *
                                scale
                        ).dp
                    )
            )
            .background(
                Color.Black.copy(
                    alpha = 0.62f
                ),
                RoundedCornerShape(
                    (
                        15f *
                            scale
                    ).dp
                )
            )
            .padding(
                horizontal =
                    (
                        19f *
                            scale
                    ).dp,
                vertical =
                    (
                        8f *
                            scale
                    ).dp
            ),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.Center
    ) {

        Box(
            modifier = Modifier
                .size(
                    (
                        10f *
                            scale
                    ).dp
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
                (
                    9f *
                        scale
                ).dp
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
 */
 @Composable
private fun RoyalTVClock(
    scale: Float,
    showDate: Boolean,
    timeTextSize: TextUnit,
    timeLineHeight: TextUnit,
    dateTextSize: TextUnit,
    dateLineHeight: TextUnit,
timeOffsetX: androidx.compose.ui.unit.Dp,
timeOffsetY: androidx.compose.ui.unit.Dp,
dateOffsetX: androidx.compose.ui.unit.Dp,
dateOffsetY: androidx.compose.ui.unit.Dp,
modifier: Modifier = Modifier,
editorSelectionEnabled: Boolean = false,
onEditorTargetSelected: ((RoyalTVEditTarget) -> Unit)? = null,
onEditorTransform: ((RoyalTVEditTarget, Offset, Float) -> Unit)? = null
) {

    Box(
        modifier = modifier
            .border(
                width =
                    (
                        1.5f *
                            scale
                    ).dp,
                color =
                    RoyalTVGold.copy(
                        alpha = 0.85f
                    ),
                shape =
                    RoundedCornerShape(
                        (
                            14f *
                                scale
                        ).dp
                    )
            )
            .background(
                Color.Black.copy(
                    alpha = 0.68f
                ),
                RoundedCornerShape(
                    (
                        14f *
                            scale
                    ).dp
                )
            )
            .padding(
                horizontal =
                    (
                        18f *
                            scale
                    ).dp,
                vertical =
                    (
                        9f *
                            scale
                    ).dp
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
),
timeModifier = Modifier
    .offset(
        x = timeOffsetX,
        y = timeOffsetY
    )
    .royalTVEditorGestures(
    target = RoyalTVEditTarget.TIME,
    enabled = editorSelectionEnabled,
    onTargetSelected = onEditorTargetSelected,
    onTransform = onEditorTransform
),
dateModifier = Modifier
    .offset(
        x = dateOffsetX,
        y = dateOffsetY
    )
    .royalTVEditorGestures(
    target = RoyalTVEditTarget.DATE,
    enabled = editorSelectionEnabled && showDate,
    onTargetSelected = onEditorTargetSelected,
    onTransform = onEditorTransform
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
 * الحركة:
 *
 * بداية النص
 *     ↓
 * يتحرك من اليمين إلى اليسار
 *     ↓
 * يختفي بالكامل داخل نهاية الإطار
 *     ↓
 * تبدأ النسخة التالية من نفس نقطة البداية
 *
 * الإطار نفسه يعمل كـ Clip،
 * لذلك لا يظهر النص خارجه.
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

    /*
     * ========================================================
     * قياس النص الحقيقي بالبكسل.
     * ========================================================
     */
    var textWidthPx by remember(text) {
        mutableIntStateOf(0)
    }

    /*
     * ========================================================
     * حركة الشريط.
     * ========================================================
     */
    val transition =
        rememberInfiniteTransition(
            label = "royal_tv_ticker"
        )

    /*
     * ========================================================
     * المسافة بين النص الأول والثاني.
     * ========================================================
     */
    val separatorWidthPx =
        (
            150f *
                scale
        ).roundToInt()

    /*
     * ========================================================
     * طول الدورة الكاملة.
     *
     * النص + المسافة الفاصلة.
     *
     * عندما يصل النص إلى هذه المسافة:
     *
     * تكون النسخة التالية في المكان الصحيح
     * تماماً لبدء الدورة من جديد.
     * ========================================================
     */
    val cycleWidthPx =
        if (textWidthPx > 0) {
            (
                textWidthPx +
                    separatorWidthPx
            ).toFloat()
        } else {
            1f
        }

    /*
     * ========================================================
     * تقدم الحركة.
     * ========================================================
     */
    val tickerX by transition.animateFloat(
        initialValue = 0f,
        targetValue = -cycleWidthPx,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis =
    RoyalTVResponsiveTicker.animationDuration(scale),
                        easing = LinearEasing
                    ),
                repeatMode =
                    RepeatMode.Restart
            ),
        label = "royal_tv_ticker_progress"
    )

    /*
     * ========================================================
     * إطار الشريط.
     * ========================================================
     *
     * clip يمنع النص من الخروج خارج الإطار.
     */
    Box(
        modifier = modifier
            .height(
                tickerHeight
            )
            .clip(
                RoundedCornerShape(
                    tickerCornerRadius
                )
            )
            .border(
                width =
                    (
                        1.7f *
                            scale
                    ).dp,
                color =
                    RoyalTVGold,
                shape =
                    RoundedCornerShape(
                        tickerCornerRadius
                    )
            )
            .background(
                Color.Black.copy(
                    alpha = 0.68f
                ),
                RoundedCornerShape(
                    tickerCornerRadius
                )
            ),
        contentAlignment =
            Alignment.CenterStart
    ) {

        /*
         * ====================================================
         * النص المتحرك.
         * ====================================================
         */
        Row(
            modifier = Modifier
                .offset {
                    IntOffset(
                        tickerX.roundToInt(),
                        0
                    )
                }
                .wrapContentWidth(
                    unbounded = true
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /*
             * =================================================
             * النسخة الأولى.
             * =================================================
             */
            Text(
                text = text,
                color = RoyalTVCream,
                fontSize = textSize,
                fontWeight =
                    FontWeight.SemiBold,
                maxLines = 1,
                overflow =
                    TextOverflow.Clip,
                onTextLayout = {
                    textWidthPx =
                        it.size.width
                }
            )

            /*
             * =================================================
             * المسافة الفاصلة.
             * =================================================
             */
            Spacer(
                modifier = Modifier.width(
                    (
                        150f *
                            scale
                    ).dp
                )
            )

            /*
             * =================================================
             * النسخة الثانية.
             *
             * تضمن الاستمرار بدون توقف.
             * =================================================
             */
            Text(
                text = text,
                color = RoyalTVCream,
                fontSize = textSize,
                fontWeight =
                    FontWeight.SemiBold,
                maxLines = 1,
                overflow =
                    TextOverflow.Clip
            )
        }
    }
}
