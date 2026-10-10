
package com.almalaki.royaltv

/**
 * دور جهاز ROYAL TV داخل النظام.
 */
enum class RoyalTVDeviceRole {
    CONTROLLER,
    DISPLAY,
    CONTROLLER_AND_DISPLAY
}

/**
 * معلومات جهاز ROYAL TV.
 */
data class RoyalTVDevice(
    val deviceId: String,
    val deviceName: String,
    val role: RoyalTVDeviceRole,
    val isOnline: Boolean = false
)

/**
 * إنشاء معرّف محلي للجهاز.
 */
fun createRoyalTVDeviceId(): String {
    return "ROYAL-TV-${java.util.UUID.randomUUID()}"
}
