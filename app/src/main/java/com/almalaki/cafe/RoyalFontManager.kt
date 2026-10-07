package com.almalaki.cafe

import android.content.Context
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * اللغة التي يرتبط بها الخط.
 */
enum class RoyalFontLanguage {
    ARABIC,
    ENGLISH
}

/**
 * الخطوط المدعومة حاليًا في نظام ROYAL.
 */
enum class RoyalFontId {

    // Arabic
    NOTO_NASKH_ARABIC,
    NOTO_KUFI_ARABIC,
    NOTO_SANS_ARABIC,
    CAIRO,
    TAJAWAL,
    AREF_RUQAA,
    AMIRI,

    // English
    NOTO_SERIF,
    LIBRE_BODONI,
    COURGETTE
}

/**
 * تعريف الخط.
 */
data class RoyalFontDefinition(
    val id: RoyalFontId,
    val name: String,
    val language: RoyalFontLanguage,
    val isRoyalPreferred: Boolean,
    val family: FontFamily
)

/**
 * النظام العام للخطوط في تطبيق ROYAL.
 */
object RoyalFontManager {

    private const val PREFS_NAME = "royal_font_system"
    private const val KEY_ARABIC_FONT = "default_arabic_font"
    private const val KEY_ENGLISH_FONT = "default_english_font"
    private const val KEY_TEXT_FONT_PREFIX = "text_font_"

    private var appContext: Context? = null

    private val _arabicFont =
        MutableStateFlow(
            RoyalFontId.NOTO_NASKH_ARABIC
        )

    val arabicFont: StateFlow<RoyalFontId> =
        _arabicFont

    private val _englishFont =
        MutableStateFlow(
            RoyalFontId.NOTO_SERIF
        )

    val englishFont: StateFlow<RoyalFontId> =
        _englishFont

    private val _textFonts =
        MutableStateFlow<Map<String, RoyalFontId>>(
            emptyMap()
        )

    val textFonts: StateFlow<Map<String, RoyalFontId>> =
        _textFonts

    /**
     * تهيئة النظام.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        loadSavedSettings()
    }

    /**
     * تعيين الخط العربي الافتراضي.
     */
    fun setArabicFont(fontId: RoyalFontId) {
        if (
            getFont(fontId).language !=
            RoyalFontLanguage.ARABIC
        ) {
            return
        }

        _arabicFont.value = fontId

        save(
            KEY_ARABIC_FONT,
            fontId.name
        )
    }

    /**
     * تعيين الخط الإنجليزي الافتراضي.
     */
    fun setEnglishFont(fontId: RoyalFontId) {
        if (
            getFont(fontId).language !=
            RoyalFontLanguage.ENGLISH
        ) {
            return
        }

        _englishFont.value = fontId

        save(
            KEY_ENGLISH_FONT,
            fontId.name
        )
    }

    /**
     * تعيين خط مستقل لنص معين.
     */
    fun setTextFont(
        textId: String,
        fontId: RoyalFontId
    ) {
        if (textId.isBlank()) {
            return
        }

        val updated =
            _textFonts.value.toMutableMap()

        updated[textId] = fontId

        _textFonts.value = updated

        save(
            KEY_TEXT_FONT_PREFIX + textId,
            fontId.name
        )
    }

    /**
     * إزالة الخط الخاص بنص.
     */
    fun clearTextFont(textId: String) {
        if (textId.isBlank()) {
            return
        }

        val updated =
            _textFonts.value.toMutableMap()

        updated.remove(textId)

        _textFonts.value = updated

        remove(
            KEY_TEXT_FONT_PREFIX + textId
        )
    }

    /**
     * الحصول على تعريف خط معين.
     */
    fun getFont(
        fontId: RoyalFontId
    ): RoyalFontDefinition {

        return when (fontId) {

            RoyalFontId.NOTO_NASKH_ARABIC ->
                RoyalFontDefinition(
                    id = fontId,
                    name = "Noto Naskh Arabic",
                    language = RoyalFontLanguage.ARABIC,
                    isRoyalPreferred = true,
                    family =
                        RoyalFontFamilies.notoNaskhArabic
                )

            RoyalFontId.NOTO_KUFI_ARABIC ->
                RoyalFontDefinition(
                    id = fontId,
                    name = "Noto Kufi Arabic",
                    language = RoyalFontLanguage.ARABIC,
                    isRoyalPreferred = true,
                    family =
                        RoyalFontFamilies.notoKufiArabic
                )

            RoyalFontId.NOTO_SANS_ARABIC ->
                RoyalFontDefinition(
                    id = fontId,
                    name = "Noto Sans Arabic",
                    language = RoyalFontLanguage.ARABIC,
                    isRoyalPreferred = true,
                    family =
                        RoyalFontFamilies.notoSansArabic
                )

            RoyalFontId.CAIRO ->
                RoyalFontDefinition(
                    id = fontId,
                    name = "Cairo",
                    language = RoyalFontLanguage.ARABIC,
                    isRoyalPreferred = false,
                    family =
                        RoyalFontFamilies.cairo
                )

            RoyalFontId.TAJAWAL ->
                RoyalFontDefinition(
                    id = fontId,
                    name = "Tajawal",
                    language = RoyalFontLanguage.ARABIC,
                    isRoyalPreferred = false,
                    family =
                        RoyalFontFamilies.tajawal
                )

            RoyalFontId.AREF_RUQAA ->
                RoyalFontDefinition(
                    id = fontId,
                    name = "Aref Ruqaa",
                    language = RoyalFontLanguage.ARABIC,
                    isRoyalPreferred = true,
                    family =
                        RoyalFontFamilies.arefRuqaa
                )

            RoyalFontId.AMIRI ->
                RoyalFontDefinition(
                    id = fontId,
                    name = "Amiri",
                    language = RoyalFontLanguage.ARABIC,
                    isRoyalPreferred = true,
                    family =
                        RoyalFontFamilies.amiri
                )

            RoyalFontId.NOTO_SERIF ->
                RoyalFontDefinition(
                    id = fontId,
                    name = "Noto Serif",
                    language = RoyalFontLanguage.ENGLISH,
                    isRoyalPreferred = true,
                    family =
                        RoyalFontFamilies.notoSerif
                )

            RoyalFontId.LIBRE_BODONI ->
                RoyalFontDefinition(
                    id = fontId,
                    name = "Libre Bodoni",
                    language = RoyalFontLanguage.ENGLISH,
                    isRoyalPreferred = true,
                    family =
                        RoyalFontFamilies.libreBodoni
                )

            RoyalFontId.COURGETTE ->
                RoyalFontDefinition(
                    id = fontId,
                    name = "Courgette",
                    language = RoyalFontLanguage.ENGLISH,
                    isRoyalPreferred = true,
                    family =
                        RoyalFontFamilies.courgette
                )
        }
    }

    /**
     * جميع الخطوط العربية.
     */
    fun getArabicFonts():
        List<RoyalFontDefinition> {

        return RoyalFontId.entries
            .filter {
                getFont(it).language ==
                    RoyalFontLanguage.ARABIC
            }
            .map {
                getFont(it)
            }
    }

    /**
     * جميع الخطوط الإنجليزية.
     */
    fun getEnglishFonts():
        List<RoyalFontDefinition> {

        return RoyalFontId.entries
            .filter {
                getFont(it).language ==
                    RoyalFontLanguage.ENGLISH
            }
            .map {
                getFont(it)
            }
    }

    /**
     * الخطوط المفضلة لهوية ROYAL.
     */
    fun getRoyalPreferredFonts():
        List<RoyalFontDefinition> {

        return RoyalFontId.entries
            .map {
                getFont(it)
            }
            .filter {
                it.isRoyalPreferred
            }
    }

    /**
     * FontFamily الحالي للغة العربية.
     */
    fun getArabicFontFamily():
        FontFamily {

        return getFont(
            _arabicFont.value
        ).family
    }

    /**
     * FontFamily الحالي للغة الإنجليزية.
     */
    fun getEnglishFontFamily():
        FontFamily {

        return getFont(
            _englishFont.value
        ).family
    }

    /**
     * معرف الخط الخاص بنص معين.
     */
    fun getTextFontId(
        textId: String
    ): RoyalFontId? {

        if (textId.isBlank()) {
            return null
        }

        return _textFonts.value[textId]
    }

    /**
     * FontFamily الخاص بنص معين.
     */
    fun getTextFontFamily(
        textId: String
    ): FontFamily {

        val fontId =
            getTextFontId(textId)

        return if (fontId != null) {
            getFont(fontId).family
        } else {
            getArabicFontFamily()
        }
    }

    /**
     * إعادة الخطوط إلى الإعدادات الافتراضية.
     */
    fun resetToDefaults() {

        _arabicFont.value =
            RoyalFontId.NOTO_NASKH_ARABIC

        _englishFont.value =
            RoyalFontId.NOTO_SERIF

        _textFonts.value =
            emptyMap()

        appContext
            ?.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            ?.edit()
            ?.clear()
            ?.apply()
    }

    /**
     * قراءة الإعدادات المحفوظة.
     */
    private fun loadSavedSettings() {

        val context =
            appContext ?: return

        val preferences =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        _arabicFont.value =
            parseFont(
                preferences.getString(
                    KEY_ARABIC_FONT,
                    null
                ),
                RoyalFontId.NOTO_NASKH_ARABIC
            ).let { fontId ->

                if (
                    getFont(fontId).language ==
                    RoyalFontLanguage.ARABIC
                ) {
                    fontId
                } else {
                    RoyalFontId.NOTO_NASKH_ARABIC
                }
            }

        _englishFont.value =
            parseFont(
                preferences.getString(
                    KEY_ENGLISH_FONT,
                    null
                ),
                RoyalFontId.NOTO_SERIF
            ).let { fontId ->

                if (
                    getFont(fontId).language ==
                    RoyalFontLanguage.ENGLISH
                ) {
                    fontId
                } else {
                    RoyalFontId.NOTO_SERIF
                }
            }

        val savedTextFonts =
            mutableMapOf<String, RoyalFontId>()

        preferences.all.forEach { entry ->

            val key = entry.key

            if (
                key.startsWith(
                    KEY_TEXT_FONT_PREFIX
                )
            ) {

                val textId =
                    key.removePrefix(
                        KEY_TEXT_FONT_PREFIX
                    )

                val fontId =
                    runCatching {
                        RoyalFontId.valueOf(
                            entry.value as? String
                                ?: ""
                        )
                    }.getOrNull()

                if (
                    textId.isNotBlank() &&
                    fontId != null
                ) {
                    savedTextFonts[textId] =
                        fontId
                }
            }
        }

        _textFonts.value =
            savedTextFonts
    }

    /**
     * تحويل القيمة المحفوظة إلى RoyalFontId.
     */
    private fun parseFont(
        value: String?,
        fallback: RoyalFontId
    ): RoyalFontId {

        if (value.isNullOrBlank()) {
            return fallback
        }

        return runCatching {
            RoyalFontId.valueOf(value)
        }.getOrDefault(fallback)
    }

    /**
     * حفظ قيمة.
     */
    private fun save(
        key: String,
        value: String
    ) {

        val context =
            appContext ?: return

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                key,
                value
            )
            .apply()
    }

    /**
     * حذف قيمة.
     */
    private fun remove(
        key: String
    ) {

        val context =
            appContext ?: return

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(key)
            .apply()
    }
}
