
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
 * يربط اكتشاف Wi-Fi Direct بالنقل المحلي TCP.
 * يدير إعادة المحاولة ويحفظ الرسائل غير المرسلة محلياً.
 *
 * يجب استدعاء initialize بعد منح أذونات Wi-Fi Direct.
 */
class RoyalTVConnectionManager(
    context: Context
) {

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
        private const val RETRY_SECONDS = 5L
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
    private var clientConnecting = false

    @Volatile
    private var retryScheduled = false

    @Volatile
    private var connected = false

    /**
     * تهيئة سجل الأجهزة ومدير Wi-Fi Direct.
     * تعيد false إذا لم تتوفر الأذونات أو تعذرت التهيئة.
     */
    @Synchronized
    fun initialize(listener: Listener): Boolean {
        this.listener = listener

        RoyalTVDeviceRegistry.initialize(appContext)

        if (!RoyalTVConnectionPermissionManager.hasAllPermissions(appContext)) {
            reportError(
                "initialize",
                "يجب منح أذونات الاتصال المطلوبة أولاً"
            )
            return false
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

                override fun onPeersAvailable(
                    devices: List<WifiP2pDevice>
                ) {
                    latestPeers = devices
                    this@RoyalTVConnectionManager.listener
                        ?.onPeersFound(devices)

                    if (!stopped.get()) {
                        connectToDiscoveredPeer()
                    }
                }

                override fun onConnectionInfoAvailable(
                    info: WifiP2pInfo
                ) {
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

    /**
     * بدء الاكتشاف التلقائي.
     *
     * جهاز العرض فقط يحاول إنشاء مجموعة.
     * جهاز التحكم يبحث عن الأجهزة ويطلب الاتصال بها.
     * يظل Android مسؤولاً عن موافقات Wi-Fi Direct الفعلية.
     */
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

        stopped.set(false)
        reportStatus("DISCOVERY_STARTING")

        when (RoyalTVDeviceRoleManager.getRole(appContext)) {
            RoyalTVDeviceRole.DISPLAY -> {
                manager.createGroup()
            }

            RoyalTVDeviceRole.CONTROLLER,
            RoyalTVDeviceRole.CONTROLLER_AND_DISPLAY -> {
                manager.discoverPeers()
            }
        }

        scheduleDiscovery()
        return true
    }

    /**
     * طلب تحديث قائمة الأجهزة المكتشفة.
     */
    fun discoverNow(): Boolean {
        if (stopped.get()) {
            reportError("discoverNow", "مدير الاتصال متوقف")
            return false
        }

        return wifiDirect?.discoverPeers() ?: false
    }

    /**
     * إتاحة قائمة الأجهزة المكتشفة للواجهة أو المتحكم.
     */
    fun getDiscoveredPeers(): List<WifiP2pDevice> =
        latestPeers.toList()

    /**
     * إرسال رسالة. تحفظ الرسالة إذا تعذر إرسالها.
     *
     * هذه الدالة تنقل نص الرسالة فقط؛
     * تفسير أوامر ROYAL TV يحتاج معالجاً على الطرف المستقبل.
     */
    fun sendMessage(message: String): Boolean {
        if (message.isBlank()) return false

        val activeTransport = transport

        if (connected &&
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
            scheduleDiscovery()
        }

        return false
    }

    /**
     * إرسال أمر ROYAL TV بتنسيق JSON موحد.
     */
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

    /**
     * عدد الرسائل التي ما زالت محفوظة محلياً.
     */
    fun getPendingMessageCount(): Int =
        readQueue().length()

    /**
     * إيقاف الاكتشاف وإغلاق اتصال النقل.
     * لا يحذف الرسائل المحفوظة.
     */
    @Synchronized
    fun stop() {
        stopped.set(true)
        connected = false
        serverStarted = false
        clientConnecting = false
        retryScheduled = false

        transport?.release()
        transport = null

        wifiDirect?.release()
        wifiDirect = null

        latestInfo = null
        latestPeers = emptyList()

        reportStatus("STOPPED")
    }

    /**
     * إيقاف نهائي للمدير.
     */
    fun release() {
        stop()
        scheduler.shutdownNow()
        listener = null
    }

    private fun connectToDiscoveredPeer() {
        if (stopped.get() || connected) return

        if (RoyalTVDeviceRoleManager.isDisplayOnly(appContext)) {
            return
        }

        val manager = wifiDirect ?: return

        val candidate = latestPeers.firstOrNull {
            it.deviceAddress.isNotBlank()
        } ?: return

        reportStatus("PEER_FOUND:${candidate.deviceName}")
        manager.connect(candidate)
    }

    private fun prepareTransport(info: WifiP2pInfo) {
        if (stopped.get()) return

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
        if (stopped.get() || serverStarted) return

        transport?.release()

        val newTransport = createTransport()
        transport = newTransport

        if (newTransport.startServer()) {
            serverStarted = true
            clientConnecting = false
            reportStatus("TCP_SERVER_STARTING")
        } else {
            transport = null
            reportError("startTransportServer", "تعذر بدء خادم TCP")
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

        existing?.release()

        val newTransport = createTransport()
        transport = newTransport
        clientConnecting = true

        reportStatus("TCP_CONNECTING")

        if (!newTransport.connect(host)) {
            clientConnecting = false
            transport = null
            scheduleDiscovery()
        }
    }

    private fun createTransport(): RoyalTVLocalTransport {
        return RoyalTVLocalTransport(
            object : RoyalTVLocalTransport.Listener {

                override fun onConnected() {
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

                    reportStatus("CONNECTED")
                    listener?.onConnected()
                    flushQueue()
                }

                override fun onDisconnected(reason: String) {
                    connected = false
                    clientConnecting = false

                    val id = RoyalTVDeviceIdentity.getDeviceId(appContext)
                    RoyalTVDeviceRegistry.updateOnlineState(id, false)

                    listener?.onDisconnected(reason)
                    reportStatus("DISCONNECTED")

                    scheduleDiscovery()
                }

                override fun onMessageReceived(message: String) {
                    listener?.onMessageReceived(message)
                }

                override fun onError(operation: String, reason: String) {
                    if (operation == "connect") {
                        clientConnecting = false
                    }

                    listener?.onError(operation, reason)
                    reportStatus("ERROR:$operation")
                    scheduleDiscovery()
                }
            }
        )
    }

    private fun scheduleDiscovery() {
        if (stopped.get() || retryScheduled) return

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
                } else {
                    manager.discoverPeers()
                    manager.requestPeers()
                }
            }, RETRY_SECONDS, TimeUnit.SECONDS)
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
