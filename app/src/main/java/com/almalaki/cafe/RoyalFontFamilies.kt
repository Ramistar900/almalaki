package com.almalaki.cafe

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * الخطوط الرسمية لتطبيق ROYAL.
 *
 * جميع ملفات الخطوط موجودة داخل:
 * app/src/main/res/font/
 *
 * هذه الطبقة تربط ملفات TTF الحقيقية مع Compose
 * حتى يمكن استخدام الخطوط في واجهة التطبيق.
 */
object RoyalFontFamilies {

    // ============================================================
    // 🇸🇦 Arabic Fonts
    // ============================================================

    /**
     * Noto Naskh Arabic
     */
    val notoNaskhArabic: FontFamily = FontFamily(
        Font(
            resId = R.font.noto_naskh_arabic,
            weight = FontWeight.Normal
        )
    )

    /**
     * Noto Kufi Arabic
     */
    val notoKufiArabic: FontFamily = FontFamily(
        Font(
            resId = R.font.noto_kufi_arabic,
            weight = FontWeight.Normal
        )
    )

    /**
     * Noto Sans Arabic
     */
    val notoSansArabic: FontFamily = FontFamily(
        Font(
            resId = R.font.noto_sans_arabic,
            weight = FontWeight.Normal
        )
    )

    /**
     * Cairo
     */
    val cairo: FontFamily = FontFamily(
        Font(
            resId = R.font.cairo,
            weight = FontWeight.Normal
        )
    )

    /**
     * Tajawal
     *
     * Tajawal موجود بثلاثة أوزان فعلية:
     * ExtraLight
     * Light
     * Black
     */
    val tajawal: FontFamily = FontFamily(
        Font(
            resId = R.font.tajawal_extra_light,
            weight = FontWeight.ExtraLight
        ),
        Font(
            resId = R.font.tajawal_light,
            weight = FontWeight.Light
        ),
        Font(
            resId = R.font.tajawal_black,
            weight = FontWeight.Black
        )
    )


    // ============================================================
    // 🇬🇧 English Fonts
    // ============================================================

    /**
     * Noto Serif
     *
     * يدعم النسخة العادية والمائلة.
     */
    val notoSerif: FontFamily = FontFamily(
        Font(
            resId = R.font.noto_serif,
            weight = FontWeight.Normal,
            style = FontStyle.Normal
        ),
        Font(
            resId = R.font.noto_serif_italic,
            weight = FontWeight.Normal,
            style = FontStyle.Italic
        )
    )

    /**
     * Playfair Display
     *
     * يدعم النسخة العادية والمائلة.
     */
    val playfairDisplay: FontFamily = FontFamily(
        Font(
            resId = R.font.playfair_display,
            weight = FontWeight.Normal,
            style = FontStyle.Normal
        ),
        Font(
            resId = R.font.playfair_display_italic,
            weight = FontWeight.Normal,
            style = FontStyle.Italic
        )
    )

    /**
     * Libre Bodoni
     *
     * يدعم النسخة العادية والمائلة.
     */
    val libreBodoni: FontFamily = FontFamily(
        Font(
            resId = R.font.libre_bodoni,
            weight = FontWeight.Normal,
            style = FontStyle.Normal
        ),
        Font(
            resId = R.font.libre_bodoni_italic,
            weight = FontWeight.Normal,
            style = FontStyle.Italic
        )
    )
}
