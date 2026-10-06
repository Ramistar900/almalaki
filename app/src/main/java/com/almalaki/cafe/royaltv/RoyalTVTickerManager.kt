package com.almalaki.cafe.royaltv

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * محرك شريط الأخبار الخاص بـ ROYAL TV.
 *
 * المسؤوليات:
 *
 * 1. حفظ نص الخبر محليًا.
 * 2. إبقاء الخبر متاحًا بدون إنترنت.
 * 3. تحديث الخبر من الهاتف / التابلت / الشاشة.
 * 4. توفير حالة واحدة لجميع واجهات ROYAL TV.
 *
 * الواجهة نفسها لا توجد هنا.
 * هذا الملف هو Core فقط.
 */
object RoyalTVTickerManager {

    private const val PREFS_NAME =
        "royal_tv_ticker"

    private const val KEY_TEXT =
        "ticker_text"

    private const val KEY_ENABLED =
        "ticker_enabled"

    private const val DEFAULT_TEXT =
        "مرحبًا بكم في ROYAL TV"

    private val _text =
        MutableStateFlow(DEFAULT_TEXT)

    val text:
        StateFlow<String> =
        _text.asStateFlow()

    private val _enabled =
        MutableStateFlow(true)

    val enabled:
        StateFlow<Boolean> =
        _enabled.asStateFlow()

    private var initialized =
        false

    /**
     * تهيئة المحرك واستعادة آخر حالة محفوظة.
     *
     * تعمل محليًا بالكامل ولا تحتاج إلى الإنترنت.
     */
    fun initialize(
        context: Context
    ) {

        if (initialized) {
            return
        }

        initialized = true

        val preferences =
            context.applicationContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

        _text.value =
            preferences.getString(
                KEY_TEXT,
                DEFAULT_TEXT
            ) ?: DEFAULT_TEXT

        _enabled.value =
            preferences.getBoolean(
                KEY_ENABLED,
                true
            )
    }

    /**
     * تحديث نص الخبر.
     *
     * النص الجديد يُحفظ فورًا محليًا.
     */
    fun setText(
        context: Context,
        value: String
    ) {

        val cleanText =
            value.trim()

        if (cleanText.isBlank()) {
            return
        }

        _text.value =
            cleanText

        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_TEXT,
                cleanText
            )
            .apply()
    }

    /**
     * تفعيل / تعطيل شريط الأخبار.
     */
    fun setEnabled(
        context: Context,
        enabled: Boolean
    ) {

        _enabled.value =
            enabled

        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                KEY_ENABLED,
                enabled
            )
            .apply()
    }

    /**
     * إعادة الخبر الافتراضي.
     */
    fun reset(
        context: Context
    ) {

        _text.value =
            DEFAULT_TEXT

        _enabled.value =
            true

        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_TEXT,
                DEFAULT_TEXT
            )
            .putBoolean(
                KEY_ENABLED,
                true
            )
            .apply()
    }

    /**
     * الحصول على النص الحالي بدون Flow.
     */
    fun getText(): String {

        return _text.value
    }

    /**
     * معرفة حالة الشريط الحالية.
     */
    fun isEnabled(): Boolean {

        return _enabled.value
    }
}
