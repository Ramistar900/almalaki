package com.almalaki.cafe.royaltv

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PairOwnerGold = Color(0xFFD4AF37)
private val PairOwnerBlack = Color(0xFF050505)
private val PairOwnerPanel = Color(0xFF111820)
private val PairOwnerText = Color(0xFFF5F0E5)

@Composable
fun RoyalTVDevicePairingScreen(
    accessToken: String,
    restaurantId: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activationCode by remember { mutableStateOf("") }
    var expiresAt by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("أصدر رمزًا مؤقتًا ثم أدخله في شاشة ROYAL TV المراد ربطها.") }
    var busy by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<RoyalTVRegisteredDevice>>(emptyList()) }
    var loadingDevices by remember { mutableStateOf(false) }
    var selectedDeviceToRevoke by remember { mutableStateOf<RoyalTVRegisteredDevice?>(null) }
    var revokingDeviceId by remember { mutableStateOf<String?>(null) }

    fun refreshDevices(updateStatus: Boolean = true) {
        if (loadingDevices || accessToken.isBlank() || restaurantId.isBlank()) return
        loadingDevices = true
        scope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    RoyalTVDeviceOwnerApi.listDevices(accessToken, restaurantId)
                }
                devices = loaded
                if (updateStatus) status = "تم تحديث قائمة الأجهزة المرتبطة."
            } catch (error: Exception) {
                if (updateStatus) status = error.message ?: "تعذر تحميل قائمة الأجهزة."
            } finally {
                loadingDevices = false
            }
        }
    }

    LaunchedEffect(accessToken, restaurantId) {
        refreshDevices(updateStatus = false)
    }

    selectedDeviceToRevoke?.let { selected ->
        AlertDialog(
            onDismissRequest = { selectedDeviceToRevoke = null },
            title = { Text("إلغاء ربط الجهاز؟", color = PairOwnerGold) },
            text = {
                Text(
                    "سيُمنع ${selected.deviceName} من الحصول على أوامر جديدة من هذا المطعم. لا يمكن التراجع عن الإلغاء؛ يمكن ربطه مجددًا برمز جديد.",
                    color = PairOwnerText
                )
            },
            confirmButton = {
                TextButton(
                    enabled = revokingDeviceId == null,
                    onClick = {
                        val deviceId = selected.deviceId
                        selectedDeviceToRevoke = null
                        revokingDeviceId = deviceId
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    RoyalTVDeviceOwnerApi.revokeDevice(accessToken, deviceId)
                                }
                                devices = devices.map {
                                    if (it.deviceId == deviceId) it.copy(revokedAt = "revoked") else it
                                }
                                status = "تم إلغاء صلاحية الجهاز. لن تُقبل له اشتراكات جديدة أو أوامر جديدة."
                                refreshDevices(updateStatus = false)
                            } catch (error: Exception) {
                                status = error.message ?: "تعذر إلغاء صلاحية الجهاز."
                            } finally {
                                revokingDeviceId = null
                            }
                        }
                    }
                ) {
                    Text("إلغاء الجهاز", color = Color(0xFFFF7777))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDeviceToRevoke = null }) {
                    Text("رجوع", color = PairOwnerText)
                }
            },
            containerColor = PairOwnerPanel
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PairOwnerBlack)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBack,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PairOwnerGold)
            ) {
                Text("رجوع")
            }
            Text(
                text = "ربط أجهزة ROYAL TV",
                modifier = Modifier.weight(1f),
                color = PairOwnerGold,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End
            )
        }

        Text(
            text = "سيُربط الجهاز بالمطعم المحدد حاليًا فقط. الرمز صالح لمدة 10 دقائق ويُستخدم مرة واحدة.",
            modifier = Modifier.fillMaxWidth(),
            color = PairOwnerText,
            fontSize = 15.sp,
            textAlign = TextAlign.Right
        )

        Button(
            onClick = {
                if (busy) return@Button
                busy = true
                activationCode = ""
                expiresAt = ""
                status = "جارٍ إصدار رمز آمن..."
                scope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            RoyalTVDeviceOwnerApi.issueActivationCode(
                                accessToken = accessToken,
                                restaurantId = restaurantId
                            )
                        }
                        activationCode = result.code
                        expiresAt = result.expiresAt
                        status = "تم إصدار الرمز. أدخله على التلفاز قبل انتهاء صلاحيته."
                    } catch (error: Exception) {
                        status = error.message ?: "تعذر إصدار رمز الربط."
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy && accessToken.isNotBlank() && restaurantId.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PairOwnerGold,
                contentColor = PairOwnerBlack
            )
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.height(22.dp),
                    color = PairOwnerBlack,
                    strokeWidth = 2.dp
                )
            } else {
                Text("إصدار رمز ربط جديد", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }

        if (activationCode.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PairOwnerPanel, RoundedCornerShape(16.dp))
                    .border(1.dp, PairOwnerGold.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = activationCode.chunked(16).joinToString("\n"),
                    color = PairOwnerGold,
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("ROYAL TV activation code", activationCode)
                        )
                        status = "تم نسخ الرمز. الصقه في شاشة التلفاز."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PairOwnerGold,
                        contentColor = PairOwnerBlack
                    )
                ) {
                    Text("نسخ الرمز")
                }
                Text(
                    text = "ينتهي في: $expiresAt",
                    color = PairOwnerText.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الأجهزة المرتبطة بهذا المطعم",
                modifier = Modifier.weight(1f),
                color = PairOwnerGold,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                textAlign = TextAlign.End
            )
            OutlinedButton(
                onClick = { refreshDevices() },
                enabled = !loadingDevices && revokingDeviceId == null
            ) {
                Text(if (loadingDevices) "جارٍ..." else "تحديث")
            }
        }

        if (devices.isEmpty() && !loadingDevices) {
            Text(
                text = "لا توجد أجهزة مسجلة لهذا المطعم بعد.",
                modifier = Modifier.fillMaxWidth(),
                color = PairOwnerText.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 230.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(devices, key = { it.deviceId }) { device ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PairOwnerPanel, RoundedCornerShape(12.dp))
                            .border(1.dp, PairOwnerGold.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = device.deviceName,
                                modifier = Modifier.weight(1f),
                                color = PairOwnerText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (device.isRevoked) "ملغى" else "نشط",
                                color = if (device.isRevoked) Color(0xFFFF7777) else Color(0xFF65D6A4),
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "معرّف الشاشة: ${device.channelDeviceId}",
                            color = PairOwnerText.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "تاريخ التسجيل: ${device.createdAt}",
                            color = PairOwnerText.copy(alpha = 0.65f),
                            fontSize = 10.sp
                        )
                        if (!device.isRevoked) {
                            OutlinedButton(
                                onClick = { selectedDeviceToRevoke = device },
                                enabled = revokingDeviceId == null,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF7777))
                            ) {
                                Text(
                                    if (revokingDeviceId == device.deviceId) "جارٍ الإلغاء..."
                                    else "إلغاء صلاحية هذا الجهاز"
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(2.dp))
        Text(
            text = status,
            modifier = Modifier.fillMaxWidth(),
            color = PairOwnerText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}
