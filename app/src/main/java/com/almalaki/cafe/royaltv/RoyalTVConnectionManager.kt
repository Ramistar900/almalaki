package com.almalaki.cafe.royaltv

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

class RoyalTVConnectionManager(
    context: Context
) {

    private val appContext =
        context.applicationContext

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.IO
        )

    private val networkMonitor =
        RoyalTVNetworkMonitor(
            appContext
        )

    private var reconnectJob:
        Job? = null

    private var syncJob:
        Job? = null

    private var deviceId:
        String = ""

    private var started =
        false

    private var lastNetworkState =
        RoyalTVNetworkState.OFFLINE

    init {

        RoyalTVOfflineCommandStore
            .initialize(
                appContext
            )
    }

    /**
     * بدء مراقبة الاتصال وربط جهاز ROYAL TV.
     */
    fun start(
        onCommand:
            (RoyalTVCommand) -> Unit,
        onConnectionStateChanged:
            (RoyalTVNetworkState) -> Unit = {}
    ) {

        if (started) {
            return
        }

        started = true

        deviceId =
            RoyalTVDeviceIdentity
                .getDeviceId(
                    appContext
                )

        networkMonitor.start { state ->

            lastNetworkState =
                state

            onConnectionStateChanged(
                state
            )

            when (state) {

                RoyalTVNetworkState.ONLINE -> {

                    scheduleReconnect(
                        onCommand
                    )
                }

                RoyalTVNetworkState.OFFLINE -> {

                    cancelReconnect()
                    cancelSync()

                    RoyalTVRealtime.stop()
                }
            }
        }
    }

    /**
     * تجهيز اتصال ROYAL TV بعد عودة الشبكة.
     */
    private fun scheduleReconnect(
        onCommand:
            (RoyalTVCommand) -> Unit
    ) {

        reconnectJob?.cancel()

        reconnectJob =
            scope.launch {

                delay(2000L)

                if (
                    !started ||
                    lastNetworkState !=
                    RoyalTVNetworkState.ONLINE
                ) {
                    return@launch
                }

                try {

                    RoyalTVRealtime.start(
                        deviceId =
                            deviceId,
                        onCommand =
                            onCommand
                    )

                    /*
                     * بعد نجاح تشغيل قناة Realtime
                     * نحاول إرسال جميع الأوامر
                     * التي تم حفظها أثناء Offline.
                     */
                    syncOfflineCommands()

                } catch (e: Exception) {

                    android.util.Log.e(
                        "RoyalTVConnection",
                        "Reconnect error: " +
                            "${e.message}",
                        e
                    )
                }
            }
    }

    /**
     * إرسال أمر جديد.
     *
     * إذا كان الجهاز Online:
     * يحاول الإرسال مباشرة.
     *
     * إذا فشل الإرسال:
     * يتم حفظ الأمر محليًا.
     *
     * إذا كان Offline:
     * يتم حفظ الأمر مباشرة.
     *
     * true  = تم الإرسال.
     * false = تم الحفظ للـ Offline.
     */
    suspend fun sendCommand(
        targetDeviceId: String,
        command: RoyalTVCommand
    ): Boolean {

        if (
            !started ||
            lastNetworkState !=
                RoyalTVNetworkState.ONLINE
        ) {

            RoyalTVOfflineCommandStore
                .enqueue(
                    targetDeviceId =
                        targetDeviceId,
                    command =
                        command
                )

            return false
        }

        val sent =
            RoyalTVRealtime.sendCommand(
                targetDeviceId =
                    targetDeviceId,
                command =
                    command
            )

        if (!sent) {

            RoyalTVOfflineCommandStore
                .enqueue(
                    targetDeviceId =
                        targetDeviceId,
                    command =
                        command
                )
        }

        return sent
    }

    /**
     * إرسال جميع الأوامر التي تم حفظها
     * أثناء انقطاع الاتصال.
     *
     * الأمر لا يُحذف إلا بعد نجاح الإرسال.
     */
    private fun syncOfflineCommands() {

        syncJob?.cancel()

        syncJob =
            scope.launch {

                val pendingCommands =
                    RoyalTVOfflineCommandStore
                        .getPendingCommands()

                if (
                    pendingCommands.isEmpty()
                ) {
                    return@launch
                }

                for (
                    pendingCommand
                    in pendingCommands
                ) {

                    if (
                        !started ||
                        lastNetworkState !=
                            RoyalTVNetworkState.ONLINE
                    ) {
                        return@launch
                    }

                    val sent =
                        RoyalTVRealtime.sendCommand(
                            targetDeviceId =
                                pendingCommand
                                    .targetDeviceId,
                            command =
                                pendingCommand.command
                        )

                    if (sent) {

                        RoyalTVOfflineCommandStore
                            .remove(
                                pendingCommand
                            )

                    } else {

                        /*
                         * إذا فشل أمر واحد،
                         * لا نحذف أي أمر فاشل.
                         *
                         * نوقف المزامنة الحالية
                         * وننتظر عودة الاتصال.
                         */
                        return@launch
                    }
                }
            }
    }

    /**
     * مزامنة يدوية عند الحاجة.
     */
    fun syncNow() {

        if (
            !started ||
            lastNetworkState !=
                RoyalTVNetworkState.ONLINE
        ) {
            return
        }

        syncOfflineCommands()
    }

    private fun cancelReconnect() {

        reconnectJob?.cancel()

        reconnectJob =
            null
    }

    private fun cancelSync() {

        syncJob?.cancel()

        syncJob =
            null
    }

    fun getNetworkState():
        RoyalTVNetworkState {

        return lastNetworkState
    }

    fun isStarted(): Boolean {

        return started
    }

    fun getDeviceId(): String {

        return deviceId
    }

    /**
     * إيقاف مدير الاتصال.
     *
     * لا يمسح الأوامر Offline.
     * ولا يمسح حالة الشاشة.
     */
    fun stop() {

        if (!started) {
            return
        }

        started =
            false

        cancelReconnect()
        cancelSync()

        networkMonitor.stop()

        RoyalTVRealtime.stop()

        scope.cancel()
    }
}
