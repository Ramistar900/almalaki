
package com.almalaki.royaltv

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build

/**
 * ROYAL TV — Bluetooth Manager
 * اكتشاف الأجهزة وطلب الاقتران عبر نظام Android.
 *
 * لا ينقل هذا الملف الفيديو أو أوامر ROYAL TV.
 * نقل البيانات سيضاف في طبقة الاتصال التالية.
 */
class RoyalTVBluetoothManager(context: Context) {

    interface Listener {
        fun onBluetoothStateChanged(enabled: Boolean) {}
        fun onDeviceFound(device: BluetoothDevice) {}
        fun onDiscoveryStateChanged(discovering: Boolean) {}
        fun onPairingStateChanged(
            device: BluetoothDevice,
            bondState: Int
        ) {}
        fun onOperationResult(
            operation: String,
            success: Boolean,
            reason: String
        ) {}
    }

    private val appContext = context.applicationContext

    private val bluetoothManager =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE)
                as BluetoothManager

    private val adapter: BluetoothAdapter?
        get() = bluetoothManager.adapter

    private var listener: Listener? = null
    private var receiverRegistered = false

    private val discoveredDevices =
        linkedMapOf<String, BluetoothDevice>()

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = getDevice(intent) ?: return
                    val address = safeAddress(device) ?: return

                    discoveredDevices[address] = device
                    listener?.onDeviceFound(device)
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    listener?.onDiscoveryStateChanged(true)
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    listener?.onDiscoveryStateChanged(false)
                }

                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )

                    listener?.onBluetoothStateChanged(
                        state == BluetoothAdapter.STATE_ON
                    )
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device = getDevice(intent) ?: return
                    val state = intent.getIntExtra(
                        BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.ERROR
                    )

                    listener?.onPairingStateChanged(device, state)
                }
            }
        }
    }

    fun initialize(listener: Listener) {
        this.listener = listener

        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            }

            if (Build.VERSION.SDK_INT >= 33) {
                appContext.registerReceiver(
                    receiver,
                    filter,
                    Context.RECEIVER_EXPORTED
                )
            } else {
                @Suppress("DEPRECATION")
                appContext.registerReceiver(receiver, filter)
            }

            receiverRegistered = true
        }

        if (adapter == null) {
            report("initialize", false, "الجهاز لا يدعم Bluetooth")
            return
        }

        if (hasConnectPermission()) {
            try {
                listener.onBluetoothStateChanged(adapter!!.isEnabled)
            } catch (_: SecurityException) {
                report(
                    "initialize",
                    false,
                    "تعذّر الوصول إلى Bluetooth"
                )
            }
        } else {
            report(
                "initialize",
                false,
                "يلزم منح صلاحية Bluetooth Connect"
            )
        }
    }

    fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            appContext.checkSelfPermission(
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= 23) {
            appContext.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            appContext.checkSelfPermission(
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(): Boolean {
        val bluetooth = adapter

        if (bluetooth == null) {
            report("discover", false, "الجهاز لا يدعم Bluetooth")
            return false
        }

        if (!hasScanPermission()) {
            report("discover", false, "صلاحية البحث غير ممنوحة")
            return false
        }

        if (!hasConnectPermission()) {
            report("discover", false, "صلاحية الاتصال غير ممنوحة")
            return false
        }

        return try {
            if (!bluetooth.isEnabled) {
                report("discover", false, "يرجى تشغيل Bluetooth")
                false
            } else {
                discoveredDevices.clear()

                if (bluetooth.isDiscovering) {
                    bluetooth.cancelDiscovery()
                }

                val started = bluetooth.startDiscovery()

                if (!started) {
                    report(
                        "discover",
                        false,
                        "تعذّر بدء البحث عن الأجهزة"
                    )
                }

                started
            }
        } catch (_: SecurityException) {
            report("discover", false, "رفض Android صلاحية البحث")
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasConnectPermission()) {
            report(
                "pairedDevices",
                false,
                "صلاحية Bluetooth Connect غير ممنوحة"
            )
            return emptyList()
        }

        return try {
            adapter?.bondedDevices?.toList() ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun requestPairing(address: String): Boolean {
        if (!hasConnectPermission()) {
            report("pair", false, "صلاحية Bluetooth Connect غير ممنوحة")
            return false
        }

        val device = try {
            discoveredDevices[address]
                ?: getPairedDevices().firstOrNull {
                    safeAddress(it) == address
                }
                ?: adapter?.getRemoteDevice(address)
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: SecurityException) {
            null
        }

        if (device == null) {
            report("pair", false, "لم يتم العثور على الجهاز")
            return false
        }

        return try {
            when (device.bondState) {
                BluetoothDevice.BOND_BONDED -> {
                    report("pair", true, "الجهاز مقترن بالفعل")
                    true
                }

                BluetoothDevice.BOND_BONDING -> {
                    report("pair", true, "الاقتران قيد التنفيذ")
                    true
                }

                else -> {
                    val started = device.createBond()

                    if (!started) {
                        report("pair", false, "تعذّر بدء الاقتران")
                    }

                    started
                }
            }
        } catch (_: SecurityException) {
            report("pair", false, "رفض Android صلاحية الاقتران")
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun cancelDiscovery() {
        if (!hasScanPermission() || !hasConnectPermission()) return

        try {
            adapter?.let {
                if (it.isDiscovering) it.cancelDiscovery()
            }
        } catch (_: SecurityException) {
            report("cancelDiscovery", false, "تعذّر إيقاف البحث")
        }
    }

    fun getDiscoveredDevices(): List<BluetoothDevice> =
        discoveredDevices.values.toList()

    fun release() {
        cancelDiscovery()

        if (receiverRegistered) {
            try {
                appContext.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
                // تم إلغاء تسجيل المستقبل سابقًا.
            }

            receiverRegistered = false
        }

        discoveredDevices.clear()
        listener = null
    }

    @Suppress("DEPRECATION")
    private fun getDevice(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(
                BluetoothDevice.EXTRA_DEVICE,
                BluetoothDevice::class.java
            )
        } else {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    @SuppressLint("MissingPermission")
    private fun safeAddress(device: BluetoothDevice): String? {
        return try {
            device.address
        } catch (_: SecurityException) {
            null
        }
    }

    private fun report(
        operation: String,
        success: Boolean,
        reason: String
    ) {
        listener?.onOperationResult(operation, success, reason)
    }
}
