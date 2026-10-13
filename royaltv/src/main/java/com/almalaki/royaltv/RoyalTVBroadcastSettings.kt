package com.almalaki.royaltv

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * إعدادات هوية البث الخاصة بتطبيق ROYAL TV المستقل.
 * منفصلة عن إعدادات تطبيق الملكي.
 */
object RoyalTVBroadcastSettings {

    private const val PREFS_NAME =
        "royal_tv_broadcast_settings"

    private const val KEY_TICKER_ENABLED =
        "ticker_enabled"

    private val _tickerEnabled =
        MutableStateFlow(true)

    val tickerEnabled: StateFlow<Boolean> =
        _tickerEnabled.asStateFlow()

    @Volatile
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return

        synchronized(this) {
            if (initialized) return

            val prefs =
                context.applicationContext.getSharedPreferences(
                    PREFS_NAME,
                    Context.MODE_PRIVATE
                )

            _tickerEnabled.value =
                prefs.getBoolean(
                    KEY_TICKER_ENABLED,
                    true
                )

            initialized = true
        }
    }

    fun isTickerEnabled(context: Context): Boolean {
        initialize(context)
        return _tickerEnabled.value
    }

    fun setTickerEnabled(
        context: Context,
        enabled: Boolean
    ) {
        initialize(context)

        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                KEY_TICKER_ENABLED,
                enabled
            )
            .apply()

        _tickerEnabled.value = enabled
    }
}
