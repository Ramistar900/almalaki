package com.almalaki.cafe

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ============================================================
 * ROYAL TV — RESPONSIVE PADDING ENGINE
 * ============================================================
 *
 * المرحلة 5.3
 *
 * محرك موحد للمسافات والحواف داخل Royal TV.
 *
 * يعمل مع Responsive Scale الموجود في:
 * RoyalTVResponsiveScale.kt
 *
 * الحد الأدنى المستهدف: 360dp
 * ============================================================
 */

object RoyalTVResponsivePadding {

    /**
     * مسافة صغيرة.
     */
    fun small(
        scale: Float
    ): Dp {
        return scaleDp(
            value = 8.dp,
            scale = scale
        )
    }

    /**
     * مسافة متوسطة.
     */
    fun medium(
        scale: Float
    ): Dp {
        return scaleDp(
            value = 16.dp,
            scale = scale
        )
    }

    /**
     * مسافة كبيرة.
     */
    fun large(
        scale: Float
    ): Dp {
        return scaleDp(
            value = 24.dp,
            scale = scale
        )
    }

    /**
     * مسافة كبيرة جدًا.
     */
    fun extraLarge(
        scale: Float
    ): Dp {
        return scaleDp(
            value = 32.dp,
            scale = scale
        )
    }

    /**
     * حافة الشاشة الأساسية.
     */
    fun screen(
        scale: Float
    ): Dp {
        return scaleDp(
            value = 24.dp,
            scale = scale
        )
    }

    /**
     * حافة البطاقات.
     */
    fun card(
        scale: Float
    ): Dp {
        return scaleDp(
            value = 16.dp,
            scale = scale
        )
    }

    /**
     * المسافة بين العناصر.
     */
    fun item(
        scale: Float
    ): Dp {
        return scaleDp(
            value = 12.dp,
            scale = scale
        )
    }

    /**
     * المسافة بين الأزرار.
     */
    fun button(
        scale: Float
    ): Dp {
        return scaleDp(
            value = 10.dp,
            scale = scale
        )
    }

    /**
     * حافة الـ Ticker.
     */
    fun ticker(
        scale: Float
    ): Dp {
        return scaleDp(
            value = 12.dp,
            scale = scale
        )
    }

    /**
     * حافة الشعار.
     */
    fun logo(
        scale: Float
    ): Dp {
        return scaleDp(
            value = 16.dp,
            scale = scale
        )
    }

    /**
     * حافة المحتوى الرئيسي.
     */
    fun content(
        scale: Float
    ): Dp {
        return scaleDp(
            value = 20.dp,
            scale = scale
        )
    }

    /**
     * تحويل Dp إلى Responsive Dp.
     *
     * نستخدم Responsive Scale الموجود
     * بدل تكرار طريقة الحساب.
     */
    private fun scaleDp(
        value: Dp,
        scale: Float
    ): Dp {
        return RoyalTVResponsiveScale.scaleDp(
            value = value,
            scale = RoyalTVResponsiveScale.safeScale(scale)
        )
    }
}
