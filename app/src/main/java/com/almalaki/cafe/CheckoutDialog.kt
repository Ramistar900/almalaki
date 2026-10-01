package com.almalaki.cafe

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val CheckoutBlack = Color(0xFF050505)
private val CheckoutBlack2 = Color(0xFF0B090D)
private val CheckoutPurple = Color(0xFF21142F)
private val CheckoutPurpleDark = Color(0xFF120B19)
private val CheckoutPurpleLight = Color(0xFF38204D)
private val CheckoutGold = Color(0xFFD4AF37)
private val CheckoutGoldLight = Color(0xFFFFE9A3)
private val CheckoutGoldDark = Color(0xFF8C6B16)
private val CheckoutBronze = Color(0xFF6E4D18)

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

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var fulfillmentType by remember { mutableStateOf("داخل المحل") }

    LaunchedEffect(successOrderNumber) {
        if (successOrderNumber.isNotEmpty()) {
            AppSounds.orderSuccess(context)
        }
    }

    val transition = rememberInfiniteTransition(label = "checkout_border")
    val shinePosition by transition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "checkout_shine"
    )

    val successTransition = rememberInfiniteTransition(label = "checkout_success")
    val successGlow by successTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "success_glow"
    )
    val successRotation by successTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "success_rotation"
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            CheckoutGoldDark,
            CheckoutGold,
            CheckoutGoldLight,
            Color.White.copy(alpha = 0.95f),
            CheckoutGold,
            CheckoutBronze,
            CheckoutGoldDark
        ),
        start = androidx.compose.ui.geometry.Offset(shinePosition * 900f, 0f),
        end = androidx.compose.ui.geometry.Offset(shinePosition * 900f + 500f, 700f)
    )

    Dialog(
        onDismissRequest = {
            if (!loading) onClose()
        },
        properties = DialogProperties(
            dismissOnBackPress = !loading,
            dismissOnClickOutside = !loading
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            CheckoutPurpleDark,
                            CheckoutPurple,
                            CheckoutBlack2,
                            CheckoutBlack
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 2.dp,
                    brush = borderBrush,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(16.dp)
        ) {
            if (successOrderNumber.isNotEmpty()) {
                CheckoutSuccessContent(
                    orderNumber = successOrderNumber,
                    rotation = successRotation,
                    glow = successGlow,
                    onClose = {
                        AppSounds.buttonClick(context)
                        onClose()
                    }
                )
            } else {
                CheckoutFormContent(
                    customerName = customerName,
                    onCustomerNameChange = { customerName = it },
                    customerPhone = customerPhone,
                    onCustomerPhoneChange = { customerPhone = it },
                    deliveryAddress = deliveryAddress,
                    onDeliveryAddressChange = { deliveryAddress = it },
                    fulfillmentType = fulfillmentType,
                    onFulfillmentTypeChange = {
                        AppSounds.buttonClick(context)
                        fulfillmentType = it
                    },
                    totalAmount = totalAmount,
                    loading = loading,
                    message = message,
                    context = context,
                    onClose = {
                        AppSounds.buttonClick(context)
                        onClose()
                    },
                    onConfirm = {
                        AppSounds.buttonClick(context)
                        onConfirm(
                            customerName,
                            customerPhone,
                            deliveryAddress,
                            fulfillmentType
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CheckoutSuccessContent(
    orderNumber: String,
    rotation: Float,
    glow: Float,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(128.dp)
                .graphicsLayer {
                    rotationZ = rotation
                    alpha = 0.55f + glow * 0.45f
                }
                .background(
                    brush = Brush.sweepGradient(
                        listOf(
                            CheckoutGoldDark,
                            CheckoutGold,
                            CheckoutGoldLight,
                            Color.White,
                            CheckoutGold,
                            CheckoutBronze,
                            CheckoutGoldDark
                        )
                    ),
                    shape = CircleShape
                )
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(
                                CheckoutPurpleLight,
                                CheckoutPurpleDark,
                                CheckoutBlack
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = CheckoutGold.copy(alpha = 0.75f + glow * 0.25f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .border(
                            width = 1.dp,
                            color = CheckoutGoldLight.copy(alpha = 0.65f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = CheckoutGoldLight,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "♛",
            color = CheckoutGoldLight,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "تم تأكيد الطلب",
            color = CheckoutGold,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "تم إرسال طلبك بنجاح",
            color = CheckoutGoldLight.copy(alpha = 0.78f),
            fontSize = 14.sp
        )

        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            CheckoutGold.copy(alpha = 0.13f),
                            CheckoutPurple.copy(alpha = 0.35f)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .border(
                    width = 1.dp,
                    color = CheckoutGold.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "رقم الطلب",
                    color = CheckoutGoldLight.copy(alpha = 0.72f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = orderNumber,
                    color = CheckoutGoldLight,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CheckoutGold,
                contentColor = CheckoutBlack
            )
        ) {
            Text(
                text = "إغلاق",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun CheckoutFormContent(
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    customerPhone: String,
    onCustomerPhoneChange: (String) -> Unit,
    deliveryAddress: String,
    onDeliveryAddressChange: (String) -> Unit,
    fulfillmentType: String,
    onFulfillmentTypeChange: (String) -> Unit,
    totalAmount: Double,
    loading: Boolean,
    message: String,
    context: android.content.Context,
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "♛",
                color = CheckoutGoldLight,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "تأكيد الطلب",
                color = CheckoutGold,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "أكمل بيانات طلبك الملكي",
                color = CheckoutGoldLight.copy(alpha = 0.70f),
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        CheckoutField(
            value = customerName,
            onValueChange = onCustomerNameChange,
            label = "الاسم",
            enabled = !loading,
            onFocus = { AppSounds.buttonClick(context) }
        )

        Spacer(Modifier.height(9.dp))

        CheckoutField(
            value = customerPhone,
            onValueChange = onCustomerPhoneChange,
            label = "رقم الهاتف (اختياري)",
            enabled = !loading,
            onFocus = { AppSounds.buttonClick(context) }
        )

        Spacer(Modifier.height(15.dp))

        Text(
            text = "طريقة استلام الطلب",
            color = CheckoutGold,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))

        CheckoutChoiceCard(
            selected = fulfillmentType == "داخل المحل",
            icon = "☕",
            title = "داخل المحل",
            subtitle = "استلام طلبك من الكافيه",
            enabled = !loading,
            onClick = { onFulfillmentTypeChange("داخل المحل") }
        )

        Spacer(Modifier.height(8.dp))

        CheckoutChoiceCard(
            selected = fulfillmentType == "توصيل إلى المنزل",
            icon = "🏠",
            title = "توصيل إلى المنزل",
            subtitle = "يصلك الطلب إلى عنوانك",
            enabled = !loading,
            onClick = { onFulfillmentTypeChange("توصيل إلى المنزل") }
        )

        if (fulfillmentType == "توصيل إلى المنزل") {
            Spacer(Modifier.height(10.dp))
            CheckoutField(
                value = deliveryAddress,
                onValueChange = onDeliveryAddressChange,
                label = "عنوان التوصيل",
                enabled = !loading,
                minLines = 2,
                onFocus = { AppSounds.buttonClick(context) }
            )
        }

        Spacer(Modifier.height(15.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(
                            CheckoutGold.copy(alpha = 0.13f),
                            CheckoutPurple.copy(alpha = 0.30f),
                            CheckoutGold.copy(alpha = 0.09f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = CheckoutGold.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الإجمالي المطلوب",
                        color = CheckoutGoldLight.copy(alpha = 0.72f),
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Royal Coffee",
                        color = CheckoutGold.copy(alpha = 0.72f),
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = formatPrice(totalAmount),
                    color = CheckoutGoldLight,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (message.isNotEmpty()) {
            Spacer(Modifier.height(9.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.Red.copy(alpha = 0.08f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        Color.Red.copy(alpha = 0.30f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(10.dp)
            ) {
                Text(
                    text = message,
                    color = Color(0xFFFF8A8A),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onClose,
                enabled = !loading,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "إلغاء",
                    color = CheckoutGold,
                    fontSize = 14.sp
                )
            }

            Button(
                onClick = onConfirm,
                enabled = !loading,
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CheckoutGold,
                    contentColor = CheckoutBlack
                )
            ) {
                Text(
                    text = if (loading) "جاري إرسال الطلب..." else "تأكيد الطلب",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
@Composable
private fun CheckoutField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    minLines: Int = 1,
    onFocus: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                if (focusState.isFocused) onFocus()
            },
        enabled = enabled,
        label = {
            Text(
                text = label,
                color = CheckoutGold
            )
        },
        singleLine = minLines == 1,
        minLines = minLines,
        shape = RoundedCornerShape(13.dp)
    )
}

@Composable
private fun CheckoutChoiceCard(
    selected: Boolean,
    icon: String,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (selected) {
                    CheckoutGold.copy(alpha = 0.13f)
                } else {
                    CheckoutBlack2
                },
                shape = RoundedCornerShape(15.dp)
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) {
                    CheckoutGold.copy(alpha = 0.65f)
                } else {
                    CheckoutGold.copy(alpha = 0.20f)
                },
                shape = RoundedCornerShape(15.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled
        )
        Spacer(Modifier.size(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$icon  $title",
                color = if (selected) CheckoutGoldLight else Color.White.copy(alpha = 0.88f),
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = if (selected) CheckoutGold.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.48f),
                fontSize = 10.sp
            )
        }
    }
}
