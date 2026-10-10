package com.almalaki.cafe

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ROYAL TV — RESPONSIVE BUTTONS
 *
 * المرحلة 5.6:
 * - أبعاد أزرار متجاوبة مع الشاشة.
 * - معالجة آمنة لقيمة القياس.
 * - الحفاظ على أسماء الدوال الحالية.
 * - استخدام محرك القياس المركزي.
 *
 * هذا الملف يحسب الأبعاد فقط.
 */
object RoyalTVResponsiveButtons {

    fun minWidth(scale: Float): Dp =
        scaleDp(120.dp, scale)

    fun minHeight(scale: Float): Dp =
        scaleDp(52.dp, scale)

    fun horizontalPadding(scale: Float): Dp =
        scaleDp(20.dp, scale)

    fun verticalPadding(scale: Float): Dp =
        scaleDp(12.dp, scale)

    fun cornerRadius(scale: Float): Dp =
        scaleDp(14.dp, scale)

    fun borderWidth(scale: Float): Dp =
        scaleDp(1.5.dp, scale)

    fun spacing(scale: Float): Dp =
        scaleDp(10.dp, scale)

    private fun scaleDp(
        value: Dp,
        scale: Float
    ): Dp {
        val validScale =
            if (scale.isFinite()) scale else 1f

        val safeScale =
            RoyalTVResponsiveScale.safeScale(
                validScale
            )

        return RoyalTVResponsiveScale.scaleDp(
            value,
            safeScale
        )
    }
}
