package com.almalaki.cafe.royaltv

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almalaki.cafe.ROYAL_TV_MAX_OFFSET_PERCENT
import com.almalaki.cafe.ROYAL_TV_MAX_SIZE_PERCENT
import com.almalaki.cafe.ROYAL_TV_MIN_OFFSET_PERCENT
import com.almalaki.cafe.ROYAL_TV_MIN_SIZE_PERCENT
import com.almalaki.cafe.RoyalTVLayoutSettings
import kotlin.math.roundToInt

/**
 * ============================================================
 * ROYAL TV DIRECT EDITOR
 * ============================================================
 *
 * محرر شاشة Royal TV المباشر.
 *
 * الفكرة:
 *
 * 1. ندخل إلى شاشة Royal TV الحقيقية.
 * 2. نعمل على نسخة مؤقتة من الإعدادات Draft.
 * 3. كل حركة تظهر فوراً على الشاشة.
 * 4. لا يتم الحفظ أثناء الحركة.
 * 5. عند الضغط على "حفظ التعديل":
 *      يتم اعتماد النسخة المؤقتة.
 * 6. عند الضغط على "إلغاء":
 *      يتم التخلص من النسخة المؤقتة.
 *
 * ============================================================
 *
 * العناصر المدعومة:
 *
 * 👑 LOGO
 * 📰 TICKER
 * 🕐 TIME
 * 📅 DATE
 * 🔴 LIVE
 *
 * ============================================================
 */

/**
 * العنصر الذي يتم تعديله حالياً.
 */
enum class RoyalTVEditTarget {
    LOGO,
    TICKER,
    TIME,
    DATE,
    LIVE
}

/**
 * ألوان محرر Royal TV.
 */
private val RoyalEditorBlack =
    Color(0xFF030303)

private val RoyalEditorGold =
    Color(0xFFD4AF37)

private val RoyalEditorGoldLight =
    Color(0xFFFFE9A3)

private val RoyalEditorCream =
    Color(0xFFF5F0E5)

private val RoyalEditorPanel =
    Color(0xFF111111)

/**
 * ============================================================
 * MAIN DIRECT EDITOR
 * ============================================================
 *
 * content:
 *
 * محتوى شاشة Royal TV الفعلية.
 *
 * يجب أن يعاد رسمها باستخدام draftSettings.
 */
@Composable
fun RoyalTVDirectEditor(
    originalSettings: RoyalTVLayoutSettings,
    target: RoyalTVEditTarget,
    onSave: (RoyalTVLayoutSettings) -> Unit,
    onCancel: () -> Unit,
    content: @Composable BoxScope.(RoyalTVLayoutSettings) -> Unit
) {

    /**
     * --------------------------------------------------------
     * DRAFT
     * --------------------------------------------------------
     *
     * هذه النسخة مؤقتة.
     *
     * لا يتم حفظها في SharedPreferences
     * أثناء التحرير.
     */
    var draftSettings by remember(originalSettings) {
        mutableStateOf(originalSettings)
    }
    var selectedTarget by remember(target) {
    mutableStateOf(target)
    }

    /**
     * --------------------------------------------------------
     * مساحة الشاشة
     * --------------------------------------------------------
     *
     * نحتاجها لتحويل حركة الإصبع
     * إلى نسبة مئوية.
     */
    var editorSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    /**
     * --------------------------------------------------------
     * أسماء العناصر
     * --------------------------------------------------------
     */
    val targetTitle =
        when (selectedTarget) {
            RoyalTVEditTarget.LOGO ->
                "👑 الشعار"

            RoyalTVEditTarget.TICKER ->
                "📰 شريط الأخبار"

            RoyalTVEditTarget.TIME ->
                "🕐 الوقت"

            RoyalTVEditTarget.DATE ->
                "📅 التاريخ"

            RoyalTVEditTarget.LIVE ->
                "🔴 مباشر"
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RoyalEditorBlack)
            .onSizeChanged {
                editorSize = it
            }
    ) {

        /**
         * ====================================================
         * ROYAL TV PREVIEW
         * ====================================================
         *
         * شاشة Royal TV الفعلية.
         *
         * يتم تمرير draftSettings حتى تتغير
         * الشاشة فوراً أثناء التحرير.
         */
        content(draftSettings)

        /**
         * ====================================================
         * DIRECT TOUCH LAYER
         * ====================================================
         *
         * هذه الطبقة تستقبل:
         *
         * - السحب
         * - التكبير
         * - التصغير
         *
         * فقط عندما يكون العنصر:
         *
         * LOGO
         * أو
         * LIVE
         *
         * لأنهما يدعمان الموقع والحجم.
         */
        if (
    selectedTarget == RoyalTVEditTarget.LOGO ||
    selectedTarget == RoyalTVEditTarget.TICKER ||
    selectedTarget == RoyalTVEditTarget.TIME ||
    selectedTarget == RoyalTVEditTarget.DATE ||
    selectedTarget == RoyalTVEditTarget.LIVE
) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(
                     selectedTarget,
                     editorSize,
                     draftSettings
) {

                        detectTransformGestures(
                            panZoomLock = false
                        ) { _, pan, zoom, _ ->

                            if (
                                editorSize.width <= 0 ||
                                editorSize.height <= 0
                            ) {
                                return@detectTransformGestures
                            }

                            /**
                             * --------------------------------
                             * تحويل حركة X إلى نسبة.
                             * --------------------------------
                             */
                            val xPercent =
                                (
                                    pan.x /
                                        editorSize.width.toFloat()
                                ) * 100f

                            /**
                             * --------------------------------
                             * تحويل حركة Y إلى نسبة.
                             * --------------------------------
                             */
                            val yPercent =
                                (
                                    pan.y /
                                        editorSize.height.toFloat()
                                ) * 100f

                            /**
                             * --------------------------------
                             * تحويل Zoom إلى نسبة.
                             * --------------------------------
                             */
                            val zoomFactor =
                                zoom.coerceIn(
                                    0.85f,
                                    1.15f
                                )

                            when (selectedTarget) {

                                /**
                                 * ==================================
                                 * LOGO
                                 * ==================================
                                 */
                                RoyalTVEditTarget.LOGO -> {

                                    val newX =
                                        (
                                            draftSettings
                                                .logoOffsetXPercent +
                                                xPercent
                                                    .roundToInt()
                                        ).coerceIn(
                                            ROYAL_TV_MIN_OFFSET_PERCENT,
                                            ROYAL_TV_MAX_OFFSET_PERCENT
                                        )

                                    val newY =
                                        (
                                            draftSettings
                                                .logoOffsetYPercent +
                                                yPercent
                                                    .roundToInt()
                                        ).coerceIn(
                                            ROYAL_TV_MIN_OFFSET_PERCENT,
                                            ROYAL_TV_MAX_OFFSET_PERCENT
                                        )

                                    val newSize =
                                        (
                                            draftSettings
                                                .logoSizePercent *
                                                zoomFactor
                                        )
                                            .roundToInt()
                                            .coerceIn(
                                                ROYAL_TV_MIN_SIZE_PERCENT,
                                                ROYAL_TV_MAX_SIZE_PERCENT
                                            )

                                    draftSettings =
                                        draftSettings.copy(
                                            logoOffsetXPercent =
                                                newX,

                                            logoOffsetYPercent =
                                                newY,

                                            logoSizePercent =
                                                newSize
                                        )
                                }

                                /**
                                 * ==================================
                                 * LIVE
                                 * ==================================
                                 */
                                RoyalTVEditTarget.LIVE -> {

                                    val newX =
                                        (
                                            draftSettings
                                                .liveOffsetXPercent +
                                                xPercent
                                                    .roundToInt()
                                        ).coerceIn(
                                            ROYAL_TV_MIN_OFFSET_PERCENT,
                                            ROYAL_TV_MAX_OFFSET_PERCENT
                                        )

                                    val newY =
                                        (
                                            draftSettings
                                                .liveOffsetYPercent +
                                                yPercent
                                                    .roundToInt()
                                        ).coerceIn(
                                            ROYAL_TV_MIN_OFFSET_PERCENT,
                                            ROYAL_TV_MAX_OFFSET_PERCENT
                                        )

                                    val newSize =
                                        (
                                            draftSettings
                                                .liveSizePercent *
                                                zoomFactor
                                        )
                                            .roundToInt()
                                            .coerceIn(
                                                ROYAL_TV_MIN_SIZE_PERCENT,
                                                ROYAL_TV_MAX_SIZE_PERCENT
                                            )

                                    draftSettings =
                                        draftSettings.copy(
                                            liveOffsetXPercent =
                                                newX,

                                            liveOffsetYPercent =
                                                newY,

                                            liveSizePercent =
                                                newSize
                                        )
                                }

                                else -> Unit
                            }
                        }
                    }
            )
        }

        /**
         * ====================================================
         * TOP EDITOR BAR
         * ====================================================
         *
         * شريط مؤقت يظهر أثناء التعديل فقط.
         */
        RoyalTVEditorTopBar(
            targetTitle = targetTitle
        )

        /**
         * ====================================================
         * BOTTOM EDITOR PANEL
         * ====================================================
         *
         * يحتوي على:
         *
         * - معلومات القيمة الحالية.
         * - أزرار التحكم.
         * - حفظ.
         * - إلغاء.
         */
        RoyalTVEditorBottomPanel(
            target = selectedTarget,
            settings = draftSettings,
            onSettingsChanged = {
                draftSettings = it
            },
            onSave = {
                onSave(draftSettings)
            },
            onCancel = {
                onCancel()
            }
        )
    }
}

/**
 * ============================================================
 * TOP BAR
 * ============================================================
 */
@Composable
private fun RoyalTVEditorTopBar(
    targetTitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 14.dp
            )
            .background(
                Color.Black.copy(
                    alpha = 0.82f
                ),
                RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = RoyalEditorGold,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            )
    ) {

        Text(
            text = "🎯 تعديل مباشر: $targetTitle",
            modifier = Modifier.fillMaxWidth(),
            color = RoyalEditorGoldLight,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * ============================================================
 * BOTTOM PANEL
 * ============================================================
 */
@Composable
private fun RoyalTVEditorBottomPanel(
    target: RoyalTVEditTarget,
    settings: RoyalTVLayoutSettings,
    onSettingsChanged: (RoyalTVLayoutSettings) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 14.dp,
                vertical = 14.dp
            )
            .background(
                RoyalEditorPanel.copy(
                    alpha = 0.94f
                ),
                RoundedCornerShape(22.dp)
            )
            .border(
                width = 1.dp,
                color = RoyalEditorGold.copy(
                    alpha = 0.75f
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(14.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        /**
         * ----------------------------------------------------
         * الحالة الحالية
         * ----------------------------------------------------
         */
        RoyalTVEditorCurrentValue(
            target = target,
            settings = settings
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        /**
         * ----------------------------------------------------
         * التحكم حسب العنصر
         * ----------------------------------------------------
         */
        when (target) {

            RoyalTVEditTarget.LOGO -> {

                RoyalTVLogoLiveControls(
                    isLogo = true,
                    settings = settings,
                    onSettingsChanged =
                        onSettingsChanged
                )
            }

            RoyalTVEditTarget.LIVE -> {

                RoyalTVLogoLiveControls(
                    isLogo = false,
                    settings = settings,
                    onSettingsChanged =
                        onSettingsChanged
                )
            }

            RoyalTVEditTarget.TICKER -> {

                RoyalTVSizeControls(
                    title = "حجم شريط الأخبار",
                    value =
                        settings.tickerSizePercent,
                    onChange = { newValue ->

                        onSettingsChanged(
                            settings.copy(
                                tickerSizePercent =
                                    newValue
                                        .coerceIn(
                                            ROYAL_TV_MIN_SIZE_PERCENT,
                                            ROYAL_TV_MAX_SIZE_PERCENT
                                        )
                            )
                        )
                    }
                )
            }

            RoyalTVEditTarget.TIME -> {

                RoyalTVSizeControls(
                    title = "حجم الوقت",
                    value =
                        settings.timeSizePercent,
                    onChange = { newValue ->

                        onSettingsChanged(
                            settings.copy(
                                timeSizePercent =
                                    newValue
                                        .coerceIn(
                                            ROYAL_TV_MIN_SIZE_PERCENT,
                                            ROYAL_TV_MAX_SIZE_PERCENT
                                        )
                            )
                        )
                    }
                )
            }

            RoyalTVEditTarget.DATE -> {

                RoyalTVSizeControls(
                    title = "حجم التاريخ",
                    value =
                        settings.dateSizePercent,
                    onChange = { newValue ->

                        onSettingsChanged(
                            settings.copy(
                                dateSizePercent =
                                    newValue
                                        .coerceIn(
                                            ROYAL_TV_MIN_SIZE_PERCENT,
                                            ROYAL_TV_MAX_SIZE_PERCENT
                                        )
                            )
                        )
                    }
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Button(
                    onClick = {

                        onSettingsChanged(
                            settings.copy(
                                showDate =
                                    !settings.showDate
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                if (
                                    settings.showDate
                                ) {
                                    RoyalEditorGold
                                } else {
                                    Color.DarkGray
                                },
                            contentColor =
                                if (
                                    settings.showDate
                                ) {
                                    Color.Black
                                } else {
                                    RoyalEditorGoldLight
                                }
                        )
                ) {

                    Text(
                        text =
                            if (
                                settings.showDate
                            ) {
                                "📅 إخفاء التاريخ"
                            } else {
                                "📅 إظهار التاريخ"
                            },
                        fontSize = 15.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        /**
         * ====================================================
         * SAVE / CANCEL
         * ====================================================
         *
         * الحفظ هنا فقط.
         *
         * لا يوجد حفظ أثناء الحركة.
         */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            Button(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFF2A2A2A),
                        contentColor =
                            RoyalEditorGoldLight
                    )
            ) {

                Text(
                    text = "↩️ إلغاء",
                    fontSize = 16.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Button(
                onClick = onSave,
                modifier = Modifier
                    .weight(1.4f)
                    .height(52.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            RoyalEditorGold,
                        contentColor =
                            Color.Black
                    )
            ) {

                Text(
                    text = "💾 حفظ التعديل",
                    fontSize = 16.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}

/**
 * ============================================================
 * CURRENT VALUE
 * ============================================================
 */
@Composable
private fun RoyalTVEditorCurrentValue(
    target: RoyalTVEditTarget,
    settings: RoyalTVLayoutSettings
) {

    val text =
        when (target) {

            RoyalTVEditTarget.LOGO ->
                "الحجم ${settings.logoSizePercent}%   •   X ${settings.logoOffsetXPercent}%   •   Y ${settings.logoOffsetYPercent}%"

            RoyalTVEditTarget.TICKER ->
                "الحجم ${settings.tickerSizePercent}%"

            RoyalTVEditTarget.TIME ->
                "الحجم ${settings.timeSizePercent}%"

            RoyalTVEditTarget.DATE ->
                "الحجم ${settings.dateSizePercent}%   •   ${
                    if (settings.showDate) {
                        "ظاهر"
                    } else {
                        "مخفي"
                    }
                }"

            RoyalTVEditTarget.LIVE ->
                "الحجم ${settings.liveSizePercent}%   •   X ${settings.liveOffsetXPercent}%   •   Y ${settings.liveOffsetYPercent}%"
        }

    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        color = RoyalEditorGoldLight,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

/**
 * ============================================================
 * LOGO / LIVE CONTROLS
 * ============================================================
 */
@Composable
private fun RoyalTVLogoLiveControls(
    isLogo: Boolean,
    settings: RoyalTVLayoutSettings,
    onSettingsChanged:
        (RoyalTVLayoutSettings) -> Unit
) {

    val size =
        if (isLogo) {
            settings.logoSizePercent
        } else {
            settings.liveSizePercent
        }

    val x =
        if (isLogo) {
            settings.logoOffsetXPercent
        } else {
            settings.liveOffsetXPercent
        }

    val y =
        if (isLogo) {
            settings.logoOffsetYPercent
        } else {
            settings.liveOffsetYPercent
        }

    /**
     * الحجم.
     */
    Text(
        text = "الحجم: $size%",
        color = RoyalEditorGoldLight,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(
        modifier = Modifier.height(6.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        Button(
            onClick = {

                val value =
                    (
                        size - 5
                    ).coerceAtLeast(
                        ROYAL_TV_MIN_SIZE_PERCENT
                    )

                onSettingsChanged(
                    if (isLogo) {
                        settings.copy(
                            logoSizePercent =
                                value
                        )
                    } else {
                        settings.copy(
                            liveSizePercent =
                                value
                        )
                    }
                )
            },
            modifier = Modifier.weight(1f),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        RoyalEditorPanel,
                    contentColor =
                        RoyalEditorGoldLight
                )
        ) {
            Text("− 5%")
        }

        Button(
            onClick = {

                onSettingsChanged(
                    if (isLogo) {
                        settings.copy(
                            logoSizePercent = 100
                        )
                    } else {
                        settings.copy(
                            liveSizePercent = 100
                        )
                    }
                )
            },
            modifier = Modifier.weight(1f),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        RoyalEditorGold,
                    contentColor =
                        Color.Black
                )
        ) {
            Text("100%")
        }

        Button(
            onClick = {

                val value =
                    (
                        size + 5
                    ).coerceAtMost(
                        ROYAL_TV_MAX_SIZE_PERCENT
                    )

                onSettingsChanged(
                    if (isLogo) {
                        settings.copy(
                            logoSizePercent =
                                value
                        )
                    } else {
                        settings.copy(
                            liveSizePercent =
                                value
                        )
                    }
                )
            },
            modifier = Modifier.weight(1f),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        RoyalEditorPanel,
                    contentColor =
                        RoyalEditorGoldLight
                )
        ) {
            Text("+ 5%")
        }
    }

    Spacer(
        modifier = Modifier.height(8.dp)
    )

    /**
     * الموقع الأفقي.
     */
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        Button(
            onClick = {

                val newValue =
                    (
                        x - 5
                    ).coerceAtLeast(
                        ROYAL_TV_MIN_OFFSET_PERCENT
                    )

                onSettingsChanged(
                    if (isLogo) {
                        settings.copy(
                            logoOffsetXPercent =
                                newValue
                        )
                    } else {
                        settings.copy(
                            liveOffsetXPercent =
                                newValue
                        )
                    }
                )
            },
            modifier = Modifier.weight(1f),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        RoyalEditorPanel,
                    contentColor =
                        RoyalEditorGoldLight
                )
        ) {
            Text("← يسار")
        }

        Button(
            onClick = {

                onSettingsChanged(
                    if (isLogo) {
                        settings.copy(
                            logoOffsetXPercent = 0
                        )
                    } else {
                        settings.copy(
                            liveOffsetXPercent = 0
                        )
                    }
                )
            },
            modifier = Modifier.weight(1f),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        RoyalEditorGold,
                    contentColor =
                        Color.Black
                )
        ) {
            Text("وسط")
        }

        Button(
            onClick = {

                val newValue =
                    (
                        x + 5
                    ).coerceAtMost(
                        ROYAL_TV_MAX_OFFSET_PERCENT
                    )

                onSettingsChanged(
                    if (isLogo) {
                        settings.copy(
                            logoOffsetXPercent =
                                newValue
                        )
                    } else {
                        settings.copy(
                            liveOffsetXPercent =
                                newValue
                        )
                    }
                )
            },
            modifier = Modifier.weight(1f),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        RoyalEditorPanel,
                    contentColor =
                        RoyalEditorGoldLight
                )
        ) {
            Text("يمين →")
        }
    }

    Spacer(
        modifier = Modifier.height(8.dp)
    )

    /**
     * الموقع العمودي.
     */
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        Button(
            onClick = {

                val newValue =
                    (
                        y - 5
                    ).coerceAtLeast(
                        ROYAL_TV_MIN_OFFSET_PERCENT
                    )

                onSettingsChanged(
                    if (isLogo) {
                        settings.copy(
                            logoOffsetYPercent =
                                newValue
                        )
                    } else {
                        settings.copy(
                            liveOffsetYPercent =
                                newValue
                        )
                    }
                )
            },
            modifier = Modifier.weight(1f),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        RoyalEditorPanel,
                    contentColor =
                        RoyalEditorGoldLight
                )
        ) {
            Text("↑ أعلى")
        }

        Button(
            onClick = {

                onSettingsChanged(
                    if (isLogo) {
                        settings.copy(
                            logoOffsetYPercent = 0
                        )
                    } else {
                        settings.copy(
                            liveOffsetYPercent = 0
                        )
                    }
                )
            },
            modifier = Modifier.weight(1f),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        RoyalEditorGold,
                    contentColor =
                        Color.Black
                )
        ) {
            Text("وسط")
        }

        Button(
            onClick = {

                val newValue =
                    (
                        y + 5
                    ).coerceAtMost(
                        ROYAL_TV_MAX_OFFSET_PERCENT
                    )

                onSettingsChanged(
                    if (isLogo) {
                        settings.copy(
                            logoOffsetYPercent =
                                newValue
                        )
                    } else {
                        settings.copy(
                            liveOffsetYPercent =
                                newValue
                        )
                    }
                )
            },
            modifier = Modifier.weight(1f),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        RoyalEditorPanel,
                    contentColor =
                        RoyalEditorGoldLight
                )
        ) {
            Text("أسفل ↓")
        }
    }
}

/**
 * ============================================================
 * SIZE CONTROLS
 * ============================================================
 */
@Composable
private fun RoyalTVSizeControls(
    title: String,
    value: Int,
    onChange: (Int) -> Unit
) {

    Text(
        text = "$title: $value%",
        color = RoyalEditorGoldLight,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(
        modifier = Modifier.height(6.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        Button(
            onClick = {
                onChange(
                    (
                        value - 5
                    ).coerceAtLeast(
                        ROYAL_TV_MIN_SIZE_PERCENT
                    )
                )
            },
            enabled =
                value >
                    ROYAL_TV_MIN_SIZE_PERCENT,
            modifier = Modifier.weight(1f),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        RoyalEditorPanel,
                    contentColor =
                        RoyalEditorGoldLight
                )
        ) {
            Text("− 5%")
        }

        Button(
            onClick = {
                onChange(100)
            },
            modifier = Modifier.weight(1f),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        RoyalEditorGold,
                    contentColor =
                        Color.Black
                )
        ) {
            Text("100%")
        }

        Button(
            onClick = {
                onChange(
                    (
                        value + 5
                    ).coerceAtMost(
                        ROYAL_TV_MAX_SIZE_PERCENT
                    )
                )
            },
            enabled =
                value <
                    ROYAL_TV_MAX_SIZE_PERCENT,
            modifier = Modifier.weight(1f),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        RoyalEditorPanel,
                    contentColor =
                        RoyalEditorGoldLight
                )
        ) {
            Text("+ 5%")
        }
    }
}
/**
 * ============================================================
 * ROYAL TV SELECTION FRAME
 * ============================================================
 *
 * إطار تحديد متقطع يظهر فقط أثناء التحرير.
 *
 * لا يغير العنصر نفسه.
 * لا يضيف خلفية.
 * لا يضيف ظل.
 * لا يحرك العنصر.
 *
 * وظيفته فقط إظهار حدود العنصر المحدد.
 */
@Composable
private fun RoyalTVDashedSelectionFrame(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        drawRect(
            color = RoyalEditorGold,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
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
