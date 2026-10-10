
package com.almalaki.royaltv

import android.content.Context

/**
 * مدير الأجهزة والشاشات المتعددة في ROYAL TV.
 *
 * يدير السجل المحلي والأدوار والإحصاءات.
 * لا ينشئ اتصالًا شبكيًا بين الأجهزة بنفسه.
 */
object RoyalTVMultiScreenManager {

    /**
     * تهيئة سجل الأجهزة المحلي.
     */
    fun initialize(context: Context) {
        RoyalTVDeviceRegistry.initialize(
            context.applicationContext
        )
    }

    /**
     * تسجيل الجهاز الحالي وفق الدور المحفوظ.
     */
    fun registerCurrentDevice(
        context: Context
    ): RoyalTVDevice {
        val appContext = context.applicationContext
        initialize(appContext)

        val device = RoyalTVDevice(
            deviceId = RoyalTVDeviceIdentity.getDeviceId(
                appContext
            ),
            deviceName = RoyalTVDeviceIdentity.getDeviceName(
                appContext
            ),
            role = RoyalTVDeviceRoleManager.getRole(
                appContext
            ),
            isOnline = false
        )

        RoyalTVDeviceRegistry.registerDevice(device)
        return device
    }

    /**
     * تسجيل الجهاز الحالي كشاشة عرض.
     */
    fun registerDisplay(
        context: Context
    ): RoyalTVDevice {
        return registerWithRole(
            context,
            RoyalTVDeviceRole.DISPLAY
        )
    }

    /**
     * تسجيل الجهاز الحالي كجهاز تحكم.
     */
    fun registerController(
        context: Context
    ): RoyalTVDevice {
        return registerWithRole(
            context,
            RoyalTVDeviceRole.CONTROLLER
        )
    }

    /**
     * تسجيل الجهاز الحالي كشاشة وجهاز تحكم معًا.
     */
    fun registerControllerAndDisplay(
        context: Context
    ): RoyalTVDevice {
        return registerWithRole(
            context,
            RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY
        )
    }

    private fun registerWithRole(
        context: Context,
        role: RoyalTVDeviceRole
    ): RoyalTVDevice {
        val appContext = context.applicationContext
        initialize(appContext)

        RoyalTVDeviceRoleManager.setRole(
            appContext,
            role
        )

        val device = RoyalTVDevice(
            deviceId = RoyalTVDeviceIdentity.getDeviceId(
                appContext
            ),
            deviceName = RoyalTVDeviceIdentity.getDeviceName(
                appContext
            ),
            role = role,
            isOnline = false
        )

        RoyalTVDeviceRegistry.registerDevice(device)
        return device
    }

    /**
     * تحديث حالة جهاز داخل السجل المحلي.
     *
     * هذه الحالة لا تثبت وحدها وجود اتصال حقيقي.
     */
    fun setDeviceOnline(
        context: Context,
        deviceId: String,
        online: Boolean
    ) {
        initialize(context)
        RoyalTVDeviceRegistry.updateOnlineState(
            deviceId = deviceId,
            isOnline = online
        )
    }

    fun getAllDevices(
        context: Context
    ): List<RoyalTVDevice> {
        initialize(context)
        return RoyalTVDeviceRegistry.getAllDevices()
    }

    fun getOnlineDevices(
        context: Context
    ): List<RoyalTVDevice> {
        initialize(context)
        return RoyalTVDeviceRegistry.getOnlineDevices()
    }

    fun getOfflineDevices(
        context: Context
    ): List<RoyalTVDevice> {
        initialize(context)
        return RoyalTVDeviceRegistry.getOfflineDevices()
    }

    fun getDisplayDevices(
        context: Context
    ): List<RoyalTVDevice> {
        return getAllDevices(context).filter {
            it.role == RoyalTVDeviceRole.DISPLAY ||
                it.role == RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY
        }
    }

    fun getControllerDevices(
        context: Context
    ): List<RoyalTVDevice> {
        return getAllDevices(context).filter {
            it.role == RoyalTVDeviceRole.CONTROLLER ||
                it.role == RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY
        }
    }

    fun getDevice(
        context: Context,
        deviceId: String
    ): RoyalTVDevice? {
        initialize(context)
        return RoyalTVDeviceRegistry.getDevice(deviceId)
    }

    fun removeDevice(
        context: Context,
        deviceId: String
    ) {
        initialize(context)
        RoyalTVDeviceRegistry.removeDevice(deviceId)
    }

    fun clearAllDevices(context: Context) {
        initialize(context)
        RoyalTVDeviceRegistry.clearRegistry()
    }

    fun getDisplayCount(context: Context): Int =
        getDisplayDevices(context).size

    fun getControllerCount(context: Context): Int =
        getControllerDevices(context).size

    fun getOnlineDisplayCount(context: Context): Int =
        getDisplayDevices(context).count { it.isOnline }

    fun getOnlineControllerCount(context: Context): Int =
        getControllerDevices(context).count { it.isOnline }

    fun isDisplayAvailable(
        context: Context,
        deviceId: String
    ): Boolean {
        val device = getDevice(context, deviceId)
            ?: return false

        return device.role == RoyalTVDeviceRole.DISPLAY ||
            device.role == RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY
    }

    fun isControllerAvailable(
        context: Context,
        deviceId: String
    ): Boolean {
        val device = getDevice(context, deviceId)
            ?: return false

        return device.role == RoyalTVDeviceRole.CONTROLLER ||
            device.role == RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY
    }
}
