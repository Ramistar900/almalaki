package com.almalaki.cafe.royaltv

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private val SenderWifiGold = Color(0xFFD4AF37)
private val SenderWifiCream = Color(0xFFF5F0E5)

@SuppressLint("MissingPermission")
@Composable
fun RoyalTVSenderWiFiDirectPanel(
    modifier: Modifier = Modifier,
    onReceiverHostFound: (String) -> Unit
) {
    val context = LocalContext.current

    val p2pManager = remember(context) {
        context.applicationContext.getSystemService(
            Context.WIFI_P2P_SERVICE
        ) as? WifiP2pManager
    }

    val channel = remember(p2pManager) {
        p2pManager?.initialize(
            context.applicationContext,
            Looper.getMainLooper(),
            null
        )
    }

    val peersState = remember {
        mutableStateOf<List<WifiP2pDevice>>(emptyList())
    }
    val statusState = remember {
        mutableStateOf("اضغط على البحث لاكتشاف شاشة ROYAL TV.")
    }
    val hostCallback = rememberUpdatedState(onReceiverHostFound)

    var status by statusState
    val peers by peersState

    val permission = remember {
        if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
    }

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestConnectionInfo() {
        val manager = p2pManager ?: return
        val activeChannel = channel ?: return

        try {
            manager.requestConnectionInfo(activeChannel) { info: WifiP2pInfo ->
                
if (info.groupFormed) {
    if (info.isGroupOwner) {
        status =
            "الهاتف أصبح مالك مجموعة الاتصال. " +
                "أعد تهيئة Wi-Fi Direct من شاشة ROYAL TV " +
                "ثم أعد الاتصال."
    } else {
        val host = info.groupOwnerAddress
            ?.hostAddress
            .orEmpty()

        if (host.isNotBlank()) {
            status = "تم الاتصال. عنوان المجموعة: $host"
            hostCallback.value.invoke(host)
        } else {
            status = "تم الاتصال، لكن عنوان المجموعة لم يظهر بعد."
        }
    }
} else {
    status = "لم يكتمل الاتصال بعد. انتظر قليلًا."
}

            }
        } catch (_: SecurityException) {
            status = "تحقق من صلاحية الأجهزة القريبة أو الموقع."
        }
    }

    val receiver = remember(context, p2pManager, channel) {
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                when (intent?.action) {
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        val manager = p2pManager ?: return
                        val activeChannel = channel ?: return

                        try {
                            manager.requestPeers(activeChannel) { list ->
                                peersState.value =
                                    list.deviceList.toList()

                                if (peersState.value.isEmpty()) {
                                    statusState.value =
                                        "لم يُعثر على أجهزة بعد. تأكد من تهيئة Wi-Fi Direct على الشاشة."
                                } else {
                                    statusState.value =
                                        "تم العثور على ${peersState.value.size} جهاز. اختر شاشة ROYAL TV."
                                }
                            }
                        } catch (_: SecurityException) {
                            statusState.value =
                                "لا توجد صلاحية لاكتشاف الأجهزة القريبة."
                        }
                    }

                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        requestConnectionInfo()
                    }

                    WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                        val enabled = intent.getIntExtra(
                            WifiP2pManager.EXTRA_WIFI_STATE,
                            WifiP2pManager.WIFI_P2P_STATE_DISABLED
                        ) == WifiP2pManager.WIFI_P2P_STATE_ENABLED

                        if (!enabled) {
                            statusState.value =
                                "فعّل Wi-Fi على الهاتف والشاشة."
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(receiver) {
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
                context.registerReceiver(
                    receiver,
                    filter,
                    Context.RECEIVER_EXPORTED
                )
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, filter)
            }
        } catch (_: Exception) {
            statusState.value =
                "تعذّر تفعيل مراقبة اتصال Wi-Fi Direct."
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
                // ربما لم يُسجل المستقبل.
            }
        }
    }

    fun startDiscovery() {
        val manager = p2pManager
        val activeChannel = channel

        if (manager == null || activeChannel == null) {
            status = "هذا الجهاز لا يدعم Wi-Fi Direct أو تعذرت تهيئته."
            return
        }

        if (!hasPermission()) {
            status = "اسمح بصلاحية الأجهزة القريبة أولًا."
            return
        }

        try {
            manager.discoverPeers(
                activeChannel,
                object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        status =
                            "بدأ البحث عن الأجهزة القريبة. انتظر ظهور الشاشة."
                    }

                    override fun onFailure(reason: Int) {
                        status =
                            "فشل البحث عن الأجهزة. رمز الخطأ: $reason"
                    }
                }
            )
        } catch (_: SecurityException) {
            status = "رفض Android صلاحية البحث عن الأجهزة."
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startDiscovery()
            } else {
                status =
                    "يجب منح صلاحية الأجهزة القريبة لاستخدام Wi-Fi Direct."
            }
        }

    fun connectTo(device: WifiP2pDevice) {
        val manager = p2pManager
        val activeChannel = channel

        if (manager == null || activeChannel == null) {
            status = "تعذرت تهيئة Wi-Fi Direct."
            return
        }

        if (!hasPermission()) {
            status = "أعد منح صلاحية الأجهزة القريبة."
            return
        }

        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }

        try {
            status = "جارٍ طلب الاتصال بالجهاز المحدد..."

            manager.connect(
                activeChannel,
                config,
                object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        status =
                            "قُبل طلب الاتصال. انتظر اكتمال الاتصال."
                        requestConnectionInfo()
                    }

                    override fun onFailure(reason: Int) {
                        status =
                            "تعذر الاتصال. رمز الخطأ: $reason"
                    }
                }
            )
        } catch (_: SecurityException) {
            status = "رفض Android طلب الاتصال بسبب الصلاحيات."
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                if (hasPermission()) {
                    startDiscovery()
                } else {
                    permissionLauncher.launch(permission)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = SenderWifiGold,
                contentColor = Color(0xFF050505)
            )
        ) {
            Text(
                text = "البحث عن شاشة ROYAL TV",
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = status,
            color = SenderWifiCream,
            fontSize = 13.sp
        )

        peers.forEach { device ->
            Button(
                onClick = { connectTo(device) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF292929),
                    contentColor = SenderWifiCream
                )
            ) {
                Text(
                    text = "اتصال: ${
                        device.deviceName.ifBlank {
                            device.deviceAddress
                        }
                    }"
                )
            }
        }
    }
}
