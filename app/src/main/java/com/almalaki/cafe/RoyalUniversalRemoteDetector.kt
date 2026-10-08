package com.almalaki.cafe

import android.view.KeyEvent

/**
 * Royal Universal Remote Detector
 *
 * المرحلة الأولى:
 * كشف جميع KeyEvent القادمة من أجهزة التحكم
 * التي يعمل بها تطبيق Android.
 *
 * لا يقوم هذا الملف بتخمين وظيفة أي زر.
 * بل يحتفظ بالمعلومات الحقيقية التي أرسلها الجهاز.
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
     * يحول KeyEvent الحقيقي إلى معلومات قابلة للقراءة
     * ليتم استخدامها لاحقًا في محرك الريموت الموحد.
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

        return RoyalRemoteKeyInfo(

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
    }
}
