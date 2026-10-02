package com.almalaki.cafe.ui.icons

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

/**
 * Royal Coffee - Custom icons for both Customer and Owner screens.
 *
 * The file is intentionally independent from the screens so icon shapes,
 * colors and animations can be changed later without touching screen logic.
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

private fun iconColor(icon: RoyalIcon): Color = when (icon) {
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

@Composable
fun RoyalIconButton(
    icon: RoyalIcon,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(icon) {
                detectTapGestures {
                    scope.launch {
                        scale.snapTo(1f)
                        scale.animateTo(1.18f, tween(110, easing = FastOutSlowInEasing))
                        scale.animateTo(1f, tween(170, easing = FastOutSlowInEasing))
                    }

                    if (icon == RoyalIcon.SETTINGS) {
                        scope.launch {
                            rotation.snapTo(0f)
                            rotation.animateTo(
                                targetValue = 360f,
                                animationSpec = tween(520, easing = FastOutSlowInEasing)
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
        ) {
            val s = size.minDimension
            val stroke = s * 0.085f
            val c = iconColor(icon)
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val r = this.size.minDimension * 0.34f

            when (icon) {
                RoyalIcon.HOME -> {
                    val roof = Path().apply {
                        moveTo(center.x - r, center.y - 0.05f * s)
                        lineTo(center.x, center.y - r)
                        lineTo(center.x + r, center.y - 0.05f * s)
                    }
                    drawPath(roof, c, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawRoundRect(
                        c,
                        topLeft = Offset(center.x - r * 0.78f, center.y - 0.05f * s),
                        size = Size(r * 1.56f, r * 1.05f),
                        cornerRadius = CornerRadius(stroke, stroke),
                        style = Stroke(stroke)
                    )
                    drawLine(c, Offset(center.x, center.y + r), Offset(center.x, center.y + r * 0.28f), stroke)
                }

                RoyalIcon.PRODUCTS -> {
                    drawRoundRect(c, Offset(center.x - r, center.y - r * 0.75f), Size(r * 2f, r * 1.7f), CornerRadius(stroke), style = Stroke(stroke))
                    drawLine(c, Offset(center.x - r, center.y - r * 0.1f), Offset(center.x + r, center.y - r * 0.1f), stroke)
                    drawLine(c, Offset(center.x - r * 0.35f, center.y - r * 0.1f), Offset(center.x - r * 0.35f, center.y + r * 0.75f), stroke)
                    drawLine(c, Offset(center.x + r * 0.35f, center.y - r * 0.1f), Offset(center.x + r * 0.35f, center.y + r * 0.75f), stroke)
                }

                RoyalIcon.ORDERS -> {
                    drawRoundRect(c, Offset(center.x - r * 0.82f, center.y - r), Size(r * 1.64f, r * 2f), CornerRadius(stroke), style = Stroke(stroke))
                    drawLine(c, Offset(center.x - r * 0.45f, center.y - r * 0.45f), Offset(center.x + r * 0.45f, center.y - r * 0.45f), stroke)
                    drawLine(c, Offset(center.x - r * 0.45f, center.y), Offset(center.x + r * 0.45f, center.y), stroke)
                    drawLine(c, Offset(center.x - r * 0.45f, center.y + r * 0.45f), Offset(center.x + r * 0.25f, center.y + r * 0.45f), stroke)
                }

                RoyalIcon.SALES -> {
                    drawLine(c, Offset(center.x - r, center.y + r * 0.7f), Offset(center.x - r * 0.35f, center.y + r * 0.1f), stroke, cap = StrokeCap.Round)
                    drawLine(c, Offset(center.x - r * 0.35f, center.y + r * 0.1f), Offset(center.x + r * 0.05f, center.y + r * 0.35f), stroke, cap = StrokeCap.Round)
                    drawLine(c, Offset(center.x + r * 0.05f, center.y + r * 0.35f), Offset(center.x + r, center.y - r * 0.7f), stroke, cap = StrokeCap.Round)
                    drawLine(c, Offset(center.x + r * 0.55f, center.y - r * 0.7f), Offset(center.x + r, center.y - r * 0.7f), stroke, cap = StrokeCap.Round)
                    drawLine(c, Offset(center.x + r, center.y - r * 0.7f), Offset(center.x + r, center.y - r * 0.25f), stroke, cap = StrokeCap.Round)
                }

                RoyalIcon.TOP_PRODUCTS -> {
                    val points = mutableListOf<Offset>()
                    for (i in 0 until 10) {
                        val angle = (-90f + i * 36f) * Math.PI.toFloat() / 180f
                        val rr = if (i % 2 == 0) r else r * 0.43f
                        points += Offset(center.x + kotlin.math.cos(angle) * rr, center.y + kotlin.math.sin(angle) * rr)
                    }
                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                        close()
                    }
                    drawPath(path, c, style = Stroke(stroke, join = StrokeJoin.Round))
                }

                RoyalIcon.SETTINGS -> {
                    rotate(rotation.value, center) {
                        drawCircle(c, r * 0.35f, center, style = Stroke(stroke))
                        for (i in 0 until 8) {
                            rotate(45f * i, center) {
                                drawRoundRect(c, Offset(center.x - stroke * 0.7f, center.y - r * 1.08f), Size(stroke * 1.4f, r * 0.45f), CornerRadius(stroke * 0.5f))
                            }
                        }
                        drawCircle(c, r * 0.62f, center, style = Stroke(stroke))
                    }
                }

                RoyalIcon.LOGOUT -> {
                    drawRoundRect(c, Offset(center.x - r, center.y - r), Size(r * 1.05f, r * 2f), CornerRadius(stroke), style = Stroke(stroke))
                    drawLine(c, Offset(center.x - r * 0.15f, center.y), Offset(center.x + r, center.y), stroke, cap = StrokeCap.Round)
                    drawLine(c, Offset(center.x + r * 0.55f, center.y - r * 0.42f), Offset(center.x + r, center.y), stroke, cap = StrokeCap.Round)
                    drawLine(c, Offset(center.x + r, center.y), Offset(center.x + r * 0.55f, center.y + r * 0.42f), stroke, cap = StrokeCap.Round)
                }

                RoyalIcon.CART -> {
                    drawLine(c, Offset(center.x - r, center.y - r * 0.65f), Offset(center.x - r * 0.65f, center.y - r * 0.65f), stroke, cap = StrokeCap.Round)
                    drawLine(c, Offset(center.x - r * 0.65f, center.y - r * 0.65f), Offset(center.x - r * 0.35f, center.y + r * 0.45f), stroke, cap = StrokeCap.Round)
                    drawLine(c, Offset(center.x - r * 0.35f, center.y + r * 0.45f), Offset(center.x + r * 0.85f, center.y + r * 0.45f), stroke, cap = StrokeCap.Round)
                    drawLine(c, Offset(center.x - r * 0.1f, center.y - r * 0.05f), Offset(center.x + r * 0.55f, center.y - r * 0.05f), stroke)
                    drawCircle(c, stroke * 1.15f, Offset(center.x - r * 0.1f, center.y + r * 0.85f))
                    drawCircle(c, stroke * 1.15f, Offset(center.x + r * 0.65f, center.y + r * 0.85f))
                }

                RoyalIcon.PROFILE -> {
                    drawCircle(c, r * 0.38f, Offset(center.x, center.y - r * 0.45f), style = Stroke(stroke))
                    drawArc(c, center.x - r, center.y + r * 0.05f, r * 2f, r * 1.35f, 200f, 140f, false, style = Stroke(stroke))
                }

                RoyalIcon.SEARCH -> {
                    drawCircle(c, r * 0.72f, Offset(center.x - r * 0.2f, center.y - r * 0.2f), style = Stroke(stroke))
                    drawLine(c, Offset(center.x + r * 0.35f, center.y + r * 0.35f), Offset(center.x + r, center.y + r), stroke, cap = StrokeCap.Round)
                }

                RoyalIcon.MENU -> {
                    repeat(3) { i ->
                        val y = center.y + (i - 1) * r * 0.55f
                        drawLine(c, Offset(center.x - r, y), Offset(center.x + r, y), stroke, cap = StrokeCap.Round)
                    }
                }

                RoyalIcon.BACK -> {
                    drawLine(c, Offset(center.x + r * 0.8f, center.y), Offset(center.x - r * 0.7f, center.y), stroke, cap = StrokeCap.Round)
                    drawLine(c, Offset(center.x - r * 0.7f, center.y), Offset(center.x - r * 0.05f, center.y - r * 0.6f), stroke, cap = StrokeCap.Round)
                    drawLine(c, Offset(center.x - r * 0.7f, center.y), Offset(center.x - r * 0.05f, center.y + r * 0.6f), stroke, cap = StrokeCap.Round)
                }

                RoyalIcon.PLUS -> {
                    drawLine(c, Offset(center.x - r * 0.8f, center.y), Offset(center.x + r * 0.8f, center.y), stroke, cap = StrokeCap.Round)
                    drawLine(c, Offset(center.x, center.y - r * 0.8f), Offset(center.x, center.y + r * 0.8f), stroke, cap = StrokeCap.Round)
                }

                RoyalIcon.MINUS -> drawLine(c, Offset(center.x - r * 0.8f, center.y), Offset(center.x + r * 0.8f, center.y), stroke, cap = StrokeCap.Round)

                RoyalIcon.DELETE -> {
                    drawRoundRect(c, Offset(center.x - r * 0.65f, center.y - r * 0.55f), Size(r * 1.3f, r * 1.45f), CornerRadius(stroke), style = Stroke(stroke))
                    drawLine(c, Offset(center.x - r * 0.85f, center.y - r * 0.8f), Offset(center.x + r * 0.85f, center.y - r * 0.8f), stroke, cap = StrokeCap.Round)
                    drawLine(c, Offset(center.x - r * 0.35f, center.y - r), Offset(center.x + r * 0.35f, center.y - r), stroke, cap = StrokeCap.Round)
                }

                RoyalIcon.CHECK -> {
                    drawLine(c, Offset(center.x - r * 0.85f, center.y), Offset(center.x - r * 0.2f, center.y + r * 0.65f), stroke, cap = StrokeCap.Round)
                    drawLine(c, Offset(center.x - r * 0.2f, center.y + r * 0.65f), Offset(center.x + r * 0.9f, center.y - r * 0.65f), stroke, cap = StrokeCap.Round)
                }

                RoyalIcon.CLOSE -> {
                    drawLine(c, Offset(center.x - r * 0.7f, center.y - r * 0.7f), Offset(center.x + r * 0.7f, center.y + r * 0.7f), stroke, cap = StrokeCap.Round)
                    drawLine(c, Offset(center.x + r * 0.7f, center.y - r * 0.7f), Offset(center.x - r * 0.7f, center.y + r * 0.7f), stroke, cap = StrokeCap.Round)
                }

                RoyalIcon.DELIVERY -> {
                    drawRoundRect(c, Offset(center.x - r, center.y - r * 0.65f), Size(r * 1.25f, r * 1.15f), CornerRadius(stroke), style = Stroke(stroke))
                    drawRoundRect(c, Offset(center.x + r * 0.25f, center.y - r * 0.35f), Size(r * 0.75f, r * 0.85f), CornerRadius(stroke), style = Stroke(stroke))
                    drawCircle(c, stroke * 1.3f, Offset(center.x - r * 0.45f, center.y + r * 0.7f))
                    drawCircle(c, stroke * 1.3f, Offset(center.x + r * 0.7f, center.y + r * 0.7f))
                }

                RoyalIcon.STORE -> {
                    drawRect(c, Offset(center.x - r, center.y - r * 0.15f), Size(r * 2f, r * 1.25f), style = Stroke(stroke))
                    drawPath(Path().apply {
                        moveTo(center.x - r * 1.05f, center.y - r * 0.15f)
                        lineTo(center.x - r * 0.8f, center.y - r * 0.75f)
                        lineTo(center.x + r * 0.8f, center.y - r * 0.75f)
                        lineTo(center.x + r * 1.05f, center.y - r * 0.15f)
                    }, c, style = Stroke(stroke, join = StrokeJoin.Round))
                    drawLine(c, Offset(center.x, center.y + r * 0.15f), Offset(center.x, center.y + r * 0.95f), stroke)
                }

                RoyalIcon.COFFEE -> {
                    drawRoundRect(c, Offset(center.x - r * 0.75f, center.y - r * 0.25f), Size(r * 1.35f, r * 0.9f), CornerRadius(r * 0.18f), style = Stroke(stroke))
                    drawArc(c, center.x + r * 0.35f, center.y - r * 0.05f, r * 0.65f, r * 0.55f, 270f, 180f, false, style = Stroke(stroke))
                    drawLine(c, Offset(center.x - r * 0.95f, center.y + r * 0.8f), Offset(center.x + r * 0.8f, center.y + r * 0.8f), stroke, cap = StrokeCap.Round)
                    drawLine(c, Offset(center.x - r * 0.35f, center.y - r * 0.8f), Offset(center.x - r * 0.45f, center.y - r * 1.05f), stroke, cap = StrokeCap.Round)
                    drawLine(c, Offset(center.x + r * 0.15f, center.y - r * 0.8f), Offset(center.x + r * 0.05f, center.y - r * 1.05f), stroke, cap = StrokeCap.Round)
                }

                RoyalIcon.DRINK -> {
                    drawRoundRect(c, Offset(center.x - r * 0.65f, center.y - r * 0.75f), Size(r * 1.3f, r * 1.7f), CornerRadius(r * 0.18f), style = Stroke(stroke))
                    drawLine(c, Offset(center.x - r * 0.15f, center.y - r * 1.05f), Offset(center.x + r * 0.15f, center.y - r * 1.05f), stroke)
                    drawLine(c, Offset(center.x + r * 0.05f, center.y - r * 1.05f), Offset(center.x + r * 0.55f, center.y - r * 1.45f), stroke, cap = StrokeCap.Round)
                }

                RoyalIcon.DESSERT -> {
                    drawRoundRect(c, Offset(center.x - r * 0.9f, center.y + r * 0.15f), Size(r * 1.8f, r * 0.3f), CornerRadius(stroke))
                    drawPath(Path().apply {
                        moveTo(center.x - r * 0.65f, center.y + r * 0.45f)
                        lineTo(center.x + r * 0.65f, center.y + r * 0.45f)
                        lineTo(center.x + r * 0.35f, center.y + r)
                        lineTo(center.x - r * 0.35f, center.y + r)
                        close()
                    }, c, style = Stroke(stroke, join = StrokeJoin.Round))
                    drawArc(c, center.x - r * 0.65f, center.y - r * 0.65f, r * 1.3f, r * 0.9f, 180f, 180f, false, style = Stroke(stroke))
                }

                RoyalIcon.JUICE -> {
                    drawRoundRect(c, Offset(center.x - r * 0.7f, center.y - r * 0.7f), Size(r * 1.4f, r * 1.55f), CornerRadius(r * 0.12f), style = Stroke(stroke))
                    drawLine(c, Offset(center.x + r * 0.15f, center.y - r * 0.75f), Offset(center.x + r * 0.55f, center.y - r * 1.3f), stroke, cap = StrokeCap.Round)
                    drawLine(c, Offset(center.x + r * 0.15f, center.y - r * 0.25f), Offset(center.x + r * 0.45f, center.y - r * 0.25f), stroke)
                }

                RoyalIcon.NOTIFICATION -> {
                    drawArc(c, center.x - r * 0.72f, center.y - r * 0.75f, r * 1.44f, r * 1.45f, 200f, 140f, false, style = Stroke(stroke))
                    drawLine(c, Offset(center.x - r * 0.72f, center.y + r * 0.25f), Offset(center.x + r * 0.72f, center.y + r * 0.25f), stroke, cap = StrokeCap.Round)
                    drawCircle(c, stroke * 1.25f, Offset(center.x, center.y + r * 0.65f))
                }
            }
        }
    }
}

/** Convenience icon for owner/admin navigation. */
@Composable
fun RoyalOwnerIcon(icon: RoyalIcon, modifier: Modifier = Modifier, onClick: () -> Unit) {
    RoyalIconButton(icon = icon, modifier = modifier, size = 58.dp, onClick = onClick)
}

/** Convenience icon for customer-facing actions. */
@Composable
fun RoyalCustomerIcon(icon: RoyalIcon, modifier: Modifier = Modifier, onClick: () -> Unit) {
    RoyalIconButton(icon = icon, modifier = modifier, size = 52.dp, onClick = onClick)
}
    
