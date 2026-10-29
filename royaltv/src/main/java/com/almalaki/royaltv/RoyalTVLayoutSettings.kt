package com.almalaki.royaltv

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Settings received from the owner's layout editor.
 * Values are clamped before being stored or exposed to the UI.
 */
data class RoyalTVLayoutState(
    val logoSizePercent: Int = 100,
    val logoOffsetXPercent: Int = 0,
    val logoOffsetYPercent: Int = 0,
    val tickerSizePercent: Int = 100,
    val tickerOffsetXPercent: Int = 0,
    val tickerOffsetYPercent: Int = 0,
    val timeSizePercent: Int = 100,
    val timeOffsetXPercent: Int = 0,
    val timeOffsetYPercent: Int = 0,
    val dateSizePercent: Int = 100,
    val dateOffsetXPercent: Int = 0,
    val dateOffsetYPercent: Int = 0,
    val showDate: Boolean = true,
    val showTicker: Boolean = true,
    val liveSizePercent: Int = 100,
    val liveOffsetXPercent: Int = 0,
    val liveOffsetYPercent: Int = 0
)

object RoyalTVLayoutSettings {
    private const val PREFS = "royal_tv_received_layout"
    private const val KEY_JSON = "layout_json"
    private val mutableState = MutableStateFlow(RoyalTVLayoutState())
    val state: StateFlow<RoyalTVLayoutState> = mutableState.asStateFlow()

    @Volatile private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val saved = prefs.getString(KEY_JSON, null)
            if (!saved.isNullOrBlank()) {
                runCatching { parse(JSONObject(saved)) }
                    .onSuccess { mutableState.value = it }
            }
            initialized = true
        }
    }

    fun applyJson(context: Context, rawJson: String): Boolean {
        val parsed = runCatching { parse(JSONObject(rawJson)) }.getOrNull() ?: return false
        val normalizedJson = JSONObject()
            .put("logoSizePercent", parsed.logoSizePercent)
            .put("logoOffsetXPercent", parsed.logoOffsetXPercent)
            .put("logoOffsetYPercent", parsed.logoOffsetYPercent)
            .put("tickerSizePercent", parsed.tickerSizePercent)
            .put("tickerOffsetXPercent", parsed.tickerOffsetXPercent)
            .put("tickerOffsetYPercent", parsed.tickerOffsetYPercent)
            .put("timeSizePercent", parsed.timeSizePercent)
            .put("timeOffsetXPercent", parsed.timeOffsetXPercent)
            .put("timeOffsetYPercent", parsed.timeOffsetYPercent)
            .put("dateSizePercent", parsed.dateSizePercent)
            .put("dateOffsetXPercent", parsed.dateOffsetXPercent)
            .put("dateOffsetYPercent", parsed.dateOffsetYPercent)
            .put("showDate", parsed.showDate)
            .put("showTicker", parsed.showTicker)
            .put("liveSizePercent", parsed.liveSizePercent)
            .put("liveOffsetXPercent", parsed.liveOffsetXPercent)
            .put("liveOffsetYPercent", parsed.liveOffsetYPercent)
            .toString()

        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_JSON, normalizedJson).apply()
        initialize(context)
        mutableState.value = parsed
        return true
    }

    private fun parse(json: JSONObject) = RoyalTVLayoutState(
        logoSizePercent = size(json.optInt("logoSizePercent", 100)),
        logoOffsetXPercent = offset(json.optInt("logoOffsetXPercent", 0)),
        logoOffsetYPercent = offset(json.optInt("logoOffsetYPercent", 0)),
        tickerSizePercent = size(json.optInt("tickerSizePercent", 100)),
        tickerOffsetXPercent = offset(json.optInt("tickerOffsetXPercent", 0)),
        tickerOffsetYPercent = offset(json.optInt("tickerOffsetYPercent", 0)),
        timeSizePercent = size(json.optInt("timeSizePercent", 100)),
        timeOffsetXPercent = offset(json.optInt("timeOffsetXPercent", 0)),
        timeOffsetYPercent = offset(json.optInt("timeOffsetYPercent", 0)),
        dateSizePercent = size(json.optInt("dateSizePercent", 100)),
        dateOffsetXPercent = offset(json.optInt("dateOffsetXPercent", 0)),
        dateOffsetYPercent = offset(json.optInt("dateOffsetYPercent", 0)),
        showDate = json.optBoolean("showDate", true),
        showTicker = json.optBoolean("showTicker", true),
        liveSizePercent = size(json.optInt("liveSizePercent", 100)),
        liveOffsetXPercent = offset(json.optInt("liveOffsetXPercent", 0)),
        liveOffsetYPercent = offset(json.optInt("liveOffsetYPercent", 0))
    )

    private fun size(value: Int) = value.coerceIn(60, 160)
    private fun offset(value: Int) = value.coerceIn(-100, 100)
}
