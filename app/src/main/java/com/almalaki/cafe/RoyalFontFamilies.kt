package com.almalaki.cafe

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

object RoyalFontFamilies {

    val notoNaskhArabic = FontFamily(
        Font(
            resId = R.font.noto_naskh_arabic,
            weight = FontWeight.Normal
        )
    )

    val notoKufiArabic = FontFamily(
        Font(
            resId = R.font.noto_kufi_arabic,
            weight = FontWeight.Normal
        )
    )

    val notoSansArabic = FontFamily(
        Font(
            resId = R.font.noto_sans_arabic,
            weight = FontWeight.Normal
        )
    )

    val cairo = FontFamily(
        Font(
            resId = R.font.cairo,
            weight = FontWeight.Normal
        )
    )

    val tajawal = FontFamily(
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

    val notoSerif = FontFamily(
        Font(
            resId = R.font.noto_serif,
            weight = FontWeight.Normal
        ),
        Font(
            resId = R.font.noto_serif_italic,
            weight = FontWeight.Normal,
            style = FontStyle.Italic
        )
    )

    val playfairDisplay = FontFamily(
        Font(
            resId = R.font.playfair_display,
            weight = FontWeight.Normal
        ),
        Font(
            resId = R.font.playfair_display_italic,
            weight = FontWeight.Normal,
            style = FontStyle.Italic
        )
    )

    val libreBodoni = FontFamily(
        Font(
            resId = R.font.libre_bodoni,
            weight = FontWeight.Normal
        ),
        Font(
            resId = R.font.libre_bodoni_italic,
            weight = FontWeight.Normal,
            style = FontStyle.Italic
        )
    )

    /*
     * لا نضيف هنا:
     * Noto Sans
     * Roboto
     * Montserrat
     *
     * لأنها ليست ضمن قائمة الخطوط الـ13 التي اخترناها.
     */
}
