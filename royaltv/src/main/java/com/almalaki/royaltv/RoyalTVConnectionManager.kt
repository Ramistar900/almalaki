
package com.almalaki.royaltv

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ROYAL TV — Connection Manager
 *
 * يدير اكتشاف Wi-Fi Direct، ونقل TCP، وإعادة الاتصال،
 * وطابور الرسائل المحلي، مع تجاهل إشعارات النقل القديم.
 */
class RoyalTVConnectionManager(context: Context) {

    interface Listener {
        fun onStatusChanged(status: String) {}
        fun onPeersFound(peers: List<WifiP2pDevice>) {}
        fun onConnected() {}
        fun onDisconnected(reason: String) {}
        fun onMessageReceived(message: String) {}
        fun onError(operation: String, reason: String) {}
        fun onPendingMessagesChanged(count: Int) {}
    }

    companion object {
        private const val PREFS = "royal_tv_connection_manager"
        private const val KEY_QUEUE = "pending_messages"
        private const val MAX_PENDING_MESSAGES = 500
    }

    private val appContext = context.applicationContext

    private val scheduler: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor()

    private val stopped = AtomicBoolean(true)

    @Volatile
    private var listener: Listener? = null

    @Volatile
    private var wifiDirect: RoyalTVWiFiDirectManager? = null

    @Volatile
    private var transport: RoyalTVLocalTransport? = null

    @Volatile
    private var latestPeers: List<WifiP2pDevice> = emptyList()

    @Volatile
    private var latestInfo: WifiP2pInfo? = null

    @Volatile
    private var serverStarted = false

    @Volatile
    private var serverStarting = false

    @Volatile
    private var clientConnecting = false

    @Volatile
    private var retryScheduled = false

    @Volatile
    private var connected = false

    /**
     * يتغير الرقم عند استبدال النقل أو إيقافه.
     * أي callback من جيل قديم يتم تجاهله.
     */
    @Volatile
    private var transportGeneration = 0L

    private fun discoveryEnabled(): Boolean =
        RoyalTVConnectionSettings.isDiscoveryEnabled(appContext)

    private fun autoReconnectEnabled(): Boolean =
        RoyalTVConnectionSettings.isAutoReconnectEnabled(appContext)

    private fun connectionPort(): Int =
        RoyalTVConnectionSettings.getPort(appContext)

    private fun retryDelaySeconds(): Long =
        RoyalTVConnectionSettings.getRetrySeconds(appContext)

    @Synchronized
    fun initialize(listener: Listener): Boolean {
        if (scheduler.isShutdown) {
            listener.onError(
                "initialize",
                "مدير الاتصال أُغلق نهائياً؛ أنشئ مديراً جديداً"
            )
            return false
        }

        this.listener = listener
        RoyalTVDeviceRegistry.initialize(appContext)

        if (!RoyalTVConnectionPermissionManager.hasAllPermissions(appContext)) {
            reportError(
                "initialize",
                "يجب منح أذونات الاتصال المطلوبة أولاً"
            )
            return false
        }

        if (wifiDirect != null) {
            reportStatus("READY")
            return true
        }

        val manager = RoyalTVWiFiDirectManager(appContext)
        wifiDirect = manager

        val ready = manager.initialize(
            object : RoyalTVWiFiDirectManager.Listener {

                override fun onWifiDirectStateChanged(enabled: Boolean) {
                    reportStatus(
                        if (enabled) "WIFI_DIRECT_ENABLED"
                        else "WIFI_DIRECT_DISABLED"
                    )

                    if (enabled && !stopped.get()) {
                        scheduleDiscovery()
                    }
                }

                override fun onPeersAvailable(devices: List<WifiP2pDevice>) {
                    latestPeers = devices.toList()
                    this@RoyalTVConnectionManager.listener
                        ?.onPeersFound(devices)

                    if (!stopped.get()) {
                        connectToDiscoveredPeer()
                    }
                }

                override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
                    latestInfo = info

                    if (!stopped.get() && info.groupFormed) {
                        prepareTransport(info)
                    }
                }

                override fun onOperationResult(
                    operation: String,
                    success: Boolean,
                    reason: String
                ) {
                    reportStatus("$operation:$reason")

                    if (!success) {
                        reportError(operation, reason)
                    }
                }
            }
        )

        if (!ready) {
            wifiDirect = null
            manager.release()
            return false
        }

        reportStatus("READY")
        return true
    }

    @Synchronized
    fun start(): Boolean {
        val manager = wifiDirect

        if (manager == null) {
            reportError("start", "يجب تنفيذ initialize أولاً")
            return false
        }

        if (!RoyalTVConnectionPermissionManager.hasAllPermissions(appContext)) {
            reportError("start", "أذونات الاتصال غير مكتملة")
            return false
        }

        if (scheduler.isShutdown) {
            reportError("start", "مدير الاتصال أُغلق نهائياً")
            return false
        }

        if (!stopped.get()) {
            reportStatus("ALREADY_STARTED")
            return true
        }

        stopped.set(false)
        reportStatus("DISCOVERY_STARTING")

        when (RoyalTVDeviceRoleManager.getRole(appContext)) {
            RoyalTVDeviceRole.DISPLAY -> {
                manager.createGroup()
                scheduleDiscovery(initialAttempt = true)
            }

            RoyalTVDeviceRole.CONTROLLER,
            RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY -> {
                if (discoveryEnabled()) {
                    manager.discoverPeers()
                    scheduleDiscovery(initialAttempt = true)
                } else {
                    reportStatus("DISCOVERY_DISABLED")
                }
            }
        }

        return true
    }

    fun discoverNow(): Boolean {
        if (stopped.get()) {
            reportError("discoverNow", "مدير الاتصال متوقف")
            return false
        }

        if (!discoveryEnabled()) {
            reportStatus("DISCOVERY_DISABLED")
            return false
        }

        return wifiDirect?.discoverPeers() ?: false
    }

    fun getDiscoveredPeers(): List<WifiP2pDevice> =
        latestPeers.toList()

    /**
     * true تعني أن الرسالة أُرسلت فوراً.
     * false تعني أنها لم تُرسل فوراً؛ وقد تكون حُفظت بالطابور.
     */
    fun sendMessage(message: String): Boolean {
        if (message.isBlank()) return false

        val activeTransport = transport

        if (
            connected &&
            activeTransport != null &&
            activeTransport.isConnected()
        ) {
            if (activeTransport.sendMessage(message)) {
                return true
            }
        }

        val saved = enqueue(message)

        if (saved) {
            reportStatus("MESSAGE_QUEUED")

            if (autoReconnectEnabled()) {
                scheduleDiscovery()
            }
        }

        return false
    }

    fun sendCommand(command: RoyalTVCommand): Boolean {
        val envelope = JSONObject().apply {
            put("protocol", "ROYAL_TV_1")
            put("deviceId", RoyalTVDeviceIdentity.getDeviceId(appContext))
            put("deviceName", RoyalTVDeviceIdentity.getDeviceName(appContext))
            put("type", command.type.name)
            put("payload", command.payload)
            put("createdAt", command.createdAt)
        }

        return sendMessage(envelope.toString())
    }

    fun getPendingMessageCount(): Int = readQueue().length()

    @Synchronized
    fun stop() {
        stopped.set(true)

        // إبطال إشعارات النقل قبل تحريره.
        transportGeneration++

        connected = false
        serverStarted = false
        serverStarting = false
        clientConnecting = false
        retryScheduled = false

        val oldTransport = transport
        transport = null
        oldTransport?.release()

        val oldWifiDirect = wifiDirect
        wifiDirect = null
        oldWifiDirect?.release()

        latestInfo = null
        latestPeers = emptyList()

        reportStatus("STOPPED")
    }

    @Synchronized
    fun release() {
        stop()
        scheduler.shutdownNow()
        listener = null
    }

    private fun connectToDiscoveredPeer() {
        if (stopped.get() || connected || clientConnecting) return
        if (RoyalTVDeviceRoleManager.isDisplayOnly(appContext)) return
        if (!discoveryEnabled()) return

        val manager = wifiDirect ?: return

        val candidate = latestPeers.firstOrNull {
            it.deviceAddress.isNotBlank()
        } ?: return

        reportStatus("PEER_FOUND:${candidate.deviceName}")
        manager.connect(candidate)
    }

    private fun prepareTransport(info: WifiP2pInfo) {
        if (stopped.get() || !info.groupFormed) return

        if (info.isGroupOwner) {
            startTransportServer()
        } else {
            val address = info.groupOwnerAddress?.hostAddress

            if (!address.isNullOrBlank()) {
                connectTransportClient(address)
            } else {
                reportError(
                    "prepareTransport",
                    "لم يقدم النظام عنوان مالك مجموعة Wi-Fi Direct"
                )
                scheduleDiscovery()
            }
        }
    }

    @Synchronized
    private fun startTransportServer() {
        if (stopped.get() || serverStarted || serverStarting) return

        // أبطل callbacks الخاصة بالنقل السابق قبل تحريره.
        transportGeneration++

        val oldTransport = transport
        transport = null
        oldTransport?.release()

        val newTransport = createTransport()
        transport = newTransport
        serverStarting = true

        val port = connectionPort()

        if (newTransport.startServer(port)) {
            reportStatus("TCP_SERVER_STARTING:$port")
        } else {
            serverStarting = false
            serverStarted = false

            if (transport === newTransport) {
                transport = null
            }

            transportGeneration++
            newTransport.release()

            reportError(
                "startTransportServer",
                "تعذر بدء مهمة خادم TCP على المنفذ $port"
            )
            scheduleDiscovery()
        }
    }

    @Synchronized
    private fun connectTransportClient(host: String) {
        if (stopped.get() || connected || clientConnecting) return

        val existing = transport

        if (existing != null && existing.isConnected()) {
            connected = true
            return
        }

        // أبطل callbacks الخاصة بالنقل السابق.
        transportGeneration++
        transport = null
        existing?.release()

        val newTransport = createTransport()
        transport = newTransport
        clientConnecting = true

        val port = connectionPort()
        reportStatus("TCP_CONNECTING:$host:$port")

        if (!newTransport.connect(host, port)) {
            clientConnecting = false

            if (transport === newTransport) {
                transport = null
                transportGeneration++
            }

            newTransport.release()
            scheduleDiscovery()
        }
    }

    /**
     * كل callback يتحقق من جيل النقل قبل تعديل الحالة.
     */
    private fun createTransport(): RoyalTVLocalTransport {
        val generation = ++transportGeneration

        fun isCurrentTransport(): Boolean =
            !stopped.get() && generation == transportGeneration

        return RoyalTVLocalTransport(
            object : RoyalTVLocalTransport.Listener {

                override fun onServerStarted(port: Int) {
                    if (!isCurrentTransport()) return

                    serverStarting = false
                    serverStarted = true
                    reportStatus("TCP_SERVER_READY:$port")
                }

                override fun onServerStopped(reason: String) {
                    if (generation != transportGeneration) return

                    serverStarting = false
                    serverStarted = false

                    if (stopped.get()) return

                    reportStatus("TCP_SERVER_STOPPED")

                    if (autoReconnectEnabled()) {
                        scheduleDiscovery()
                    }
                }

                override fun onConnected() {
                    if (!isCurrentTransport()) return

                    connected = true
                    clientConnecting = false

                    val id = RoyalTVDeviceIdentity.getDeviceId(appContext)

                    RoyalTVDeviceRegistry.registerDevice(
                        RoyalTVDevice(
                            deviceId = id,
                            deviceName = RoyalTVDeviceIdentity
                                .getDeviceName(appContext),
                            role = RoyalTVDeviceRoleManager.getRole(appContext),
                            isOnline = true
                        )
                    )

                    reportStatus(
                        if (serverStarted) "TCP_CLIENT_CONNECTED"
                        else "CONNECTED"
                    )

                    listener?.onConnected()
                    flushQueue()
                }

                override fun onDisconnected(reason: String) {
                    if (generation != transportGeneration) return

                    connected = false
                    clientConnecting = false

                    val activeTransport = transport
                    val serverStillListening =
                        serverStarted &&
                        activeTransport?.isServerListening() == true

                    if (serverStillListening) {
                        // انقطع العميل، لكن خادم الشاشة ما زال يعمل.
                        reportStatus("TCP_SERVER_WAITING_FOR_CLIENT")
                    } else {
                        reportStatus("DISCONNECTED")

                        val id = RoyalTVDeviceIdentity
                            .getDeviceId(appContext)

                        RoyalTVDeviceRegistry.updateOnlineState(id, false)
                    }

                    listener?.onDisconnected(reason)

                    if (!stopped.get() && autoReconnectEnabled()) {
                        scheduleDiscovery()
                    }
                }

                override fun onMessageReceived(message: String) {
                    if (!isCurrentTransport()) return
                    listener?.onMessageReceived(message)
                }

                override fun onError(operation: String, reason: String) {
                    if (!isCurrentTransport()) return

                    if (operation == "connect") {
                        clientConnecting = false
                    }

                    if (
                        operation == "startServer" ||
                        operation == "accept"
                    ) {
                        serverStarted = false
                        serverStarting = false
                    }

                    listener?.onError(operation, reason)
                    reportStatus("ERROR:$operation")

                    if (!stopped.get() && autoReconnectEnabled()) {
                        scheduleDiscovery()
                    }
                }
            }
        )
    }

    /**
     * المحاولة الأولية لا تتطلب تفعيل إعادة الاتصال.
     * المحاولات التالية تحترم إعداد إعادة الاتصال التلقائية.
     */
    @Synchronized
    private fun scheduleDiscovery(initialAttempt: Boolean = false) {
        if (stopped.get() || retryScheduled) return

        if (!initialAttempt && !autoReconnectEnabled()) {
            reportStatus("AUTO_RECONNECT_DISABLED")
            return
        }

        if (scheduler.isShutdown) return

        retryScheduled = true

        try {
            scheduler.schedule({
                retryScheduled = false

                if (stopped.get()) return@schedule

                val manager = wifiDirect ?: return@schedule

                if (connected) {
                    flushQueue()
                    return@schedule
                }

                val info = latestInfo

                if (info?.groupFormed == true) {
                    prepareTransport(info)
                } else if (
                    RoyalTVDeviceRoleManager.isDisplayOnly(appContext)
                ) {
                    manager.createGroup()
                } else if (discoveryEnabled()) {
                    manager.discoverPeers()
                    manager.requestPeers()
                } else {
                    reportStatus("DISCOVERY_DISABLED")
                }
            }, retryDelaySeconds(), TimeUnit.SECONDS)
        } catch (_: Exception) {
            retryScheduled = false
        }
    }

    private fun flushQueue() {
        if (!connected) return

        val activeTransport = transport ?: return
        val queue = readQueue()
        val remaining = JSONArray()

        for (index in 0 until queue.length()) {
            val message = queue.optString(index, "")
            if (message.isBlank()) continue

            if (!activeTransport.sendMessage(message)) {
                remaining.put(message)

                for (rest in index + 1 until queue.length()) {
                    remaining.put(queue.optString(rest, ""))
                }

                break
            }
        }

        saveQueue(remaining)
        listener?.onPendingMessagesChanged(remaining.length())
    }

    @Synchronized
    private fun enqueue(message: String): Boolean {
        val queue = readQueue()

        if (queue.length() >= MAX_PENDING_MESSAGES) {
            reportError(
                "enqueue",
                "وصلت قائمة الرسائل المحفوظة إلى الحد الأقصى"
            )
            return false
        }

        queue.put(message)
        saveQueue(queue)
        listener?.onPendingMessagesChanged(queue.length())
        return true
    }

    private fun readQueue(): JSONArray {
        val prefs = appContext.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

        val raw = prefs.getString(KEY_QUEUE, null) ?: return JSONArray()

        return try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun saveQueue(queue: JSONArray) {
        appContext.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).edit()
            .putString(KEY_QUEUE, queue.toString())
            .apply()
    }

    private fun reportStatus(status: String) {
        listener?.onStatusChanged(status)
    }

    private fun reportError(operation: String, reason: String) {
        listener?.onError(operation, reason)
    }
}
