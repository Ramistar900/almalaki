
package com.almalaki.cafe

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Royal TV — Responsive Ticker
 *
 * إعدادات شريط الأخبار المتجاوب.
 * مدة الحركة الأساسية 30 ثانية تقريبًا عند scale = 1.
 */
object RoyalTVResponsiveTicker {

    private const val BASE_HEIGHT = 58f
    private const val BASE_HORIZONTAL_PADDING = 22f
    private const val BASE_VERTICAL_PADDING = 10f
    private const val BASE_CORNER_RADIUS = 18f
    private const val BASE_BORDER_WIDTH = 1.5f
    private const val BASE_ICON_SIZE = 28f
    private const val BASE_TEXT_SIZE = 22f
    private const val BASE_SPACING = 12f

    /**
     * ارتفاع الشريط.
     */
    fun height(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_HEIGHT.dp,
            scale
        )

    /**
     * المسافة الأفقية الداخلية.
     */
    fun horizontalPadding(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_HORIZONTAL_PADDING.dp,
            scale
        )

    /**
     * المسافة العمودية الداخلية.
     */
    fun verticalPadding(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_VERTICAL_PADDING.dp,
            scale
        )

    /**
     * استدارة الزوايا.
     */
    fun cornerRadius(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_CORNER_RADIUS.dp,
            scale
        )

    /**
     * سماكة الإطار.
     */
    fun borderWidth(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_BORDER_WIDTH.dp,
            scale
        )

    /**
     * حجم الأيقونة.
     */
    fun iconSize(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_ICON_SIZE.dp,
            scale
        )

    /**
     * حجم النص.
     */
    fun textSize(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_TEXT_SIZE.dp,
            scale
        )

    /**
     * المسافة بين عناصر الشريط.
     */
    fun spacing(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_SPACING.dp,
            scale
        )

    /**
     * مدة دورة حركة النص بالمللي ثانية.
     *
     * عند scale = 1: حوالي 30 ثانية.
     * تتكيف المدة مع مقياس العرض،
     * مع منع القيم الصغيرة أو الكبيرة جدًا.
     */
    fun animationDuration(scale: Float): Int {
        val safeScale =
            RoyalTVResponsiveScale.safeScale(scale)

        return (30000f / safeScale)
            .toInt()
            .coerceIn(
                18000,
                45000
            )
    }

    /**
     * المسافة بين النص وبقية عناصر الشريط.
     */
    fun contentSpacing(scale: Float): Dp =
        spacing(scale)

    /**
     * ارتفاع حاوية النص.
     */
    fun textContainerHeight(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            38.dp,
            scale
        )
}
