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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val connectionManager = remember(context) {
        RoyalTVConnectionManager(context)
    }

    val tickerController = remember(connectionManager) {
        RoyalTVTabletTickerController(connectionManager)
    }

    var targetDeviceId by remember {
        mutableStateOf("")
    }

    var tickerText by remember {
        mutableStateOf("")
    }

    var statusText by remember {
        mutableStateOf("أدخل معرّف شاشة ROYAL TV أولًا")
    }

    var busy by remember {
        mutableStateOf(false)
    }

    DisposableEffect(connectionManager) {
        connectionManager.start(
            onCommand = {}
        )

        onDispose {
            connectionManager.stop()
        }
    }

    fun runTickerCommand(
        action: suspend (String) -> Boolean,
        successMessage: String
    ) {
        val target = targetDeviceId.trim()

        if (target.isBlank()) {
            statusText = "يرجى إدخال معرّف جهاز ROYAL TV"
            return
        }

        if (busy) return

        busy = true
        statusText = "جارٍ تجهيز الأمر..."

        scope.launch {
            try {
                val result = action(target)

                statusText = if (result) {
                    successMessage
                } else {
                    "تعذّر الإرسال المباشر؛ تحقق من الاتصال. قد يكون الأمر محفوظًا للإرسال لاحقًا."
                }
            } catch (e: Exception) {
                statusText = "حدث خطأ: ${e.message ?: "تعذّر تنفيذ الأمر"}"
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
            text = "أرسل الخبر إلى شاشة التلفزيون المطلوبة.",
            color = TickerWhite,
            fontSize = 15.sp
        )

        OutlinedTextField(
            value = targetDeviceId,
            onValueChange = { targetDeviceId = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("معرّف جهاز ROYAL TV") },
            placeholder = { Text("مثال: ROYAL-TV-001") },
            singleLine = true
        )

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
                    runTickerCommand(
                        action = { target ->
                            tickerController.updateTicker(
                                targetDeviceId = target,
                                text = cleanText
                            )
                        },
                        successMessage = "تم إرسال أمر تحديث الخبر"
                    )
                }
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TickerGold,
                contentColor = TickerBlack
            )
        ) {
            Text(
                text = "تحديث نص الخبر",
                fontWeight = FontWeight.Bold
            )
        }

        Button(
            onClick = {
                runTickerCommand(
                    action = { target ->
                        tickerController.enableTicker(target)
                    },
                    successMessage = "تم إرسال أمر تشغيل الشريط"
                )
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF176B60),
                contentColor = Color.White
            )
        ) {
            Text("تشغيل الشريط")
        }

        Button(
            onClick = {
                runTickerCommand(
                    action = { target ->
                        tickerController.disableTicker(target)
                    },
                    successMessage = "تم إرسال أمر إيقاف الشريط"
                )
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
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
