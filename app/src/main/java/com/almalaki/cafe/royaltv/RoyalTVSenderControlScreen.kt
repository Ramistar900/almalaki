package com.almalaki.cafe.royaltv

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private val SenderGold = Color(0xFFD4AF37)
private val SenderBlack = Color(0xFF050505)
private val SenderCream = Color(0xFFF5F0E5)

@Composable
fun RoyalTVSenderControlScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    var receiverHost by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf(
            "أدخل عنوان IP لجهاز العرض المتصل بالشبكة نفسها."
        )
    }

    var isStarting by remember {
        mutableStateOf(false)
    }

    val projectionManager = remember(context) {
        context.getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager
    }

    val capturePermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (
                result.resultCode ==
                android.app.Activity.RESULT_OK &&
                result.data != null
            ) {
                try {
                    val serviceIntent = Intent(
                        context,
                        RoyalTVSenderCaptureService::class.java
                    ).apply {
                        action =
                            RoyalTVSenderCaptureService.ACTION_START

                        putExtra(
                            RoyalTVSenderCaptureService.EXTRA_RESULT_CODE,
                            result.resultCode
                        )

                        putExtra(
                            RoyalTVSenderCaptureService.EXTRA_RESULT_DATA,
                            result.data
                        )

                        putExtra(
                            RoyalTVSenderCaptureService.EXTRA_RECEIVER_HOST,
                            receiverHost.trim()
                        )

                        putExtra(
                            RoyalTVSenderCaptureService.EXTRA_RECEIVER_PORT,
                            39872
                        )
                    }

                    ContextCompat.startForegroundService(
                        context,
                        serviceIntent
                    )

                    isStarting = true
                    message =
                        "تم إرسال طلب بدء الخدمة. جارٍ تجهيز الاتصال."

                } catch (error: Exception) {
                    isStarting = false
                    message =
                        "تعذر بدء الإرسال: ${
                            error.message ?: "خطأ غير معروف"
                        }"
                }
            } else {
                isStarting = false
                message = "أُلغيت مشاركة الشاشة."
            }
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SenderBlack)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "ROYAL TV",
            color = SenderGold,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "بث شاشة هاتف المالك",
            color = SenderCream,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text =
                "أدخل عنوان IP لجهاز التلفزيون أو TV Box. " +
                "يجب أن يكون الجهازان متصلين بالشبكة المحلية نفسها.",
            color = SenderCream,
            fontSize = 14.sp
        )

        OutlinedTextField(
            value = receiverHost,
            onValueChange = {
                receiverHost = it
                isStarting = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("عنوان IP لجهاز العرض")
            },
            placeholder = {
                Text("مثال: 192.168.1.50")
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii
            )
        )

        Text(
            text = "منفذ الفيديو: 39872",
            color = SenderGold,
            fontSize = 14.sp
        )

        Button(
            onClick = {
                val host = receiverHost.trim()

                if (
                    host.isBlank() ||
                    host.any { it.isWhitespace() }
                ) {
                    message = "أدخل عنوان IP صالحًا أولًا."
                } else {
                    message =
                        "وافق على مشاركة الشاشة في نافذة Android التالية."
                    capturePermissionLauncher.launch(
                        projectionManager.createScreenCaptureIntent()
                    )
                }
            },
            enabled = !isStarting,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SenderGold,
                contentColor = SenderBlack
            )
        ) {
            Text(
                "بدء بث الشاشة",
                fontWeight = FontWeight.Bold
            )
        }

        Button(
            onClick = {
                val stopIntent = Intent(
                    context,
                    RoyalTVSenderCaptureService::class.java
                ).apply {
                    action =
                        RoyalTVSenderCaptureService.ACTION_STOP
                }

                context.startService(stopIntent)
                isStarting = false
                message = "تم إرسال طلب إيقاف البث."
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF292929),
                contentColor = SenderCream
            )
        ) {
            Text("إيقاف البث")
        }

        Text(
            text = message,
            color = SenderGold,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF292929),
                    contentColor = SenderCream
                )
            ) {
                Text("رجوع")
            }
        }

        Text(
            text =
                "ملاحظة: ظهور رسالة بدء الخدمة لا يؤكد وصول الصورة. " +
                "يجب تجهيز مستقبل الفيديو على جهاز العرض أولًا.",
            color = SenderCream,
            fontSize = 12.sp
        )
    }
}
