package com.almalaki.royaltv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoyalTVDevicePairingScreen(
    displayId: String,
    checking: Boolean,
    busy: Boolean,
    canPair: Boolean,
    message: String,
    onPair: (String) -> Unit,
    onRetry: () -> Unit
) {
    var activationCode by remember { mutableStateOf("") }
    val background = Color(0xFF080C12)
    val gold = Color(0xFFD4AF37)
    val muted = Color(0xFFB7BEC8)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.76f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ROYAL TV",
                color = gold,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "ربط شاشة المطعم",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "معرّف الشاشة: $displayId",
                color = muted,
                fontSize = 17.sp
            )
            Spacer(Modifier.height(26.dp))

            if (checking) {
                CircularProgressIndicator(color = gold)
            }

            Text(
                text = message,
                color = if (canPair) gold else muted,
                fontSize = 17.sp,
                textAlign = TextAlign.Center
            )

            if (canPair && !checking) {
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = activationCode,
                    onValueChange = { next ->
                        activationCode = next
                            .filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
                            .take(16)
                            .lowercase()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("رمز الربط المكوّن من 16 خانة") },
                    placeholder = { Text("0000000000000000") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = gold,
                        unfocusedBorderColor = muted,
                        focusedLabelColor = gold,
                        unfocusedLabelColor = muted,
                        cursorColor = gold
                    )
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { onPair(activationCode) },
                    enabled = !busy && activationCode.length == 16,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gold,
                        contentColor = background,
                        disabledContainerColor = Color(0xFF4A4430),
                        disabledContentColor = muted
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            color = background,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "ربط التلفاز بالمطعم",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (!checking) {
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onRetry,
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gold,
                        contentColor = background
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إعادة المحاولة", fontSize = 17.sp)
                }
            }
        }
    }
}
