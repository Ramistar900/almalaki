package com.almalaki.cafe

import androidx.compose.ui.text.font.FontFamily

/**
 * نقطة الربط المركزية لعائلات خطوط ROYAL.
 *
 * في هذه المرحلة نستخدم الخطوط النظامية كحل مؤقت آمن
 * حتى نضيف ملفات الخطوط الحقيقية داخل res/font.
 *
 * بعد إضافة ملفات TTF/OTF سيتم استبدال هذه القيم
 * بـ FontFamily مبنية من R.font.
 */
object RoyalFontFamilies {

    /**
     * Noto Naskh Arabic
     */
    val notoNaskhArabic: FontFamily =
        FontFamily.Default

    /**
     * Noto Kufi Arabic
     */
    val notoKufiArabic: FontFamily =
        FontFamily.Default

    /**
     * Noto Sans Arabic
     */
    val notoSansArabic: FontFamily =
        FontFamily.Default

    /**
     * Cairo
     */
    val cairo: FontFamily =
        FontFamily.Default

    /**
     * Tajawal
     */
    val tajawal: FontFamily =
        FontFamily.Default

    /**
     * Noto Sans
     */
    val notoSans: FontFamily =
        FontFamily.Default

    /**
     * Noto Serif
     */
    val notoSerif: FontFamily =
        FontFamily.Default

    /**
     * Roboto
     */
    val roboto: FontFamily =
        FontFamily.Default

    /**
     * Montserrat
     */
    val montserrat: FontFamily =
        FontFamily.Default
}
