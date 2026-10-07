package com.almalaki.cafe

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ROYAL TV — RESPONSIVE TICKER
 * المرحلة 5.7
 *
 * أبعاد شريط الأخبار المتجاوبة.
 */
object RoyalTVResponsiveTicker {

    fun height(scale: Float): Dp =
        scaleDp(56.dp, scale)

    fun horizontalPadding(scale: Float): Dp =
        scaleDp(18.dp, scale)

    fun verticalPadding(scale: Float): Dp =
        scaleDp(10.dp, scale)

    fun cornerRadius(scale: Float): Dp =
        scaleDp(16.dp, scale)

    fun borderWidth(scale: Float): Dp =
        scaleDp(1.5.dp, scale)

    fun iconSize(scale: Float): Dp =
        scaleDp(28.dp, scale)

    fun spacing(scale: Float): Dp =
        scaleDp(12.dp, scale)

    private fun scaleDp(
        value: Dp,
        scale: Float
    ): Dp {
        return RoyalTVResponsiveScale.scaleDp(
            value,
            RoyalTVResponsiveScale.safeScale(scale)
        )
    }
}
