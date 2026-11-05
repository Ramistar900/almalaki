package com.almalaki.cafe

import android.graphics.drawable.Animatable
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Displays the ROYAL v9 Android VectorDrawable / AnimatedVectorDrawable assets.
 * The drawable starts only while its Compose host is on screen and is stopped
 * when the host leaves composition.
 */
@Composable
fun RoyalAnimatedIcon(
    iconRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val context = LocalContext.current
    val drawable = remember(iconRes) {
        ContextCompat.getDrawable(context, iconRes)?.mutate()
    }

    DisposableEffect(drawable, animated) {
        val motion = drawable as? Animatable
        if (animated) motion?.start() else motion?.stop()
        onDispose { motion?.stop() }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            ImageView(viewContext).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                this.contentDescription = contentDescription
            }
        },
        update = { imageView ->
            imageView.contentDescription = contentDescription
            if (imageView.drawable !== drawable) {
                imageView.setImageDrawable(drawable)
            }
        }
    )
}

/** Resolve customer-entered category names to the closest available food icon. */
fun royalFoodIconResource(category: String): Int? {
    val value = category.trim().lowercase(Locale.ROOT)
    return when {
        listOf("قهوة", "كافي", "كافيه", "coffee", "espresso", "لاتيه").any { value.contains(it, ignoreCase = true) } ->
            R.drawable.royal_coffee_animated
        listOf("عصير", "juice", "سموذي", "smoothie").any { value.contains(it, ignoreCase = true) } ->
            R.drawable.royal_juice_animated
        listOf("شاورما", "shawarma").any { value.contains(it, ignoreCase = true) } ->
            R.drawable.royal_food_shawarma_animated
        listOf("برغر", "برجر", "burger", "hamburger").any { value.contains(it, ignoreCase = true) } ->
            R.drawable.royal_food_burger_animated
        listOf("بيتزا", "pizza").any { value.contains(it, ignoreCase = true) } ->
            R.drawable.royal_food_pizza_animated
        listOf("حلويات", "حلوى", "dessert", "sweets", "كيك", "cake").any { value.contains(it, ignoreCase = true) } ->
            R.drawable.royal_food_dessert_animated
        listOf("شاي", "tea").any { value.contains(it, ignoreCase = true) } ->
            R.drawable.royal_food_tea_animated
        listOf("سلطة", "salad").any { value.contains(it, ignoreCase = true) } ->
            R.drawable.royal_food_salad_animated
        listOf("آيس كريم", "ايس كريم", "بوظة", "ice cream", "ice_cream").any { value.contains(it, ignoreCase = true) } ->
            R.drawable.royal_food_ice_cream_animated
        listOf("ساندويش", "ساندويتش", "سندويش", "sandwich").any { value.contains(it, ignoreCase = true) } ->
            R.drawable.royal_food_sandwich_animated
        else -> null
    }
}

/** Icons reflect the persisted order status; unknown states keep the text-only fallback. */
fun royalOrderStatusIconResource(status: String): Int? = when (status.trim().lowercase(Locale.ROOT)) {
    "new" -> R.drawable.royal_order_new_animated
    "preparing" -> R.drawable.royal_order_preparing_animated
    "ready" -> R.drawable.royal_order_ready_animated
    "completed", "complete" -> R.drawable.royal_purchase_complete_animated
    "cancelled", "canceled" -> R.drawable.royal_order_cancelled_animated
    else -> null
}
