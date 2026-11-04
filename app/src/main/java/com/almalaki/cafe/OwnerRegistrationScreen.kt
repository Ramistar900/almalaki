package com.almalaki.cafe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val OwnerGold = Color(0xFFD4AF37)
private val OwnerCream = Color(0xFFF5F0E5)

@Composable
fun OwnerRegistrationScreen(
    onBack: () -> Unit,
    onActivated: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var restaurantName by remember { mutableStateOf("") }
    var activationCode by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("أنشئ حساب المالك أولاً، ثم أكّد البريد الإلكتروني قبل تفعيل الرمز.") }
    var busy by remember { mutableStateOf(false) }
    var signupRequested by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Royal Coffee", color = OwnerGold, fontSize = 30.sp)
        Spacer(Modifier.height(8.dp))
        Text("إنشاء حساب مالك", color = OwnerCream, fontSize = 23.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "يتطلب إنشاء حساب مالك رمز تفعيل يصدره المطوّر الملكي. لا يمكن تحويل حساب الزبون إلى مالك دون رمز صالح.",
            color = OwnerCream,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = restaurantName,
            onValueChange = { restaurantName = it.take(100) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("اسم المطعم أو المقهى") },
            singleLine = true,
            enabled = !busy
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("البريد الإلكتروني") },
            singleLine = true,
            enabled = !busy
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("كلمة المرور — 8 أحرف على الأقل") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            enabled = !busy
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = activationCode,
            onValueChange = { activationCode = it.take(100) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("رمز تفعيل المالك") },
            singleLine = true,
            enabled = !busy
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    busy = true
                    message = "جارٍ إنشاء حساب المصادقة..."
                    try {
                        RoyalSupabaseAuth.register(email.trim(), password)
                        signupRequested = true
                        message = "تم إرسال طلب إنشاء الحساب. افتح رسالة التأكيد في بريدك، ثم ارجع إلى التطبيق واضغط تفعيل الحساب."
                    } catch (error: Exception) {
                        message = error.message ?: "تعذر إنشاء الحساب."
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy && email.contains("@") && password.length >= 8,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = OwnerGold, contentColor = Color.Black)
        ) {
            Text(if (busy) "جارٍ إنشاء الحساب..." else "إنشاء الحساب وإرسال تأكيد البريد")
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    busy = true
                    message = "جارٍ التحقق من البريد ورمز التفعيل..."
                    try {
                        var accessToken = RoyalSupabaseAuth.accessTokenOrNull()
                        if (accessToken.isNullOrBlank()) {
                            RoyalSupabaseAuth.signIn(email.trim(), password)
                            accessToken = RoyalSupabaseAuth.accessTokenOrNull()
                        }
                        if (accessToken.isNullOrBlank()) {
                            throw Exception("لم نحصل على جلسة صالحة. أكّد البريد ثم سجّل الدخول مجدداً.")
                        }
                        activateOwnerAccount(
                            accessToken = accessToken,
                            code = activationCode,
                            restaurantName = restaurantName
                        )
                        message = "تم تفعيل حساب المالك وإنشاء المطعم بنجاح."
                        onActivated(accessToken)
                    } catch (error: Exception) {
                        message = error.message ?: "تعذر تفعيل حساب المالك."
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy && restaurantName.trim().length >= 2 &&
                email.contains("@") && password.length >= 8 && activationCode.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = OwnerGold, contentColor = Color.Black)
        ) {
            Text(if (busy) "جارٍ التحقق والتفعيل..." else "تفعيل الحساب بعد تأكيد البريد")
        }

        if (signupRequested) {
            Text(
                "بعد فتح رابط التأكيد والعودة إلى التطبيق، استخدم زر التفعيل أعلاه. يمكن أيضاً استخدام حساب موجود إذا كان البريد مؤكداً.",
                color = OwnerGold,
                fontSize = 11.sp,
                lineHeight = 17.sp
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(message, color = OwnerGold, fontSize = 13.sp, lineHeight = 19.sp)
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onBack, enabled = !busy) {
            Text("العودة إلى دخول المالك", color = OwnerCream)
        }
    }
}

private suspend fun activateOwnerAccount(
    accessToken: String,
    code: String,
    restaurantName: String
) {
    withContext(Dispatchers.IO) {
        require(accessToken.isNotBlank()) { "جلسة تسجيل الدخول غير متوفرة." }
        require(code.isNotBlank()) { "أدخل رمز تفعيل المالك." }
        require(restaurantName.trim().length in 2..100) { "اسم المطعم يجب أن يتراوح بين حرفين و100 حرف." }

        val connection = URL(
            "${RoyalSupabaseAuth.PROJECT_URL}/functions/v1/owner-activation"
        ).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("apikey", RoyalSupabaseAuth.PUBLISHABLE_KEY)
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            val body = JSONObject()
                .put("action", "redeem")
                .put("code", code.trim())
                .put("restaurant_name", restaurantName.trim())
                .toString()
            connection.outputStream.use {
                it.write(body.toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val response = (
                if (status in 200..299) connection.inputStream else connection.errorStream
            )?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val error = runCatching {
                    JSONObject(response).optString("error")
                }.getOrNull().orEmpty()
                throw Exception(
                    when {
                        error.contains("Confirm your email") -> "أكد بريدك الإلكتروني أولاً ثم أعد المحاولة."
                        error.contains("Invalid or inactive") -> "رمز التفعيل غير صحيح أو موقوف أو سبق استخدامه."
                        error.contains("already has an active owner") -> "هذا الحساب مرتبط بالفعل بعضوية مالك نشطة."
                        else -> "تعذر تفعيل حساب المالك (HTTP $status)."
                    }
                )
            }
        } finally {
            connection.disconnect()
        }
    }
}
