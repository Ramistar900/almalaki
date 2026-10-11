package com.almalaki.royaltv

import android.content.Context

/**
 * يستقبل تحديثات الشريط الإخباري دون تغيير المحتوى الرئيسي
 * المعروض على شاشة ROYAL TV.
 */
object RoyalTVTickerReceiver {

    private const val TICKER_PREFIX = "TICKER_UPDATE|"

    /**
     * يعالج أمر تحديث الشريط.
     *
     * true  = تمت معالجة أمر الشريط.
     * false = الأمر ليس تحديثًا للشريط.
     */
    fun receive(
        context: Context,
        command: RoyalTVCommand
    ): Boolean {
        val text = extractText(command) ?: return false

        RoyalTVTickerManager.setText(
            context = context,
            value = text
        )

        return true
    }

    /**
     * يتحقق من أن الأمر مخصص لتحديث الشريط.
     */
    fun isTickerCommand(
        command: RoyalTVCommand
    ): Boolean {
        return command.type == RoyalTVCommandType.SHOW_CONTENT &&
            command.payload.startsWith(TICKER_PREFIX)
    }

    /**
     * يستخرج نص الشريط من الحمولة.
     */
    fun extractText(
        command: RoyalTVCommand
    ): String? {
        if (!isTickerCommand(command)) {
            return null
        }

        return command.payload
            .removePrefix(TICKER_PREFIX)
            .trim()
            .takeIf { it.isNotBlank() }
    }
}
