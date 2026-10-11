
package com.almalaki.royaltv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * ROYAL TV — Home Screen Broadcast Session
 *
 * مسؤول عن حالة جلسة بث الشاشة الرئيسية.
 *
 * هذا الملف ينظم دورة الجلسة فقط؛
 * لا يدّعي أنه يلتقط الشاشة أو ينقل الفيديو
 * قبل ربط خدمة MediaProjection والنقل الفعلي.
 */
enum class RoyalTVHomeBroadcastState {
    IDLE,
    REQUESTING_PERMISSION,
    WAITING_FOR_SOURCE,
    CAPTURING,
    STREAMING,
    STOPPING,
    ERROR
}

data class RoyalTVHomeBroadcastStatus(
    val state: RoyalTVHomeBroadcastState,
    val message: String,
    val sourceDeviceId: String? = null,
    val error: String? = null
)

class RoyalTVHomeScreenBroadcast {

    var status by mutableStateOf(
        RoyalTVHomeBroadcastStatus(
            state = RoyalTVHomeBroadcastState.IDLE,
            message = "البث متوقف"
        )
    )
        private set

    /**
     * يبدأ طلب إنشاء جلسة مشاركة الشاشة.
     * يجب على الواجهة استدعاء طلب إذن Android
     * الرسمي بعد هذه الخطوة.
     */
    fun requestStart() {
        if (
            status.state != RoyalTVHomeBroadcastState.IDLE &&
            status.state != RoyalTVHomeBroadcastState.ERROR
        ) {
            return
        }

        status = RoyalTVHomeBroadcastStatus(
            state = RoyalTVHomeBroadcastState.REQUESTING_PERMISSION,
            message = "بانتظار موافقة مشاركة الشاشة"
        )
    }

    /**
     * يُستدعى بعد منح الإذن الرسمي.
     * منح الإذن وحده لا يعني أن البث بدأ.
     */
    fun onPermissionGranted() {
        if (
            status.state !=
            RoyalTVHomeBroadcastState.REQUESTING_PERMISSION
        ) {
            return
        }

        status = RoyalTVHomeBroadcastStatus(
            state = RoyalTVHomeBroadcastState.WAITING_FOR_SOURCE,
            message = "بانتظار تجهيز مصدر الشاشة"
        )
    }

    /**
     * يُستدعى عند رفض إذن مشاركة الشاشة.
     */
    fun onPermissionDenied() {
        status = RoyalTVHomeBroadcastStatus(
            state = RoyalTVHomeBroadcastState.IDLE,
            message = "لم تتم الموافقة على مشاركة الشاشة"
        )
    }

    /**
     * يُستدعى عندما يبدأ الالتقاط الحقيقي بنجاح.
     */
    fun onCaptureStarted(
        sourceDeviceId: String
    ) {
        if (sourceDeviceId.isBlank()) {
            fail("معرّف جهاز المصدر غير صالح")
            return
        }

        status = RoyalTVHomeBroadcastStatus(
            state = RoyalTVHomeBroadcastState.CAPTURING,
            message = "بدأ التقاط الشاشة؛ بانتظار اتصال البث",
            sourceDeviceId = sourceDeviceId
        )
    }

    /**
     * يُستدعى بعد نجاح نقل الصورة واستقبالها.
     * لا تستدعِه قبل تأكيد اتصال النقل الفعلي.
     */
    fun onStreamConnected() {
        if (
            status.state != RoyalTVHomeBroadcastState.CAPTURING &&
            status.state != RoyalTVHomeBroadcastState.WAITING_FOR_SOURCE
        ) {
            return
        }

        status = status.copy(
            state = RoyalTVHomeBroadcastState.STREAMING,
            message = "البث الحي متصل"
        )
    }

    /**
     * تسجيل خطأ حقيقي في جلسة البث.
     */
    fun fail(reason: String) {
        status = RoyalTVHomeBroadcastStatus(
            state = RoyalTVHomeBroadcastState.ERROR,
            message = "تعذر تشغيل البث",
            sourceDeviceId = status.sourceDeviceId,
            error = reason.ifBlank {
                "حدث خطأ غير محدد"
            }
        )
    }

    /**
     * طلب إنهاء جلسة البث.
     * يجب إيقاف الالتقاط والنقل الفعليين
     * قبل اعتبار الجلسة متوقفة نهائيًا.
     */
    fun requestStop() {
        if (status.state == RoyalTVHomeBroadcastState.IDLE) {
            return
        }

        status = status.copy(
            state = RoyalTVHomeBroadcastState.STOPPING,
            message = "جارٍ إيقاف جلسة البث"
        )
    }

    /**
     * يُستدعى بعد تأكيد إيقاف موارد البث الفعلية.
     */
    fun onStopped() {
        status = RoyalTVHomeBroadcastStatus(
            state = RoyalTVHomeBroadcastState.IDLE,
            message = "البث متوقف"
        )
    }
}
