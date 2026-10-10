
package com.almalaki.royaltv

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper

/**
 * ROYAL TV — إدارة Wi-Fi Direct.
 *
 * يوفر:
 * - تهيئة Wi-Fi Direct.
 * - اكتشاف الأجهزة القريبة.
 * - إنشاء مجموعة اتصال مباشرة.
 * - طلب الاتصال بجهاز محدد.
 * - متابعة حالة الاتصال ومعلومات المجموعة.
 *
 * لا ينقل الفيديو أو الأوامر بنفسه؛
 * نقل البيانات الفعلي يحتاج طبقة اتصال منفصلة.
 */
class RoyalTVWiFiDirectManager(
    context: Context
) {

    interface Listener {
        fun onWifiDirectStateChanged(enabled: Boolean) {}

        fun onPeersAvailable(
            devices: List<WifiP2pDevice>
        ) {}

        fun onConnectionInfoAvailable(
            info: WifiP2pInfo
        ) {}

        fun onOperationResult(
            operation: String,
            success: Boolean,
            reason: String
        ) {}
    }

    private val appContext =
        context.applicationContext

    private val wifiManager =
        appContext.getSystemService(
            Context.WIFI_P2P_SERVICE
        ) as? WifiP2pManager

    private var channel: WifiP2pManager.Channel? =
        null

    private var listener: Listener? = null

    private var receiverRegistered = false

    private var initialized = false

    private val receiver =
        object : BroadcastReceiver() {

            override fun onReceive(
                context: Context,
                intent: Intent
            ) {
                when (intent.action) {

                    WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                        val state = intent.getIntExtra(
                            WifiP2pManager.EXTRA_WIFI_STATE,
                            WifiP2pManager.WIFI_P2P_STATE_DISABLED
                        )

                        listener?.onWifiDirectStateChanged(
                            state ==
                                WifiP2pManager.WIFI_P2P_STATE_ENABLED
                        )
                    }

                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        requestPeers()
                    }

                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        requestConnectionInfo()
                        requestPeers()
                    }
                }
            }
        }

    /**
     * تهيئة القناة وتسجيل مستقبل أحداث Wi-Fi Direct.
     * يجب استدعاء هذه الدالة بعد منح الصلاحيات اللازمة.
     */
    fun initialize(
        listener: Listener
    ): Boolean {
        this.listener = listener

        if (!hasRequiredPermissions()) {
            report(
                "initialize",
                false,
                "PERMISSION_REQUIRED"
            )
            return false
        }

        val manager = wifiManager ?: run {
            report(
                "initialize",
                false,
                "WIFI_P2P_UNAVAILABLE"
            )
            return false
        }

        if (channel == null) {
            channel = manager.initialize(
                appContext,
                Looper.getMainLooper(),
                null
            )
        }

        if (channel == null) {
            report(
                "initialize",
                false,
                "CHANNEL_INITIALIZATION_FAILED"
            )
            return false
        }

        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(
                    WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION
                )
                addAction(
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION
                )
                addAction(
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
                )
            }

            try {
                if (Build.VERSION.SDK_INT >= 33) {
                    appContext.registerReceiver(
                        receiver,
                        filter,
                        Context.RECEIVER_EXPORTED
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appContext.registerReceiver(
                        receiver,
                        filter
                    )
                }

                receiverRegistered = true
            } catch (_: SecurityException) {
                report(
                    "initialize",
                    false,
                    "RECEIVER_PERMISSION_DENIED"
                )
                return false
            } catch (_: Exception) {
                report(
                    "initialize",
                    false,
                    "RECEIVER_REGISTRATION_FAILED"
                )
                return false
            }
        }

        initialized = true

        report(
            "initialize",
            true,
            "READY"
        )

        return true
    }

    /**
     * التحقق من الصلاحيات المطلوبة حسب إصدار Android.
     */
    fun hasRequiredPermissions(): Boolean {
        val permission =
            if (Build.VERSION.SDK_INT >= 33) {
                Manifest.permission.NEARBY_WIFI_DEVICES
            } else {
                Manifest.permission.ACCESS_FINE_LOCATION
            }

        return appContext.checkSelfPermission(
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * البحث عن أجهزة Wi-Fi Direct القريبة.
     */
    @SuppressLint("MissingPermission")
    fun discoverPeers(): Boolean {
        val activeChannel = readyChannel()
            ?: return false

        if (!hasRequiredPermissions()) {
            report(
                "discoverPeers",
                false,
                "PERMISSION_REQUIRED"
            )
            return false
        }

        try {
            wifiManager?.discoverPeers(
                activeChannel,
                actionListener("discoverPeers")
            )
        } catch (_: SecurityException) {
            report(
                "discoverPeers",
                false,
                "PERMISSION_DENIED"
            )
            return false
        }

        return true
    }

    /**
     * طلب قائمة الأجهزة التي اكتشفها النظام.
     */
    @SuppressLint("MissingPermission")
    fun requestPeers(): Boolean {
        val activeChannel = readyChannel()
            ?: return false

        if (!hasRequiredPermissions()) {
            report(
                "requestPeers",
                false,
                "PERMISSION_REQUIRED"
            )
            return false
        }

        try {
            wifiManager?.requestPeers(
                activeChannel
            ) { peerList: WifiP2pDeviceList ->
                listener?.onPeersAvailable(
                    peerList.deviceList.toList()
                )
            }
        } catch (_: SecurityException) {
            report(
                "requestPeers",
                false,
                "PERMISSION_DENIED"
            )
            return false
        }

        return true
    }

    /**
     * طلب الاتصال بجهاز اكتشفه النظام.
     *
     * هذه الدالة تطلب الاتصال فقط؛
     * قد يتطلب النظام موافقة المستخدم.
     */
    @SuppressLint("MissingPermission")
    fun connect(
        device: WifiP2pDevice
    ): Boolean {
        val activeChannel = readyChannel()
            ?: return false

        if (!hasRequiredPermissions()) {
            report(
                "connect",
                false,
                "PERMISSION_REQUIRED"
            )
            return false
        }

        if (device.deviceAddress.isBlank()) {
            report(
                "connect",
                false,
                "INVALID_DEVICE_ADDRESS"
            )
            return false
        }

        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }

        try {
            wifiManager?.connect(
                activeChannel,
                config,
                actionListener("connect")
            )
        } catch (_: SecurityException) {
            report(
                "connect",
                false,
                "PERMISSION_DENIED"
            )
            return false
        }

        return true
    }

    /**
     * طلب إنشاء مجموعة Wi-Fi Direct.
     *
     * قد يختار Android الجهاز الذي يصبح مالك المجموعة.
     * لا يمكن ضمان إنشاء نقطة اتصال باسم ثابت على كل جهاز.
     */
    @SuppressLint("MissingPermission")
    fun createGroup(): Boolean {
        val activeChannel = readyChannel()
            ?: return false

        if (!hasRequiredPermissions()) {
            report(
                "createGroup",
                false,
                "PERMISSION_REQUIRED"
            )
            return false
        }

        try {
            wifiManager?.createGroup(
                activeChannel,
                actionListener("createGroup")
            )
        } catch (_: SecurityException) {
            report(
                "createGroup",
                false,
                "PERMISSION_DENIED"
            )
            return false
        }

        return true
    }

    /**
     * قراءة معلومات الاتصال الحالي.
     */
    @SuppressLint("MissingPermission")
    fun requestConnectionInfo(): Boolean {
        val activeChannel = readyChannel()
            ?: return false

        if (!hasRequiredPermissions()) {
            report(
                "requestConnectionInfo",
                false,
                "PERMISSION_REQUIRED"
            )
            return false
        }

        try {
            wifiManager?.requestConnectionInfo(
                activeChannel
            ) { info: WifiP2pInfo ->
                listener?.onConnectionInfoAvailable(info)
            }
        } catch (_: SecurityException) {
            report(
                "requestConnectionInfo",
                false,
                "PERMISSION_DENIED"
            )
            return false
        }

        return true
    }

    /**
     * قطع الاتصال بالمجموعة المحلية.
     */
    @SuppressLint("MissingPermission")
    fun disconnect(): Boolean {
        val activeChannel = readyChannel()
            ?: return false

        if (!hasRequiredPermissions()) {
            report(
                "disconnect",
                false,
                "PERMISSION_REQUIRED"
            )
            return false
        }

        try {
            wifiManager?.removeGroup(
                activeChannel,
                actionListener("disconnect")
            )
        } catch (_: SecurityException) {
            report(
                "disconnect",
                false,
                "PERMISSION_DENIED"
            )
            return false
        }

        return true
    }

    /**
     * إيقاف استقبال الأحداث وتحرير الموارد المحلية.
     *
     * لا يوقف هذا بالضرورة المجموعة التي يديرها Android.
     */
    fun release() {
        if (receiverRegistered) {
            try {
                appContext.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
                // المستقبل غير مسجل بالفعل.
            }

            receiverRegistered = false
        }

        channel?.close()
        channel = null
        listener = null
        initialized = false
    }

    private fun readyChannel():
        WifiP2pManager.Channel? {

        if (!initialized) {
            report(
                "operation",
                false,
                "MANAGER_NOT_INITIALIZED"
            )
            return null
        }

        if (!hasRequiredPermissions()) {
            report(
                "operation",
                false,
                "PERMISSION_REQUIRED"
            )
            return null
        }

        return channel
    }

    private fun actionListener(
        operation: String
    ): WifiP2pManager.ActionListener {

        return object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                report(
                    operation,
                    true,
                    "REQUEST_ACCEPTED"
                )
            }

            override fun onFailure(reason: Int) {
                val message = when (reason) {
                    WifiP2pManager.ERROR ->
                        "SYSTEM_ERROR"

                    WifiP2pManager.P2P_UNSUPPORTED ->
                        "P2P_UNSUPPORTED"

                    WifiP2pManager.BUSY ->
                        "SYSTEM_BUSY"

                    WifiP2pManager.NO_SERVICE_REQUESTS ->
                        "NO_SERVICE_REQUESTS"

                    else ->
                        "ERROR_$reason"
                }

                report(
                    operation,
                    false,
                    message
                )
            }
        }
    }

    private fun report(
        operation: String,
        success: Boolean,
        reason: String
    ) {
        listener?.onOperationResult(
            operation,
            success,
            reason
        )
    }
}
