
package com.almalaki.cafe

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Royal TV — Responsive Logo
 *
 * المرحلة 5.8:
 * - حجم متجاوب حسب أبعاد الشاشة.
 * - تحكم يدوي من 60% إلى 160%.
 * - الحفاظ على نسب الشعار.
 * - المحافظة على توافق الدوال الحالية.
 *
 * هذا الملف يحسب الأحجام والمسافات فقط.
 * لا يغيّر رسم الشعار أو طبقاته أو مؤثراته.
 */
object RoyalTVResponsiveLogo {

    private const val BASE_SIZE = 118f
    private const val BASE_START_PADDING = 34f
    private const val BASE_TOP_PADDING = 28f

    const val MIN_SIZE_PERCENT = 60
    const val DEFAULT_SIZE_PERCENT = 100
    const val MAX_SIZE_PERCENT = 160

    const val SIZE_STEP_PERCENT = 5

    /**
     * يحصر نسبة الحجم ضمن المجال المسموح.
     */
    fun clampSizePercent(percent: Int): Int =
        percent.coerceIn(
            MIN_SIZE_PERCENT,
            MAX_SIZE_PERCENT
        )

    /**
     * زيادة أو إنقاص الحجم بخطوات مقدارها 5%.
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
     * نسبة الحجم الافتراضية.
     */
    fun defaultSizePercent(): Int =
        DEFAULT_SIZE_PERCENT

    /**
     * تنظيف مقياس الشاشة قبل استخدامه.
     */
    private fun safeDisplayScale(scale: Float): Float {
        val validScale =
            if (scale.isFinite()) scale else 1f

        return RoyalTVResponsiveScale.safeScale(
            validScale
        )
    }

    /**
     * الحجم النهائي للشعار بوحدة Dp.
     *
     * scale:
     * مقياس الشاشة المتجاوب.
     *
     * sizePercent:
     * نسبة الحجم اليدوية من 60% إلى 160%.
     */
    fun size(
        scale: Float,
        sizePercent: Int = DEFAULT_SIZE_PERCENT
    ): Dp {
        val safeScale = safeDisplayScale(scale)
        val safePercent = clampSizePercent(sizePercent)

        val manualMultiplier =
            safePercent / 100f

        val scaledBaseSize =
            BASE_SIZE * manualMultiplier

        return RoyalTVResponsiveScale.scaleDp(
            scaledBaseSize.dp,
            safeScale
        )
    }

    /**
     * المسافة الأفقية الأساسية المتجاوبة.
     */
    fun startPadding(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_START_PADDING.dp,
            safeDisplayScale(scale)
        )

    /**
     * المسافة العلوية الأساسية المتجاوبة.
     */
    fun topPadding(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_TOP_PADDING.dp,
            safeDisplayScale(scale)
        )
}
