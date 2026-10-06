package com.almalaki.cafe.royaltv

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
 *
 * هذا الملف لا ينشئ اتصالًا بعد.
 * وظيفته تجهيز هوية الجهاز والدور الذي سيعمل به.
 */
data class RoyalTVDevice(
    val deviceId: String,
    val deviceName: String,
    val role: RoyalTVDeviceRole,
    val isOnline: Boolean = false
)

/**
 * إنشاء معرّف محلي للجهاز.
 *
 * سنستخدمه لاحقًا عند تسجيل الجهاز في ROYAL TV Core.
 */
fun createRoyalTVDeviceId(): String {
    return "ROYAL-TV-${java.util.UUID.randomUUID()}"
}
