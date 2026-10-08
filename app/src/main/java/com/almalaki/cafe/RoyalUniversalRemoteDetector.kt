package com.almalaki.cafe

import android.view.KeyEvent

/**
 * Royal Universal Remote Detector
 *
 * المرحلة 18.3:
 * استقبال وتحليل أحداث أزرار أجهزة التحكم
 * والاحتفاظ بآخر حدث تم اكتشافه.
 *
 * هذا الملف لا يخمن وظيفة أي زر.
 * بل يحتفظ بالمعلومات الحقيقية التي أرسلها جهاز Android.
 */
data class RoyalRemoteKeyInfo(
    val keyCode: Int,
    val keyName: String,
    val action: Int,
    val actionName: String,
    val repeatCount: Int,
    val deviceId: Int,
    val scanCode: Int,
    val source: Int
)

object RoyalUniversalRemoteDetector {

    /**
     * آخر زر تم اكتشافه.
     *
     * نستخدمه لاحقاً في شاشة تشخيص الريموت.
     */
    private var lastDetectedKey: RoyalRemoteKeyInfo? = null

    /**
     * يحول KeyEvent الحقيقي إلى معلومات قابلة للقراءة.
     *
     * لا يتم هنا ربط الزر بأي وظيفة.
     */
    fun detect(
        event: KeyEvent
    ): RoyalRemoteKeyInfo {

        val actionName =
            when (event.action) {

                KeyEvent.ACTION_DOWN ->
                    "DOWN"

                KeyEvent.ACTION_UP ->
                    "UP"

                KeyEvent.ACTION_MULTIPLE ->
                    "MULTIPLE"

                else ->
                    "UNKNOWN"
            }

        val keyName =
            try {

                KeyEvent.keyCodeToString(
                    event.keyCode
                )

            } catch (_: Exception) {

                "KEYCODE_UNKNOWN_${event.keyCode}"
            }

        val info =
            RoyalRemoteKeyInfo(

                keyCode =
                    event.keyCode,

                keyName =
                    keyName,

                action =
                    event.action,

                actionName =
                    actionName,

                repeatCount =
                    event.repeatCount,

                deviceId =
                    event.deviceId,

                scanCode =
                    event.scanCode,

                source =
                    event.source
            )

        /**
         * حفظ آخر حدث مكتشف.
         */
        lastDetectedKey = info

        return info
    }

    /**
     * يعيد آخر زر تم اكتشافه.
     *
     * يستخدم لاحقاً بواسطة شاشة تشخيص الريموت.
     */
    fun getLastDetectedKey(): RoyalRemoteKeyInfo? {
        return lastDetectedKey
    }

    /**
     * مسح آخر حدث مكتشف.
     *
     * سيستخدم لاحقاً عند بدء جلسة تشخيص جديدة.
     */
    fun clearLastDetectedKey() {
        lastDetectedKey = null
    }
}
