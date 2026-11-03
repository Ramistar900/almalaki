package com.almalaki.cafe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val LOGIN_SUPABASE_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val LOGIN_SUPABASE_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

@Composable
fun LoginScreen(
    developerOnly: Boolean = false,
    onSuccess: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    var showReset by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505)),
        contentAlignment = Alignment.Center
    ) {
        if (showReset) {
            PasswordResetContent(
                email = resetEmail,
                onEmailChange = { resetEmail = it },
                message = message,
                loading = resetLoading,
                onSendReset = {
                    if (resetEmail.isBlank()) {
                        message = "أدخل البريد الإلكتروني."
                        return@PasswordResetContent
                    }

                    resetLoading = true
                    message = ""

                    Thread {
                        try {
                            sendPasswordResetEmail(resetEmail.trim())

                            AppSounds.passwordChanged(context)

                            message =
                                "تم إرسال رابط استعادة كلمة المرور إلى بريدك الإلكتروني 📧"
                        } catch (e: Exception) {
                            message =
                                e.message ?: "تعذر إرسال رابط الاستعادة."
                        } finally {
                            resetLoading = false
                        }
                    }.start()
                },
                onBack = {
                    showReset = false
                    message = ""
                }
            )
        } else {
            LoginContent(
                email = email,
                password = password,
                onEmailChange = {
                    email = it
                    message = ""
                },
                onPasswordChange = {
                    password = it
                    message = ""
                },
                message = message,
                loading = loading,
                developerOnly = developerOnly,
                onLogin = {
                    if (email.isBlank()) {
                        message = "أدخل البريد الإلكتروني."
                        return@LoginContent
                    }

                    if (password.isBlank()) {
                        message = "أدخل كلمة المرور."
                        return@LoginContent
                    }

                    loading = true
                    message = ""

                    Thread {
                        try {
                            val accessToken = loginOwner(
                                context = context,
                                email = email.trim(),
                                password = password
                            )

                            if (developerOnly) {
                                verifyDeveloperAccount(accessToken)
                            }
                            onSuccess(accessToken)
                        } catch (e: Exception) {
                            AppSounds.wrongPassword(context)

                            message =
                                e.message ?: "تعذر تسجيل الدخول."
                        } finally {
                            loading = false
                        }
                    }.start()
                },
                onForgotPassword = {
                    resetEmail = email
                    message = ""
                    showReset = true
                },
                onBack = onBack
            )
        }
    }
}

@Composable
private fun LoginContent(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    message: String,
    loading: Boolean,
    developerOnly: Boolean,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Royal Coffee",
            color = Color(0xFFD4AF37),
            fontSize = 32.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (developerOnly) "المطور الملكي 🔐" else "دخول المالك 👑",
            color = Color(0xFFF5F0E5),
            fontSize = 23.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("البريد الإلكتروني")
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("كلمة المرور")
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onLogin,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD4AF37),
                contentColor = Color.Black
            )
        ) {
            Text(
                text = if (loading) "جاري الدخول..." else "دخول"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onForgotPassword,
            enabled = !loading
        ) {
            Text(
                text = "نسيت كلمة المرور؟",
                color = Color(0xFFD4AF37)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        TextButton(
            onClick = onBack,
            enabled = !loading
        ) {
            Text(
                text = "العودة",
                color = Color(0xFFF5F0E5)
            )
        }

        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                color = Color(0xFFD4AF37),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun PasswordResetContent(
    email: String,
    onEmailChange: (String) -> Unit,
    message: String,
    loading: Boolean,
    onSendReset: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Royal Coffee",
            color = Color(0xFFD4AF37),
            fontSize = 32.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "استعادة كلمة المرور 🔐",
            color = Color(0xFFF5F0E5),
            fontSize = 23.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "أدخل بريد المالك لإرسال رابط استعادة كلمة المرور.",
            color = Color(0xFFF5F0E5),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(22.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("البريد الإلكتروني")
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onSendReset,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD4AF37),
                contentColor = Color.Black
            )
        ) {
            Text(
                text = if (loading)
                    "جاري الإرسال..."
                else
                    "إرسال رابط الاستعادة"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onBack,
            enabled = !loading
        ) {
            Text(
                text = "العودة لتسجيل الدخول",
                color = Color(0xFFF5F0E5)
            )
        }

        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                color = Color(0xFFD4AF37),
                fontSize = 14.sp
            )
        }
    }
}


private fun verifyDeveloperAccount(accessToken: String) {
    val connection = URL(
        "$LOGIN_SUPABASE_URL/rest/v1/rpc/is_dashboard_admin"
    ).openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("apikey", LOGIN_SUPABASE_KEY)
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")

        connection.outputStream.use {
            it.write("{}".toByteArray(Charsets.UTF_8))
        }

        val code = connection.responseCode
        val response = (
            if (code in 200..299) connection.inputStream else connection.errorStream
        )?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

        if (code !in 200..299) {
            throw Exception("تعذر التحقق من صلاحية المطوّر (HTTP $code).")
        }
        if (!response.trim().equals("true", ignoreCase = true)) {
            throw Exception("هذا الحساب غير مخوّل للدخول إلى قسم المطوّر الملكي.")
        }
    } finally {
        connection.disconnect()
    }
}

private fun loginOwner(
    context: android.content.Context,
    email: String,
    password: String
): String {
    val url = URL(
        "$LOGIN_SUPABASE_URL/auth/v1/token?grant_type=password"
    )

    val connection =
        url.openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "POST"
        connection.doOutput = true

        connection.setRequestProperty(
            "apikey",
            LOGIN_SUPABASE_KEY
        )

        connection.setRequestProperty(
            "Content-Type",
            "application/json"
        )

        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
        }.toString()

        connection.outputStream.use {
            it.write(
                body.toByteArray(Charsets.UTF_8)
            )
        }

        val code = connection.responseCode

        val response =
            if (code in 200..299) {
                connection.inputStream
                    .bufferedReader()
                    .readText()
            } else {
                connection.errorStream
                    ?.bufferedReader()
                    ?.readText()
                    ?: ""
            }

        if (code !in 200..299) {
            val error = try {
                JSONObject(response)
                    .optString(
                        "msg",
                        JSONObject(response)
                            .optString(
                                "error_description",
                                "بيانات الدخول غير صحيحة."
                            )
                    )
            } catch (_: Exception) {
                "بيانات الدخول غير صحيحة."
            }

            throw Exception(error)
        }

        val json = JSONObject(response)

        val accessToken = json.optString("access_token")
            .ifBlank {
                throw Exception(
                    "لم يتم الحصول على جلسة تسجيل الدخول."
                )
            }

        // Register only the app-generated random installation ID after successful owner auth.
        // Failure to register the device must not block a valid owner login.
        runCatching {
            val userId = json.optJSONObject("user")?.optString("id").orEmpty()
            if (userId.isNotBlank()) {
                val deviceId = RoyalDeviceIdentity.getOrCreate(context)
                val deviceBody = JSONObject()
                    .put("user_id", userId)
                    .put("device_id", deviceId)
                    .put("device_label", "Android app")
                    .put("platform", "android")
                    .put(
                        "last_seen_at",
                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                            .format(java.util.Date())
                    )
                    .toString()
                val deviceConnection = URL(
                    "$LOGIN_SUPABASE_URL/rest/v1/user_devices?on_conflict=user_id,device_id"
                ).openConnection() as HttpURLConnection
                try {
                    deviceConnection.requestMethod = "POST"
                    deviceConnection.doOutput = true
                    deviceConnection.connectTimeout = 10000
                    deviceConnection.readTimeout = 10000
                    deviceConnection.setRequestProperty("apikey", LOGIN_SUPABASE_KEY)
                    deviceConnection.setRequestProperty("Authorization", "Bearer $accessToken")
                    deviceConnection.setRequestProperty("Content-Type", "application/json")
                    deviceConnection.setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
                    deviceConnection.outputStream.use {
                        it.write(deviceBody.toByteArray(Charsets.UTF_8))
                    }
                    if (deviceConnection.responseCode !in 200..299) {
                        throw Exception("Device registration failed")
                    }
                } finally {
                    deviceConnection.disconnect()
                }
            }
        }

        return accessToken
    } finally {
        connection.disconnect()
    }
}

private fun sendPasswordResetEmail(
    email: String
) {
    val url = URL(
        "$LOGIN_SUPABASE_URL/auth/v1/recover"
    )

    val connection =
        url.openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "POST"
        connection.doOutput = true

        connection.setRequestProperty(
            "apikey",
            LOGIN_SUPABASE_KEY
        )

        connection.setRequestProperty(
            "Content-Type",
            "application/json"
        )

        val body = JSONObject().apply {
            put("email", email)
        }.toString()

        connection.outputStream.use {
            it.write(
                body.toByteArray(Charsets.UTF_8)
            )
        }

        val code = connection.responseCode

        if (code !in 200..299) {
            val response =
                connection.errorStream
                    ?.bufferedReader()
                    ?.readText()
                    ?: ""

            val error = try {
                JSONObject(response)
                    .optString(
                        "msg",
                        "تعذر إرسال رابط استعادة كلمة المرور."
                    )
            } catch (_: Exception) {
                "تعذر إرسال رابط استعادة كلمة المرور."
            }

            throw Exception(error)
        }
    } finally {
        connection.disconnect()
    }
}
