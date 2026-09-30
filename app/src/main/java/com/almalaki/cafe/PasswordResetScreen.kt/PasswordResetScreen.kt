package com.almalaki.cafe

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PasswordResetScreen(
    recoveryToken: String,
    onFinished: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var success by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CardBlack)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "الملكي",
                    color = Gold,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    if (success) "تم تغيير كلمة المرور"
                    else "استعادة كلمة المرور",
                    color = GoldLight,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                if (success) {
                    Text(
                        "تم تغيير كلمة المرور بنجاح ✅",
                        color = Cream,
                        fontSize = 17.sp
                    )

                    Button(
                        onClick = onFinished,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold,
                            contentColor = Black
                        )
                    ) {
                        Text("العودة إلى تسجيل الدخول")
                    }
                } else {
                    Text(
                        "أدخل كلمة المرور الجديدة ثم أكدها.",
                        color = Cream,
                        fontSize = 15.sp
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            message = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("كلمة المرور الجديدة") },
                        singleLine = true,
                        enabled = !loading,
                        visualTransformation =
                            androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            message = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("تأكيد كلمة المرور") },
                        singleLine = true,
                        enabled = !loading,
                        visualTransformation =
                            androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )

                    if (message.isNotBlank()) {
                        Text(message, color = Color.Red, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            when {
                                newPassword.length < 6 ->
                                    message =
                                        "كلمة المرور يجب أن تكون 6 أحرف أو أكثر."

                                newPassword != confirmPassword ->
                                    message
