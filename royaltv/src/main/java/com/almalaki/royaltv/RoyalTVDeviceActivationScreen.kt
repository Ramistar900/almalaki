package com.almalaki.royaltv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val PairingGold = Color(0xFFD4AF37)
private val PairingBlack = Color(0xFF050609)
private val PairingPanel = Color(0xFF121820)
private val PairingText = Color(0xFFF5F0E5)

@Composable
fun RoyalTVDeviceActivationScreen(
    deviceId: String,
    onActivate: suspend (String) -> Unit,
    onActivated: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var activationCode by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("اطلب رمز ربط مؤقتًا من مالك المطعم داخل تطبيق الملكي.") }
    var failed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF071014), PairingBlack, Color(0xFF10151C))
                )
            )
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .background(PairingPanel, RoundedCornerShape(24.dp))
                .border(1.dp, PairingGold.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
                .padding(horizontal = 32.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ROYAL TV",
                color = PairingGold,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "ربط التلفاز بالمطعم",
                color = PairingText,
                fontSize = 23.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "كل تلفاز يُسجّل بهوية مستقلة ويرتبط بمطعم واحد. لا تُدخل إلا الرمز الذي أصدره مالك المطعم المقصود.",
                color = PairingText.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "معرّف هذا الجهاز: $deviceId",
                color = PairingGold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = activationCode,
                onValueChange = {
                    activationCode = it.filter { ch -> ch.isLetterOrDigit() }.take(64)
                    failed = false
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                label = { Text("رمز التفعيل") },
                placeholder = { Text("أدخل الرمز المكوّن من 64 خانة") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (busy) return@Button
                    if (!activationCode.trim().matches(Regex("^[0-9a-fA-F]{64}$"))) {
                        failed = true
                        status = "تحقق من الرمز؛ يجب أن يتكون من 64 خانة سداسية عشرية."
                    } else {
                        busy = true
                        failed = false
                        status = "جارٍ التحقق من الرمز وربط هذا التلفاز..."
                        scope.launch {
                            try {
                                onActivate(activationCode.trim())
                                status = "تم ربط التلفاز بنجاح."
                                onActivated()
                            } catch (error: Exception) {
                                failed = true
                                status = error.message ?: "تعذر ربط التلفاز. حاول مجددًا."
                            } finally {
                                busy = false
                            }
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PairingGold,
                    contentColor = PairingBlack
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(22.dp),
                        color = PairingBlack,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("تفعيل وربط التلفاز", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = status,
                color = if (failed) Color(0xFFFF7777) else PairingText.copy(alpha = 0.9f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "الرمز يُستخدم مرة واحدة وتنتهي صلاحيته بعد 10 دقائق.",
                color = PairingGold.copy(alpha = 0.85f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
