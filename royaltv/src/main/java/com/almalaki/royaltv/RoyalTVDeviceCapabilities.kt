
package com.almalaki.royaltv

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

/**
 * أنواع الأجهزة التي يمكن أن تعمل مع ROYAL TV.
 */
enum class RoyalTVDeviceType {
    PHONE,
    TABLET,
    ANDROID_TV,
    TV_BOX,
    ANDROID_RECEIVER,
    UNKNOWN
}

/**
 * الإمكانات المكتشفة للجهاز الحالي.
 */
data class RoyalTVDeviceCapabilities(
    val deviceType: RoyalTVDeviceType,
    val supportsDisplay: Boolean,
    val supportsController: Boolean,
    val isAndroidTV: Boolean,
    val hasBuiltInDisplay: Boolean,
    val supportsLocalControl: Boolean
)

/**
 * اكتشاف نوع الجهاز وإمكاناته محليًا.
 *
 * لا ينشئ اتصالًا بالشاشات ولا يحتاج إلى الإنترنت.
 */
object RoyalTVDeviceCapabilitiesDetector {

    fun detect(context: Context): RoyalTVDeviceCapabilities {
        val appContext = context.applicationContext
        val packageManager = appContext.packageManager
        val configuration = appContext.resources.configuration

        val isAndroidTV = packageManager.hasSystemFeature(
            PackageManager.FEATURE_LEANBACK
        )

        val screenSize = configuration.screenLayout and
            Configuration.SCREENLAYOUT_SIZE_MASK

        val isTablet = !isAndroidTV &&
            screenSize >= Configuration.SCREENLAYOUT_SIZE_LARGE

        val deviceType = when {
            isAndroidTV -> RoyalTVDeviceType.ANDROID_TV
            isTablet -> RoyalTVDeviceType.TABLET
            else -> RoyalTVDeviceType.PHONE
        }

        val supportsDisplay = deviceType ==
            RoyalTVDeviceType.ANDROID_TV

        return RoyalTVDeviceCapabilities(
            deviceType = deviceType,
            supportsDisplay = supportsDisplay,
            supportsController = true,
            isAndroidTV = isAndroidTV,
            hasBuiltInDisplay = true,
            supportsLocalControl = true
        )
    }

    fun supportsDisplay(context: Context): Boolean =
        detect(context).supportsDisplay

    fun supportsController(context: Context): Boolean =
        detect(context).supportsController

    fun isAndroidTV(context: Context): Boolean =
        detect(context).isAndroidTV

    fun isPhone(context: Context): Boolean =
        detect(context).deviceType == RoyalTVDeviceType.PHONE

    fun isTablet(context: Context): Boolean =
        detect(context).deviceType == RoyalTVDeviceType.TABLET

    fun getDeviceType(context: Context): RoyalTVDeviceType =
        detect(context).deviceType
}
