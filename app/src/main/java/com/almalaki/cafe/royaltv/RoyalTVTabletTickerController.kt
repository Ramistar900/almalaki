package com.almalaki.cafe.royaltv

/**
 * متحكم شريط أخبار ROYAL TV من جهة التابلت.
 *
 * مسؤول عن:
 *
 * 1. إرسال نص الخبر إلى جهاز TV محدد.
 * 2. استخدام نفس RoyalTVTickerSyncManager
 *    المستخدم من الهاتف.
 * 3. عدم إنشاء قناة اتصال مستقلة للتابلت.
 * 4. إبقاء منطق ROYAL TV داخل Core.
 *
 * التابلت هنا Controller فقط.
 * أما عرض الخبر فيبقى مسؤولية جهاز TV.
 */
class RoyalTVTabletTickerController(
    private val connectionManager: RoyalTVConnectionManager
) {

    /**
     * تحديث شريط الأخبار على جهاز TV.
     *
     * @param targetDeviceId
     * معرف جهاز TV المستهدف.
     *
     * @param text
     * النص الجديد لشريط الأخبار.
     *
     * @return
     * true إذا تم إرسال الأمر بنجاح،
     * false إذا تعذر الإرسال وتم التعامل معه
     * بواسطة نظام الاتصال/الطابور.
     */
    suspend fun updateTicker(
        targetDeviceId: String,
        text: String
    ): Boolean {

        val cleanText =
            text.trim()

        if (cleanText.isBlank()) {
            return false
        }

        val command =
            RoyalTVCommand(
                type =
                    RoyalTVCommandType.SHOW_CONTENT,
                payload =
                    "TICKER_UPDATE|$cleanText"
            )

        return connectionManager.sendCommand(
            targetDeviceId = targetDeviceId,
            command = command
        )
    }

    /**
     * إيقاف/تعطيل شريط الأخبار على جهاز TV.
     *
     * لا نحذف النص المحفوظ،
     * وإنما نرسل أمرًا خاصًا بتعطيل العرض.
     */
    suspend fun disableTicker(
        targetDeviceId: String
    ): Boolean {

        val command =
            RoyalTVCommand(
                type =
                    RoyalTVCommandType.SHOW_CONTENT,
                payload =
                    "TICKER_ENABLED|false"
            )

        return connectionManager.sendCommand(
            targetDeviceId = targetDeviceId,
            command = command
        )
    }

    /**
     * إعادة تفعيل شريط الأخبار على جهاز TV.
     */
    suspend fun enableTicker(
        targetDeviceId: String
    ): Boolean {

        val command =
            RoyalTVCommand(
                type =
                    RoyalTVCommandType.SHOW_CONTENT,
                payload =
                    "TICKER_ENABLED|true"
            )

        return connectionManager.sendCommand(
            targetDeviceId = targetDeviceId,
            command = command
        )
    }
}
