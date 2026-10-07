package com.almalaki.cafe

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * ROYAL TV — RESPONSIVE TYPOGRAPHY
 * المرحلة 5.4
 *
 * أحجام نصوص متجاوبة من 360dp إلى الشاشات الكبيرة.
 */
object RoyalTVResponsiveTypography {

    fun title(scale: Float): TextUnit =
        scaleSp(42f, scale)

    fun subtitle(scale: Float): TextUnit =
        scaleSp(26f, scale)

    fun heading(scale: Float): TextUnit =
        scaleSp(32f, scale)

    fun body(scale: Float): TextUnit =
        scaleSp(22f, scale)

    fun small(scale: Float): TextUnit =
        scaleSp(16f, scale)

    fun button(scale: Float): TextUnit =
        scaleSp(20f, scale)

    fun price(scale: Float): TextUnit =
        scaleSp(24f, scale)

    fun ticker(scale: Float): TextUnit =
        scaleSp(20f, scale)

    private fun scaleSp(
        value: Float,
        scale: Float
    ): TextUnit {
        val safeScale =
            RoyalTVResponsiveScale.safeScale(scale)

        return (value * safeScale).sp
    }
}
