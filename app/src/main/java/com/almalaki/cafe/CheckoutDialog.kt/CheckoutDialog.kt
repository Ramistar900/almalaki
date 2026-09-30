package com.almalaki.cafe

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
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
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
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
            }
        )

        return
    }

    AlertDialog(
        onDismissRequest = {
            if (!loading) {
                onClose()
            }
        },

        containerColor = CardBlack,

        title = {
            Text(
                "تأكيد الطلب",
                color = Gold,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        },

        text = {
            Column {

                OutlinedTextField(
                    customerName,
                    { customerName = it },
                    Modifier.fillMaxWidth(),
                    label = {
                        Text("الاسم")
                    },
                    singleLine = true
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                OutlinedTextField(
                    customerPhone,
                    { customerPhone = it },
                    Modifier.fillMaxWidth(),
                    label = {
                        Text("رقم الهاتف (اختياري)")
                    },
                    singleLine = true
                )

                Spacer(
                    Modifier.height(10.dp)
                )

                Text(
                    "طريقة استلام الطلب",
                    color = Gold,
                    fontSize = 15.sp
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceEvenly
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        RadioButton(
                            fulfillmentType == "داخل المحل",
                            {
                                fulfillmentType =
                                    "داخل المحل"
                            },
                            enabled = !loading
                        )

                        Text(
                            "داخل المحل",
                            color = Cream
                        )
                    }

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        RadioButton(
                            fulfillmentType ==
                                "توصيل إلى المنزل",
                            {
                                fulfillmentType =
                                    "توصيل إلى المنزل"
                            },
                            enabled = !loading
                        )

                        Text(
                            "توصيل للمنزل",
                            color = Cream
                        )
                    }
                }

                if (fulfillmentType ==
                    "توصيل إلى المنزل"
                ) {
                    Spacer(
                        Modifier.height(8.dp)
                    )

                    OutlinedTextField(
                        deliveryAddress,
                        {
                            deliveryAddress = it
                        },
                        Modifier.fillMaxWidth(),
                        label = {
                            Text("عنوان التوصيل")
                        },
                        minLines = 2
                    )
                }

                Spacer(
                    Modifier.height(10.dp)
                )

                Text(
                    "المبلغ المطلوب: ${
                        formatPrice(totalAmount)
                    }",
                    color = GoldLight,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                if (message.isNotEmpty()) {
                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Text(
                        message,
                        color = Color.Red
                    )
                }
            }
        },

        dismissButton = {
            TextButton(
                onClick = onClose,
                enabled = !loading
            ) {
                Text(
                    "إلغاء",
                    color = Gold
                )
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        customerName,
                        customerPhone,
                        deliveryAddress,
                        fulfillmentType
                    )
                },

                enabled = !loading,

                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = Black
                )
            ) {
                Text(
                    if (loading)
                        "جاري إرسال الطلب..."
                    else
                        "تأكيد الطلب"
                )
            }
        }
    )
}
