package com.almalaki.cafe.royaltv

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object RoyalTVDeviceRegistry {

private const val PREFS_NAME =
    "royal_tv_device_registry"

private const val KEY_DEVICES =
    "registered_devices"

private var preferences: SharedPreferences? = null

fun initialize(
    context: Context
) {
    if (preferences != null) {
        return
    }

    preferences =
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
}

fun registerDevice(
    device: RoyalTVDevice
) {

    val prefs =
        preferences
            ?: return

    val devices =
        getDevicesInternal().toMutableList()

    val existingIndex =
        devices.indexOfFirst {
            it.deviceId == device.deviceId
        }

    if (existingIndex >= 0) {
        devices[existingIndex] =
            device
    } else {
        devices.add(device)
    }

    saveDevices(
        prefs,
        devices
    )
}

fun updateDevice(
    device: RoyalTVDevice
) {
    registerDevice(device)
}

fun updateOnlineState(
    deviceId: String,
    isOnline: Boolean
) {

    val existing =
        getDevice(deviceId)
            ?: return

    registerDevice(
        existing.copy(
            isOnline = isOnline
        )
    )
}

fun getDevice(
    deviceId: String
): RoyalTVDevice? {

    return getDevicesInternal()
        .firstOrNull {
            it.deviceId == deviceId
        }
}

fun getAllDevices(): List<RoyalTVDevice> {

    return getDevicesInternal()
}

fun removeDevice(
    deviceId: String
) {

    val prefs =
        preferences
            ?: return

    val devices =
        getDevicesInternal()
            .filterNot {
                it.deviceId == deviceId
            }

    saveDevices(
        prefs,
        devices
    )
}

fun clearRegistry() {

    preferences
        ?.edit()
        ?.remove(KEY_DEVICES)
        ?.apply()
}

fun containsDevice(
    deviceId: String
): Boolean {

    return getDevice(deviceId) != null
}

fun getOnlineDevices(): List<RoyalTVDevice> {

    return getDevicesInternal()
        .filter {
            it.isOnline
        }
}

fun getOfflineDevices(): List<RoyalTVDevice> {

    return getDevicesInternal()
        .filter {
            !it.isOnline
        }
}

private fun getDevicesInternal():
    List<RoyalTVDevice> {

    val prefs =
        preferences
            ?: return emptyList()

    val raw =
        prefs.getString(
            KEY_DEVICES,
            null
        )
            ?: return emptyList()

    return try {

        val jsonArray =
            JSONArray(raw)

        val devices =
            mutableListOf<RoyalTVDevice>()

        for (
            index in
            0 until jsonArray.length()
        ) {

            val item =
                jsonArray
                    .optJSONObject(index)
                    ?: continue

            val deviceId =
                item.optString(
                    "deviceId",
                    ""
                )

            val deviceName =
                item.optString(
                    "deviceName",
                    deviceId
                )

            val roleName =
                item.optString(
                    "role",
                    RoyalTVDeviceRole
                        .CONTROLLER_AND_DISPLAY
                        .name
                )

            val isOnline =
                item.optBoolean(
                    "isOnline",
                    false
                )

            if (deviceId.isBlank()) {
                continue
            }

            val role =
                try {

                    RoyalTVDeviceRole
                        .valueOf(
                            roleName
                        )

                } catch (_: Exception) {

                    RoyalTVDeviceRole
                        .CONTROLLER_AND_DISPLAY
                }

            devices.add(
                RoyalTVDevice(
                    deviceId = deviceId,
                    deviceName = deviceName,
                    role = role,
                    isOnline = isOnline
                )
            )
        }

        devices

    } catch (_: Exception) {

        emptyList()
    }
}

private fun saveDevices(
    prefs: SharedPreferences,
    devices: List<RoyalTVDevice>
) {

    val jsonArray =
        JSONArray()

    devices.forEach { device ->

        val item =
            JSONObject()

        item.put(
            "deviceId",
            device.deviceId
        )

        item.put(
            "deviceName",
            device.deviceName
        )

        item.put(
            "role",
            device.role.name
        )

        item.put(
            "isOnline",
            device.isOnline
        )

        jsonArray.put(item)
    }

    prefs.edit()
        .putString(
            KEY_DEVICES,
            jsonArray.toString()
        )
        .apply()
}

}
