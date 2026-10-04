package com.almalaki.cafe

import android.content.Context
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.view.WindowManager

/*
 * ============================================================
 * إعدادات أجهزة التلفزيون والرسيفرات Android
 * ============================================================
 *
 * هذا الملف مخصص للأجهزة الكبيرة التي تعمل كواجهة تلفزيون:
 *
 * - Android TV
 * - Google TV
 * - Android TV Box
 * - Android Receiver
 * - الشاشات الذكية التي تعمل بنظام Android
 *
 * ولا يجب أن تظهر هذه الإعدادات على:
 *
 * - الهاتف
 * - التابلت
 *
 * ============================================================
 */

/**
 * تحديد ما إذا كان الجهاز مناسبًا لواجهة التلفزيون.
 *
 * نستخدم أكثر من طريقة لأن بعض TV Box والرسيفرات
 * لا تعرّف نفسها رسميًا كـ Android TV.
 */
fun isRoyalTV(context: Context): Boolean {

    val configuration = context.resources.configuration

    val uiModeType =
        configuration.uiMode and Configuration.UI_MODE_TYPE_MASK

    // Android TV الرسمي.
    if (uiModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
        return true
    }

    // أجهزة التلفزيون أو الأجهزة المخصصة للتلفزيون
    // غالبًا تحتوي على هذه الميزات.
    val packageManager = context.packageManager

    val hasLeanback =
        packageManager.hasSystemFeature(
            PackageManager.FEATURE_LEANBACK
        )

    val hasTelevision =
        packageManager.hasSystemFeature(
            "android.software.leanback"
        )

    if (hasLeanback || hasTelevision) {
        return true
    }

    /*
     * بعض TV Box والرسيفرات لا تسجل Leanback بشكل صحيح.
     *
     * في هذه الحالة نتحقق من وجود WindowManager
     * وطريقة تشغيل الجهاز، مع تجنب اعتبار الأجهزة
     * المحمولة تلفزيونًا تلقائيًا.
     */
    val isLargeScreen =
        configuration.screenWidthDp >= 720 ||
            configuration.screenHeightDp >= 720

    val isNotPhoneSized =
        configuration.smallestScreenWidthDp >= 600

    /*
     * الأجهزة ذات الشاشة الكبيرة جدًا قد تكون TV Box
     * أو شاشة Android.
     *
     * لا نستخدم هذا الشرط وحده للأجهزة الصغيرة.
     */
    if (isLargeScreen && isNotPhoneSized) {
        return true
    }

    return false
}

/*
 * ============================================================
 * إعدادات العرض الخاصة بالتلفزيون
 * ============================================================
 */

data class RoyalTVDisplaySettings(
    val timeScale: Float = 1.35f,
    val timeBoxScale: Float = 1.30f,
    val dateScale: Float = 1.30f,
    val tickerScale: Float = 1.35f,
    val tickerBoxScale: Float = 1.25f,
    val bottomLogoScale: Float = 1.35f
)

/*
 * ============================================================
 * مستوى المؤثرات
 * ============================================================
 */

enum class RoyalTVEffectLevel {
    STRONG,
    MEDIUM,
    WEAK,
    OFF
}

data class RoyalTVEffectSettings(
    val shineLevel: RoyalTVEffectLevel =
        RoyalTVEffectLevel.WEAK,

    val focusLevel: RoyalTVEffectLevel =
        RoyalTVEffectLevel.MEDIUM,

    val focusEnabled: Boolean = true
)

/*
 * ============================================================
 * سرعة الشريط الإخباري
 * ============================================================
 */

enum class RoyalTVTickerSpeed {
    SLOW,
    FAST,
    FASTER
}

data class RoyalTVTickerSettings(
    val speed: RoyalTVTickerSpeed =
        RoyalTVTickerSpeed.FAST
)

/*
 * ============================================================
 * أزرار الريموت
 * ============================================================
 */

enum class RoyalTVRemoteAction {
    MOVE_UP,
    MOVE_DOWN,
    MOVE_LEFT,
    MOVE_RIGHT,
    SELECT,
    BACK
}

data class RoyalTVRemoteSettings(
    val upAction: RoyalTVRemoteAction =
        RoyalTVRemoteAction.MOVE_UP,

    val downAction: RoyalTVRemoteAction =
        RoyalTVRemoteAction.MOVE_DOWN,

    val leftAction: RoyalTVRemoteAction =
        RoyalTVRemoteAction.MOVE_LEFT,

    val rightAction: RoyalTVRemoteAction =
        RoyalTVRemoteAction.MOVE_RIGHT,

    val okAction: RoyalTVRemoteAction =
        RoyalTVRemoteAction.SELECT,

    val exitAction: RoyalTVRemoteAction =
        RoyalTVRemoteAction.BACK
)

/*
 * ============================================================
 * تلميحات التنقل
 * ============================================================
 */

data class RoyalTVNavigationSettings(
    val showNavigationHintsFirstTime: Boolean = true
)

/*
 * ============================================================
 * جميع إعدادات التلفزيون
 * ============================================================
 */

data class RoyalTVSettings(
    val display: RoyalTVDisplaySettings =
        RoyalTVDisplaySettings(),

    val effects: RoyalTVEffectSettings =
        RoyalTVEffectSettings(),

    val ticker: RoyalTVTickerSettings =
        RoyalTVTickerSettings(),

    val remote: RoyalTVRemoteSettings =
        RoyalTVRemoteSettings(),

    val navigation: RoyalTVNavigationSettings =
        RoyalTVNavigationSettings()
)

/*
 * ============================================================
 * الإعدادات الافتراضية
 * ============================================================
 */

fun defaultRoyalTVSettings(): RoyalTVSettings {
    return RoyalTVSettings()
}
