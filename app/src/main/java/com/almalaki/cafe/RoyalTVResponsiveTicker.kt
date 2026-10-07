package com.almalaki.cafe

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.almalaki.cafe.royaltv.RoyalTVResponsiveScale

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

    fun height(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_HEIGHT.dp,
            scale
        )

    fun horizontalPadding(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_HORIZONTAL_PADDING.dp,
            scale
        )

    fun verticalPadding(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_VERTICAL_PADDING.dp,
            scale
        )

    fun cornerRadius(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_CORNER_RADIUS.dp,
            scale
        )

    fun borderWidth(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_BORDER_WIDTH.dp,
            scale
        )

    fun iconSize(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_ICON_SIZE.dp,
            scale
        )

    fun textSize(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_TEXT_SIZE.dp,
            scale
        )

    fun spacing(scale: Float): Dp =
        RoyalTVResponsiveScale.scaleDp(
            BASE_SPACING.dp,
            scale
        )

    /**
     * سرعة حركة النص المتجاوبة.
     *
     * القيمة الأساسية محسوبة على شاشة 1280×720.
     */
    fun animationDuration(
        scale: Float
    ): Int {
        val safeScale = RoyalTVResponsiveScale.safeScale(scale)

        return (9000f / safeScale)
            .toInt()
            .coerceIn(5000, 16000)
    }

    /**
     * الحد الأدنى للمسافة بين النص
     * وتابعات شريط الأخبار.
     */
    fun contentSpacing(
        scale: Float
    ): Dp =
        spacing(scale)

    /**
     * ارتفاع مناسب لمنطقة النص
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
