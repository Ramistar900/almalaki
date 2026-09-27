package com.almalaki.cafe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val LOGIN_URL =
    "https://duvxxskgdmgrtaleedqu.supabase.co"

private const val LOGIN_KEY =
    "sb_publishable_L-BfALjb0TkdTlyLTWbFIg_aez3fjQd"

private const val LOGIN_OWNER_ID =
    "ab88911b-6713-4cf3-84be-5a1d9128281a"

private val LoginGold = Color(0xFFD4AF37)
private val LoginBlack = Color(0xFF050505)
private val LoginCream = Color(0xFFF5F0E5)

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onSuccess: (String) -> Unit
) {

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginBlack)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Royal Coffee",
            color = LoginGold,
            fontSize = 36.sp
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "دخول المالك 👑",
            color = LoginCream,
            fontSize = 24.sp
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    "البريد الإلكتروني",
                    color = LoginGold
                )
            }
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    "كلمة المرور",
                    color = LoginGold
                )
            }
        )

        Spacer(
            modifier = Modifier.height(15.dp)
        )

        Button(
            onClick = {

                loading = true
                message = ""

                Thread {

                    try {

                        val result =
                            loginToSupabase(
                                email.trim(),
                                password
                            )

                        val token =
                            result.getString(
                                "access_token"
                            )

                        val userId =
                            result
                                .getJSONObject("user")
                                .getString("id")

                        if (userId == LOGIN_OWNER_ID) {

                            onSuccess(token)

                        } else {

                            message =
                                "هذا الحساب ليس حساب المالك."
                        }

                    } catch (e: Exception) {

                        message =
                            e.message
                                ?: "فشل تسجيل الدخول."
                    }

                    loading = false

                }.start()

            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = LoginGold,
                contentColor = LoginBlack
            )
        ) {

            Text(
                if (loading)
                    "جاري الدخول..."
                else
                    "دخول"
            )
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        TextButton(
            onClick = onBack
        ) {

            Text(
                "العودة",
                color = LoginGold
            )
        }

        if (message.isNotEmpty()) {

            Spacer(
                modifier = Modifier.height(8.dp)
