package com.almalaki.cafe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private fun authConnection(
    accessToken: String,
    method: String
): HttpURLConnection {
    val connection = URL(
        "${ADMIN_SUPABASE_URL}/auth/v1/user"
    ).openConnection() as HttpURLConnection

    connection.requestMethod = method
    connection.setRequestProperty("apikey", ADMIN_SUPABASE_KEY)
    connection.setRequestProperty("Authorization", "Bearer $accessToken")
    connection.setRequestProperty("Content-Type", "application/json")

    return connection
}

fun loadCurrentAccountEmail(accessToken: String): String {
    val connection = authConnection(accessToken, "GET")

    try {
        val code = connection.responseCode
        if (code !in 200..299) {
            throw Exception(
                connection.errorStream?.bufferedReader()?.readText()
                    ?: "تعذر تحميل بيانات الحساب."
            )
        }

        val json = JSONObject(
            connection.inputStream.bufferedReader().readText()
        )

        return json.optString("email", "")
    } finally {
        connection.disconnect()
    }
}

fun updateAccountEmail(
    accessToken: String,
    newEmail: String
) {
    val email = newEmail.trim()

    if (email.isBlank() || !email.contains("@")) {
        throw Exception("أدخل بريدًا إلكترونيًا صحيحًا.")
    }

    val connection = authConnection(accessToken, "PUT")

    try {
        connection.doOutput = true

        val body = JSONObject().apply {
            put("email", email)
        }.toString()

        connection.outputStream.use {
            it.write(body.toByteArray(Charsets.UTF_8))
        }

        val code = connection.responseCode

        if (code !in 200..299) {
            throw Exception(
                "HTTP $code: " +
                    (connection.errorStream?.bufferedReader()?.readText()
                        ?: "تعذر تغيير البريد الإلكتروني.")
            )
        }
    } finally {
        connection.disconnect()
    }
}

fun updateAccountPassword(
    accessToken: String,
    currentPassword: String,
    newPassword: String,
    confirmPassword: String
) {
    if (newPassword.length < 8) {
        throw Exception("كلمة المرور الجديدة يجب أن تكون 8 أحرف على الأقل.")
    }

    if (newPassword != confirmPassword) {
        throw Exception("تأكيد كلمة المرور غير مطابق.")
    }

    val connection = authConnection(accessToken, "PUT")

    try {
        connection.doOutput = true

        val body = JSONObject().apply {
            if (currentPassword.isNotBlank()) {
                put("current_password", currentPassword)
            }
            put("password", newPassword)
        }.toString()

        connection.outputStream.use {
            it.write(body.toByteArray(Charsets.UTF_8))
        }

        val code = connection.responseCode

        if (code !in 200..299) {
            throw Exception(
                "HTTP $code: " +
                    (connection.errorStream?.bufferedReader()?.readText()
                        ?: "تعذر تغيير كلمة المرور.")
            )
        }
    } finally {
        connection.disconnect()
    }
}

@Composable
fun AccountSettingsScreen(
    currentEmail: String,
    loading: Boolean,
    onRefresh: () -> Unit,
    onChangeEmail: (String) -> Unit,
    onChangePassword: (String, String, String) -> Unit
) {
    var newEmail by remember(currentEmail) { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val currentPasswordFocus = remember { FocusRequester() }
    val newPasswordFocus = remember { FocusRequester() }
    val confirmPasswordFocus = remember { FocusRequester() }

    fun submitPasswordChange() {
        if (!loading && newPassword.isNotBlank() && confirmPassword.isNotBlank()) {
            focusManager.clearFocus()
            onChangePassword(
                currentPassword,
                newPassword,
                confirmPassword
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "إعدادات الحساب ⚙",
                color = AdminGold,
                fontSize = 24.sp
            )

            OutlinedButton(
                onClick = onRefresh,
                enabled = !loading
            ) {
                Text(if (loading) "جاري..." else "تحديث")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AdminPanel)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("البريد الحالي", color = AdminCream)

                Text(
                    text = if (currentEmail.isBlank()) "غير متوفر" else currentEmail,
                    color = AdminGold,
                    fontSize = 17.sp
                )

                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("البريد الإلكتروني الجديد") },
                    singleLine = true
                )

                Button(
                    onClick = {
                        onChangeEmail(newEmail)
                    },
                    enabled = !loading && newEmail.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdminGold,
                        contentColor = AdminBlack
                    )
                ) {
                    Text("تغيير البريد الإلكتروني")
                }

                Text(
                    text = "قد يرسل Supabase رسالة تأكيد إلى البريد الجديد قبل اعتماده.",
                    color = AdminCream,
                    fontSize = 13.sp
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AdminPanel)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "تغيير كلمة المرور 🔐",
                    color = AdminGold,
                    fontSize = 19.sp
                )

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(currentPasswordFocus),
                    label = { Text("كلمة المرور الحالية") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            newPasswordFocus.requestFocus()
                        }
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(newPasswordFocus),
                    label = { Text("كلمة المرور الجديدة") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            confirmPasswordFocus.requestFocus()
                        }
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(confirmPasswordFocus),
                    label = { Text("تأكيد كلمة المرور الجديدة") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            submitPasswordChange()
                        }
                    ),
                    singleLine = true
                )

                Button(
                    onClick = {
                        submitPasswordChange()
                    },
                    enabled = !loading && newPassword.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdminGold,
                        contentColor = AdminBlack
                    )
                ) {
                    Text("تغيير كلمة المرور")
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "استخدم كلمة مرور قوية من 8 أحرف أو أكثر.",
                    color = AdminCream,
                    fontSize = 13.sp
                )
            }
        }
    }
}
