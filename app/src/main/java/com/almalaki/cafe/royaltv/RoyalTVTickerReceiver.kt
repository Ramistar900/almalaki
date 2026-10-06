package com.almalaki.cafe.royaltv

import android.content.Context

/**
 * مستقبل تحديثات شريط أخبار ROYAL TV.
 *
 * المسؤوليات:
 *
 * 1. استقبال أمر SHOW_CONTENT القادم من Core.
 * 2. التحقق من أن الأمر خاص بشريط الأخبار.
 * 3. استخراج النص من Payload.
 * 4. حفظ النص محليًا على جهاز TV.
 * 5. تحديث StateFlow الخاص بـ RoyalTVTickerManager.
 *
 * لا توجد واجهة رسومية هنا.
 */
object RoyalTVTickerReceiver {

    private const val TICKER_PREFIX =
        "TICKER_UPDATE|"

    /**
     * محاولة استقبال أمر خاص بشريط الأخبار.
     *
     * @return
     * true  = تم التعرف على الأمر وتطبيقه.
     * false = الأمر ليس أمر شريط أخبار صالحًا.
     */
    fun receive(
        context: Context,
        command: RoyalTVCommand
    ): Boolean {

        if (
            command.type !=
            RoyalTVCommandType.SHOW_CONTENT
        ) {
            return false
        }

        val payload =
            command.payload

        if (
            !payload.startsWith(
                TICKER_PREFIX
            )
        ) {
            return false
        }

        val text =
            payload
                .removePrefix(
                    TICKER_PREFIX
                )
                .trim()

        if (text.isBlank()) {
            return false
        }

        /*
         * حفظ الخبر محليًا على جهاز TV.
         *
         * بعد ذلك يبقى الخبر موجودًا
         * حتى لو انقطع الإنترنت.
         */
        RoyalTVTickerManager.setText(
            context = context,
            value = text
        )

        return true
    }

    /**
     * التحقق فقط من نوع الأمر
     * بدون تطبيقه.
     */
    fun isTickerCommand(
        command: RoyalTVCommand
    ): Boolean {

        return command.type ==
            RoyalTVCommandType.SHOW_CONTENT &&
            command.payload
                .startsWith(
                    TICKER_PREFIX
                )
    }

    /**
     * استخراج نص الخبر من الأمر
     * بدون تغيير الحالة المحلية.
     */
    fun extractText(
        command: RoyalTVCommand
    ): String? {

        if (!isTickerCommand(command)) {
            return null
        }

        val text =
            command.payload
                .removePrefix(
                    TICKER_PREFIX
                )
                .trim()

        return text.takeIf {
            it.isNotBlank()
        }
    }
}
