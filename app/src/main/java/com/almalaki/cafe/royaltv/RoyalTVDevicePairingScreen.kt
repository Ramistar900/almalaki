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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
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
