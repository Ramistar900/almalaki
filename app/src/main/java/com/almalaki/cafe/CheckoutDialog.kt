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
import androidx.compose.foundation.shape.CircleShape
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
            start =
                androidx.compose.ui.geometry.Offset(
                    x = shinePosition * 900f,
                    y = 0f
                ),
            end =
                androidx.compose.ui.geometry.Offset(
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

                /*
                 * ==========================================
                 * دائرة النجاح الملكية
                 * ==========================================
                 */

                val successTransition =
                    rememberInfiniteTransition(
                        label = "royal_success_animation"
                    )

                val successGlow by
                    successTransition.animateFloat(
                        initialValue = 0.45f,
                        targetValue = 1f,
                        animationSpec =
                            infiniteRepeatable(
                                animation =
                                    tween(
                                        durationMillis = 1300,
                                        easing = LinearEasing
                                    ),
                                repeatMode = RepeatMode.Reverse
                            ),
                        label = "royal_success_glow"
                    )

                val successRotation by
                    successTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec =
                            infiniteRepeatable(
                                animation =
                                    tween(
                                        durationMillis = 4200,
                                        easing = LinearEasing
                                    ),
                                repeatMode = RepeatMode.Restart
                            ),
                        label = "royal_success_rotation"
                    )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Box(
                        modifier = Modifier.size(132.dp),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        /*
                         * الحلقة الذهبية الخارجية
                         * تدور ببطء
                         */
                        Box(
                            modifier =
                                Modifier
                                    .size(128.dp)
                                    .graphicsLayer {
                                        rotationZ =
                                            successRotation
                                    }
                                    .border(
                                        width =
                                            (2.5f *
                                                successGlow).dp,
                                        brush =
                                            Brush.sweepGradient(
                                                colors =
                                                    listOf(
                                                        goldDark,
                                                        gold,
                                                        goldLight,
                                                        Color.White.copy(
                                                            alpha = 0.95f
                                                        ),
                                                        gold,
                                                        royalBronze,
                                                        goldDark
                                                    )
                                            ),
                                        shape = CircleShape
                                    )
                        )

                        /*
                         * الدائرة الداخلية:
                         * بنفسجي + أسود
                         */
                        Box(
                            modifier =
                                Modifier
                                    .size(108.dp)
                                    .background(
                                        brush =
                                            Brush.linearGradient(
                                                colors =
                                                    listOf(
                                                        Color(
