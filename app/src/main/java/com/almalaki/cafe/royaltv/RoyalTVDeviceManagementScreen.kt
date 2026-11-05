package com.almalaki.cafe.royaltv

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import kotlinx.coroutines.launch

private val DeviceManagementGold = Color(0xFFD4AF37)
private val DeviceManagementBlack = Color(0xFF050505)
private val DeviceManagementPanel = Color(0xFF111111)
private val DeviceManagementText = Color(0xFFF5F0E5)
private val DeviceManagementMuted = Color(0xFFB8B4AA)

@Composable
fun RoyalTVDeviceManagementScreen(
    accessToken: String,
    restaurantId: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var devices by remember(restaurantId) {
        mutableStateOf<List<RoyalTVManagedDevice>>(emptyList())
    }
    var activationCode by remember(restaurantId) {
        mutableStateOf<RoyalTVActivationCode?>(null)
    }
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember {
        mutableStateOf("تظهر هنا الشاشات المسجلة لهذا المطعم فقط.")
    }
    var revokeTarget by remember {
        mutableStateOf<RoyalTVManagedDevice?>(null)
    }

    fun refreshDevices() {
        if (restaurantId.isBlank() || accessToken.isBlank()) {
            status = "تعذر تحديد حساب المالك أو المطعم الحالي."
            return
        }
        loading = true
        scope.launch {
            try {
                devices = RoyalTVOwnerDeviceManager.listDevices(
                    accessToken = accessToken,
                    restaurantId = restaurantId
                )
                status = if (devices.isEmpty()) {
                    "لا توجد شاشات مربوطة بهذا المطعم حتى الآن."
                } else {
                    "تم تحميل أجهزة هذا المطعم."
                }
            } catch (error: Exception) {
                status = error.message ?: "تعذر تحميل أجهزة المطعم."
            } finally {
                loading = false
            }
        }
    }

    fun createActivationCode() {
        if (busy) return
        busy = true
        activationCode = null
        status = "جارٍ إنشاء رمز ربط مؤقت..."
        scope.launch {
            try {
                activationCode = RoyalTVOwnerDeviceManager.issueActivationCode(
                    accessToken = accessToken,
                    restaurantId = restaurantId
                )
                status = "الرمز صالح لمدة 10 دقائق ويُستخدم مرة واحدة."
            } catch (error: Exception) {
                status = error.message ?: "تعذر إنشاء رمز الربط."
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(accessToken, restaurantId) {
        refreshDevices()
    }

    if (revokeTarget != null) {
        AlertDialog(
            onDismissRequest = { revokeTarget = null },
            title = { Text("إلغاء ربط الشاشة", color = DeviceManagementGold) },
            text = {
                Text(
                    "لن تستقبل هذه الشاشة أوامر هذا المطعم بعد الإلغاء. يمكن ربطها من جديد برمز جديد.",
                    color = DeviceManagementText
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        val selected = revokeTarget ?: return@TextButton
                        revokeTarget = null
                        busy = true
                        scope.launch {
                            try {
                                RoyalTVOwnerDeviceManager.revokeDevice(
                                    accessToken = accessToken,
                                    restaurantId = restaurantId,
                                    deviceId = selected.deviceId
                                )
                                status = "تم إلغاء ربط ${selected.displayId}."
                                refreshDevices()
                            } catch (error: Exception) {
                                status = error.message ?: "تعذر إلغاء ربط الشاشة."
                            } finally {
                                busy = false
                            }
                        }
                    }
                ) {
                    Text("إلغاء الربط", color = Color(0xFFE16B6B))
                }
            },
            dismissButton = {
                TextButton(onClick = { revokeTarget = null }) {
                    Text("تراجع", color = DeviceManagementMuted)
                }
            },
            containerColor = DeviceManagementPanel
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeviceManagementBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "أجهزة ROYAL TV",
                color = DeviceManagementGold,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onBack) {
                Text("رجوع", color = DeviceManagementGold)
            }
        }

        Text(
            "إدارة شاشات المطعم الحالي فقط",
            color = DeviceManagementMuted,
            fontSize = 14.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DeviceManagementPanel),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "ربط شاشة جديدة",
                    color = DeviceManagementGold,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "أنشئ رمزًا هنا، ثم أدخله في شاشة ROYAL TV المراد ربطها.",
                    color = DeviceManagementText,
                    fontSize = 14.sp
                )
                Button(
                    onClick = { createActivationCode() },
                    enabled = !busy && restaurantId.isNotBlank() && accessToken.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeviceManagementGold,
                        contentColor = DeviceManagementBlack
                    )
                ) {
                    Text("إنشاء رمز ربط جديد", fontWeight = FontWeight.Bold)
                }
                activationCode?.let { issued ->
                    HorizontalDivider(color = DeviceManagementGold.copy(alpha = 0.35f))
                    Text(
                        issued.code,
                        modifier = Modifier.fillMaxWidth(),
                        color = DeviceManagementGold,
                        fontSize = 27.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "ينتهي: ${issued.expiresAt}",
                        color = DeviceManagementMuted,
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(
                                Context.CLIPBOARD_SERVICE
                            ) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    "ROYAL TV activation code",
                                    issued.code
                                )
                            )
                            status = "تم نسخ رمز الربط."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeviceManagementGold.copy(alpha = 0.18f),
                            contentColor = DeviceManagementGold
                        )
                    ) {
                        Text("نسخ الرمز")
                    }
                    Text(
                        "لا تشارك الرمز إلا مع شاشة المطعم المقصود؛ الرمز استخدام واحد.",
                        color = DeviceManagementMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "الشاشات المسجلة",
                color = DeviceManagementGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { refreshDevices() }, enabled = !loading) {
                Text("تحديث", color = DeviceManagementGold)
            }
        }

        Text(status, color = DeviceManagementMuted, fontSize = 13.sp)

        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = DeviceManagementGold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(devices, key = { it.deviceId }) { device ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeviceManagementPanel),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            device.displayId,
                            color = DeviceManagementGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(device.deviceName, color = DeviceManagementText)
                        Text(
                            if (device.isActive) "الحالة: مربوط ونشط" else "الحالة: ملغى الربط",
                            color = if (device.isActive) Color(0xFF53C99A) else Color(0xFFE16B6B),
                            fontSize = 13.sp
                        )
                        Text(
                            "آخر اتصال: ${device.lastSeenAt ?: "لم يتصل بعد"}",
                            color = DeviceManagementMuted,
                            fontSize = 12.sp
                        )
                        if (device.isActive) {
                            Button(
                                onClick = { revokeTarget = device },
                                enabled = !busy,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF552525),
                                    contentColor = DeviceManagementText
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إلغاء ربط هذه الشاشة")
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}
