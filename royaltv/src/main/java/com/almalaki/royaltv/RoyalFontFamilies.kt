package com.almalaki.royaltv

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * نظام الخطوط المستقل لتطبيق ROYAL TV.
 *
 * تعتمد هذه النسخة على موارد الخطوط الموجودة في:
 * royaltv/src/main/res/font/
 *
 * لا تعتمد على موارد الخطوط الخاصة بالتطبيق الأصلي.
 */
object RoyalFontFamilies {

    // =========================================================
    // Arabic Fonts
    // =========================================================

    val notoNaskhArabic: FontFamily = FontFamily(
        Font(
            resId = R.font.noto_naskh_arabic,
            weight = FontWeight.Normal,
            style = FontStyle.Normal
        )
    )

    val notoKufiArabic: FontFamily = FontFamily(
        Font(
            resId = R.font.noto_kufi_arabic,
            weight = FontWeight.Normal,
            style = FontStyle.Normal
        )
    )

    val notoSansArabic: FontFamily = FontFamily(
        Font(
            resId = R.font.noto_sans_arabic,
            weight = FontWeight.Normal,
            style = FontStyle.Normal
        )
    )

    val cairo: FontFamily = FontFamily(
        Font(
            resId = R.font.cairo,
            weight = FontWeight.Normal,
            style = FontStyle.Normal
        )
    )

    // =========================================================
    // Tajawal — جميع الأوزان المتوفرة
    // =========================================================

    val tajawal: FontFamily = FontFamily(
        Font(
            resId = R.font.tajawal_extralight,
            weight = FontWeight.ExtraLight
        ),
        Font(
            resId = R.font.tajawal_light,
            weight = FontWeight.Light
        ),
        Font(
            resId = R.font.tajawal_regular,
            weight = FontWeight.Normal
        ),
        Font(
            resId = R.font.tajawal_medium,
            weight = FontWeight.Medium
        ),
        Font(
            resId = R.font.tajawal_bold,
            weight = FontWeight.Bold
        ),
        Font(
            resId = R.font.tajawal_extrabold,
            weight = FontWeight.ExtraBold
        ),
        Font(
            resId = R.font.tajawal_black,
            weight = FontWeight.Black
        )
    )

    // =========================================================
    // Aref Ruqaa
    // =========================================================

    val arefRuqaa: FontFamily = FontFamily(
        Font(
            resId = R.font.aref_ruqaa_regular,
            weight = FontWeight.Normal,
            style = FontStyle.Normal
        ),
        Font(
            resId = R.font.aref_ruqaa_bold,
            weight = FontWeight.Bold,
            style = FontStyle.Normal
        )
    )

    // =========================================================
    // Amiri
    // =========================================================

    val amiri: FontFamily = FontFamily(
        Font(
            resId = R.font.amiri_regular,
            weight = FontWeight.Normal,
            style = FontStyle.Normal
        ),
        Font(
            resId = R.font.amiri_bold,
            weight = FontWeight.Bold,
            style = FontStyle.Normal
        ),
        Font(
            resId = R.font.amiri_italic,
            weight = FontWeight.Normal,
            style = FontStyle.Italic
        ),
        Font(
            resId = R.font.amiri_bold_italic,
            weight = FontWeight.Bold,
            style = FontStyle.Italic
        )
    )

    // =========================================================
    // English Fonts
    // =========================================================

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

    // =========================================================
    // Libre Bodoni
    // =========================================================

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

    // =========================================================
    // Courgette
    // =========================================================

    val courgette: FontFamily = FontFamily(
        Font(
            resId = R.font.courgette_regular,
            weight = FontWeight.Normal,
            style = FontStyle.Normal
        )
    )
}
