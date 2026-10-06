package com.almalaki.cafe

import android.content.Context
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class RoyalFontLanguage {
    ARABIC,
    ENGLISH
}

enum class RoyalFontId {
    NOTO_NASKH_ARABIC,
    NOTO_KUFI_ARABIC,
    NOTO_SANS_ARABIC,
    CAIRO,
    TAJAWAL,

    NOTO_SANS,
    NOTO_SERIF,
    ROBOTO,
    MONTSERRAT
}

data class RoyalFontDefinition(
    val id: RoyalFontId,
    val name: String,
    val language: RoyalFontLanguage,
    val isRoyalPreferred: Boolean,
    val family: FontFamily
)

object RoyalFontManager {

    private const val PREFS_NAME = "royal_font_system"

    private const val KEY_ARABIC_FONT =
        "default_arabic_font"

    private const val KEY_ENGLISH_FONT =
        "default_english_font"

    private const val KEY_TEXT_PREFIX =
        "text_font_"

    private val _arabicFont =
        MutableStateFlow(
            RoyalFontId.NOTO_NASKH_ARABIC
        )

    val arabicFont: StateFlow<RoyalFontId> =
        _arabicFont

    private val _englishFont =
        MutableStateFlow(
            RoyalFontId.NOTO_SANS
        )

    val englishFont: StateFlow<RoyalFontId> =
        _englishFont

    private val _textFonts =
        MutableStateFlow<Map<String, RoyalFontId>>(
            emptyMap()
        )

    val textFonts: StateFlow<Map<String, RoyalFontId>> =
        _textFonts

    private var appContext: Context? = null

    val fonts: List<RoyalFontDefinition>
        get() = listOf(

            RoyalFontDefinition(
                id = RoyalFontId.NOTO_NASKH_ARABIC,
                name = "Noto Naskh Arabic",
                language = RoyalFontLanguage.ARABIC,
                isRoyalPreferred = true,
                family = RoyalFontFamilies.notoNaskhArabic
            ),

            RoyalFontDefinition(
                id = RoyalFontId.NOTO_KUFI_ARABIC,
                name = "Noto Kufi Arabic",
                language = RoyalFontLanguage.ARABIC,
                isRoyalPreferred = true,
                family = RoyalFontFamilies.notoKufiArabic
            ),

            RoyalFontDefinition(
                id = RoyalFontId.NOTO_SANS_ARABIC,
                name = "Noto Sans Arabic",
                language = RoyalFontLanguage.ARABIC,
                isRoyalPreferred = true,
                family = RoyalFontFamilies.notoSansArabic
            ),

            RoyalFontDefinition(
                id = RoyalFontId.CAIRO,
                name = "Cairo",
                language = RoyalFontLanguage.ARABIC,
                isRoyalPreferred = false,
                family = RoyalFontFamilies.cairo
            ),

            RoyalFontDefinition(
                id = RoyalFontId.TAJAWAL,
                name = "Tajawal",
                language = RoyalFontLanguage.ARABIC,
                isRoyalPreferred = false,
                family = RoyalFontFamilies.tajawal
            ),

            RoyalFontDefinition(
                id = RoyalFontId.NOTO_SANS,
                name = "Noto Sans",
                language = RoyalFontLanguage.ENGLISH,
                isRoyalPreferred = true,
                family = RoyalFontFamilies.notoSans
            ),

            RoyalFontDefinition(
                id = RoyalFontId.NOTO_SERIF,
                name = "Noto Serif",
                language = RoyalFontLanguage.ENGLISH,
                isRoyalPreferred = true,
                family = RoyalFontFamilies.notoSerif
            ),

            RoyalFontDefinition(
                id = RoyalFontId.ROBOTO,
                name = "Roboto",
                language = RoyalFontLanguage.ENGLISH,
                isRoyalPreferred = false,
                family = RoyalFontFamilies.roboto
            ),

            RoyalFontDefinition(
                id = RoyalFontId.MONTSERRAT,
                name = "Montserrat",
                language = RoyalFontLanguage.ENGLISH,
                isRoyalPreferred = true,
                family = RoyalFontFamilies.montserrat
            )
        )

    fun initialize(context: Context) {
        appContext = context.applicationContext
        loadSavedSettings()
    }

    fun setArabicFont(
        fontId: RoyalFontId
    ): Boolean {

        val font = getFont(fontId)
            ?: return false

        if (
            font.language !=
            RoyalFontLanguage.ARABIC
        ) {
            return false
        }

        _arabicFont.value = fontId

        save(
            key = KEY_ARABIC_FONT,
            value = fontId.name
        )

        return true
    }

    fun setEnglishFont(
        fontId: RoyalFontId
    ): Boolean {

        val font = getFont(fontId)
            ?: return false

        if (
            font.language !=
            RoyalFontLanguage.ENGLISH
        ) {
            return false
        }

        _englishFont.value = fontId

        save(
            key = KEY_ENGLISH_FONT,
            value = fontId.name
        )

        return true
    }

    fun setTextFont(
        textId: String,
        fontId: RoyalFontId
    ): Boolean {

        if (textId.isBlank()) {
            return false
        }

        if (getFont(fontId) == null) {
            return false
        }

        _textFonts.value =
            _textFonts.value +
                (textId to fontId)

        save(
            key = KEY_TEXT_PREFIX + textId,
            value = fontId.name
        )

        return true
    }

    fun clearTextFont(
        textId: String
    ): Boolean {

        if (textId.isBlank()) {
            return false
        }

        _textFonts.value =
            _textFonts.value - textId

        remove(
            key = KEY_TEXT_PREFIX + textId
        )

        return true
    }

    fun getFont(
        fontId: RoyalFontId
    ): RoyalFontDefinition? {
        return fonts.firstOrNull {
            it.id == fontId
        }
    }

    fun getArabicFonts():
        List<RoyalFontDefinition> {
        return fonts.filter {
            it.language ==
                RoyalFontLanguage.ARABIC
        }
    }

    fun getEnglishFonts():
        List<RoyalFontDefinition> {
        return fonts.filter {
            it.language ==
                RoyalFontLanguage.ENGLISH
        }
    }

    fun getRoyalPreferredFonts():
        List<RoyalFontDefinition> {
        return fonts.filter {
            it.isRoyalPreferred
        }
    }

    fun getArabicFontFamily(): FontFamily {
        return getFont(
            _arabicFont.value
        )?.family
            ?: RoyalFontFamilies.notoNaskhArabic
    }

    fun getEnglishFontFamily(): FontFamily {
        return getFont(
            _englishFont.value
        )?.family
            ?: RoyalFontFamilies.notoSans
    }

    fun getTextFontId(
        textId: String,
        language: RoyalFontLanguage
    ): RoyalFontId {

        return _textFonts.value[textId]
            ?: when (language) {
                RoyalFontLanguage.ARABIC ->
                    _arabicFont.value

                RoyalFontLanguage.ENGLISH ->
                    _englishFont.value
            }
    }

    fun getTextFontFamily(
        textId: String,
        language: RoyalFontLanguage
    ): FontFamily {

        val fontId =
            getTextFontId(
                textId = textId,
                language = language
            )

        return getFont(fontId)?.family
            ?: when (language) {
                RoyalFontLanguage.ARABIC ->
                    RoyalFontFamilies.notoNaskhArabic

                RoyalFontLanguage.ENGLISH ->
                    RoyalFontFamilies.notoSans
            }
    }

    fun resetToDefaults() {

        _arabicFont.value =
            RoyalFontId.NOTO_NASKH_ARABIC

        _englishFont.value =
            RoyalFontId.NOTO_SANS

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
            )

        _englishFont.value =
            parseFont(
                preferences.getString(
                    KEY_ENGLISH_FONT,
                    null
                ),
                RoyalFontId.NOTO_SANS
            )

        val savedTextFonts =
            mutableMapOf<String, RoyalFontId>()

        preferences.all.forEach { entry ->

            if (
                !entry.key.startsWith(
                    KEY_TEXT_PREFIX
                )
            ) {
                return@forEach
            }

            val textId =
                entry.key.removePrefix(
                    KEY_TEXT_PREFIX
                )

            val fontId =
                parseFont(
                    entry.value as? String,
                    null
                )

            if (
                textId.isNotBlank() &&
                fontId != null
            ) {
                savedTextFonts[textId] =
                    fontId
            }
        }

        _textFonts.value =
            savedTextFonts
    }

    private fun parseFont(
        value: String?,
        fallback: RoyalFontId?
    ): RoyalFontId {

        if (value.isNullOrBlank()) {
            return fallback
                ?: RoyalFontId.NOTO_NASKH_ARABIC
        }

        return runCatching {
            RoyalFontId.valueOf(value)
        }.getOrDefault(
            fallback
                ?: RoyalFontId.NOTO_NASKH_ARABIC
        )
    }

    private fun save(
        key: String,
        value: String
    ) {

        appContext
            ?.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            ?.edit()
            ?.putString(
                key,
                value
            )
            ?.apply()
    }

    private fun remove(
        key: String
    ) {

        appContext
            ?.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            ?.edit()
            ?.remove(key)
            ?.apply()
    }
}
