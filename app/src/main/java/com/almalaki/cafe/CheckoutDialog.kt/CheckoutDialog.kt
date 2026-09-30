package com.almalaki.cafe

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CheckoutDialog(
    totalAmount: Double,
    loading: Boolean,
    message: String,
    successOrderNumber: String,
    onClose: () -> Unit,
    onConfirm: (
        String,
        String,
        String,
        String
    ) -> Unit
) {
    var customerName by remember {
        mutableStateOf("")
    }

    var customerPhone by remember {
        mutableStateOf("")
    }

    var deliveryAddress by remember {
        mutableStateOf("")
    }

    var fulfillmentType by remember {
        mutableStateOf("داخل المحل")
    }

    if (successOrderNumber.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onClose,
            containerColor = CardBlack,

            title = {
                Text(
                    "تم تأكيد الطلب ✅",
                    color = Gold,
                    fontSize = 24.sp
                )
            },

            text = {
                Column {
                    Text(
                        "تم إرسال طلبك بنجاح.",
                        color = Cream,
                        fontSize = 16.sp
                    )

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    Text(
                        "رقم الطلب",
                        color = Gold
                    )

                    Text(
                        successOrderNumber,
                        color = GoldLight,
                        fontSize = 22.sp
                    )
                }
            },

            confirmButton = {
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = Black
                    )
                ) {
                    Text("إغلاق")
                }
