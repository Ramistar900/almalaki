package com.almalaki.cafe

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Royal TV — Responsive Logo
 *
 * نظام حجم الشعار:
 * 1. يتكيف تلقائيًا مع أبعاد الشاشة.
 * 2. يسمح بتحكم يدوي من 60% إلى 160%.
 * 3. الحجم الأساسي = 100%.
 *
 * لا يغيّر:
 * - نسب الشعار
 * - طبقات PNG
 * - حركة الشعار
 * - اللمعة
 * - مواصفات الهوية الأصلية
 */
object RoyalTVResponsiveLogo {

    /*
     * الحجم الأساسي على شاشة المرجع.
     */
    private const val BASE_SIZE = 118f

    /*
     * موضع الشعار الأساسي.
     */
    private const val BASE_START_PADDING = 34f
    private const val BASE_TOP_PADDING = 28f

    /*
     * حدود التحكم اليدوي.
     */
    const val MIN_SIZE_PERCENT = 60
    const val DEFAULT_SIZE_PERCENT = 100
    const val MAX_SIZE_PERCENT = 160

    /*
     * مقدار التغيير في كل ضغطة.
     */
    const val SIZE_STEP_PERCENT = 5

    /**
     * التأكد من أن قيمة الحجم ضمن الحدود المسموحة.
     */
    fun clampSizePercent(percent: Int): Int =
        percent.coerceIn(
            MIN_SIZE_PERCENT,
            MAX_SIZE_PERCENT
        )

    /**
     * تكبير أو تصغير الحجم يدويًا.
     */
    fun applySizeStep(
        currentPercent: Int,
        increase: Boolean
    ): Int {

        val current = clampSizePercent(currentPercent)

        return if (increase) {
            (current + SIZE_STEP_PERCENT)
                .coerceAtMost(MAX_SIZE_PERCENT)
        } else {
            (current - SIZE_STEP_PERCENT)
                .coerceAtLeast(MIN_SIZE_PERCENT)
        }
    }

    /**
     * استعادة الحجم الأساسي.
     */
    fun defaultSizePercent(): Int =
        DEFAULT_SIZE_PERCENT

    /**
     * الحجم النهائي للشعار.
     *
     * scale:
     * الحجم التلقائي حسب الشاشة.
     *
     * sizePercent:
     * التحكم اليدوي من 60% إلى 160%.
     */
    fun size(
        scale: Float,
        sizePercent: Int = DEFAULT_SIZE_PERCENT
    ): Dp {

        val safePercent =
            clampSizePercent(sizePercent)

        val manualMultiplier =
            safePercent / 100f

        return RoyalTVResponsiveScale.scaleDp(
            (BASE_SIZE * manualMultiplier).dp,
            scale
        )
    }

    /**
     * المسافة الأفقية المتجاوبة.
     */
    fun startPadding(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_START_PADDING.dp,
            scale
        )

    /**
     * المسافة العلوية المتجاوبة.
     */
    fun topPadding(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_TOP_PADDING.dp,
            scale
        )
}
