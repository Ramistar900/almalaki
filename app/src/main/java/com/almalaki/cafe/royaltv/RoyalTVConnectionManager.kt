package com.almalaki.cafe.royaltv

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

private val ROYAL_TV_PRIVATE_TOPIC_PATTERN = Regex(
    "^royal-tv:[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
    RegexOption.IGNORE_CASE
)

class RoyalTVConnectionManager(
    context: Context
) {

    private val appContext = context.applicationContext

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private val networkMonitor = RoyalTVNetworkMonitor(appContext)

    private var reconnectJob: Job? = null
    private var syncJob: Job? = null
    private var deviceId: String = ""
    private var ownerAccessToken: String = ""
    private var started = false
    private var lastNetworkState = RoyalTVNetworkState.OFFLINE

    init {
        RoyalTVOfflineCommandStore.initialize(appContext)
    }

    /**
     * Owner-side command manager.
     * targetDeviceId parameters below now mean a server-issued private
     * channel topic, never the physical TV's local display ID.
     */
    fun start(
        ownerAccessToken: String = "",
        onCommand: (RoyalTVCommand) -> Unit = {},
        onConnectionStateChanged: (RoyalTVNetworkState) -> Unit = {}
    ) {
        if (started) {
            this.ownerAccessToken = ownerAccessToken.trim()
            return
        }

        this.ownerAccessToken = ownerAccessToken.trim()
        started = true

        // Kept for compatibility with diagnostics only. This value is never
        // used as an authorization identity or Realtime topic.
        deviceId = RoyalTVDeviceIdentity.getDeviceId(appContext)

        networkMonitor.start { state ->
            lastNetworkState = state
            onConnectionStateChanged(state)

            when (state) {
                RoyalTVNetworkState.ONLINE -> scheduleReconnect()
                RoyalTVNetworkState.OFFLINE -> {
                    cancelReconnect()
                    cancelSync()
                    RoyalTVRealtime.stop()
                }
            }
        }
    }

    /**
     * Wait for network recovery, then replay only queued server-issued private
     * channel topics. No owner-side public subscription is ever opened.
     */
    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(2_000L)

            if (!started ||
                ownerAccessToken.isBlank() ||
                lastNetworkState != RoyalTVNetworkState.ONLINE
            ) {
                return@launch
            }

            syncOfflineCommands()
        }
    }

    /**
     * targetDeviceId must be the server-issued channel topic
     * (royal-tv:<server-device-uuid>), not a legacy display identifier.
     *
     * true  = sent successfully.
     * false = not sent; the command is retained only when it can safely be
     *         replayed later using the private topic.
     */
    suspend fun sendCommand(
        targetDeviceId: String,
        command: RoyalTVCommand
    ): Boolean {
        val privateTopic = targetDeviceId.trim()
        if (!privateTopic.matches(ROYAL_TV_PRIVATE_TOPIC_PATTERN)) {
            android.util.Log.e(
                "RoyalTVConnection",
                "Rejected non-private or invalid ROYAL TV target"
            )
            return false
        }

        if (ownerAccessToken.isBlank()) {
            android.util.Log.e(
                "RoyalTVConnection",
                "Owner authentication is required before queuing or sending"
            )
            return false
        }

        if (!started ||
            lastNetworkState != RoyalTVNetworkState.ONLINE
        ) {
            RoyalTVOfflineCommandStore.enqueue(
                targetDeviceId = privateTopic,
                command = command
            )
            return false
        }

        val sent = RoyalTVRealtime.sendCommand(
            ownerAccessToken = ownerAccessToken,
            channelTopic = privateTopic,
            command = command
        )

        if (!sent) {
            RoyalTVOfflineCommandStore.enqueue(
                targetDeviceId = privateTopic,
                command = command
            )
        }

        return sent
    }

    /**
     * Replay persisted commands only while the current owner session is
     * present. Failed commands remain in the queue.
     */
    private fun syncOfflineCommands() {
        syncJob?.cancel()
        syncJob = scope.launch {
            if (ownerAccessToken.isBlank()) return@launch

            val pendingCommands =
                RoyalTVOfflineCommandStore.getPendingCommands()

            for (pendingCommand in pendingCommands) {
                if (!started ||
                    ownerAccessToken.isBlank() ||
                    lastNetworkState != RoyalTVNetworkState.ONLINE
                ) {
                    return@launch
                }

                if (!pendingCommand.targetDeviceId.matches(
                        ROYAL_TV_PRIVATE_TOPIC_PATTERN
                    )
                ) {
                    // Do not replay old commands addressed by public/local IDs.
                    continue
                }

                val sent = RoyalTVRealtime.sendCommand(
                    ownerAccessToken = ownerAccessToken,
                    channelTopic = pendingCommand.targetDeviceId,
                    command = pendingCommand.command
                )

                if (sent) {
                    RoyalTVOfflineCommandStore.remove(pendingCommand)
                } else {
                    return@launch
                }
            }
        }
    }

    fun syncNow() {
        if (!started ||
            ownerAccessToken.isBlank() ||
            lastNetworkState != RoyalTVNetworkState.ONLINE
        ) {
            return
        }
        syncOfflineCommands()
    }

    private fun cancelReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
    }

    private fun cancelSync() {
        syncJob?.cancel()
        syncJob = null
    }

    fun getNetworkState(): RoyalTVNetworkState = lastNetworkState

    fun isStarted(): Boolean = started

    /**
     * Local display ID retained for diagnostics only.
     */
    fun getDeviceId(): String = deviceId

    fun stop() {
        if (!started) return

        started = false
        ownerAccessToken = ""

        cancelReconnect()
        cancelSync()

        networkMonitor.stop()
        RoyalTVRealtime.stop()
        scope.cancel()
    }
}
