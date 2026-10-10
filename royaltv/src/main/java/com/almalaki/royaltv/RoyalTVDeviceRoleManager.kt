
package com.almalaki.royaltv

import android.content.Context

/**
 * إدارة دور جهاز ROYAL TV محليًا.
 */
object RoyalTVDeviceRoleManager {

    private const val PREFS_NAME = "royal_tv_device_role"
    private const val KEY_ROLE = "device_role"

    /**
     * الحصول على الدور المحفوظ.
     * الدور الافتراضي هو Controller + Display.
     */
    fun getRole(context: Context): RoyalTVDeviceRole {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val savedRole = prefs.getString(KEY_ROLE, null)

        return try {
            if (savedRole.isNullOrBlank()) {
                RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY
            } else {
                RoyalTVDeviceRole.valueOf(savedRole)
            }
        } catch (_: IllegalArgumentException) {
            RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY
        }
    }

    /**
     * حفظ دور الجهاز.
     */
    fun setRole(context: Context, role: RoyalTVDeviceRole) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ROLE, role.name)
            .apply()
    }

    fun canDisplay(context: Context): Boolean =
        when (getRole(context)) {
            RoyalTVDeviceRole.DISPLAY,
            RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY -> true

            RoyalTVDeviceRole.CONTROLLER -> false
        }

    fun canControl(context: Context): Boolean =
        when (getRole(context)) {
            RoyalTVDeviceRole.CONTROLLER,
            RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY -> true

            RoyalTVDeviceRole.DISPLAY -> false
        }

    fun isDisplayOnly(context: Context): Boolean =
        getRole(context) == RoyalTVDeviceRole.DISPLAY

    fun isControllerOnly(context: Context): Boolean =
        getRole(context) == RoyalTVDeviceRole.CONTROLLER

    fun isControllerAndDisplay(context: Context): Boolean =
        getRole(context) == RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY

    /**
     * إعادة الدور إلى الوضع الافتراضي.
     */
    fun resetRole(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ROLE)
            .apply()
    }
}
