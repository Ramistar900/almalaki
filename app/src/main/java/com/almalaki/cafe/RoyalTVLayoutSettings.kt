package com.almalaki.cafe

import android.content.Context

/*
 * ============================================================
 * ROYAL TV — CENTRAL LAYOUT SETTINGS
 * ============================================================
 *
 * هذا الملف هو المصدر المركزي لإعدادات تخطيط Royal TV.
 *
 * الوظائف الحالية:
 *
 * 👑 الشعار:
 * - الحجم
 * - الموقع الأفقي
 * - الموقع العمودي
 *
 * 📰 الشريط الإخباري:
 * - الحجم
 *
 * 🕐 الوقت:
 * - الحجم
 *
 * 📅 التاريخ:
 * - الحجم
 * - إظهار / إخفاء
 *
 * 🔴 مباشر:
 * - الحجم
 * - الموقع الأفقي
 * - الموقع العمودي
 *
 * 💾 جميع القيم محفوظة في SharedPreferences واحد
 * خاص بشاشة Royal TV.
 *
 * ============================================================
 *
 * هذا الملف لا يرسم أي عنصر على الشاشة.
 *
 * ولا يغير:
 * - حركة الشعار
 * - طبقات PNG
 * - الهوية
 * - النصوص
 *
 * وظيفته فقط:
 *
 * "أين؟ وكم الحجم؟ وهل يظهر العنصر؟"
 *
 * ============================================================
 */

private const val ROYAL_TV_LAYOUT_PREFS =
    "royal_tv_layout_settings"

/*
 * ============================================================
 * KEYS
 * ============================================================
 */

private const val KEY_LOGO_SIZE_PERCENT =
    "logo_size_percent"

private const val KEY_LOGO_OFFSET_X_PERCENT =
    "logo_offset_x_percent"

private const val KEY_LOGO_OFFSET_Y_PERCENT =
    "logo_offset_y_percent"

private const val KEY_TICKER_SIZE_PERCENT =
    "ticker_size_percent"

private const val KEY_TIME_SIZE_PERCENT =
    "time_size_percent"

private const val KEY_DATE_SIZE_PERCENT =
    "date_size_percent"

private const val KEY_SHOW_DATE =
    "show_date"

private const val KEY_LIVE_SIZE_PERCENT =
    "live_size_percent"

private const val KEY_LIVE_OFFSET_X_PERCENT =
    "live_offset_x_percent"

private const val KEY_LIVE_OFFSET_Y_PERCENT =
    "live_offset_y_percent"

/*
 * ============================================================
 * DEFAULT VALUES
 * ============================================================
 *
 * هذه القيم تحافظ على التصميم الحالي.
 *
 * الحجم الأساسي = 100%
 *
 * التاريخ:
 * ظاهر افتراضيًا.
 */

const val ROYAL_TV_DEFAULT_LOGO_SIZE_PERCENT =
    100

const val ROYAL_TV_DEFAULT_TICKER_SIZE_PERCENT =
    100

const val ROYAL_TV_DEFAULT_TIME_SIZE_PERCENT =
    100

const val ROYAL_TV_DEFAULT_DATE_SIZE_PERCENT =
    100

const val ROYAL_TV_DEFAULT_LIVE_SIZE_PERCENT =
    100

const val ROYAL_TV_DEFAULT_LOGO_OFFSET_X_PERCENT =
    0

const val ROYAL_TV_DEFAULT_LOGO_OFFSET_Y_PERCENT =
    0

const val ROYAL_TV_DEFAULT_LIVE_OFFSET_X_PERCENT =
    0

const val ROYAL_TV_DEFAULT_LIVE_OFFSET_Y_PERCENT =
    0

/*
 * ============================================================
 * LIMITS
 * ============================================================
 */

const val ROYAL_TV_MIN_SIZE_PERCENT =
    60

const val ROYAL_TV_MAX_SIZE_PERCENT =
    160

const val ROYAL_TV_SIZE_STEP_PERCENT =
    5

/*
 * موقع العنصر:
 *
 * -100 = أقصى جهة سالبة
 *  0   = الوضع الافتراضي
 * +100 = أقصى جهة موجبة
 */

const val ROYAL_TV_MIN_OFFSET_PERCENT =
    -100

const val ROYAL_TV_MAX_OFFSET_PERCENT =
    100

const val ROYAL_TV_OFFSET_STEP_PERCENT =
    5

/*
 * ============================================================
 * DATA MODEL
 * ============================================================
 */

data class RoyalTVLayoutSettings(

    val logoSizePercent: Int =
        ROYAL_TV_DEFAULT_LOGO_SIZE_PERCENT,

    val logoOffsetXPercent: Int =
        ROYAL_TV_DEFAULT_LOGO_OFFSET_X_PERCENT,

    val logoOffsetYPercent: Int =
        ROYAL_TV_DEFAULT_LOGO_OFFSET_Y_PERCENT,

    val tickerSizePercent: Int =
        ROYAL_TV_DEFAULT_TICKER_SIZE_PERCENT,

    val timeSizePercent: Int =
        ROYAL_TV_DEFAULT_TIME_SIZE_PERCENT,

    val dateSizePercent: Int =
        ROYAL_TV_DEFAULT_DATE_SIZE_PERCENT,

    /*
     * هل التاريخ ظاهر على شاشة Royal TV؟
     *
     * true  = ظاهر
     * false = مخفي
     */
    val showDate: Boolean = true,

    val liveSizePercent: Int =
        ROYAL_TV_DEFAULT_LIVE_SIZE_PERCENT,

    val liveOffsetXPercent: Int =
        ROYAL_TV_DEFAULT_LIVE_OFFSET_X_PERCENT,

    val liveOffsetYPercent: Int =
        ROYAL_TV_DEFAULT_LIVE_OFFSET_Y_PERCENT
)

/*
 * ============================================================
 * NORMALIZATION
 * ============================================================
 */

/**
 * التأكد من أن قيمة الحجم
 * تقع داخل المجال المسموح.
 */
fun clampRoyalTVSizePercent(
    percent: Int
): Int {
    return percent.coerceIn(
        ROYAL_TV_MIN_SIZE_PERCENT,
        ROYAL_TV_MAX_SIZE_PERCENT
    )
}

/**
 * التأكد من أن موقع العنصر
 * يقع داخل المجال المسموح.
 */
fun clampRoyalTVOffsetPercent(
    percent: Int
): Int {
    return percent.coerceIn(
        ROYAL_TV_MIN_OFFSET_PERCENT,
        ROYAL_TV_MAX_OFFSET_PERCENT
    )
}

/**
 * زيادة أو إنقاص الحجم بمقدار 5%.
 */
fun changeRoyalTVSizePercent(
    currentPercent: Int,
    increase: Boolean
): Int {

    val current =
        clampRoyalTVSizePercent(
            currentPercent
        )

    return if (increase) {
        (
            current +
                ROYAL_TV_SIZE_STEP_PERCENT
            ).coerceAtMost(
                ROYAL_TV_MAX_SIZE_PERCENT
            )
    } else {
        (
            current -
                ROYAL_TV_SIZE_STEP_PERCENT
            ).coerceAtLeast(
                ROYAL_TV_MIN_SIZE_PERCENT
            )
    }
}

/**
 * تحريك العنصر أفقيًا بمقدار 5%.
 */
fun changeRoyalTVOffsetXPercent(
    currentPercent: Int,
    increase: Boolean
): Int {

    val current =
        clampRoyalTVOffsetPercent(
            currentPercent
        )

    return if (increase) {
        (
            current +
                ROYAL_TV_OFFSET_STEP_PERCENT
            ).coerceAtMost(
                ROYAL_TV_MAX_OFFSET_PERCENT
            )
    } else {
        (
            current -
                ROYAL_TV_OFFSET_STEP_PERCENT
            ).coerceAtLeast(
                ROYAL_TV_MIN_OFFSET_PERCENT
            )
    }
}

/**
 * تحريك العنصر عموديًا بمقدار 5%.
 */
fun changeRoyalTVOffsetYPercent(
    currentPercent: Int,
    increase: Boolean
): Int {

    val current =
        clampRoyalTVOffsetPercent(
            currentPercent
        )

    return if (increase) {
        (
            current +
                ROYAL_TV_OFFSET_STEP_PERCENT
            ).coerceAtMost(
                ROYAL_TV_MAX_OFFSET_PERCENT
            )
    } else {
        (
            current -
                ROYAL_TV_OFFSET_STEP_PERCENT
            ).coerceAtLeast(
                ROYAL_TV_MIN_OFFSET_PERCENT
            )
        }
}

/*
 * ============================================================
 * PREFERENCES
 * ============================================================
 */

private fun royalTVLayoutPreferences(
    context: Context
) =
    context.getSharedPreferences(
        ROYAL_TV_LAYOUT_PREFS,
        Context.MODE_PRIVATE
    )

/*
 * ============================================================
 * LOAD
 * ============================================================
 */

/**
 * تحميل جميع إعدادات تخطيط Royal TV.
 *
 * إذا لم توجد إعدادات محفوظة،
 * يتم استخدام القيم الافتراضية.
 */
fun loadRoyalTVLayoutSettings(
    context: Context
): RoyalTVLayoutSettings {

    val prefs =
        royalTVLayoutPreferences(context)

    return RoyalTVLayoutSettings(

        logoSizePercent =
            clampRoyalTVSizePercent(
                prefs.getInt(
                    KEY_LOGO_SIZE_PERCENT,
                    ROYAL_TV_DEFAULT_LOGO_SIZE_PERCENT
                )
            ),

        logoOffsetXPercent =
            clampRoyalTVOffsetPercent(
                prefs.getInt(
                    KEY_LOGO_OFFSET_X_PERCENT,
                    ROYAL_TV_DEFAULT_LOGO_OFFSET_X_PERCENT
                )
            ),

        logoOffsetYPercent =
            clampRoyalTVOffsetPercent(
                prefs.getInt(
                    KEY_LOGO_OFFSET_Y_PERCENT,
                    ROYAL_TV_DEFAULT_LOGO_OFFSET_Y_PERCENT
                )
            ),

        tickerSizePercent =
            clampRoyalTVSizePercent(
                prefs.getInt(
                    KEY_TICKER_SIZE_PERCENT,
                    ROYAL_TV_DEFAULT_TICKER_SIZE_PERCENT
                )
            ),

        timeSizePercent =
            clampRoyalTVSizePercent(
                prefs.getInt(
                    KEY_TIME_SIZE_PERCENT,
                    ROYAL_TV_DEFAULT_TIME_SIZE_PERCENT
                )
            ),

        dateSizePercent =
            clampRoyalTVSizePercent(
                prefs.getInt(
                    KEY_DATE_SIZE_PERCENT,
                    ROYAL_TV_DEFAULT_DATE_SIZE_PERCENT
                )
            ),

        showDate =
            prefs.getBoolean(
                KEY_SHOW_DATE,
                true
            ),

        liveSizePercent =
            clampRoyalTVSizePercent(
                prefs.getInt(
                    KEY_LIVE_SIZE_PERCENT,
                    ROYAL_TV_DEFAULT_LIVE_SIZE_PERCENT
                )
            ),

        liveOffsetXPercent =
            clampRoyalTVOffsetPercent(
                prefs.getInt(
                    KEY_LIVE_OFFSET_X_PERCENT,
                    ROYAL_TV_DEFAULT_LIVE_OFFSET_X_PERCENT
                )
            ),

        liveOffsetYPercent =
            clampRoyalTVOffsetPercent(
                prefs.getInt(
                    KEY_LIVE_OFFSET_Y_PERCENT,
                    ROYAL_TV_DEFAULT_LIVE_OFFSET_Y_PERCENT
                )
            )
    )
}

/*
 * ============================================================
 * SAVE
 * ============================================================
 */

/**
 * حفظ جميع إعدادات تخطيط Royal TV دفعة واحدة.
 */
fun saveRoyalTVLayoutSettings(
    context: Context,
    settings: RoyalTVLayoutSettings
) {

    val safeSettings =
        settings.copy(

            logoSizePercent =
                clampRoyalTVSizePercent(
                    settings.logoSizePercent
                ),

            logoOffsetXPercent =
                clampRoyalTVOffsetPercent(
                    settings.logoOffsetXPercent
                ),

            logoOffsetYPercent =
                clampRoyalTVOffsetPercent(
                    settings.logoOffsetYPercent
                ),

            tickerSizePercent =
                clampRoyalTVSizePercent(
                    settings.tickerSizePercent
                ),

            timeSizePercent =
                clampRoyalTVSizePercent(
                    settings.timeSizePercent
                ),

            dateSizePercent =
                clampRoyalTVSizePercent(
                    settings.dateSizePercent
                ),

            showDate =
                settings.showDate,

            liveSizePercent =
                clampRoyalTVSizePercent(
                    settings.liveSizePercent
                ),

            liveOffsetXPercent =
                clampRoyalTVOffsetPercent(
                    settings.liveOffsetXPercent
                ),

            liveOffsetYPercent =
                clampRoyalTVOffsetPercent(
                    settings.liveOffsetYPercent
                )
        )

    royalTVLayoutPreferences(context)
        .edit()

        .putInt(
            KEY_LOGO_SIZE_PERCENT,
            safeSettings.logoSizePercent
        )

        .putInt(
            KEY_LOGO_OFFSET_X_PERCENT,
            safeSettings.logoOffsetXPercent
        )

        .putInt(
            KEY_LOGO_OFFSET_Y_PERCENT,
            safeSettings.logoOffsetYPercent
        )

        .putInt(
            KEY_TICKER_SIZE_PERCENT,
            safeSettings.tickerSizePercent
        )

        .putInt(
            KEY_TIME_SIZE_PERCENT,
            safeSettings.timeSizePercent
        )

        .putInt(
            KEY_DATE_SIZE_PERCENT,
            safeSettings.dateSizePercent
        )

        .putBoolean(
            KEY_SHOW_DATE,
            safeSettings.showDate
        )

        .putInt(
            KEY_LIVE_SIZE_PERCENT,
            safeSettings.liveSizePercent
        )

        .putInt(
            KEY_LIVE_OFFSET_X_PERCENT,
            safeSettings.liveOffsetXPercent
        )

        .putInt(
            KEY_LIVE_OFFSET_Y_PERCENT,
            safeSettings.liveOffsetYPercent
        )

        .apply()
}

/*
 * ============================================================
 * RESET
 * ============================================================
 */

/**
 * استعادة جميع إعدادات التخطيط
 * إلى الوضع الافتراضي.
 *
 * التاريخ سيعود إلى:
 * ظاهر.
 *
 * لا يحذف:
 * - اللوجو
 * - الصور
 * - النصوص
 * - الهوية
 * - إعدادات أخرى خارج هذا الملف.
 */
fun resetRoyalTVLayoutSettings(
    context: Context
) {

    saveRoyalTVLayoutSettings(
        context = context,
        settings =
            RoyalTVLayoutSettings()
    )
}

/*
 * ============================================================
 * INDIVIDUAL SAVE HELPERS
 * ============================================================
 */

/**
 * حفظ حجم وموقع اللوجو.
 */
fun saveRoyalTVLogoLayout(
    context: Context,
    sizePercent: Int,
    offsetXPercent: Int,
    offsetYPercent: Int
) {

    val current =
        loadRoyalTVLayoutSettings(context)

    saveRoyalTVLayoutSettings(
        context = context,
        settings =
            current.copy(
                logoSizePercent =
                    clampRoyalTVSizePercent(
                        sizePercent
                    ),

                logoOffsetXPercent =
                    clampRoyalTVOffsetPercent(
                        offsetXPercent
                    ),

                logoOffsetYPercent =
                    clampRoyalTVOffsetPercent(
                        offsetYPercent
                    )
            )
    )
}

/**
 * حفظ حجم الشريط فقط.
 */
fun saveRoyalTVTickerLayout(
    context: Context,
    sizePercent: Int
) {

    val current =
        loadRoyalTVLayoutSettings(context)

    saveRoyalTVLayoutSettings(
        context = context,
        settings =
            current.copy(
                tickerSizePercent =
                    clampRoyalTVSizePercent(
                        sizePercent
                    )
            )
    )
}

/**
 * حفظ أحجام الوقت والتاريخ.
 */
fun saveRoyalTVClockLayout(
    context: Context,
    timeSizePercent: Int,
    dateSizePercent: Int
) {

    val current =
        loadRoyalTVLayoutSettings(context)

    saveRoyalTVLayoutSettings(
        context = context,
        settings =
            current.copy(
                timeSizePercent =
                    clampRoyalTVSizePercent(
                        timeSizePercent
                    ),

                dateSizePercent =
                    clampRoyalTVSizePercent(
                        dateSizePercent
                    )
            )
    )
}

/**
 * حفظ حجم وموقع مؤشر مباشر.
 */
fun saveRoyalTVLiveLayout(
    context: Context,
    sizePercent: Int,
    offsetXPercent: Int,
    offsetYPercent: Int
) {

    val current =
        loadRoyalTVLayoutSettings(context)

    saveRoyalTVLayoutSettings(
        context = context,
        settings =
            current.copy(
                liveSizePercent =
                    clampRoyalTVSizePercent(
                        sizePercent
                    ),

                liveOffsetXPercent =
                    clampRoyalTVOffsetPercent(
                        offsetXPercent
                    ),

                liveOffsetYPercent =
                    clampRoyalTVOffsetPercent(
                        offsetYPercent
                    )
            )
    )
}
