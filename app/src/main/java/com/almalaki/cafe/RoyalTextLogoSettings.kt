package com.almalaki.cafe

import android.content.Context

/**
 * إعدادات الشعار النصي المتحرك لهوية ROYAL.
 * تحفظ محليًا على الجهاز وتبقى بعد إغلاق التطبيق.
 */
enum class RoyalTextLogoStyle {
    CLASSIC_CALLIGRAPHY,
    MODERN_SIGNATURE
}

data class RoyalTextLogoConfig(
    val text: String = "Royal Coffee",
    val style: RoyalTextLogoStyle =
        RoyalTextLogoStyle.MODERN_SIGNATURE,
    val fontSizeSp: Int = 27
)

object RoyalTextLogoSettings {

    private const val PREFS_NAME =
        "royal_text_logo_settings"

    private const val KEY_TEXT =
        "logo_text"

    private const val KEY_STYLE =
        "logo_style"

    private const val KEY_FONT_SIZE =
        "logo_font_size"

    private const val MIN_FONT_SIZE = 16
    private const val MAX_FONT_SIZE = 56

    /**
     * قراءة آخر إعدادات محفوظة.
     */
    fun load(context: Context): RoyalTextLogoConfig {
        val prefs = context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val savedStyle = runCatching {
            RoyalTextLogoStyle.valueOf(
                prefs.getString(
                    KEY_STYLE,
                    RoyalTextLogoStyle.MODERN_SIGNATURE.name
                ) ?: RoyalTextLogoStyle.MODERN_SIGNATURE.name
            )
        }.getOrDefault(
            RoyalTextLogoStyle.MODERN_SIGNATURE
        )

        return RoyalTextLogoConfig(
            text = prefs.getString(
                KEY_TEXT,
                "Royal Coffee"
            )?.takeIf { it.isNotBlank() }
                ?: "Royal Coffee",
            style = savedStyle,
            fontSizeSp = prefs.getInt(
                KEY_FONT_SIZE,
                27
            ).coerceIn(
                MIN_FONT_SIZE,
                MAX_FONT_SIZE
            )
        )
    }

    /**
     * حفظ النص والنمط والحجم.
     */
    fun save(
        context: Context,
        config: RoyalTextLogoConfig
    ) {
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_TEXT,
                config.text.trim()
                    .ifEmpty { "Royal Coffee" }
            )
            .putString(
                KEY_STYLE,
                config.style.name
            )
            .putInt(
                KEY_FONT_SIZE,
                config.fontSizeSp.coerceIn(
                    MIN_FONT_SIZE,
                    MAX_FONT_SIZE
                )
            )
            .apply()
    }
}
