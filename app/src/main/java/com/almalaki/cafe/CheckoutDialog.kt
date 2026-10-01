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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "royal_checkout_shine"
        )

    val shinePosition by
        infiniteTransition.animateFloat(
            initialValue = -1.2f,
            targetValue = 1.2f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 2600,
                            easing = LinearEasing
                        ),
                    repeatMode = RepeatMode.Restart
                ),
            label = "royal_checkout_shine_position"
        )

    val royalBlack = Color(0xFF050505)
    val royalBlack2 = Color(0xFF0B090D)
    val royalPurple = Color(0xFF21142F)
    val royalPurpleDark = Color(0xFF120B19)
    val royalBronze = Color(0xFF6E4D18)

    val goldDark = Color(0xFF8C6B16)
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFFE9A3)

    val dialogBackground =
        Brush.verticalGradient(
            colors =
                listOf(
                    royalPurpleDark,
                    royalPurple,
                    royalBlack2,
                    royalBlack
                )
        )

    val animatedBorder =
        Brush.linearGradient(
            colors =
                listOf(
                    goldDark,
                    gold,
                    goldLight,
                    Color.White.copy(alpha = 0.95f),
                    gold,
                    royalBronze,
                    goldDark
                ),
            start = androidx.compose.ui.geometry.Offset(
                x = shinePosition * 900f,
                y = 0f
            ),
            end = androidx.compose.ui.geometry.Offset(
                x = shinePosition * 900f + 500f,
                y = 700f
            )
        )

    Dialog(
        onDismissRequest = {
            if (!loading) {
                onClose()
            }
        },
        properties =
            DialogProperties(
                dismissOnBackPress = !loading,
                dismissOnClickOutside = !loading
            )
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        brush = dialogBackground,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 2.dp,
                        brush = animatedBorder,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(16.dp)
        ) {

            if (successOrderNumber.isNotEmpty()) {

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "♛",
                        color = goldLight,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = "تم تأكيد الطلب",
                        color = gold,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "تم إرسال طلبك بنجاح",
                        color = goldLight.copy(alpha = 0.75f),
                        fontSize = 14.sp
                    )

                    Spacer(
                        modifier = Modifier.height(18.dp)
                    )

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    color = gold.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = gold.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "رقم الطلب",
                                color =
                                    goldLight.copy(
                                        alpha = 0.72f
                                    ),
                                fontSize = 12.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(5.dp)
                            )

                            Text(
                                text = successOrderNumber,
                                color = goldLight,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )

                    Button(
                        onClick = {
                            AppSounds.buttonClick(context)
                            onClose()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = gold,
                                contentColor = royalBlack
                            )
                    ) {
                        Text(
                            text = "إغلاق",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

            } else {

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "♛",
                            color = goldLight,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(2.dp)
                        )

                        Text(
                            text = "تأكيد الطلب",
                            color = gold,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(3.dp)
                        )

                        Text(
                            text = "أكمل بيانات طلبك الملكي",
                            color = goldLight.copy(alpha = 0.68f),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    CheckoutField(
                        value = customerName,
                        onValueChange = {
                            customerName = it
                        },
                        label = "الاسم",
                        enabled = !loading,
                        onFocus = {
                            AppSounds.buttonClick(context)
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(9.dp)
                    )

                    CheckoutField(
                        value = customerPhone,
                        onValueChange = {
                            customerPhone = it
                        },
                        label = "رقم الهاتف (اختياري)",
                        enabled = !loading,
                        onFocus = {
                            AppSounds.buttonClick(context)
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(15.dp)
                    )

                    Text(
                        text = "طريقة استلام الطلب",
                        color = gold,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    color =
                                        if (
                                            fulfillmentType ==
                                                "داخل المحل"
                                        ) {
                                            gold.copy(
                                                alpha = 0.12f
                                            )
                                        } else {
                                            royalBlack2
                                        },
                                    shape =
                                        RoundedCornerShape(14.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color =
                                        if (
                                            fulfillmentType ==
                                                "داخل المحل"
                                        ) {
                                            gold.copy(
                                                alpha = 0.45f
                                            )
                                        } else {
                                            gold.copy(
                                                alpha = 0.18f
                                            )
                                        },
                                    shape =
                                        RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        CheckoutChoiceButton(
                            selected =
                                fulfillmentType ==
                                    "داخل المحل",
                            icon = "☕",
                            title = "داخل المحل",
                            enabled = !loading,
                            onClick = {
                                AppSounds.buttonClick(context)
                                fulfillmentType =
                                    "داخل المحل"
                            }
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(7.dp)
                    )

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    color =
                                        if (
                                            fulfillmentType ==
                                                "توصيل إلى المنزل"
                                        ) {
                                            gold.copy(
                                                alpha = 0.12f
                                            )
                                        } else {
                                            royalBlack2
                                        },
                                    shape =
                                        RoundedCornerShape(14.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color =
                                        if (
                                            fulfillmentType ==
                                                "توصيل إلى المنزل"
                                        ) {
                                            gold.copy(
                                                alpha = 0.45f
                                            )
                                        } else {
                                            gold.copy(
                                                alpha = 0.18f
                                            )
                                        },
                                    shape =
                                        RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        CheckoutChoiceButton(
                            selected =
                                fulfillmentType ==
                                    "توصيل إلى المنزل",
                            icon = "🏠",
                            title = "توصيل إلى المنزل",
                            enabled = !loading,
                            onClick = {
                                AppSounds.buttonClick(context)
                                fulfillmentType =
                                    "توصيل إلى المنزل"
                            }
                        )
                    }

                    if (
                        fulfillmentType ==
                            "توصيل إلى المنزل"
                    ) {
                        Spacer(
                            modifier =
                                Modifier.height(10.dp)
                        )

                        CheckoutField(
                            value = deliveryAddress,
                            onValueChange = {
                                deliveryAddress = it
                            },
                            label = "عنوان التوصيل",
                            enabled = !loading,
                            minLines = 2,
                            onFocus = {
                                AppSounds.buttonClick(context)
                            }
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    color =
                                        gold.copy(
                                            alpha = 0.08f
                                        ),
                                    shape =
                                        RoundedCornerShape(15.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color =
                                        gold.copy(
                                            alpha = 0.28f
                                        ),
                                    shape =
                                        RoundedCornerShape(15.dp)
                                )
                                .padding(13.dp)
                    ) {
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Column(
                                modifier =
                                    Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "المبلغ المطلوب",
                                    color =
                                        
