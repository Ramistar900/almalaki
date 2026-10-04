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
 * Royal Coffee layered logo.
 *
 * Layer 1:
 * Crown + shield.
 * ثابت بدون دوران أو تكبير.
 *
 * Layer 2:
 * RC.
 * دوران 3D حول المحور العمودي rotationY
 * بدون دوران مسطح وبدون تكبير.
 *
 * أسماء ملفات PNG:
 * royal_crest_layer.png
 * royal_rc_layer.png
 */

@Composable
fun RoyalLayeredLogoAnimation(
    @DrawableRes crownShieldRes: Int,
    @DrawableRes rcRes: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Royal Coffee logo"
) {
    Box(
        modifier = modifier.clipToBounds()
    ) {

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
 * Crown + Shield
 *
 * الصورة نفسها ثابتة.
 * لا دوران.
 * لا تكبير.
 *
 * الحركة الحالية محفوظة بشكل مستقل
 * حتى نضيف لمعان الذهب الحقيقي لاحقًا.
 */
@Composable
fun RoyalCrownShieldShine(
    @DrawableRes drawableRes: Int,
    contentDescription: String? = "Royal crown and shield",
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(
        label = "royal_crown_shield_shine"
    )

    val shineX by transition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = SHINE_MS,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "gold_shine_position"
    )

    Box(
        modifier = modifier
    ) {

        Image(
            painter = painterResource(drawableRes),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        /*
         * طبقة الحركة محفوظة بشكل مستقل.
         *
         * سنستبدلها لاحقًا بلمعة ذهبية حقيقية
         * تمر فوق التاج والدرع فقط.
         */
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
 * RC 3D
 *
 * دوران حول المحور العمودي Y.
 *
 * لا يوجد:
 * - دوران Z
 * - تكبير
 * - تصغير
 *
 * يبدأ أماميًا،
 * يتوقف 5 ثوانٍ،
 * ثم يدور 360 درجة،
 * ثم يتوقف 5 ثوانٍ مرة أخرى.
 */
@Composable
fun RoyalRC3D(
    @DrawableRes drawableRes: Int,
    contentDescription: String? = "RC logo",
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(
        label = "royal_rc_3d"
    )

    val rotationY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = RC_FRONT_PAUSE_MS + RC_TURN_MS

                0f at 0

                // توقف أمامي 5 ثوانٍ
                0f at RC_FRONT_PAUSE_MS

                // دوران 3D كامل
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

            // دوران حول المحور العمودي فقط
            this.rotationY = rotationY

            // منظور 3D
            cameraDistance = 24f * density
        },
        contentScale = ContentScale.Fit
    )
}


/**
 * الاستخدام النهائي للشعار.
 *
 * يعتمد على طبقتي PNG الحقيقيتين الموجودتين
 * داخل res/drawable.
 */
@Composable
fun RoyalAppLogo(
    modifier: Modifier = Modifier
) {
    RoyalLayeredLogoAnimation(
        crownShieldRes = R.drawable.royal_crest_layer,
        rcRes = R.drawable.royal_rc_layer,
        modifier = modifier
    )
}
