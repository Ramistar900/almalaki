package com.almalaki.cafe

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * ============================================================
 * ROYAL TV — RESPONSIVE SCALE ENGINE
 * ============================================================
 *
 * المرحلة 5.2
 *
 * محرك موحد لحساب حجم عناصر Royal TV
 * اعتمادًا على حجم الشاشة.
 *
 * الحد الأدنى المستهدف: 360dp
 * المرجع الأساسي: 1280 × 720
 *
 * لا يفرض دقة معينة على الجهاز.
 * ============================================================
 */

object RoyalTVResponsiveScale {

    /**
     * أصغر عرض مدعوم في تصميم Royal TV.
     */
    const val MIN_SUPPORTED_WIDTH_DP = 360f

    /**
     * العرض المرجعي.
     */
    const val REFERENCE_WIDTH_DP = 1280f

    /**
     * الارتفاع المرجعي.
     */
    const val REFERENCE_HEIGHT_DP = 720f

    /**
     * أقل Scale مسموح به.
     *
     * يمنع العناصر من أن تصبح صغيرة جدًا
     * على الشاشات الصغيرة.
     */
    const val MIN_SCALE = 0.72f

    /**
     * أكبر Scale مسموح به.
     *
     * يمنع العناصر من التضخم بشكل مبالغ فيه
     * على الشاشات الكبيرة جدًا.
     */
    const val MAX_SCALE = 2.40f

    /**
     * حساب Responsive Scale الأساسي.
     *
     * يعتمد على العرض والارتفاع معًا.
     */
    fun calculate(
        metrics: RoyalTVDisplayMetrics
    ): Float {

        val width =
            metrics.widthDp.value
                .coerceAtLeast(MIN_SUPPORTED_WIDTH_DP)

        val height =
            metrics.heightDp.value
                .coerceAtLeast(1f)

        val widthScale =
            width / REFERENCE_WIDTH_DP

        val heightScale =
            height / REFERENCE_HEIGHT_DP

        return min(
            widthScale,
            heightScale
        ).coerceIn(
            MIN_SCALE,
            MAX_SCALE
        )
    }

    /**
     * Scale اعتمادًا على العرض فقط.
     */
    fun calculateFromWidth(
        widthDp: Dp
    ): Float {

        val safeWidth =
            widthDp.value
                .coerceAtLeast(MIN_SUPPORTED_WIDTH_DP)

        return (
            safeWidth / REFERENCE_WIDTH_DP
        ).coerceIn(
            MIN_SCALE,
            MAX_SCALE
        )
    }

    /**
     * Scale اعتمادًا على الارتفاع فقط.
     */
    fun calculateFromHeight(
        heightDp: Dp
    ): Float {

        val safeHeight =
            heightDp.value
                .coerceAtLeast(1f)

        return (
            safeHeight / REFERENCE_HEIGHT_DP
        ).coerceIn(
            MIN_SCALE,
            MAX_SCALE
        )
    }

    /**
     * تحويل قيمة Dp إلى حجم Responsive.
     *
     * مثال:
     *
     * 100.dp → 72.dp
     * عندما يكون Scale = 0.72
     */
    fun scaleDp(
        value: Dp,
        scale: Float
    ): Dp {

        return (
            value.value * scale
        ).dp
    }

    /**
     * تحويل قيمة Float إلى Responsive Float.
     *
     * مفيد للأبعاد والحركات والمؤثرات.
     */
    fun scaleValue(
        value: Float,
        scale: Float
    ): Float {

        return value * scale
    }

    /**
     * Scale آمن للهواتف والشاشات الصغيرة.
     *
     * يمنع النزول تحت الحد الأدنى
     * حتى لا تنهار الواجهة.
     */
    fun safeScale(
        scale: Float
    ): Float {

        return scale.coerceIn(
            MIN_SCALE,
            MAX_SCALE
        )
    }
}
