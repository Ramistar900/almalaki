package com.almalaki.royaltv

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * مدير شريط الأخبار المستقل في ROYAL TV.
 * يحفظ آخر نص محليًا ويتيح للواجهة مراقبة تحديثاته.
 */
object RoyalTVTickerManager {

    private const val PREFS_NAME = "royal_tv_ticker"
    private const val KEY_TEXT = "ticker_text"

    private const val DEFAULT_TEXT =
        "جودة فاخرة  •  أهلاً بكم في الملكي  •  طعم يستحق التجربة  •"

    private val _text = MutableStateFlow(DEFAULT_TEXT)

    val text: StateFlow<String> = _text.asStateFlow()

    @Volatile
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return

        synchronized(this) {
            if (initialized) return

            val prefs = context.applicationContext
                .getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

            _text.value = prefs.getString(
                KEY_TEXT,
                DEFAULT_TEXT
            )?.takeIf { it.isNotBlank() }
                ?: DEFAULT_TEXT

            initialized = true
        }
    }

    fun setText(context: Context, value: String) {
        val cleanText = value.trim()
        if (cleanText.isBlank()) return

        initialize(context)

        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(KEY_TEXT, cleanText)
            .apply()

        _text.value = cleanText
    }

    fun getText(): String = _text.value
}
