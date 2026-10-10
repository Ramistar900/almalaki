
package com.almalaki.cafe.royaltv

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * مدير الإشعارات المؤقتة المستقل لـ ROYAL TV.
 *
 * لا يستبدل مصدر البث ولا يغيّر RoyalTVState.
 * تبقى طبقة العرض مسؤولة عن إظهار الإشعار فوق
 * نافذة البث الفعلية دون إيقاف المشغل.
 *
 * soundRequests يرسل طلب التنبيه الصوتي.
 * تشغيل ملف الصوت ومزجه مع صوت البث يحتاجان
 * إلى الربط في طبقة العرض.
 */
object RoyalTVOverlayManager {

    const val DEFAULT_ALERT_SOUND_DURATION_MS = 5_000L
    const val DEFAULT_OVERLAY_DURATION_MS = 8_000L

    data class OverlayNotification(
        val id: String = UUID.randomUUID().toString(),
        val title: String = "",
        val message: String = "",
        val logoKey: String = "royal_app_logo",
        val durationMs: Long = DEFAULT_OVERLAY_DURATION_MS,
        val priority: Int = 0,
        val createdAt: Long = System.currentTimeMillis(),
        val playAlertSound: Boolean = true,
        val alertSoundDurationMs: Long =
            DEFAULT_ALERT_SOUND_DURATION_MS
    ) {
        val safeDurationMs: Long
            get() = durationMs.coerceAtLeast(1_000L)

        val safeAlertSoundDurationMs: Long
            get() = alertSoundDurationMs.coerceIn(0L, 5_000L)
    }

    data class AlertSoundRequest(
        val notificationId: String,
        val durationMs: Long = DEFAULT_ALERT_SOUND_DURATION_MS
    )

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _activeOverlay =
        MutableStateFlow<OverlayNotification?>(null)

    val activeOverlay: StateFlow<OverlayNotification?> =
        _activeOverlay.asStateFlow()

    private val _soundRequests =
        MutableSharedFlow<AlertSoundRequest>(
            replay = 0,
            extraBufferCapacity = 8
        )

    val soundRequests: SharedFlow<AlertSoundRequest> =
        _soundRequests.asSharedFlow()

    private val queueLock = Any()
    private val pending = mutableListOf<OverlayNotification>()
    private var workerRunning = false

    /**
     * إضافة إشعار إلى الطابور.
     * الأولوية الأعلى تُعرض أولًا بين الإشعارات المنتظرة.
     */
    fun enqueue(notification: OverlayNotification): String {
        synchronized(queueLock) {
            pending.add(notification)

            pending.sortWith(
                compareByDescending<OverlayNotification> {
                    it.priority
                }.thenBy {
                    it.createdAt
                }
            )

            if (!workerRunning) {
                workerRunning = true
                scope.launch {
                    processQueue()
                }
            }
        }

        return notification.id
    }

    /**
     * إنشاء إشعار بطريقة مختصرة.
     * مدة الإشعار مستقلة عن مدة التنبيه الصوتي.
     */
    fun show(
        title: String,
        message: String,
        durationMs: Long = DEFAULT_OVERLAY_DURATION_MS,
        priority: Int = 0,
        playAlertSound: Boolean = true
    ): String {
        return enqueue(
            OverlayNotification(
                title = title,
                message = message,
                durationMs = durationMs,
                priority = priority,
                playAlertSound = playAlertSound
            )
        )
    }

    /**
     * مسح الطابور وإخفاء الإشعار الحالي.
     */
    fun clearAll() {
        synchronized(queueLock) {
            pending.clear()
            _activeOverlay.value = null
        }
    }

    /**
     * عدد الإشعارات التي تنتظر العرض.
     */
    fun pendingCount(): Int =
        synchronized(queueLock) {
            pending.size
        }

    /**
     * يعرض الإشعارات بالتتابع دون تداخل.
     */
    private suspend fun processQueue() {
        while (true) {
            val next = synchronized(queueLock) {
                if (pending.isEmpty()) {
                    workerRunning = false
                    null
                } else {
                    pending.removeAt(0)
                }
            } ?: return

            _activeOverlay.value = next

            if (
                next.playAlertSound &&
                next.safeAlertSoundDurationMs > 0L
            ) {
                _soundRequests.emit(
                    AlertSoundRequest(
                        notificationId = next.id,
                        durationMs = next.safeAlertSoundDurationMs
                    )
                )
            }

            delay(next.safeDurationMs)

            if (_activeOverlay.value?.id == next.id) {
                _activeOverlay.value = null
            }
        }
    }
}
