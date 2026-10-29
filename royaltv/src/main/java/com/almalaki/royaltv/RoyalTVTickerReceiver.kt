package com.almalaki.royaltv

import android.content.Context

/**
 * يستقبل أوامر الشريط الإخباري الخاصة بـ ROYAL TV.
 *
 * يدعم:
 * 1. تحديث نص الشريط.
 * 2. إظهار الشريط.
 * 3. إخفاء الشريط مع الاحتفاظ بالنص المحفوظ.
 */
object RoyalTVTickerReceiver {

    private const val TICKER_PREFIX =
        "TICKER_UPDATE|"

    private const val TICKER_ENABLED_PREFIX =
        "TICKER_ENABLED|"

    /**
     * يعالج أوامر الشريط.
     *
     * true  = تمت معالجة الأمر.
     * false = الأمر ليس أمرًا خاصًا بالشريط.
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

        val payload = command.payload

        if (payload.startsWith("LAYOUT_SETTINGS|")) {
            val json = payload.removePrefix("LAYOUT_SETTINGS|")
            return RoyalTVLayoutSettings.applyJson(context, json)
        }

        // تشغيل الشريط أو إيقافه.
        if (
            payload.startsWith(
                TICKER_ENABLED_PREFIX
            )
        ) {
            val value = payload
                .removePrefix(
                    TICKER_ENABLED_PREFIX
                )
                .trim()

            val enabled = when (value) {
                "true" -> true
                "false" -> false
                else -> return false
            }

            RoyalTVBroadcastSettings.setTickerEnabled(
                context = context,
                enabled = enabled
            )

            return true
        }

        // تحديث نص الشريط مع الحفاظ على السلوك السابق.
        val text = extractText(command)
            ?: return false

        RoyalTVTickerManager.setText(
            context = context,
            value = text
        )

        return true
    }

    /**
     * يتحقق من أن الأمر خاص بالشريط.
     */
    fun isTickerCommand(
        command: RoyalTVCommand
    ): Boolean {
        return command.type ==
            RoyalTVCommandType.SHOW_CONTENT &&
            (
                command.payload.startsWith("LAYOUT_SETTINGS|") ||
                command.payload.startsWith(
                    TICKER_PREFIX
                ) ||
                command.payload.startsWith(
                    TICKER_ENABLED_PREFIX
                )
            )
    }

    /**
     * يستخرج نص الشريط من أمر التحديث.
     */
    fun extractText(
        command: RoyalTVCommand
    ): String? {

        if (
            command.type !=
            RoyalTVCommandType.SHOW_CONTENT ||
            !command.payload.startsWith(
                TICKER_PREFIX
            )
        ) {
            return null
        }

        return command.payload
            .removePrefix(TICKER_PREFIX)
            .trim()
            .takeIf { it.isNotBlank() }
    }
}
