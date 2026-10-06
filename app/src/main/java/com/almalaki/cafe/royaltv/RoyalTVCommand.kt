package com.almalaki.cafe.royaltv

/**
 * الأوامر المشتركة بين الهاتف والتابلت والتلفزيون.
 *
 * هذا الملف يعرّف بروتوكول ROYAL TV فقط.
 * لا ينفذ اتصالًا ولا يغيّر واجهة المستخدم.
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
 * نوع المصدر الذي يعرضه التلفزيون حاليًا.
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
 * حالة العرض المشتركة التي ستستخدمها لاحقًا
 * واجهة الهاتف والتابلت والتلفزيون.
 */
data class RoyalTVState(
    val source: RoyalTVSource = RoyalTVSource.IDLE,
    val commandType: RoyalTVCommandType? = null,
    val payload: String = "",
    val isPlaying: Boolean = false,
    val isFullscreen: Boolean = true
)

/**
 * أمر ROYAL TV قابل للنقل بين الواجهات.
 *
 * payload يبقى كنص في هذه المرحلة حتى نربطه لاحقًا
 * بالـ JSON والبيانات الحقيقية للطلب/YouTube/الوسائط.
 */
data class RoyalTVCommand(
    val type: RoyalTVCommandType,
    val payload: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
