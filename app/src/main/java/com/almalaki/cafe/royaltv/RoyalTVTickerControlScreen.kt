package com.almalaki.cafe.royaltv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val TickerGold = Color(0xFFD4AF37)
private val TickerBlack = Color(0xFF050505)
private val TickerPanel = Color(0xFF111111)
private val TickerWhite = Color(0xFFF5F0E5)

@Composable
fun RoyalTVTickerControlScreen(
    accessToken: String,
    restaurantId: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val connectionManager = remember(accessToken, restaurantId) {
        RoyalTVConnectionManager(context)
    }
    val tickerController = remember(connectionManager) {
        RoyalTVTabletTickerController(connectionManager)
    }

    DisposableEffect(connectionManager, accessToken) {
        connectionManager.start(
            ownerAccessToken = accessToken,
            restaurantId = restaurantId,
            onCommand = {}
        )
        onDispose {
            connectionManager.stop()
        }
    }

    var devices by remember(restaurantId) {
        mutableStateOf<List<RoyalTVManagedDevice>>(emptyList())
    }
    var selectedDevice by remember(restaurantId) {
        mutableStateOf<RoyalTVManagedDevice?>(null)
    }
    var deviceMenuExpanded by remember { mutableStateOf(false) }
    var tickerText by remember { mutableStateOf("") }
    var statusText by remember {
        mutableStateOf("جارٍ تحميل شاشات هذا المطعم...")
    }
    var loadingDevices by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(accessToken, restaurantId) {
        if (accessToken.isBlank() || restaurantId.isBlank()) {
            devices = emptyList()
            selectedDevice = null
            statusText = "تعذر تحديد حساب المالك أو المطعم الحالي."
            return@LaunchedEffect
        }

        loadingDevices = true
        try {
            devices = RoyalTVOwnerDeviceManager.listDevices(
                accessToken = accessToken,
                restaurantId = restaurantId
            ).filter { it.isActive }

            if (selectedDevice?.deviceId !in devices.map { it.deviceId }) {
                selectedDevice = devices.firstOrNull()
            }

            statusText = if (devices.isEmpty()) {
                "لا توجد شاشة نشطة لهذا المطعم. اربط شاشة من إدارة أجهزة ROYAL TV أولًا."
            } else {
                "اختر شاشة مسجلة في المطعم لإرسال أوامر الشريط إليها."
            }
        } catch (error: Exception) {
            devices = emptyList()
            selectedDevice = null
            statusText = error.message ?: "تعذر تحميل الشاشات المسجلة."
        } finally {
            loadingDevices = false
        }
    }

    fun sendTickerCommand(
        action: suspend (String) -> Boolean,
        successMessage: String
    ) {
        val target = selectedDevice
        if (target == null) {
            statusText = "اختر شاشة نشطة ومربوطة بهذا المطعم أولًا."
            return
        }
        if (busy) return

        busy = true
        statusText = "جارٍ إرسال الأمر إلى ${target.displayId}..."
        scope.launch {
            try {
                val sent = action(target.channelTopic)
                statusText = if (sent) {
                    successMessage
                } else {
                    "لم يصل الأمر مباشرة؛ احتفظنا به للمزامنة عند عودة الاتصال، وقد تحتاج إلى إعادة التحقق من صلاحية الشاشة."
                }
            } catch (error: Exception) {
                statusText = error.message ?: "تعذر إرسال الأمر."
            } finally {
                busy = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TickerBlack)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "شريط أخبار ROYAL TV",
            color = TickerGold,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "الأجهزة المعروضة هنا هي الشاشات النشطة التابعة للمطعم المختار فقط.",
            color = TickerWhite,
            fontSize = 14.sp
        )

        androidx.compose.foundation.layout.Box {
            Button(
                onClick = { deviceMenuExpanded = true },
                enabled = !loadingDevices && devices.isNotEmpty() && !busy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TickerPanel,
                    contentColor = TickerGold
                )
            ) {
                Text(
                    selectedDevice?.let { "${it.displayId} — ${it.deviceName}" }
                        ?: "اختر شاشة ROYAL TV"
                )
            }
            DropdownMenu(
                expanded = deviceMenuExpanded,
                onDismissRequest = { deviceMenuExpanded = false }
            ) {
                devices.forEach { device ->
                    DropdownMenuItem(
                        text = {
                            Text("${device.displayId} — ${device.deviceName}")
                        },
                        onClick = {
                            selectedDevice = device
                            deviceMenuExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = tickerText,
            onValueChange = { tickerText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("نص الخبر") },
            placeholder = { Text("اكتب الخبر الذي سيظهر أسفل الشاشة") },
            minLines = 3,
            maxLines = 5
        )

        Button(
            onClick = {
                val cleanText = tickerText.trim()
                if (cleanText.isBlank()) {
                    statusText = "اكتب نص الخبر أولًا"
                } else {
                    sendTickerCommand(
                        action = { topic ->
                            tickerController.updateTicker(
                                targetDeviceId = topic,
                                text = cleanText
                            )
                        },
                        successMessage = "تم إرسال أمر تحديث الخبر إلى الشاشة المحددة."
                    )
                }
            },
            enabled = !busy && !loadingDevices && selectedDevice != null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TickerGold,
                contentColor = TickerBlack
            )
        ) {
            Text("تحديث نص الخبر", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = {
                sendTickerCommand(
                    action = { topic ->
                        tickerController.enableTicker(topic)
                    },
                    successMessage = "تم إرسال أمر تشغيل الشريط."
                )
            },
            enabled = !busy && !loadingDevices && selectedDevice != null,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF176B60),
                contentColor = Color.White
            )
        ) {
            Text("تشغيل الشريط")
        }

        Button(
            onClick = {
                sendTickerCommand(
                    action = { topic ->
                        tickerController.disableTicker(topic)
                    },
                    successMessage = "تم إرسال أمر إيقاف الشريط."
                )
            },
            enabled = !busy && !loadingDevices && selectedDevice != null,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8B3030),
                contentColor = Color.White
            )
        ) {
            Text("إيقاف الشريط")
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = statusText,
            color = TickerWhite,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = TickerPanel,
                contentColor = TickerGold
            )
        ) {
            Text("رجوع")
        }
    }
}
