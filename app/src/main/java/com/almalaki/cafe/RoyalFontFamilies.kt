package com.almalaki.cafe

import androidx.compose.ui.text.font.FontFamily

/**
 * Royal Font Families
 *
 * هذه الطبقة هي نقطة الربط بين نظام الخطوط
 * وملفات الخطوط الفعلية التي ستضاف لاحقًا داخل:
 *
 * app/src/main/res/font/
 *
 * في هذه المرحلة نستخدم FontFamily.Default
 * حتى يبقى المشروع قابلًا للبناء قبل إضافة ملفات
 * TTF / OTF الفعلية.
 */
object RoyalFontFamilies {

    val notoNaskhArabic: FontFamily =
        FontFamily.Default

    val notoKufiArabic: FontFamily =
        FontFamily.Default

    val notoSansArabic: FontFamily =
        FontFamily.Default

    val cairo: FontFamily =
        FontFamily.Default

    val tajawal: FontFamily =
        FontFamily.Default

    val notoSans: FontFamily =
        FontFamily.Default

    val notoSerif: FontFamily =
        FontFamily.Default

    val roboto: FontFamily =
        FontFamily.Default

    val montserrat: FontFamily =
        FontFamily.Default
}
