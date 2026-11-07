package com.almalaki.cafe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

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
    NOTIFICATION,
    RESERVATIONS,
    ARCHIVE,
    TV,
    PALETTE
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
        RoyalIcon.RESERVATIONS -> RoyalCyan
        RoyalIcon.ARCHIVE -> RoyalSilver
        RoyalIcon.TV -> RoyalPurple
        RoyalIcon.PALETTE -> RoyalPink
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
    onClick: () -> Unit,
    interactive: Boolean = true,
    tint: Color? = null
) {
    val context = LocalContext.current
    val isTV = isRoyalTV(context)
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (interactive) {
                    Modifier.pointerInput(icon) {
                        detectTapGestures {
                            if (!isTV) {
                                scope.launch {
                                    scale.snapTo(1f)
                                    scale.animateTo(
                                        targetValue = 1.12f,
                                        animationSpec = tween(
                                            durationMillis = 120,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                    scale.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(
                                            durationMillis = 180,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                }
                            }
                            if (icon == RoyalIcon.SETTINGS && !isTV) {
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
                    }
                } else {
                    Modifier
                }
            )
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                rotationZ = rotation.value
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = royalUnifiedIconResource(icon)),
            contentDescription = contentDescription,
            modifier = Modifier.size(size * 0.76f),
            colorFilter = ColorFilter.tint(tint ?: Color(0xFFB78B32))
        )
    }
}

private fun royalUnifiedIconResource(icon: RoyalIcon): Int = when (icon) {
    RoyalIcon.HOME -> R.drawable.royal_unified_home
    RoyalIcon.PRODUCTS -> R.drawable.royal_unified_products
    RoyalIcon.ORDERS -> R.drawable.royal_unified_orders
    RoyalIcon.SALES -> R.drawable.royal_unified_sales
    RoyalIcon.TOP_PRODUCTS -> R.drawable.royal_unified_top_products
    RoyalIcon.SETTINGS -> R.drawable.royal_unified_settings
    RoyalIcon.LOGOUT -> R.drawable.royal_unified_logout
    RoyalIcon.CART -> R.drawable.royal_unified_cart
    RoyalIcon.PROFILE -> R.drawable.royal_unified_profile
    RoyalIcon.SEARCH -> R.drawable.royal_unified_search
    RoyalIcon.MENU -> R.drawable.royal_unified_menu
    RoyalIcon.BACK -> R.drawable.royal_unified_back
    RoyalIcon.PLUS -> R.drawable.royal_unified_plus
    RoyalIcon.MINUS -> R.drawable.royal_unified_minus
    RoyalIcon.DELETE -> R.drawable.royal_unified_delete
    RoyalIcon.CHECK -> R.drawable.royal_unified_check
    RoyalIcon.CLOSE -> R.drawable.royal_unified_close
    RoyalIcon.DELIVERY -> R.drawable.royal_unified_delivery
    RoyalIcon.STORE -> R.drawable.royal_unified_store
    RoyalIcon.COFFEE -> R.drawable.royal_unified_coffee
    RoyalIcon.DRINK -> R.drawable.royal_unified_drink
    RoyalIcon.DESSERT -> R.drawable.royal_unified_dessert
    RoyalIcon.JUICE -> R.drawable.royal_unified_juice
    RoyalIcon.NOTIFICATION -> R.drawable.royal_unified_notification
    RoyalIcon.RESERVATIONS -> R.drawable.royal_unified_reservations
    RoyalIcon.ARCHIVE -> R.drawable.royal_unified_archive
    RoyalIcon.TV -> R.drawable.royal_unified_tv
    RoyalIcon.PALETTE -> R.drawable.royal_unified_palette
}
