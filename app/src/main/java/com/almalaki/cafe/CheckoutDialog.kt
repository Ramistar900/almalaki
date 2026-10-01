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

    val successTransition = rememberInfiniteTransition(label = "success_animation")
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

    val royalBlack = Color(0xFF050505)
    val royalBlack2 = Color(0xFF0B090D)
    val royalPurpleDark = Color(0xFF120B19)
    val royalPurple = Color(0xFF21142F)
    val royalPurpleLight = Color(0xFF38204D)
    val bronze = Color(0xFF6E4D18)
    val goldDark = Color(0xFF8C6B16)
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFFFE9A3)

    val dialogBackground = Brush.verticalGradient(
        colors = listOf(
            royalPurpleDark,
            royalPurple,
            royalBlack2,
            royalBlack
        )
    )

    val animatedBorder = Brush.linearGradient(
        colors = listOf(
            goldDark,
            gold,
            goldLight,
            Color.White.copy(alpha = 0.95f),
            gold,
            bronze,
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
                                rotationZ = successRotation
                                alpha = 0.55f + successGlow * 0.45f
                            }
                            .background(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        goldDark,
                                        gold,
                                        goldLight,
                                        Color.White,
                                        gold,
                                        bronze,
                                        goldDark
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
                                        colors = listOf(
                                            royalPurpleLight,
                                            royalPurpleDark,
                                            royalBlack
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color = gold.copy(
                                        alpha = 0.75f + successGlow * 0.25f
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .border(
                                        width = 1.dp,
                                        color = goldLight.copy(alpha = 0.65f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✓",
                                    color = goldLight,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "♛",
                        color = goldLight,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "تم تأكيد الطلب",
                        color = gold,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "تم إرسال طلبك بنجاح",
                        color = goldLight.copy(alpha = 0.78f),
                        fontSize = 14.sp
                    )

                    Spacer(Modifier.height(18.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        gold.copy(alpha = 0.13f),
                                        royalPurple.copy(alpha = 0.35f)
                                    )
                                ),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = gold.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "رقم الطلب",
                                color = goldLight.copy(alpha = 0.72f),
                                fontSize = 12.sp
                            )

                            Spacer(Modifier.height(5.dp))

                            Text(
                                text = successOrderNumber,
                                color = goldLight,
                                fontSize = 29.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            AppSounds.buttonClick(context)
                            onClose()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
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

                    Spacer(Modifier.height(4.dp))
                }
            } else {
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
                            color = goldLight,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = "تأكيد الطلب",
                            color = gold,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(3.dp))

                        Text(
                            text = "أكمل بيانات طلبك الملكي",
                            color = goldLight.copy(alpha = 0.70f),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    CheckoutField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = "الاسم",
                        enabled = !loading,
                        onFocus = { AppSounds.buttonClick(context) }
                    )

                    Spacer(Modifier.height(9.dp))

                    CheckoutField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = "رقم الهاتف (اختياري)",
                        enabled = !loading,
                        onFocus = { AppSounds.buttonClick(context) }
                    )

                    Spacer(Modifier.height(15.dp))

                    Text(
                        text = "طريقة استلام الطلب",
                        color = gold,
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
                        gold = gold,
                        goldLight = goldLight,
                        royalBlack2 = royalBlack2,
                        onClick = {
                            AppSounds.buttonClick(context)
                            fulfillmentType = "داخل المحل"
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    CheckoutChoiceCard(
                        selected = fulfillmentType == "توصيل إلى المنزل",
                        icon = "🏠",
                        title = "توصيل إلى المنزل",
                        subtitle = "يصلك الطلب إلى عنوانك",
                        enabled = !loading,
                        gold = gold,
                        goldLight = goldLight,
                        royalBlack2 = royalBlack2,
                        onClick = {
                            AppSounds.buttonClick(context)
                            fulfillmentType = "توصيل إلى المنزل"
                        }
                    )

                    if (fulfillmentType == "توصيل إلى المنزل") {
                        Spacer(Modifier.height(10.dp))

                        CheckoutField(
                            value = deliveryAddress,
                            onValueChange = { deliveryAddress = it },
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
                                    colors = listOf(
                                        gold.copy(alpha = 0.13f),
                                        royalPurple.copy(alpha = 0.30f),
                                        gold.copy(alpha = 0.09f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = gold.copy(alpha = 0.45f),
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
                                    color = goldLight.copy(alpha = 0.72f),
                                    fontSize = 12.sp
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = "Royal Coffee",
                                    color = gold.copy(alpha = 0.72f),
                                    fontSize = 11.sp
                                )
                            }

                            Text(
                                text = formatPrice(totalAmount),
                                color = goldLight,
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
                                    color = Color.Red.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color.Red.copy(alpha = 0.30f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Text(
                                text = message,
                                color = Color(0xFFFF8A8A),
                                fontSize = 12.sp
                            )
         
