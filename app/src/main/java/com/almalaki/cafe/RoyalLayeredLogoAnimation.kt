package com.almalaki.cafe

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

private const val RC_FRONT_PAUSE_MS = 5_000
private const val RC_TURN_MS = 2_400
private const val SHINE_MS = 3_200

/**
 * Independent layered Royal logo animation.
 *
 * Crown + shield: stationary; a moving shine will be added/adjusted when
 * the real transparent PNG is installed.
 *
 * RC: vertical-axis 3D rotation (rotationY), no scaling, with a 5-second
 * front-facing pause after each turn.
 *
 * Expected future assets:
 *   res/drawable/royal_crown_shield.png
 *   res/drawable/royal_rc.png
 */
@Composable
fun RoyalLayeredLogoAnimation(
    @DrawableRes crownShieldRes: Int,
    @DrawableRes rcRes: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Royal Coffee logo"
) {
    Box(modifier = modifier.clipToBounds()) {
        RoyalCrownShieldShine(
            drawableRes = crownShieldRes,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize()
        )

        RoyalRC3D(
            drawableRes = rcRes,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Crown + shield layer.
 * The PNG itself never rotates or scales.
 * A moving highlight is overlaid independently.
 */
@Composable
fun RoyalCrownShieldShine(
    @DrawableRes drawableRes: Int,
    contentDescription: String? = "Royal crown and shield",
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "royal_crown_shield_shine")

    val shineX by transition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(SHINE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gold_shine_position"
    )

    Box(modifier = modifier) {
        Image(
            painter = painterResource(drawableRes),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Reserved independent shine layer.
        // It is intentionally kept subtle until the actual PNG is installed.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = shineX * 180f
                    alpha = 0.10f
                }
        )
    }
}

/**
 * RC rotates around the vertical axis using rotationY.
 * It does not perform a flat Z rotation and does not scale.
 */
@Composable
fun RoyalRC3D(
    @DrawableRes drawableRes: Int,
    contentDescription: String? = "RC logo",
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "royal_rc_3d")

    val rotationY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = RC_FRONT_PAUSE_MS + RC_TURN_MS
                0f at 0
                0f at RC_FRONT_PAUSE_MS
                360f at RC_FRONT_PAUSE_MS + RC_TURN_MS
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "rc_rotation_y"
    )

    Image(
        painter = painterResource(drawableRes),
        contentDescription = contentDescription,
        modifier = modifier.graphicsLayer {
            this.rotationY = rotationY
            cameraDistance = 24f * density
        },
        contentScale = ContentScale.Fit
    )
}

/**
 * Future convenience wrapper.
 * Activate only after the two PNG files exist in res/drawable.
 */
/*
@Composable
fun RoyalAppLogo(modifier: Modifier = Modifier) {
    RoyalLayeredLogoAnimation(
        crownShieldRes = R.drawable.royal_crown_shield,
        rcRes = R.drawable.royal_rc,
        modifier = modifier
    )
}
*/
