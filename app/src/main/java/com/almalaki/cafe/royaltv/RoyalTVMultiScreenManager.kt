package com.almalaki.cafe.royaltv

import android.content.Context

object RoyalTVMultiScreenManager {

    private var initialized = false

    fun initialize(
        context: Context
    ) {
        if (initialized) {
            return
        }

        RoyalTVDeviceRegistry.initialize(
            context
        )

        initialized = true
    }

    fun registerCurrentDevice(
        context: Context
    ): RoyalTVDevice {

        initialize(context)

        val deviceId =
            RoyalTVDeviceIdentity.getDeviceId(
                context
            )

        val deviceName =
            RoyalTVDeviceIdentity.getDeviceName(
                context
            )

        val role =
            RoyalTVDeviceRoleManager.getRole(
                context
            )

        val device =
            RoyalTVDevice(
                deviceId = deviceId,
                deviceName = deviceName,
                role = role,
                isOnline = false
            )

        RoyalTVDeviceRegistry.registerDevice(
            device
        )

        return device
    }

    fun registerDisplay(
        context: Context
    ): RoyalTVDevice {

        initialize(context)

        val deviceId =
            RoyalTVDeviceIdentity.getDeviceId(
                context
            )

        val deviceName =
            RoyalTVDeviceIdentity.getDeviceName(
                context
            )

        val device =
            RoyalTVDevice(
                deviceId = deviceId,
                deviceName = deviceName,
                role = RoyalTVDeviceRole.DISPLAY,
                isOnline = false
            )

        RoyalTVDeviceRegistry.registerDevice(
            device
        )

        return device
    }

    fun registerController(
        context: Context
    ): RoyalTVDevice {

        initialize(context)

        val deviceId =
            RoyalTVDeviceIdentity.getDeviceId(
                context
            )

        val deviceName =
            RoyalTVDeviceIdentity.getDeviceName(
                context
            )

        val device =
            RoyalTVDevice(
                deviceId = deviceId,
                deviceName = deviceName,
                role = RoyalTVDeviceRole.CONTROLLER,
                isOnline = false
            )

        RoyalTVDeviceRegistry.registerDevice(
            device
        )

        return device
    }

    fun registerControllerAndDisplay(
        context: Context
    ): RoyalTVDevice {

        initialize(context)

        val deviceId =
            RoyalTVDeviceIdentity.getDeviceId(
                context
            )

        val deviceName =
            RoyalTVDeviceIdentity.getDeviceName(
                context
            )

        val device =
            RoyalTVDevice(
                deviceId = deviceId,
                deviceName = deviceName,
                role =
                    RoyalTVDeviceRole
                        .CONTROLLER_AND_DISPLAY,
                isOnline = false
            )

        RoyalTVDeviceRegistry.registerDevice(
            device
        )

        return device
    }

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

        return RoyalTVDeviceRegistry
            .getAllDevices()
    }

    fun getOnlineDevices(
        context: Context
    ): List<RoyalTVDevice> {

        initialize(context)

        return RoyalTVDeviceRegistry
            .getOnlineDevices()
    }

    fun getOfflineDevices(
        context: Context
    ): List<RoyalTVDevice> {

        initialize(context)

        return RoyalTVDeviceRegistry
            .getOfflineDevices()
    }

    fun getDisplayDevices(
        context: Context
    ): List<RoyalTVDevice> {

        initialize(context)

        return RoyalTVDeviceRegistry
            .getAllDevices()
            .filter {
                it.role ==
                    RoyalTVDeviceRole.DISPLAY ||
                    it.role ==
                    RoyalTVDeviceRole
                        .CONTROLLER_AND_DISPLAY
            }
    }

    fun getControllerDevices(
        context: Context
    ): List<RoyalTVDevice> {

        initialize(context)

        return RoyalTVDeviceRegistry
            .getAllDevices()
            .filter {
                it.role ==
                    RoyalTVDeviceRole.CONTROLLER ||
                    it.role ==
                    RoyalTVDeviceRole
                        .CONTROLLER_AND_DISPLAY
            }
    }

    fun getDevice(
        context: Context,
        deviceId: String
    ): RoyalTVDevice? {

        initialize(context)

        return RoyalTVDeviceRegistry
            .getDevice(
                deviceId
            )
    }

    fun removeDevice(
        context: Context,
        deviceId: String
    ) {

        initialize(context)

        RoyalTVDeviceRegistry.removeDevice(
            deviceId
        )
    }

    fun clearAllDevices(
        context: Context
    ) {

        initialize(context)

        RoyalTVDeviceRegistry.clearRegistry()
    }

    fun getDisplayCount(
        context: Context
    ): Int {

        return getDisplayDevices(
            context
        ).size
    }

    fun getControllerCount(
        context: Context
    ): Int {

        return getControllerDevices(
            context
        ).size
    }

    fun getOnlineDisplayCount(
        context: Context
    ): Int {

        return getDisplayDevices(
            context
        ).count {
            it.isOnline
        }
    }

    fun getOnlineControllerCount(
        context: Context
    ): Int {

        return getControllerDevices(
            context
        ).count {
            it.isOnline
        }
    }

    fun isDisplayAvailable(
        context: Context,
        deviceId: String
    ): Boolean {

        val device =
            getDevice(
                context,
                deviceId
            )

        return device != null &&
            (
                device.role ==
                    RoyalTVDeviceRole.DISPLAY ||
                device.role ==
                    RoyalTVDeviceRole
                        .CONTROLLER_AND_DISPLAY
            )
    }

    fun isControllerAvailable(
        context: Context,
        deviceId: String
    ): Boolean {

        val device =
            getDevice(
                context,
                deviceId
            )

        return device != null &&
            (
                device.role ==
                    RoyalTVDeviceRole.CONTROLLER ||
                device.role ==
                    RoyalTVDeviceRole
                        .CONTROLLER_AND_DISPLAY
            )
    }
}
