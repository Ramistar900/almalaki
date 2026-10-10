
package com.almalaki.royaltv

/**
 * أنواع الأوامر المشتركة بين الهاتف والتابلت والتلفزيون.
 *
 * هذا الملف يعرّف أنواع البيانات فقط،
 * ولا ينفذ الاتصال أو العرض بنفسه.
 */
enum class RoyalTVCommandType {
    SHOW_ORDER,
    SHOW_YOUTUBE,
    SHOW_MEDIA,
    SHOW_AD,
    SHOW_CONTENT,
    REMOTE_COMMAND,
    CLEAR_SCREEN
}

/**
 * مصدر المحتوى الذي يمكن أن يظهر على التلفزيون.
 */
enum class RoyalTVSource {
    IDLE,
    ORDER,
    YOUTUBE,
    MEDIA,
    AD,
    CONTENT
}

/**
 * حالة العرض الحالية.
 */
data class RoyalTVState(
    val source: RoyalTVSource = RoyalTVSource.IDLE,
    val commandType: RoyalTVCommandType? = null,
    val payload: String = "",
    val isPlaying: Boolean = false,
    val isFullscreen: Boolean = true
)

/**
 * أمر قابل للتمرير بين مكونات ROYAL TV.
 *
 * payload يحمل بيانات الأمر كنص في هذه المرحلة.
 */
data class RoyalTVCommand(
    val type: RoyalTVCommandType,
    val payload: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
