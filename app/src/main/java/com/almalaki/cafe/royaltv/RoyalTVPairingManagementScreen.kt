package com.almalaki.cafe.royaltv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val PairGold = Color(0xFFD4AF37)
private val PairCream = Color(0xFFF5F0E5)
private val PairBlack = Color(0xFF050505)
private val PairPanel = Color(0xFF111111)

@Composable
fun RoyalTVPairingManagementScreen(
    accessToken: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var restaurantId by remember { mutableStateOf("") }
    var ownedRestaurants by remember { mutableStateOf<List<String>>(emptyList()) }
    var loadingRestaurants by remember { mutableStateOf(false) }
    var pairedDevices by remember { mutableStateOf<List<RoyalTVPairedDevice>>(emptyList()) }
    var loadingDevices by remember { mutableStateOf(false) }
    var challenge by remember { mutableStateOf<RoyalTVPairingChallenge?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("أنشئ رمزًا مؤقتًا ثم أدخله على جهاز ROYAL TV.") }

    val client = remember(accessToken) {
        RoyalTVOwnerAuthorizationClient { accessToken }
    }

    LaunchedEffect(accessToken) {
        if (accessToken.isBlank()) {
            ownedRestaurants = emptyList()
            pairedDevices = emptyList()
            message = "يرجى تسجيل الدخول بحساب المالك."
            return@LaunchedEffect
        }
        loadingRestaurants = true
        try {
            ownedRestaurants = client.listOwnedRestaurants()
            if (restaurantId !in ownedRestaurants) {
                restaurantId = ownedRestaurants.firstOrNull().orEmpty()
                challenge = null
            }
            message = if (ownedRestaurants.isEmpty()) {
                "لم يعثر الخادم على مطعم نشط تملك صلاحية إدارته."
            } else {
                "اختر المطعم الذي تريد ربط جهاز ROYAL TV به."
            }
        } catch (error: Exception) {
            ownedRestaurants = emptyList()
            restaurantId = ""
            message = error.message?.take(240) ?: "تعذر تحميل مطاعم حساب المالك."
        } finally {
            loadingRestaurants = false
        }
    }

    LaunchedEffect(accessToken, restaurantId) {
        pairedDevices = emptyList()
        if (accessToken.isBlank() || restaurantId.isBlank() || restaurantId !in ownedRestaurants) {
            return@LaunchedEffect
        }
        loadingDevices = true
        try {
            pairedDevices = client.listPairedDevices(restaurantId)
        } catch (error: Exception) {
            message = error.message?.take(240) ?: "تعذر تحميل الأجهزة المقترنة."
        } finally {
            loadingDevices = false
        }
    }

    Column(
        modifier = modifier.fillMaxSize().background(PairBlack).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PairPanel,
                    contentColor = PairGold
                )
            ) { Text("رجوع") }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "ربط ROYAL TV",
                color = PairGold,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "يجب استخدام حساب مالك المطعم. يتحقق الخادم من صلاحية المالك عند إنشاء الرمز؛ لا يكفي إدخال معرّف المطعم وحده.",
            color = PairCream,
            fontSize = 14.sp
        )

        Text(
            text = if (loadingRestaurants) "جارٍ تحميل المطاعم المصرح بها…" else "المطاعم التي يملكها هذا الحساب",
            color = PairGold,
            fontWeight = FontWeight.SemiBold
        )

        if (!loadingRestaurants && ownedRestaurants.isEmpty()) {
            Text(
                text = "لا توجد مطاعم متاحة للاختيار. تحقق من تسجيل الدخول وصلاحية المالك، ثم أعد المحاولة بعد نشر الدالة الخادمية المعتمدة.",
                color = PairCream,
                fontSize = 13.sp
            )
        }

        ownedRestaurants.forEach { id ->
            Button(
                onClick = {
                    restaurantId = id
                    challenge = null
                    message = "تم اختيار المطعم. يمكنك الآن إنشاء رمز اقتران مؤقت."
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (restaurantId == id) PairGold else PairPanel,
                    contentColor = if (restaurantId == id) PairBlack else PairCream
                )
            ) {
                Text(
                    text = if (restaurantId == id) "✓  $id" else id,
                    fontSize = 12.sp
                )
            }
        }

        Button(
            onClick = {
                scope.launch {
                    busy = true
                    challenge = null
                    message = ""
                    try {
                        challenge = client.createPairingChallenge(restaurantId.trim())
                        message = "تم إنشاء الرمز. صالح لمدة محدودة ويُستخدم مرة واحدة."
                    } catch (error: Exception) {
                        message = error.message?.take(240) ?: "تعذر إنشاء رمز الاقتران."
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy && !loadingRestaurants && accessToken.isNotBlank() && restaurantId.isNotBlank() && restaurantId in ownedRestaurants,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = PairGold,
                contentColor = PairBlack
            )
        ) {
            Text(if (busy) "جارٍ الاتصال بالخادم…" else "إنشاء رمز اقتران مؤقت")
        }

        Text(
            text = message,
            color = PairCream,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "الأجهزة المقترنة بهذا المطعم",
            color = PairGold,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        loadingDevices = true
                        try {
                            pairedDevices = client.listPairedDevices(restaurantId)
                        } catch (error: Exception) {
                            message = error.message?.take(240) ?: "تعذر تحديث قائمة الأجهزة."
                        } finally {
                            loadingDevices = false
                        }
                    }
                },
                enabled = !loadingDevices && restaurantId.isNotBlank() && restaurantId in ownedRestaurants,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loadingDevices) "جارٍ تحديث الأجهزة…" else "تحديث قائمة الأجهزة")
            }
        }
        if (!loadingDevices && pairedDevices.isEmpty()) {
            Text("لا توجد أجهزة مسجلة لهذا المطعم، أو لم تُربط بعد.", color = PairCream, fontSize = 13.sp)
        }
        pairedDevices.forEach { device ->
            Column(
                modifier = Modifier.fillMaxWidth().background(PairPanel).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(device.deviceName, color = PairGold, fontWeight = FontWeight.Bold)
                Text("المعرّف: ${device.deviceId}", color = PairCream, fontSize = 11.sp)
                Text("الحالة: ${if (device.status == "active") "نشط" else "ملغى"}", color = PairCream, fontSize = 13.sp)
                Text("تاريخ الاقتران: ${device.pairedAt}", color = PairCream, fontSize = 11.sp)
                Text("آخر اتصال: ${device.lastSeenAt ?: "لم يتصل بعد"}", color = PairCream, fontSize = 11.sp)
                if (device.status == "active") {
                    Button(
                        onClick = {
                            scope.launch {
                                busy = true
                                try {
                                    client.revokeDevice(restaurantId, device.deviceId)
                                    pairedDevices = client.listPairedDevices(restaurantId)
                                    message = "تم إلغاء صلاحية الجهاز. لن يستطيع تنفيذ طلبات جديدة باستخدام بيانات اعتماده."
                                } catch (error: Exception) {
                                    message = error.message?.take(240) ?: "تعذر إلغاء صلاحية الجهاز."
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7A2020),
                            contentColor = PairCream
                        )
                    ) {
                        Text("إلغاء اقتران الجهاز")
                    }
                }
            }
        }


        challenge?.let { active ->
            Column(
                modifier = Modifier.fillMaxWidth().background(PairPanel).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("رمز الاقتران — أدخله على التلفزيون", color = PairGold, fontWeight = FontWeight.Bold)
                Text(
                    text = active.code,
                    color = PairCream,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("ينتهي في: ${active.expiresAt}", color = PairCream, fontSize = 12.sp)
                Text(
                    "لا تشارك الرمز مع أحد. إذا انتهت صلاحيته، أنشئ رمزًا جديدًا. لا يُعرض الرمز مجددًا بعد مغادرة هذه الشاشة.",
                    color = PairCream,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "هذه المرحلة تفعّل إنشاء رمز الاقتران فقط. إدارة الأجهزة المقترنة وإرسال الأوامر ستُربط بعد إضافة واجهة خادمية موثقة لقائمة الأجهزة واختبارها.",
            color = PairGold,
            fontSize = 12.sp
        )
    }
}
