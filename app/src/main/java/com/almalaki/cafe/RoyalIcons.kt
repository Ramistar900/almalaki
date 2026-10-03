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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Royal Coffee
 * Custom Icons
 *
 * ملف مستقل عن:
 * - Supabase
 * - الطلبات
 * - المنتجات
 * - الصوت
 *
 * يحتوي فقط على أيقونات واجهة التطبيق وحركاتها.
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

/* =========================
   ألوان الأيقونات
   ========================= */

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

/* =========================
   زر الأيقونة الرئيسي
   ========================= */

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

                    /* حركة التكبير الصغيرة عند الضغط */
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

                    /* ترس الإعدادات يدور */
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

                /* =========================
                   HOME
                   ========================= */

                RoyalIcon.HOME -> {

                    val path = Path().apply {

                        moveTo(
                            center.x - r,
                            center.y
                        )

                        lineTo(
                            center.x,
                            center.y - r
                        )

                        lineTo(
                            center.x + r,
                            center.y
                        )

                        lineTo(
                            center.x + r * 0.82f,
                            center.y
                        )

                        lineTo(
                            center.x + r * 0.82f,
                            center.y + r
                        )

                        lineTo(
                            center.x - r * 0.82f,
                            center.y + r
                        )

                        lineTo(
                            center.x - r * 0.82f,
                            center.y
                        )

                        close()
                    }

                    drawPath(
                        path = path,
                        color = c,
                        style = Stroke(
                            width = stroke,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.25f,
                            center.y + r
                        ),
                        Offset(
                            center.x - r * 0.25f,
                            center.y + r * 0.35f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.25f,
                            center.y + r * 0.35f
                        ),
                        Offset(
                            center.x + r * 0.25f,
                            center.y + r * 0.35f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x + r * 0.25f,
                            center.y + r * 0.35f
                        ),
                        Offset(
                            center.x + r * 0.25f,
                            center.y + r
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                /* =========================
                   PRODUCTS
                   ========================= */

                RoyalIcon.PRODUCTS -> {

                    drawRoundRect(
                        color = c,
                        topLeft = Offset(
                            center.x - r,
                            center.y - r * 0.82f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            r * 2f,
                            r * 1.64f
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            r * 0.15f,
                            r * 0.15f
                        ),
                        style = Stroke(
                            width = stroke
                        )
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.65f,
                            center.y - r * 0.38f
                        ),
                        Offset(
                            center.x + r * 0.65f,
                            center.y - r * 0.38f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.65f,
                            center.y
                        ),
                        Offset(
                            center.x + r * 0.65f,
                            center.y
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.65f,
                            center.y + r * 0.38f
                        ),
                        Offset(
                            center.x + r * 0.65f,
                            center.y + r * 0.38f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                /* =========================
                   ORDERS
                   ========================= */

                RoyalIcon.ORDERS -> {

                    drawRoundRect(
                        color = c,
                        topLeft = Offset(
                            center.x - r * 0.78f,
                            center.y - r
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            r * 1.56f,
                            r * 2f
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            r * 0.12f,
                            r * 0.12f
                        ),
                        style = Stroke(
                            width = stroke
                        )
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.45f,
                            center.y - r * 0.35f
                        ),
                        Offset(
                            center.x + r * 0.45f,
                            center.y - r * 0.35f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
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
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.45f,
                            center.y + r * 0.35f
                        ),
                        Offset(
                            center.x + r * 0.45f,
                            center.y + r * 0.35f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                /* =========================
                   SALES
                   ========================= */

                RoyalIcon.SALES -> {

                    drawLine(
                        c,
                        Offset(
                            center.x - r,
                            center.y + r
                        ),
                        Offset(
                            center.x - r,
                            center.y - r
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r,
                            center.y + r
                        ),
                        Offset(
                            center.x + r,
                            center.y + r
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.65f,
                            center.y + r * 0.45f
                        ),
                        Offset(
                            center.x - r * 0.1f,
                            center.y
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.1f,
                            center.y
                        ),
                        Offset(
                            center.x + r * 0.25f,
                            center.y + r * 0.25f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x + r * 0.25f,
                            center.y + r * 0.25f
                        ),
                        Offset(
                            center.x + r * 0.82f,
                            center.y - r * 0.55f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                /* =========================
                   TOP PRODUCTS
                   ========================= */

                RoyalIcon.TOP_PRODUCTS -> {

                    drawCircle(
                        color = c,
                        radius = r * 0.9f,
                        center = center,
                        style = Stroke(
                            width = stroke
                        )
                    )

                    val star = Path()

                    for (i in 0 until 10) {

                        val angle =
                            (-90.0 + i * 36.0) *
                                Math.PI / 180.0

                        val radius =
                            if (i % 2 == 0) {
                                r * 0.62f
                            } else {
                                r * 0.28f
                            }

                        val x =
                            center.x +
                                (cos(angle) * radius).toFloat()

                        val y =
                            center.y +
                                (sin(angle) * radius).toFloat()

                        if (i == 0) {
                            star.moveTo(x, y)
                        } else {
                            star.lineTo(x, y)
                        }
                    }

                    star.close()

                    drawPath(
                        path = star,
                        color = c,
                        style = Stroke(
                            width = stroke,
                            join = StrokeJoin.Round
                        )
                    )
                }

                /* =========================
                   SETTINGS
                   ========================= */

                RoyalIcon.SETTINGS -> {

                    rotate(
                        degrees = rotation.value,
                        pivot = center
                    ) {

                        drawCircle(
                            color = c,
                            radius = r * 0.36f,
                            center = center,
                            style = Stroke(
                                width = stroke
                            )
                        )

                        for (i in 0 until 8) {

                            val angle =
                                i * 45f * Math.PI / 180f

                            val inner =
                                r * 0.55f

                            val outer =
                                r * 0.92f

                            val start = Offset(
                                center.x +
                                    (cos(angle) * inner)
                                        .toFloat(),
                                center.y +
                                    (sin(angle) * inner)
                                        .toFloat()
                            )

                            val end = Offset(
                                center.x +
                                    (cos(angle) * outer)
                                        .toFloat(),
                                center.y +
                                    (sin(angle) * outer)
                                        .toFloat()
                            )

                            drawLine(
                                c,
                                start,
                                end,
                                strokeWidth = stroke * 1.25f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                /* =========================
                   LOGOUT
                   ========================= */

                RoyalIcon.LOGOUT -> {

                    drawRoundRect(
                        color = c,
                        topLeft = Offset(
                            center.x - r * 0.85f,
                            center.y - r
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            r * 1.05f,
                            r * 2f
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            r * 0.12f,
                            r * 0.12f
                        ),
                        style = Stroke(
                            width = stroke
                        )
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x + r,
                            center.y
                        ),
                        Offset(
                            center.x - r * 0.05f,
                            center.y
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x + r * 0.45f,
                            center.y - r * 0.45f
                        ),
                        Offset(
                            center.x + r,
                            center.y
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x + r,
                            center.y
                        ),
                        Offset(
                            center.x + r * 0.45f,
                            center.y + r * 0.45f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                /* =========================
                   CART
                   ========================= */

                RoyalIcon.CART -> {

                    drawLine(
                        c,
                        Offset(
                            center.x - r,
                            center.y - r * 0.75f
                        ),
                        Offset(
                            center.x - r * 0.65f,
                            center.y + r * 0.55f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.65f,
                            center.y + r * 0.55f
                        ),
                        Offset(
                            center.x + r * 0.75f,
                            center.y + r * 0.55f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.82f,
                            center.y - r * 0.75f
                        ),
                        Offset(
                            center.x - r,
                            center.y - r * 0.75f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawCircle(
                        color = c,
                        radius = stroke * 0.8f,
                        center = Offset(
                            center.x - r * 0.3f,
                            center.y + r * 0.88f
                        )
                    )

                    drawCircle(
                        color = c,
                        radius = stroke * 0.8f,
                        center = Offset(
                            center.x + r * 0.55f,
                            center.y + r * 0.88f
                        )
                    )
                }

                /* =========================
                   PROFILE
                   ========================= */

                RoyalIcon.PROFILE -> {

                    drawCircle(
                        color = c,
                        radius = r * 0.32f,
                        center = Offset(
                            center.x,
                            center.y - r * 0.4f
                        )
                    )

                    drawArc(
                        color = c,
                        startAngle = 205f,
                        sweepAngle = 130f,
                        useCenter = false,
                        topLeft = Offset(
                            center.x - r * 0.75f,
                            center.y - r * 0.05f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            r * 1.5f,
                            r * 1.35f
                        ),
                        style = Stroke(
                            width = stroke,
                            cap = StrokeCap.Round
                        )
                    )
                }

                /* =========================
                   SEARCH
                   ========================= */
                   RoyalIcon.SEARCH -> {

                    drawCircle(
                        color = c,
                        radius = r * 0.58f,
                        center = Offset(
                            center.x - r * 0.18f,
                            center.y - r * 0.18f
                        ),
                        style = Stroke(
                            width = stroke
                        )
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x + r * 0.25f,
                            center.y + r * 0.25f
                        ),
                        Offset(
                            center.x + r * 0.85f,
                            center.y + r * 0.85f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                /* =========================
                   MENU
                   ========================= */

                RoyalIcon.MENU -> {

                    for (i in -1..1) {

                        val y =
                            center.y + i * r * 0.55f

                        drawLine(
                            c,
                            Offset(
                                center.x - r,
                                y
                            ),
                            Offset(
                                center.x + r,
                                y
                            ),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                    }
                }

                /* =========================
                   BACK
                   ========================= */

                RoyalIcon.BACK -> {

                    drawLine(
                        c,
                        Offset(
                            center.x + r * 0.75f,
                            center.y
                        ),
                        Offset(
                            center.x - r * 0.65f,
                            center.y
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.65f,
                            center.y
                        ),
                        Offset(
                            center.x - r * 0.05f,
                            center.y - r * 0.55f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.65f,
                            center.y
                        ),
                        Offset(
                            center.x - r * 0.05f,
                            center.y + r * 0.55f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                /* =========================
                   PLUS
                   ========================= */
                   RoyalIcon.PLUS -> {

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.7f,
                            center.y
                        ),
                        Offset(
                            center.x + r * 0.7f,
                            center.y
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x,
                            center.y - r * 0.7f
                        ),
                        Offset(
                            center.x,
                            center.y + r * 0.7f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                /* =========================
                   MINUS
                   ========================= */

                RoyalIcon.MINUS -> {

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.7f,
                            center.y
                        ),
                        Offset(
                            center.x + r * 0.7f,
                            center.y
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                /* =========================
                   DELETE
                   ========================= */

                RoyalIcon.DELETE -> {

                    drawRoundRect(
                        color = c,
                        topLeft = Offset(
                            center.x - r * 0.55f,
                            center.y - r * 0.55f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            r * 1.1f,
                            r * 1.45f
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            r * 0.08f,
                            r * 0.08f
                        ),
                        style = Stroke(
                            width = stroke
                        )
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.72f,
                            center.y - r * 0.72f
                        ),
                        Offset(
                            center.x + r * 0.72f,
                            center.y - r * 0.72f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.3f,
                            center.y - r * 0.95f
                        ),
                        Offset(
                            center.x + r * 0.3f,
                            center.y - r * 0.95f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                /* =========================
                   CHECK
                   ========================= */
                   RoyalIcon.CHECK -> {

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.75f,
                            center.y
                        ),
                        Offset(
                            center.x - r * 0.2f,
                            center.y + r * 0.55f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.2f,
                            center.y + r * 0.55f
                        ),
                        Offset(
                            center.x + r * 0.85f,
                            center.y - r * 0.65f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                /* =========================
                   CLOSE
                   ========================= */

                RoyalIcon.CLOSE -> {

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.65f,
                            center.y - r * 0.65f
                        ),
                        Offset(
                            center.x + r * 0.65f,
                            center.y + r * 0.65f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x + r * 0.65f,
                            center.y - r * 0.65f
                        ),
                        Offset(
                            center.x - r * 0.65f,
                            center.y + r * 0.65f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                /* =========================
                   DELIVERY
                   ========================= */

                RoyalIcon.DELIVERY -> {

                    drawRoundRect(
                        color = c,
                        topLeft = Offset(
                            center.x - r,
                            center.y - r * 0.5f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            r * 1.2f,
                            r
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            r * 0.1f,
                            r * 0.1f
                        ),
                        style = Stroke(
                            width = stroke
                        )
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x + r * 0.2f,
                            center.y - r * 0.5f
                        ),
                        Offset(
                            center.x + r * 0.2f,
                            center.y + r * 0.5f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x + r * 0.2f,
                            center.y + r * 0.5f
                        ),
                        Offset(
                            center.x + r * 0.85f,
                            center.y + r * 0.5f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawCircle(
                        color = c,
                        radius = stroke * 1.1f,
                        center = Offset(
                            center.x - r * 0.45f,
                            center.y + r * 0.7f
                        )
                    )

                    drawCircle(
                        color = c,
                        radius = stroke * 1.1f,
                        center = Offset(
                            center.x + r * 0.55f,
                            center.y + r * 0.7f
                        )
                    )
                }

                /* =========================
                   STORE
                   ========================= */
                   RoyalIcon.STORE -> {

                    drawRect(
                        color = c,
                        topLeft = Offset(
                            center.x - r * 0.8f,
                            center.y - r * 0.15f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            r * 1.6f,
                            r * 1.15f
                        ),
                        style = Stroke(
                            width = stroke
                        )
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r,
                            center.y - r * 0.15f
                        ),
                        Offset(
                            center.x - r * 0.75f,
                            center.y - r * 0.8f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.75f,
                            center.y - r * 0.8f
                        ),
                        Offset(
                            center.x + r * 0.75f,
                            center.y - r * 0.8f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x + r * 0.75f,
                            center.y - r * 0.8f
                        ),
                        Offset(
                            center.x + r,
                            center.y - r * 0.15f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x,
                            center.y + r
                        ),
                        Offset(
                            center.x,
                            center.y + r * 0.15f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                /* =========================
                   COFFEE
                   ========================= */

                RoyalIcon.COFFEE -> {

                    drawRoundRect(
                        color = c,
                        topLeft = Offset(
                            center.x - r * 0.65f,
                            center.y - r * 0.45f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            r * 1.15f,
                            r * 1.1f
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            r * 0.12f,
                            r * 0.12f
                        ),
                        style = Stroke(
                            width = stroke
                        )
                    )

                    drawArc(
                        color = c,
                        startAngle = -90f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(
                            center.x + r * 0.35f,
                            center.y - r * 0.25f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            r * 0.65f,
                            r * 0.65f
                        ),
                        style = Stroke(
                            width = stroke
                        )
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.75f,
                            center.y + r * 0.8f
                        ),
                        Offset(
                            center.x + r * 0.75f,
                            center.y + r * 0.8f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                /* =========================
                   DRINK
                   ========================= */
                   RoyalIcon.DRINK -> {

                    drawRoundRect(
                        color = c,
                        topLeft = Offset(
                            center.x - r * 0.55f,
                            center.y - r * 0.55f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            r * 1.1f,
                            r * 1.45f
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            r * 0.08f,
                            r * 0.08f
                        ),
                        style = Stroke(
                            width = stroke
                        )
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.25f,
                            center.y - r * 0.85f
                        ),
                        Offset(
                            center.x + r * 0.35f,
                            center.y - r * 0.85f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x + r * 0.05f,
                            center.y - r * 0.85f
                        ),
                        Offset(
                            center.x + r * 0.25f,
                            center.y - r * 1.15f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                /* =========================
                   DESSERT
                   ========================= */

                RoyalIcon.DESSERT -> {

                    drawArc(
                        color = c,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(
                            center.x - r,
                            center.y - r * 0.35f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            r * 2f,
                            r * 1.35f
                        ),
                        style = Stroke(
                            width = stroke
                        )
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r,
                            center.y + r * 0.3f
                        ),
                        Offset(
                            center.x + r,
                            center.y + r * 0.3f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.7f,
                            center.y + r * 0.7f
                        ),
                        Offset(
                            center.x + r * 0.7f,
                            center.y + r * 0.7f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                /* =========================
                   JUICE
                   ========================= */

                RoyalIcon.JUICE -> {

                    drawPath(
                        path = Path().apply {

                            moveTo(
                                center.x - r * 0.65f,
                                center.y - r * 0.55f
                            )

                            lineTo(
                                center.x + r * 0.65f,
                                center.y - r * 0.55f
                            )

                            lineTo(
                                center.x + r * 0.45f,
                                center.y + r * 0.85f
                            )

                            lineTo(
                                center.x - r * 0.45f,
                                center.y + r * 0.85f
                            )

                            close()
                        },
                        color = c,
                        style = Stroke(
                            width = stroke,
                            join = StrokeJoin.Round
                        )
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x,
                            center.y - r * 0.55f
                        ),
                        Offset(
                            center.x + r * 0.25f,
                            center.y - r
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }

                /* =========================
                   NOTIFICATION
                   ========================= */

                RoyalIcon.NOTIFICATION -> {

                    drawArc(
                        color = c,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(
                            center.x - r * 0.7f,
                            center.y - r * 0.55f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            r * 1.4f,
                            r * 1.5f
                        ),
                        style = Stroke(
                            width = stroke,
                            cap = StrokeCap.Round
                        )
                    )

                    drawLine(
                        c,
                        Offset(
                            center.x - r * 0.7f,
                            center.y + r * 0.35f
                        ),
                        Offset(
                            center.x + r * 0.7f,
                            center.y + r * 0.35f
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )

                    drawCircle(
                        color = c,
                        radius = stroke * 0.9f,
                        center = Offset(
                            center.x,
                            center.y + r * 0.65f
                        )
                    )
                }
            }
        }
    }
}
 
   
