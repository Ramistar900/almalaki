package com.almalaki.cafe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ============================================================
 * إعدادات العميل — خط النظام
 * ============================================================
 *
 * هذه الشاشة مسؤولة عن اختيار الخط الافتراضي للنظام.
 *
 * ملاحظة:
 * التطبيق الفعلي للخط على جميع الواجهات سيتم ربطه
 * لاحقًا مع Theme / CompositionLocal الخاص بالتطبيق.
 *
 * هنا نختار الخط ونحفظه فقط عبر RoyalFontManager.
 */
@Composable
fun RoyalCustomerSettings(
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onAccountClick: () -> Unit = {},
    onOrdersClick: () -> Unit = {}
) {

    /*
     * --------------------------------------------------------
     * الخط العربي الحالي المحفوظ.
     * --------------------------------------------------------
     */
    val savedArabicFont =
        RoyalFontManager.arabicFont.value

    /*
     * --------------------------------------------------------
     * الخط الإنجليزي الحالي المحفوظ.
     * --------------------------------------------------------
     */
    val savedEnglishFont =
        RoyalFontManager.englishFont.value

    /*
     * --------------------------------------------------------
     * الاختيار المؤقت داخل الشاشة.
     *
     * لا يتم الحفظ إلا عند الضغط على "تطبيق".
     * --------------------------------------------------------
     */
    var selectedArabicFont by remember(savedArabicFont) {
        mutableStateOf(savedArabicFont)
    }

    var selectedEnglishFont by remember(savedEnglishFont) {
        mutableStateOf(savedEnglishFont)
    }

    var expandedSection by remember { mutableStateOf<String?>(null) }

    /*
     * --------------------------------------------------------
     * ألوان واجهة ROYAL.
     * --------------------------------------------------------
     */
    val background =
        if (isDarkMode) {
            Color(0xFF050505)
        } else {
            Color(0xFFF5F0E5)
        }

    val cardColor =
        if (isDarkMode) {
            Color(0xFF111111)
        } else {
            Color.White
        }

    val primaryText =
        if (isDarkMode) {
            Color.White
        } else {
            Color(0xFF171717)
        }

    val secondaryText =
        if (isDarkMode) {
            Color(0xFFBDBDBD)
        } else {
            Color(0xFF555555)
        }

    val gold =
        Color(0xFFD4AF37)

    val goldLight =
        Color(0xFFFFE9A3)

    /*
     * --------------------------------------------------------
     * قائمة الخطوط العربية.
     * --------------------------------------------------------
     */
    val arabicFonts =
        remember {
            RoyalFontManager.getArabicFonts()
        }

    /*
     * --------------------------------------------------------
     * قائمة الخطوط الإنجليزية.
     * --------------------------------------------------------
     */
    val englishFonts =
        remember {
            RoyalFontManager.getEnglishFonts()
        }

    /*
     * --------------------------------------------------------
     * الشاشة.
     * --------------------------------------------------------
     */
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(background)
    ) {

        /*
         * ====================================================
         * الشريط العلوي
         * ====================================================
         */
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 10.dp,
                        vertical = 10.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector =
                        Icons.Default.ArrowBack,

                    contentDescription =
                        "رجوع",

                    tint = gold
                )
            }

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            Column {

                Text(
                    text = "إعدادات الملكي",

                    color = gold,

                    fontSize = 21.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(2.dp)
                )

                Text(
                    text =
                        "حسابك وطلباتك وتفضيلاتك في مكان واحد",

                    color = secondaryText,

                    fontSize = 12.sp
                )
            }
        }

        /*
         * ====================================================
         * المحتوى
         * ====================================================
         */
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            item {
                RoyalSettingsCompactRow(
                    title = "حسابي",
                    glyph = "◎",
                    isDarkMode = isDarkMode,
                    expanded = false,
                    onClick = {
                        onBack()
                        onAccountClick()
                    }
                )
            }
            item {
                RoyalSettingsCompactRow(
                    title = "طلباتي وفواتيري",
                    glyph = "▤",
                    isDarkMode = isDarkMode,
                    expanded = false,
                    onClick = {
                        onBack()
                        onOrdersClick()
                    }
                )
            }
            item {
                RoyalSettingsCompactRow(
                    title = "خط التطبيق",
                    glyph = "Aa",
                    isDarkMode = isDarkMode,
                    expanded = expandedSection == "fonts",
                    onClick = {
                        expandedSection = if (expandedSection == "fonts") null else "fonts"
                    }
                )
            }
            if (expandedSection == "fonts") {
                item {
                    RoyalFontSettingsInfoCard(
                        cardColor = cardColor,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        gold = gold,
                        goldLight = goldLight
                    )
                }
                item {
                    Text(
                        text = "الخط العربي",
                        modifier = Modifier.padding(top = 3.dp, bottom = 1.dp),
                        color = gold,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(items = arabicFonts, key = { "ar_${it.id.name}" }) { font ->
                    RoyalFontChoiceCard(
                        font = font,
                        selected = selectedArabicFont == font.id,
                        isDarkMode = isDarkMode,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        gold = gold,
                        goldLight = goldLight,
                        onClick = { selectedArabicFont = font.id }
                    )
                }
                item {
                    Text(
                        text = "الخط الإنجليزي",
                        modifier = Modifier.padding(top = 5.dp, bottom = 1.dp),
                        color = gold,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(items = englishFonts, key = { "en_${it.id.name}" }) { font ->
                    RoyalFontChoiceCard(
                        font = font,
                        selected = selectedEnglishFont == font.id,
                        isDarkMode = isDarkMode,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        gold = gold,
                        goldLight = goldLight,
                        onClick = { selectedEnglishFont = font.id }
                    )
                }
                item {
                    Button(
                        onClick = {
                            RoyalFontManager.setArabicFont(selectedArabicFont)
                            RoyalFontManager.setEnglishFont(selectedEnglishFont)
                            expandedSection = null
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = gold,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("حفظ الخطوط", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                RoyalSettingsCompactRow(
                    title = "الأخبار والإشعارات",
                    glyph = "◉",
                    isDarkMode = isDarkMode,
                    expanded = expandedSection == "news",
                    onClick = {
                        expandedSection = if (expandedSection == "news") null else "news"
                    }
                )
            }
            if (expandedSection == "news") {
                item {
                    RoyalSettingsInfoCard(
                        glyph = "◉",
                        title = "أخبار المطعم",
                        message = "تظهر رسائل وأخبار المطعم عبر الشريط الإخباري في الواجهة الرئيسية.",
                        isDarkMode = isDarkMode
                    )
                }
            }
            item {
                RoyalSettingsCompactRow(
                    title = "الخصوصية والأمان",
                    glyph = "◇",
                    isDarkMode = isDarkMode,
                    expanded = expandedSection == "privacy",
                    onClick = {
                        expandedSection = if (expandedSection == "privacy") null else "privacy"
                    }
                )
            }
            if (expandedSection == "privacy") {
                item {
                    RoyalSettingsInfoCard(
                        glyph = "◇",
                        title = "بياناتك على جهازك",
                        message = "يحفظ التطبيق بيانات الملف الشخصي وسجل الطلبات محليًا على هذا الجهاز. يمكنك مراجعة بيانات حسابك من قسم «حسابي».",
                        isDarkMode = isDarkMode
                    )
                }
            }
            item {
                RoyalSettingsCompactRow(
                    title = "معلومات التطبيق ومشاركته",
                    glyph = "ⓘ",
                    isDarkMode = isDarkMode,
                    expanded = expandedSection == "sharing",
                    onClick = {
                        expandedSection = if (expandedSection == "sharing") null else "sharing"
                    }
                )
            }
            if (expandedSection == "sharing") {
                item {
                    RoyalSettingsInfoCard(
                        glyph = "ⓘ",
                        title = "تطبيق الملكي",
                        message = "واجهة العميل لعرض قائمة المطعم وإرسال الطلبات ومتابعة سجلها.",
                        isDarkMode = isDarkMode
                    )
                }
                item {
                    CustomerAppSharing(isDarkMode = isDarkMode)
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "اضغط على أي خيار لعرض تفاصيله",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                    color = secondaryText,
                    fontSize = 11.sp
                )
            }
        }
    }
}


/**
 * ============================================================
 * بطاقة تعريف نظام الخط
 * ============================================================
 */
@Composable
private fun RoyalFontSettingsInfoCard(
    cardColor: Color,
    primaryText: Color,
    secondaryText: Color,
    gold: Color,
    goldLight: Color
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = cardColor,
                    shape = RoundedCornerShape(18.dp)
                )
                .border(
                    width = 1.dp,
                    color =
                        gold.copy(
                            alpha = 0.45f
                        ),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(16.dp)
    ) {

        Text(
            text = "خط النظام",

            color = gold,

            fontSize = 17.sp,

            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(7.dp)
        )

        Text(
            text =
                "اختر الخط الذي تريد استخدامه كخط افتراضي للتطبيق. " +
                    "يمكن إضافة خطوط جديدة لاحقًا إلى النظام.",

            color = secondaryText,

            fontSize = 13.sp,

            lineHeight = 21.sp
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        Text(
            text =
                "سيتم حفظ اختيارك على الجهاز.",

            color =
                goldLight.copy(
                    alpha = 0.85f
                ),

            fontSize = 12.sp
        )
    }
}


/**
 * ============================================================
 * بطاقة اختيار الخط
 * ============================================================
 */
@Composable
private fun RoyalFontChoiceCard(
    font: RoyalFontDefinition,
    selected: Boolean,
    isDarkMode: Boolean,
    primaryText: Color,
    secondaryText: Color,
    gold: Color,
    goldLight: Color,
    onClick: () -> Unit
) {

    val cardBackground =
        if (selected) {

            if (isDarkMode) {
                Color(0xFF211B0A)
            } else {
                Color(0xFFFFF8DE)
            }

        } else {

            if (isDarkMode) {
                Color(0xFF111111)
            } else {
                Color.White
            }
        }

    val borderColor =
        if (selected) {
            gold
        } else {
            gold.copy(alpha = 0.22f)
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = cardBackground,
                    shape =
                        RoundedCornerShape(16.dp)
                )
                .border(
                    width =
                        if (selected) {
                            1.5.dp
                        } else {
                            1.dp
                        },
                    color = borderColor,
                    shape =
                        RoundedCornerShape(16.dp)
                )
                .clickable(
                    onClick = onClick
                )
                .padding(
                    horizontal = 15.dp,
                    vertical = 13.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        /*
         * ----------------------------------------------------
         * علامة الاختيار
         * ----------------------------------------------------
         */
        Box(
            modifier =
                Modifier
                    .width(28.dp)
                    .height(28.dp)
                    .background(
                        color =
                            if (selected) {
                                gold
                            } else {
                                Color.Transparent
                            },
                        shape =
                            RoundedCornerShape(50)
                    )
                    .border(
                        width = 1.dp,
                        color =
                            if (selected) {
                                gold
                            } else {
                                gold.copy(
                                    alpha = 0.5f
                                )
                            },
                        shape =
                            RoundedCornerShape(50)
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            if (selected) {

                Text(
                    text = "✓",

                    color = Color.Black,

                    fontSize = 16.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier =
                Modifier.width(13.dp)
        )

        /*
         * ----------------------------------------------------
         * اسم الخط + المعاينة
         * ----------------------------------------------------
         */
        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text = font.name,

                color =
                    if (selected) {
                        gold
                    } else {
                        primaryText
                    },

                fontSize = 15.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            Text(
    text =
        if (
            font.language ==
                RoyalFontLanguage.ARABIC
        ) {
            "سحر الشرق يبدأ من دمشق"
        } else {
            "The Magic of the East Begins in Damascus"
        },

    color =
        if (selected) {
            goldLight
        } else {
            secondaryText
        },

    fontFamily =
        font.family,

    fontSize = 17.sp,

    maxLines = 1
)
        }
    }
}


@Composable
private fun RoyalSettingsCompactRow(
    title: String,
    glyph: String,
    isDarkMode: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val gold = Color(0xFFD4AF37)
    val cardColor = if (isDarkMode) Color(0xFF111111) else Color.White
    val primaryText = if (isDarkMode) Color.White else Color(0xFF171717)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardColor, RoundedCornerShape(16.dp))
            .border(1.dp, gold.copy(alpha = if (expanded) 0.75f else 0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(38.dp)
                .background(Color(0xFF123529), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = glyph,
                color = Color(0xFFFFE9A3),
                fontSize = if (glyph == "Aa") 16.sp else 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = primaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (expanded) "⌃" else "‹",
            color = gold,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RoyalSettingsSectionHeading(
    title: String,
    subtitle: String,
    glyph: String = "✦"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(42.dp)
                .background(
                    Color(0xFF123529),
                    RoundedCornerShape(14.dp)
                )
                .border(
                    1.dp,
                    Color(0xFFD4AF37).copy(alpha = 0.45f),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = glyph,
                color = Color(0xFFFFE9A3),
                fontSize = if (glyph == "Aa") 16.sp else 21.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(11.dp))
        Column {
            Text(
                text = title,
                color = Color(0xFFD4AF37),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF8C877B),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun RoyalSettingsActionCard(
    glyph: String,
    title: String,
    subtitle: String,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val gold = Color(0xFFD4AF37)
    val cardColor = if (isDarkMode) Color(0xFF111111) else Color.White
    val primaryText = if (isDarkMode) Color.White else Color(0xFF171717)
    val secondaryText = if (isDarkMode) Color(0xFFBDBDBD) else Color(0xFF555555)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardColor, RoundedCornerShape(17.dp))
            .border(1.dp, gold.copy(alpha = 0.42f), RoundedCornerShape(17.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(43.dp)
                .height(43.dp)
                .background(
                    if (isDarkMode) Color(0xFF18372C) else Color(0xFFFFF3CD),
                    RoundedCornerShape(13.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = glyph,
                color = gold,
                fontSize = if (glyph == "Aa") 17.sp else 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = primaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = secondaryText,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "‹",
            color = gold,
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RoyalSettingsInfoCard(
    glyph: String,
    title: String,
    message: String,
    isDarkMode: Boolean
) {
    val gold = Color(0xFFD4AF37)
    val cardColor = if (isDarkMode) Color(0xFF111111) else Color.White
    val primaryText = if (isDarkMode) Color.White else Color(0xFF171717)
    val secondaryText = if (isDarkMode) Color(0xFFBDBDBD) else Color(0xFF555555)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardColor, RoundedCornerShape(17.dp))
            .border(1.dp, gold.copy(alpha = 0.28f), RoundedCornerShape(17.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(38.dp)
                .background(
                    if (isDarkMode) Color(0xFF18372C) else Color(0xFFFFF3CD),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = glyph,
                color = gold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = message,
                color = secondaryText,
                fontSize = 12.sp,
                lineHeight = 19.sp
            )
        }
    }
}
