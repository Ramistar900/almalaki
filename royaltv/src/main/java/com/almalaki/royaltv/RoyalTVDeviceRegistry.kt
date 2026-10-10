
package com.almalaki.royaltv

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * سجل أجهزة ROYAL TV.
 *
 * يحفظ قائمة الأجهزة محليًا على هذا الجهاز.
 * لا يُعدّ هذا السجل وحده اتصالًا شبكيًا بين الأجهزة.
 */
object RoyalTVDeviceRegistry {

    private const val PREFS_NAME =
        "royal_tv_device_registry"

    private const val KEY_DEVICES =
        "registered_devices"

    @Volatile
    private var preferences: SharedPreferences? = null

    /**
     * تهيئة السجل.
     * استدعِ هذه الدالة قبل استخدام بقية الدوال.
     */
    fun initialize(context: Context) {
        if (preferences != null) return

        synchronized(this) {
            if (preferences == null) {
                preferences = context.applicationContext
                    .getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                    )
            }
        }
    }

    /**
     * تسجيل جهاز جديد أو تحديث بيانات جهاز مسجل.
     */
    fun registerDevice(device: RoyalTVDevice) {
        val prefs = preferences ?: return
        val devices = getDevicesInternal().toMutableList()

        val existingIndex = devices.indexOfFirst {
            it.deviceId == device.deviceId
        }

        if (existingIndex >= 0) {
            devices[existingIndex] = device
        } else {
            devices.add(device)
        }

        saveDevices(prefs, devices)
    }

    /**
     * تحديث بيانات جهاز مسجل.
     */
    fun updateDevice(device: RoyalTVDevice) {
        registerDevice(device)
    }

    /**
     * تحديث حالة الاتصال المحلية المسجلة.
     */
    fun updateOnlineState(
        deviceId: String,
        isOnline: Boolean
    ) {
        val existing = getDevice(deviceId) ?: return

        registerDevice(
            existing.copy(isOnline = isOnline)
        )
    }

    /**
     * البحث عن جهاز باستخدام معرّفه.
     */
    fun getDevice(deviceId: String): RoyalTVDevice? {
        return getDevicesInternal().firstOrNull {
            it.deviceId == deviceId
        }
    }

    /**
     * استرجاع جميع الأجهزة المسجلة.
     */
    fun getAllDevices(): List<RoyalTVDevice> {
        return getDevicesInternal()
    }

    /**
     * حذف جهاز من السجل المحلي.
     */
    fun removeDevice(deviceId: String) {
        val prefs = preferences ?: return

        val devices = getDevicesInternal().filterNot {
            it.deviceId == deviceId
        }

        saveDevices(prefs, devices)
    }

    /**
     * مسح قائمة الأجهزة المسجلة محليًا.
     */
    fun clearRegistry() {
        preferences
            ?.edit()
            ?.remove(KEY_DEVICES)
            ?.apply()
    }

    /**
     * التحقق من وجود الجهاز في السجل.
     */
    fun containsDevice(deviceId: String): Boolean {
        return getDevice(deviceId) != null
    }

    /**
     * استرجاع الأجهزة المسجلة على أنها متصلة.
     */
    fun getOnlineDevices(): List<RoyalTVDevice> {
        return getDevicesInternal().filter {
            it.isOnline
        }
    }

    /**
     * استرجاع الأجهزة المسجلة على أنها غير متصلة.
     */
    fun getOfflineDevices(): List<RoyalTVDevice> {
        return getDevicesInternal().filter {
            !it.isOnline
        }
    }

    /**
     * قراءة السجل من التخزين المحلي.
     */
    private fun getDevicesInternal(): List<RoyalTVDevice> {
        val prefs = preferences ?: return emptyList()

        val raw = prefs.getString(KEY_DEVICES, null)
            ?: return emptyList()

        return try {
            val jsonArray = JSONArray(raw)
            val devices = mutableListOf<RoyalTVDevice>()

            for (index in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(index)
                    ?: continue

                val deviceId = item.optString(
                    "deviceId",
                    ""
                )

                if (deviceId.isBlank()) continue

                val deviceName = item.optString(
                    "deviceName",
                    deviceId
                )

                val roleName = item.optString(
                    "role",
                    RoyalTVDeviceRole
                        .CONTROLLER_AND_DISPLAY
                        .name
                )

                val role = try {
                    RoyalTVDeviceRole.valueOf(roleName)
                } catch (_: IllegalArgumentException) {
                    RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY
                }

                val isOnline = item.optBoolean(
                    "isOnline",
                    false
                )

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

    /**
     * حفظ قائمة الأجهزة في التخزين المحلي.
     */
    private fun saveDevices(
        prefs: SharedPreferences,
        devices: List<RoyalTVDevice>
    ) {
        val jsonArray = JSONArray()

        devices.forEach { device ->
            val item = JSONObject().apply {
                put("deviceId", device.deviceId)
                put("deviceName", device.deviceName)
                put("role", device.role.name)
                put("isOnline", device.isOnline)
            }

            jsonArray.put(item)
        }

        prefs.edit()
            .putString(KEY_DEVICES, jsonArray.toString())
            .apply()
    }
}
