package com.almalaki.cafe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Royal Coffee - Custom icons for Customer and Owner screens.
 *
 * هذا الملف مستقل عن الشاشات.
 * لا يحتوي على أي كود خاص بالطلبات أو Supabase أو الصوت.
 */

enum class RoyalIcon {
    HOME,
    PRODUCTS,
    ORDERS,
    SALES,
    TOP_PRODUCTS,
    SETTINGS,
    LOGOUT,
    CART,
    PROFILE,
    SEARCH,
    MENU,
    BACK,
    PLUS,
    MINUS,
    DELETE,
    CHECK,
    CLOSE,
    DELIVERY,
    STORE,
    COFFEE,
    DRINK,
    DESSERT,
    JUICE,
    NOTIFICATION
}

private val RoyalBlue = Color(0xFF42A5F5)
private val RoyalOrange = Color(0xFFFFA726)
private val RoyalGreen = Color(0xFF66BB6A)
private val RoyalRed = Color(0xFFEF5350)
private val RoyalYellow = Color(0xFFFFD54F)
private val RoyalSilver = Color(0xFFBDBDBD)
private val RoyalPurple = Color(0xFFAB47BC)
private val RoyalCyan = Color(0xFF26C6DA)
private val RoyalPink = Color(0xFFEC407A)

private fun iconColor(icon: RoyalIcon): Color {
    return when (icon) {
        RoyalIcon.HOME -> RoyalBlue
        RoyalIcon.PRODUCTS -> RoyalOrange
        RoyalIcon.ORDERS -> RoyalGreen
        RoyalIcon.SALES -> RoyalRed
        RoyalIcon.TOP_PRODUCTS -> RoyalYellow
        RoyalIcon.SETTINGS -> RoyalSilver
        RoyalIcon.LOGOUT -> RoyalPurple
        RoyalIcon.CART -> RoyalOrange
        RoyalIcon.PROFILE -> RoyalCyan
        RoyalIcon.SEARCH -> RoyalBlue
        RoyalIcon.MENU -> RoyalSilver
        RoyalIcon.BACK -> RoyalSilver
        RoyalIcon.PLUS -> RoyalGreen
        RoyalIcon.MINUS -> RoyalRed
        RoyalIcon.DELETE -> RoyalRed
        RoyalIcon.CHECK -> RoyalGreen
        RoyalIcon.CLOSE -> RoyalRed
        RoyalIcon.DELIVERY -> RoyalPurple
        RoyalIcon.STORE -> RoyalOrange
        RoyalIcon.COFFEE -> RoyalOrange
        RoyalIcon.DRINK -> RoyalCyan
        RoyalIcon.DESSERT -> RoyalPink
        RoyalIcon.JUICE -> RoyalGreen
        RoyalIcon.NOTIFICATION -> RoyalYellow
    }
}

@Composable
fun RoyalIconButton(
    icon: RoyalIcon,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val scale = remember {
        Animatable(1f)
    }

    val rotation = remember {
        Animatable(0f)
    }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(icon) {
                detectTapGestures {
                    /*
                     * تكبير الأيقونة عند الضغط.
                     */
                    scope.launch {
                        scale.snapTo(1f)

                        scale.animateTo(
                            targetValue = 1.18f,
                            animationSpec = tween(
                                durationMillis = 110,
                                easing = FastOutSlowInEasing
                            )
                        )

                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 170,
                                easing = FastOutSlowInEasing
                            )
                        )
                    }

                    /*
                     * ترس الإعدادات يدور 360 درجة
                     * قبل تنفيذ الأمر.
                     */
                    if (icon == RoyalIcon.SETTINGS) {

                        scope.launch {
                            rotation.snapTo(0f)

                            rotation.animateTo(
                                targetValue = 360f,
                                animationSpec = tween(
                                    durationMillis = 520,
                                    easing = FastOutSlowInEasing
                                )
                            )

                            onClick()

                            rotation.snapTo(0f)
                        }

                    } else {
                        onClick()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {

        Canvas(
            modifier = Modifier
                .size(size * 0.72f)
                .graphicsLayer {
                    /*
                     * تطبيق حركة التكبير فعليًا على الرسم.
                     */
                    scaleX = scale.value
                    scaleY = scale.value
                }
        ) {

            val s = this.size.minDimension

            val stroke = s * 0.085f

            val c = iconColor(icon)

            val center = Offset(
                this.size.width / 2f,
                this.size.height / 2f
            )

            val r = this.size.minDimension * 0.34f

            when (icon) {

                // ---------------------------------------------------------
                // HOME
                // ---------------------------------------------------------

                RoyalIcon.HOME -> {

                    val roof = Path().apply {

                        moveTo(
                            center.x - r,
                            center.y - 0.05f * s
                        )

                        lineTo(
                            center.x,
                            center.y - r
                        )

                        lineTo(
                            center.x + r,
                            center.y - 0.05f * s
                        )
                    }

                    drawPath(
                        path = roof,
                        color = c,
                        style = Stroke(
                            width = stroke,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    drawRoundRect(
                        color = c,
                        topLeft = Offset(
                            center.x - r * 0.78f,
                            center.y - 0.05f * s
                        ),
                        size = Size(
                            r * 1.56f,
                            r * 1.05f
                        ),
                        cornerRadius = CornerRadius(
                            stroke,
                            stroke
                        ),
                        style = Stroke(stroke)
                    )

                    drawLine(
                        color = c,
                        start = Offset(
                            center.x,
                            center.y + r
                        ),
                        end = Offset(
                            center.x,
                            center.y + r * 0.28f
                        ),
                        strokeWidth = stroke
                    )
                }

                // ---------------------------------------------------------
                // PRODUCTS
                // ---------------------------------------------------------

                RoyalIcon.PRODUCTS -> {

                    drawRoundRect(
                        color = c,
                        topLeft = Offset(
                            center.x - r,
                            center.y - r * 0.75f
                        ),
                        size = Size(
                            r * 2f,
                            r * 1.7f
                        ),
                        cornerRadius = CornerRadius(stroke),
                        style = Stroke(stroke)
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r,
                            center.y - r * 0.1f
                        ),
                        Offset(
                            center.x + r,
                            center.y - r * 0.1f
                        ),
                        stroke
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.35f,
                            center.y - r * 0.1f
                        ),
                        Offset(
                            center.x - r * 0.35f,
                            center.y + r * 0.75f
                        ),
                        stroke
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x + r * 0.35f,
                            center.y - r * 0.1f
                        ),
                        Offset(
                            center.x + r * 0.35f,
                            center.y + r * 0.75f
                        ),
                        stroke
                    )
                }

                // ---------------------------------------------------------
                // ORDERS
                // ---------------------------------------------------------

                RoyalIcon.ORDERS -> {

                    drawRoundRect(
                        color = c,
                        topLeft = Offset(
                            center.x - r * 0.82f,
                            center.y - r
                        ),
                        size = Size(
                            r * 1.64f,
                            r * 2f
                        ),
                        cornerRadius = CornerRadius(stroke),
                        style = Stroke(stroke)
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.45f,
                            center.y - r * 0.45f
                        ),
                        Offset(
                            center.x + r * 0.45f,
                            center.y - r * 0.45f
                        ),
                        stroke
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.45f,
                            center.y
                        ),
                        Offset(
                            center.x + r * 0.45f,
                            center.y
                        ),
                        stroke
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.45f,
                            center.y + r * 0.45f
                        ),
                        Offset(
                            center.x + r * 0.25f,
                            center.y + r * 0.45f
                        ),
                        stroke
                    )
                }

                // ---------------------------------------------------------
                // SALES
                // ---------------------------------------------------------

                RoyalIcon.SALES -> {

                    drawLine(
                        c,
                        Offset(
                            center.x - r,
                            center.y + r * 0.7f
                        ),
                        Offset(
                            center.x - r * 0.35f,
                            center.y + r * 0.1f
                        ),
                        stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.35f,
                            center.y + r * 0.1f
                        ),
                        Offset(
                            center.x + r * 0.05f,
                            center.y + r * 0.35f
                        ),
                        stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x + r * 0.05f,
                            center.y + r * 0.35f
                        ),
                        Offset(
                            center.x + r,
                            center.y - r * 0.7f
                        ),
                        stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x + r * 0.55f,
                            center.y - r * 0.7f
                        ),
                        Offset(
                            center.x + r,
                            center.y - r * 0.7f
                        ),
                        stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x + r,
                            center.y - r * 0.7f
                        ),
                        Offset(
                            center.x + r,
                            center.y - r * 0.25f
                        ),
                        stroke,
                        cap = StrokeCap.Round
                    )
                }

                // ---------------------------------------------------------
                // TOP PRODUCTS
                // ---------------------------------------------------------

                RoyalIcon.TOP_PRODUCTS -> {

                    val points = mutableListOf<Offset>()

                    for (i in 0 until 10) {

                        val angle =
                            (-90.0 + i * 36.0) * Math.PI / 180.0

                        val rr =
                            if (i % 2 == 0) {
                                r
                            } else {
                                r * 0.43f
                            }

                        points += Offset(
                            center.x + cos(angle).toFloat() * rr,
                            center.y + sin(angle).toFloat() * rr
                        )
                    }

                    val path = Path().apply {

                        moveTo(
                            points[0].x,
                            points[0].y
                        )

                        for (i in 1 until points.size) {
                            lineTo(
                                points[i].x,
                                points[i].y
                            )
                        }

                        close()
                    }

                    drawPath(
                        path = path,
                        color = c,
                        style = Stroke(
                            width = stroke,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // ---------------------------------------------------------
                // SETTINGS
                // ---------------------------------------------------------

                RoyalIcon.SETTINGS -> {

                    rotate(
                        degrees = rotation.value,
                        pivot = center
                    ) {

                        drawCircle(
                            color = c,
                            radius = r * 0.35f,
                            center = center,
                            style = Stroke(stroke)
                        )

                        for (i in 0 until 8) {

                            rotate(
                                degrees = 45f * i,
                                pivot = center
                            ) {

                                drawRoundRect(
                                    color = c,
                                    topLeft = Offset(
                                        center.x - stroke * 0.7f,
                                        center.y - r * 1.08f
                                    ),
                                    size = Size(
                                        stroke * 1.4f,
                                        r * 0.45f
                                    ),
                                    cornerRadius = CornerRadius(
                                        stroke * 0.5f
                                    )
                                )
                            }
                        }

                        drawCircle(
                            color = c,
                            radius = r * 0.62f,
                            center = center,
                            style = Stroke(stroke)
                        )
                    }
                }

                // ---------------------------------------------------------
                // LOGOUT
                // ---------------------------------------------------------

                RoyalIcon.LOGOUT -> {

                    drawRoundRect(
                        color = c,
                        topLeft = Offset(
                            center.x - r,
                            center.y - r
                        ),
                        size = Size(
                            r * 1.05f,
                            r * 2f
                        ),
                        cornerRadius = CornerRadius(stroke),
                        style = Stroke(stroke)
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.15f,
                            center.y
                        ),
                        Offset(
                            center.x + r,
                            center.y
                        ),
                        stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x + r * 0.55f,
                            center.y - r * 0.42f
                        ),
                        Offset(
                            center.x + r,
                            center.y
                        ),
                        stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x + r,
                            center.y
                        ),
                        Offset(
                        
