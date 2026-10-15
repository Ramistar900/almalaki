package com.almalaki.royaltv

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.net.wifi.p2p.WifiP2pInfo
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun RoyalTVWiFiDirectHostPanel(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var status by remember {
        mutableStateOf("جهّز اتصال Wi-Fi Direct لربط هاتف المالك.")
    }

    var address by remember {
        mutableStateOf("")
    }

    val manager = remember(context) {
        RoyalTVWiFiDirectManager(context)
    }

    val listener = remember(manager) {
        object : RoyalTVWiFiDirectManager.Listener {

            override fun onConnectionInfoAvailable(
                info: WifiP2pInfo
            ) {
                if (info.groupFormed) {
                    val host = info.groupOwnerAddress
                        ?.hostAddress
                        .orEmpty()

                    address = host

                    status = if (info.isGroupOwner) {
                        "جهاز ROYAL TV جاهز لاستقبال اتصال المرسل."
                    } else {
                        "اتصال Wi-Fi Direct قائم؛ تحقق من الجهاز المالك للمجموعة."
                    }
                } else {
                    status = "لم يكتمل الاتصال بعد. انتظر أو أعد المحاولة."
                }
            }

            override fun onOperationResult(
                operation: String,
                success: Boolean,
                reason: String
            ) {
                if (!success) {
                    status = "عملية $operation لم تنجح: $reason"
                } else if (operation == "createGroup") {
                    status = "قُبل طلب إنشاء المجموعة؛ جارٍ انتظار جاهزيتها."
                    manager.requestConnectionInfo()
                }
            }

            override fun onWifiDirectStateChanged(
                enabled: Boolean
            ) {
                if (!enabled) {
                    status = "فعّل Wi-Fi من إعدادات جهاز الشاشة أولًا."
                }
            }
        }
    }

    fun startHost() {
        address = ""

        if (!manager.initialize(listener)) {
            status = "تعذّرت تهيئة Wi-Fi Direct. تحقق من الصلاحيات."
            return
        }

        status = "جارٍ إنشاء اتصال مباشر..."

        if (!manager.createGroup()) {
            status = "تعذّر طلب إنشاء المجموعة. تحقق من Wi-Fi Direct."
        }
    }

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.NEARBY_WIFI_DEVICES
    } else {
        Manifest.permission.ACCESS_FINE_LOCATION
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startHost()
            } else {
                status = "يجب السماح بصلاحية الأجهزة القريبة أو الموقع حسب إصدار Android."
            }
        }

    DisposableEffect(manager) {
        onDispose {
            manager.release()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED

                if (granted) {
                    startHost()
                } else {
                    permissionLauncher.launch(permission)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFB99A52),
                contentColor = Color(0xFF080C12)
            )
        ) {
            Text(
                text = "تهيئة اتصال Wi-Fi Direct",
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = status,
            color = Color(0xFFDCE5EE),
            fontSize = 13.sp
        )

        if (address.isNotBlank()) {
            Text(
                text = "عنوان المجموعة: $address",
                color = Color(0xFFB99A52),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "منفذ استقبال الفيديو: 39872",
            color = Color(0xFFDCE5EE),
            fontSize = 12.sp
        )
    }
}
