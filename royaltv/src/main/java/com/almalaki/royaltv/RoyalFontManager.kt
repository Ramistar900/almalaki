package com.almalaki.royaltv

import android.content.Context
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * لغة الخط.
 */
enum class RoyalFontLanguage {
    ARABIC,
    ENGLISH
}

/**
 * معرفات الخطوط المتاحة في ROYAL TV.
 */
enum class RoyalFontId {
    NOTO_NASKH_ARABIC,
    NOTO_KUFI_ARABIC,
    NOTO_SANS_ARABIC,
    CAIRO,
    TAJAWAL,
    AREF_RUQAA,
    AMIRI,
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
 * مدير الخطوط المستقل لتطبيق ROYAL TV.
 *
 * يستخدم إعدادات محفوظة مستقلة عن التطبيق الأساسي.
 */
object RoyalFontManager {

    private const val PREFS_NAME = "royaltv_font_system"
    private const val KEY_ARABIC_FONT = "default_arabic_font"
    private const val KEY_ENGLISH_FONT = "default_english_font"
    private const val KEY_TEXT_FONT_PREFIX = "text_font_"

    private var appContext: Context? = null

    private val _arabicFont =
        MutableStateFlow(RoyalFontId.NOTO_NASKH_ARABIC)

    val arabicFont: StateFlow<RoyalFontId> =
        _arabicFont

    private val _englishFont =
        MutableStateFlow(RoyalFontId.NOTO_SERIF)

    val englishFont: StateFlow<RoyalFontId> =
        _englishFont

    private val _textFonts =
        MutableStateFlow<Map<String, RoyalFontId>>(emptyMap())

    val textFonts: StateFlow<Map<String, RoyalFontId>> =
        _textFonts

    /**
     * تهيئة مدير الخطوط.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        loadSavedSettings()
    }

    /**
     * تغيير الخط العربي الافتراضي.
     */
    fun setArabicFont(fontId: RoyalFontId) {
        if (getFont(fontId).language != RoyalFontLanguage.ARABIC) {
            return
        }

        _arabicFont.value = fontId
        save(KEY_ARABIC_FONT, fontId.name)
    }

    /**
     * تغيير الخط الإنجليزي الافتراضي.
     */
    fun setEnglishFont(fontId: RoyalFontId) {
        if (getFont(fontId).language != RoyalFontLanguage.ENGLISH) {
            return
        }

        _englishFont.value = fontId
        save(KEY_ENGLISH_FONT, fontId.name)
    }

    /**
     * تعيين خط خاص لنص معين.
     */
    fun setTextFont(
        textId: String,
        fontId: RoyalFontId
    ) {
        if (textId.isBlank()) return

        val updated = _textFonts.value.toMutableMap()
        updated[textId] = fontId
        _textFonts.value = updated

        save(KEY_TEXT_FONT_PREFIX + textId, fontId.name)
    }

    /**
     * إزالة الخط الخاص بنص.
     */
    fun clearTextFont(textId: String) {
        if (textId.isBlank()) return

        val updated = _textFonts.value.toMutableMap()
        updated.remove(textId)
        _textFonts.value = updated

        remove(KEY_TEXT_FONT_PREFIX + textId)
    }

    /**
     * الحصول على تعريف خط.
     */
    fun getFont(fontId: RoyalFontId): RoyalFontDefinition {
        return when (fontId) {

            RoyalFontId.NOTO_NASKH_ARABIC ->
                definition(
                    fontId,
                    "Noto Naskh Arabic",
                    RoyalFontLanguage.ARABIC,
                    true,
                    RoyalFontFamilies.notoNaskhArabic
                )

            RoyalFontId.NOTO_KUFI_ARABIC ->
                definition(
                    fontId,
                    "Noto Kufi Arabic",
                    RoyalFontLanguage.ARABIC,
                    true,
                    RoyalFontFamilies.notoKufiArabic
                )

            RoyalFontId.NOTO_SANS_ARABIC ->
                definition(
                    fontId,
                    "Noto Sans Arabic",
                    RoyalFontLanguage.ARABIC,
                    true,
                    RoyalFontFamilies.notoSansArabic
                )

            RoyalFontId.CAIRO ->
                definition(
                    fontId,
                    "Cairo",
                    RoyalFontLanguage.ARABIC,
                    false,
                    RoyalFontFamilies.cairo
                )

            RoyalFontId.TAJAWAL ->
                definition(
                    fontId,
                    "Tajawal",
                    RoyalFontLanguage.ARABIC,
                    false,
                    RoyalFontFamilies.tajawal
                )

            RoyalFontId.AREF_RUQAA ->
                definition(
                    fontId,
                    "Aref Ruqaa",
                    RoyalFontLanguage.ARABIC,
                    true,
                    RoyalFontFamilies.arefRuqaa
                )

            RoyalFontId.AMIRI ->
                definition(
                    fontId,
                    "Amiri",
                    RoyalFontLanguage.ARABIC,
                    true,
                    RoyalFontFamilies.amiri
                )

            RoyalFontId.NOTO_SERIF ->
                definition(
                    fontId,
                    "Noto Serif",
                    RoyalFontLanguage.ENGLISH,
                    true,
                    RoyalFontFamilies.notoSerif
                )

            RoyalFontId.LIBRE_BODONI ->
                definition(
                    fontId,
                    "Libre Bodoni",
                    RoyalFontLanguage.ENGLISH,
                    true,
                    RoyalFontFamilies.libreBodoni
                )

            RoyalFontId.COURGETTE ->
                definition(
                    fontId,
                    "Courgette",
                    RoyalFontLanguage.ENGLISH,
                    true,
                    RoyalFontFamilies.courgette
                )
        }
    }

    private fun definition(
        id: RoyalFontId,
        name: String,
        language: RoyalFontLanguage,
        preferred: Boolean,
        family: FontFamily
    ): RoyalFontDefinition {
        return RoyalFontDefinition(
            id = id,
            name = name,
            language = language,
            isRoyalPreferred = preferred,
            family = family
        )
    }

    /**
     * جميع الخطوط العربية.
     */
    fun getArabicFonts(): List<RoyalFontDefinition> {
        return RoyalFontId.entries
            .map { getFont(it) }
            .filter { it.language == RoyalFontLanguage.ARABIC }
    }

    /**
     * جميع الخطوط الإنجليزية.
     */
    fun getEnglishFonts(): List<RoyalFontDefinition> {
        return RoyalFontId.entries
            .map { getFont(it) }
            .filter { it.language == RoyalFontLanguage.ENGLISH }
    }

    /**
     * الخطوط المفضلة لهوية ROYAL.
     */
    fun getRoyalPreferredFonts(): List<RoyalFontDefinition> {
        return RoyalFontId.entries
            .map { getFont(it) }
            .filter { it.isRoyalPreferred }
    }

    /**
     * الخط العربي الحالي.
     */
    fun getArabicFontFamily(): FontFamily {
        return getFont(_arabicFont.value).family
    }

    /**
     * الخط الإنجليزي الحالي.
     */
    fun getEnglishFontFamily(): FontFamily {
        return getFont(_englishFont.value).family
    }

    /**
     * معرفة الخط المخصص لنص.
     */
    fun getTextFontId(textId: String): RoyalFontId? {
        if (textId.isBlank()) return null
        return _textFonts.value[textId]
    }

    /**
     * الحصول على خط نص معين.
     *
     * إذا لم يوجد خط مخصص، يُستخدم الخط العربي الافتراضي.
     */
    fun getTextFontFamily(textId: String): FontFamily {
        val fontId = getTextFontId(textId)

        return if (fontId != null) {
            getFont(fontId).family
        } else {
            getArabicFontFamily()
        }
    }

    /**
     * إعادة الإعدادات إلى الوضع الافتراضي.
     */
    fun resetToDefaults() {
        _arabicFont.value = RoyalFontId.NOTO_NASKH_ARABIC
        _englishFont.value = RoyalFontId.NOTO_SERIF
        _textFonts.value = emptyMap()

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
     * تحميل الإعدادات المحفوظة.
     */
    private fun loadSavedSettings() {
        val context = appContext ?: return

        val preferences = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        val arabicId = parseFont(
            preferences.getString(KEY_ARABIC_FONT, null),
            RoyalFontId.NOTO_NASKH_ARABIC
        )

        _arabicFont.value =
            if (getFont(arabicId).language == RoyalFontLanguage.ARABIC) {
                arabicId
            } else {
                RoyalFontId.NOTO_NASKH_ARABIC
            }

        val englishId = parseFont(
            preferences.getString(KEY_ENGLISH_FONT, null),
            RoyalFontId.NOTO_SERIF
        )

        _englishFont.value =
            if (getFont(englishId).language == RoyalFontLanguage.ENGLISH) {
                englishId
            } else {
                RoyalFontId.NOTO_SERIF
            }

        val savedTextFonts = mutableMapOf<String, RoyalFontId>()

        preferences.all.forEach { entry ->
            val key = entry.key

            if (key.startsWith(KEY_TEXT_FONT_PREFIX)) {
                val textId = key.removePrefix(KEY_TEXT_FONT_PREFIX)

                val fontId = runCatching {
                    RoyalFontId.valueOf(entry.value as? String ?: "")
                }.getOrNull()

                if (textId.isNotBlank() && fontId != null) {
                    savedTextFonts[textId] = fontId
                }
            }
        }

        _textFonts.value = savedTextFonts
    }

    private fun parseFont(
        value: String?,
        fallback: RoyalFontId
    ): RoyalFontId {
        if (value.isNullOrBlank()) return fallback

        return runCatching {
            RoyalFontId.valueOf(value)
        }.getOrDefault(fallback)
    }

    private fun save(
        key: String,
        value: String
    ) {
        val context = appContext ?: return

        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .putString(key, value)
            .apply()
    }

    private fun remove(key: String) {
        val context = appContext ?: return

        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).edit()
            .remove(key)
            .apply()
    }
}
