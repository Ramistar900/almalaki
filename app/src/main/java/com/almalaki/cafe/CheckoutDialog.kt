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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

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

    LaunchedEffect(successOrderNumber) {
        if (successOrderNumber.isNotEmpty()) {
            AppSounds.orderSuccess(context)
        }
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

                    Spacer(Modifier.height(12.dp))

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
                    onClick = {
                        AppSounds.buttonClick(context)
                        onClose()
                    },
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
                fontSize = 24.sp
            )
        },

        text = {
            Column {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = {
                        customerName = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            if (it.isFocused) {
                                AppSounds.buttonClick(context)
                            }
                        },
                    label = {
                        Text("الاسم")
                    },
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = {
                        customerPhone = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            if (it.isFocused) {
                                AppSounds.buttonClick(context)
                            }
                        },
                    label = {
                        Text("رقم الهاتف (اختياري)")
                    },
                    singleLine = true
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    "طريقة استلام الطلب",
                    color = Gold,
                    fontSize = 15.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = fulfillmentType == "داخل المحل",
                        onClick = {
                            AppSounds.buttonClick(context)
                            fulfillmentType = "داخل المحل"
                        },
                        enabled = !loading
                    )

                    Text(
                        "داخل المحل",
                        color = Cream
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = fulfillmentType == "توصيل إلى المنزل",
                        onClick = {
                            AppSounds.buttonClick(context)
                            fulfillmentType = "توصيل إلى المنزل"
                        },
                        enabled = !loading
                    )

                    Text(
                        "توصيل للمنزل",
                        color = Cream
                    )
                }

                if (fulfillmentType == "توصيل إلى المنزل") {
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = deliveryAddress,
                        onValueChange = {
                            deliveryAddress = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged {
                                if (it.isFocused) {
                                    AppSounds.buttonClick(context)
                                }
                            },
                        label = {
                            Text("عنوان التوصيل")
                        },
                        minLines = 2
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    "المبلغ المطلوب: ${formatPrice(totalAmount)}",
                    color = GoldLight,
                    fontSize = 17.sp
                )

                if (message.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))

                    Text(
                        message,
                        color = androidx.compose.ui.graphics.Color.Red
                    )
                }
            }
        },

        dismissButton = {
            TextButton(
                onClick = {
                    AppSounds.buttonClick(context)
                    onClose()
                },
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
                    AppSounds.buttonClick(context)

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
