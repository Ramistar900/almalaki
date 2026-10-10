
package com.almalaki.royaltv

/**
 * أولوية أنواع المحتوى في ROYAL TV.
 * الرقم الأعلى يعني أولوية أعلى.
 */
enum class RoyalTVContentPriority(
    val value: Int
) {
    IDLE(0),
    NORMAL(10),
    ADVERTISEMENT(20),
    IMPORTANT_CONTENT(30),
    YOUTUBE(40),
    MEDIA(50),
    READY_ORDER(100)
}

/**
 * حساب أولوية الأوامر وتحديد إمكانية مقاطعة المحتوى.
 */
object RoyalTVContentPriorityManager {

    fun getPriority(
        commandType: RoyalTVCommandType
    ): RoyalTVContentPriority {
        return when (commandType) {
            RoyalTVCommandType.SHOW_ORDER ->
                RoyalTVContentPriority.READY_ORDER

            RoyalTVCommandType.SHOW_YOUTUBE ->
                RoyalTVContentPriority.YOUTUBE

            RoyalTVCommandType.SHOW_MEDIA ->
                RoyalTVContentPriority.MEDIA

            RoyalTVCommandType.SHOW_AD ->
                RoyalTVContentPriority.ADVERTISEMENT

            RoyalTVCommandType.SHOW_CONTENT ->
                RoyalTVContentPriority.IMPORTANT_CONTENT

            RoyalTVCommandType.REMOTE_COMMAND ->
                RoyalTVContentPriority.NORMAL

            RoyalTVCommandType.CLEAR_SCREEN ->
                RoyalTVContentPriority.IDLE
        }
    }

    fun getPriorityValue(
        command: RoyalTVCommand
    ): Int {
        return getPriority(command.type).value
    }

    fun isHigherPriority(
        first: RoyalTVCommand,
        second: RoyalTVCommand
    ): Boolean {
        return getPriorityValue(first) >
            getPriorityValue(second)
    }

    fun isSamePriority(
        first: RoyalTVCommand,
        second: RoyalTVCommand
    ): Boolean {
        return getPriorityValue(first) ==
            getPriorityValue(second)
    }

    /**
     * يسمح بمقاطعة المحتوى الحالي إذا كانت أولوية
     * الأمر الوارد أعلى، أو إذا لم يوجد محتوى حالي.
     */
    fun canInterrupt(
        current: RoyalTVCommand?,
        incoming: RoyalTVCommand
    ): Boolean {
        if (current == null) return true

        return isHigherPriority(
            incoming,
            current
        )
    }

    fun isReadyOrder(
        command: RoyalTVCommand
    ): Boolean {
        return command.type ==
            RoyalTVCommandType.SHOW_ORDER
    }

    fun isAdvertisement(
        command: RoyalTVCommand
    ): Boolean {
        return command.type ==
            RoyalTVCommandType.SHOW_AD
    }

    /**
     * يحدد أن عرض الطلب له سلوك خاص عند انتهاء عرضه.
     * الرجوع الفعلي للمحتوى السابق يحتاج إلى منطق تشغيل منفصل.
     */
    fun shouldReturnToPreviousContent(
        command: RoyalTVCommand
    ): Boolean {
        return isReadyOrder(command)
    }
}
