package com.almalaki.cafe

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ROYAL TV — RESPONSIVE CARDS
 * المرحلة 5.5
 *
 * أحجام البطاقات المتجاوبة.
 */
object RoyalTVResponsiveCards {

    fun minWidth(scale: Float): Dp =
        scaleDp(220.dp, scale)

    fun maxWidth(scale: Float): Dp =
        scaleDp(420.dp, scale)

    fun minHeight(scale: Float): Dp =
        scaleDp(120.dp, scale)

    fun maxHeight(scale: Float): Dp =
        scaleDp(260.dp, scale)

    fun cornerRadius(scale: Float): Dp =
        scaleDp(18.dp, scale)

    fun borderWidth(scale: Float): Dp =
        scaleDp(1.5.dp, scale)

    fun elevation(scale: Float): Dp =
        scaleDp(6.dp, scale)

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
