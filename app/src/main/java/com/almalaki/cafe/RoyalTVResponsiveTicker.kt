package com.almalaki.cafe

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Royal TV — Responsive Ticker
 *
 * يجعل شريط الأخبار متجاوبًا مع مختلف أحجام ودقات الشاشات.
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
     * الارتفاع الأساسي لشريط الأخبار.
     */
    fun height(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_HEIGHT.dp,
            scale
        )

    /**
     * المسافة الأفقية داخل شريط الأخبار.
     */
    fun horizontalPadding(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_HORIZONTAL_PADDING.dp,
            scale
        )

    /**
     * المسافة العمودية داخل شريط الأخبار.
     */
    fun verticalPadding(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_VERTICAL_PADDING.dp,
            scale
        )

    /**
     * نصف قطر زوايا شريط الأخبار.
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
     * حجم أيقونة شريط الأخبار.
     */
    fun iconSize(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_ICON_SIZE.dp,
            scale
        )

    /**
     * حجم منطقة النص الأساسية.
     */
    fun textSize(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_TEXT_SIZE.dp,
            scale
        )

    /**
     * المسافة بين عناصر شريط الأخبار.
     */
    fun spacing(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_SPACING.dp,
            scale
        )

    /**
     * سرعة حركة النص المتجاوبة.
     *
     * القيمة الأساسية محسوبة على شاشة 1280×720.
     *
     * كلما صغرت الشاشة زادت مدة الحركة
     * للحفاظ على حركة مقروءة وراقية.
     */
    fun animationDuration(
        scale: Float
    ): Int {
        val safeScale =
            RoyalTVResponsiveScale.safeScale(scale)

        return (9000f / safeScale)
            .toInt()
            .coerceIn(
                5000,
                16000
            )
    }

    /**
     * الحد الأدنى للمسافة بين النص
     * وبقية عناصر شريط الأخبار.
     */
    fun contentSpacing(
        scale: Float
    ): Dp =
        spacing(scale)

    /**
     * الارتفاع المناسب لمنطقة النص
     * داخل شريط الأخبار.
     */
    fun textContainerHeight(
        scale: Float
    ): Dp =
        RoyalTVResponsiveScale.scaleDp(
            38.dp,
            scale
        )
}
