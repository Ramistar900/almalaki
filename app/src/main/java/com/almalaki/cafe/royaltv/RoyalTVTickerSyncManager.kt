package com.almalaki.cafe.royaltv

import android.content.Context

/**
 * مزامنة شريط أخبار ROYAL TV
 * بين الهاتف / التابلت وشاشة ROYAL TV.
 *
 * المسؤوليات:
 *
 * 1. حفظ الخبر محليًا.
 * 2. تجهيز أمر تحديث الخبر.
 * 3. إرسال الأمر إلى جهاز ROYAL TV.
 * 4. الاستفادة من نظام Offline Queue الموجود في Core.
 *
 * لا توجد واجهة رسومية هنا.
 */
class RoyalTVTickerSyncManager(
    context: Context,
    private val connectionManager: RoyalTVConnectionManager
) {

    private val appContext =
        context.applicationContext

    /**
     * تحديث الخبر وإرساله إلى شاشة ROYAL TV.
     *
     * إذا كان الاتصال متاحًا:
     * يتم الإرسال مباشرة.
     *
     * إذا لم يكن الاتصال متاحًا:
     * يقوم RoyalTVConnectionManager
     * بحفظ الأمر في Offline Queue.
     *
     * @param targetDeviceId معرف شاشة ROYAL TV
     * @param text نص الخبر الجديد
     *
     * @return
     * true  = تم الإرسال مباشرة.
     * false = تم حفظه ليُرسل لاحقًا.
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

        /*
         * حفظ الخبر محليًا أولًا.
         *
         * بهذه الطريقة يبقى آخر خبر
         * موجودًا على الهاتف حتى بدون إنترنت.
         */
        RoyalTVTickerManager.setText(
            context = appContext,
            value = cleanText
        )

        /*
         * نستخدم SHOW_CONTENT في Core
         * لأن الشريط جزء من محتوى ROYAL TV.
         *
         * Prefix واضح حتى يستطيع مستقبل TV
         * تمييز أن هذا الأمر خاص بالشريط.
         */
        val command =
            RoyalTVCommand(
                type =
                    RoyalTVCommandType.SHOW_CONTENT,
                payload =
                    buildTickerPayload(
                        cleanText
                    )
            )

        /*
         * يمر الإرسال عبر ConnectionManager
         * للاستفادة من:
         *
         * Online إرسال مباشر
         * Offline حفظ الأمر
         * Auto Reconnect
         * Offline → Online Sync
         */
        return connectionManager.sendCommand(
            targetDeviceId =
                targetDeviceId,
            command =
                command
        )
    }

    /**
     * تجهيز Payload خاص بالشريط.
     *
     * الصيغة:
     *
     * TICKER_UPDATE|النص
     *
     * وسيتم فك هذه الصيغة لاحقًا
     * في TV Display / Core Receiver.
     */
    private fun buildTickerPayload(
        text: String
    ): String {

        return "TICKER_UPDATE|$text"
    }

    /**
     * قراءة نص الشريط الحالي
     * المحفوظ محليًا على الهاتف / التابلت.
     */
    fun getLocalTickerText(): String {

        return RoyalTVTickerManager
            .getText()
    }

    /**
     * معرفة ما إذا كان الشريط
     * مفعّلًا محليًا.
     */
    fun isLocalTickerEnabled(): Boolean {

        return RoyalTVTickerManager
            .isEnabled()
    }
}
