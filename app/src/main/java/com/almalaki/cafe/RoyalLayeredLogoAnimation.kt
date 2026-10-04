package com.almalaki.cafe

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset

private const val RC_FRONT_PAUSE_MS = 5_000
private const val RC_TURN_MS = 2_400

/*
 * مدة مرور اللمعة على الدرع والتاج.
 */
private const val SHINE_MS = 4_800


/**
 * ============================================================
 * الشعار الملكي بطبقتين
 * ============================================================
 *
 * الطبقة الأولى:
 * التاج + الدرع
 *
 * ثابتة تمامًا.
 * لا دوران.
 * لا تكبير.
 *
 * عليها:
 * ✨ لمعة ذهبية تمر على الدرع.
 * ✨ ومضة نجمة ذهبية براقة.
 * 👑 لمعة مستقلة تمر على التاج.
 *
 *
 * الطبقة الثانية:
 * RC
 *
 * دوران 3D حول المحور العمودي فقط.
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

        /*
         * ─────────────────────────────────────────
         * الطبقة الأولى
         * التاج + الدرع
         * ─────────────────────────────────────────
         */
        RoyalCrownShieldShine(
            drawableRes = crownShieldRes,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize()
        )

        /*
         * ─────────────────────────────────────────
         * الطبقة الثانية
         * RC
         * ─────────────────────────────────────────
         */
        RoyalRC3D(
            drawableRes = rcRes,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize()
        )
    }
}


/**
 * ============================================================
 * التاج + الدرع
 * ============================================================
 *
 * الصورة نفسها ثابتة.
 *
 * لا دوران.
 * لا تكبير.
 * لا تصغير.
 *
 * التأثيرات:
 *
 * 1. شعاع ذهبي يمر على كامل الشعار.
 *
 * 2. نجمة ذهبية صغيرة براقة
 *    تعبر منطقة الدرع.
 *
 * 3. لمعة مستقلة وناعمة
 *    تمر فوق التاج.
 */
@Composable
fun RoyalCrownShieldShine(
    @DrawableRes drawableRes: Int,
    contentDescription: String? = "Royal crown and shield",
    modifier: Modifier = Modifier
) {

    val transition =
        rememberInfiniteTransition(
            label = "royal_crown_shield_shine"
        )

    /*
     * حركة اللمعة.
     *
     * تبدأ خارج اليسار
     * ثم تمر فوق الشعار
     * وتنتهي خارج اليمين.
     */
    val shineProgress by
        transition.animateFloat(

            initialValue = -0.25f,

            targetValue = 1.25f,

            animationSpec =
                infiniteRepeatable(

                    animation =
                        tween(
                            durationMillis = SHINE_MS,
                            easing = LinearEasing
                        ),

                    repeatMode =
                        RepeatMode.Restart
                ),

            label = "royal_gold_shine_progress"
        )


    Box(

        modifier =
            modifier
                .clipToBounds()
                .graphicsLayer {

                    /*
                     * يجعل تأثير اللمعة
                     * يعمل بشكل صحيح فوق صورة PNG.
                     */
                    compositingStrategy =
                        CompositingStrategy.Offscreen
                }

    ) {

        /*
         * ====================================================
         * PNG الأصلي
         * ====================================================
         *
         * لا يتم تغييره إطلاقًا.
         */
        Image(

            painter =
                painterResource(
                    drawableRes
                ),

            contentDescription =
                contentDescription,

            modifier =
                Modifier.fillMaxSize(),

            contentScale =
                ContentScale.Fit
        )


        /*
         * ====================================================
         * طبقة اللمعان
         * ====================================================
         */
        Canvas(

            modifier =
                Modifier.fillMaxSize()

        ) {

            val w = size.width
            val h = size.height


            /*
             * =================================================
             * ✨ اللمعة الرئيسية
             * =================================================
             *
             * شريط ذهبي مائل
             * يمر فوق كامل الدرع والتاج.
             */
            val beamCenterX =
                w * shineProgress

            val beamWidth =
                w * 0.20f


            val beamBrush =
                Brush.linearGradient(

                    colors =
                        listOf(

                            Color.Transparent,

                            Color(0xFFD4AF37).copy(
                                alpha = 0.12f
                            ),

                            Color(0xFFFFE9A3).copy(
                                alpha = 0.72f
                            ),

                            Color.White.copy(
                                alpha = 0.92f
                            ),

                            Color(0xFFFFE9A3).copy(
                                alpha = 0.72f
                            ),

                            Color(0xFFD4AF37).copy(
                                alpha = 0.12f
                            ),

                            Color.Transparent
                        ),

                    start =
                        Offset(
                            beamCenterX - beamWidth,
                            h
                        ),

                    end =
                        Offset(
                            beamCenterX + beamWidth,
                            0f
                        )
                )


            drawRect(

                brush = beamBrush,

                blendMode =
                    BlendMode.SrcAtop
            )


            /*
             * =================================================
             * ✨ النجمة الذهبية البارقة
             * =================================================
             *
             * ومضة صغيرة تظهر على الدرع
             * أثناء مرور الضوء.
             */
            val starX =
                w * shineProgress

            /*
             * موضع النجمة داخل الدرع.
             */
            val starY =
                h * 0.57f


            val starRadius =
                w * 0.055f

            val rayLength =
                w * 0.105f


            /*
             * نجعل النجمة تخفت
             * عند بداية ونهاية المسار.
             */
            val starAlpha =

                if (
                    shineProgress in
                    0.03f..0.97f
                ) {

                    1f

                } else {

                    0.35f
                }


            val diagonal =
                rayLength * 0.70f


            /*
             * ─────────────────────────────────
             * الشعاع الأفقي
             * ─────────────────────────────────
             */
            drawLine(

                color =
                    Color.White.copy(
                        alpha =
                            0.92f *
                                starAlpha
                    ),

                start =
                    Offset(
                        starX - rayLength,
                        starY
                    ),

                end =
                    Offset(
                        starX + rayLength,
                        starY
                    ),

                strokeWidth =
                    w * 0.012f,

                blendMode =
                    BlendMode.SrcAtop
            )


            /*
             * ─────────────────────────────────
             * الشعاع العمودي
             * ─────────────────────────────────
             */
            drawLine(

                color =
                    Color(0xFFFFE9A3).copy(
                        alpha =
                            0.98f *
                                starAlpha
                    ),

                start =
                    Offset(
                        starX,
                        starY - rayLength
                    ),

                end =
                    Offset(
                        starX,
                        starY + rayLength
                    ),

                strokeWidth =
                    w * 0.012f,

                blendMode =
                    BlendMode.SrcAtop
            )


            /*
             * ─────────────────────────────────
             * الشعاع القطري الأول
             * ─────────────────────────────────
             */
            drawLine(

                color =
                    Color.White.copy(
                        alpha =
                            0.72f *
                                starAlpha
                    ),

                start =
                    Offset(
                        starX - diagonal,
                        starY - diagonal
                    ),

                end =
                    Offset(
                        starX + diagonal,
                        starY + diagonal
                    ),

                strokeWidth =
                    w * 0.008f,

                blendMode =
                    BlendMode.SrcAtop
            )


            /*
             * ─────────────────────────────────
             * الشعاع القطري الثاني
             * ─────────────────────────────────
             */
            drawLine(

                color =
                    Color.White.copy(
                        alpha =
                            0.72f *
                                starAlpha
                    ),

                start =
                    Offset(
                        starX - diagonal,
                        starY + diagonal
                    ),

                end =
                    Offset(
                        starX + diagonal,
                        starY - diagonal
                    ),

                strokeWidth =
                    w * 0.008f,

                blendMode =
                    BlendMode.SrcAtop
            )


            /*
             * ─────────────────────────────────
             * مركز النجمة
             * ─────────────────────────────────
             */
            drawCircle(

                color =
                    Color.White.copy(
                        alpha =
                            0.98f *
                                starAlpha
                    ),

                radius =
                    starRadius,

                center =
                    Offset(
                        starX,
                        starY
                    ),

                blendMode =
                    BlendMode.SrcAtop
            )


            /*
             * =================================================
             * 👑 لمعة التاج
             * =================================================
             *
             * أهدأ وأنحف من لمعة الدرع.
             */
            val crownX =
                w *
                    (
                        shineProgress -
                            0.18f
                    )


            val crownBrush =
                Brush.linearGradient(

                    colors =
                        listOf(

                            Color.Transparent,

                            Color(0xFFFFE9A3).copy(
                                alpha = 0.08f
                            ),

                            Color.White.copy(
                                alpha = 0.72f
                            ),

                            Color(0xFFFFE9A3).copy(
                                alpha = 0.20f
                            ),

                            Color.Transparent
                        ),

                    start =
                        Offset(
                            crownX -
                                w * 0.10f,

                            h * 0.38f
                        ),

                    end =
                        Offset(
                            crownX +
                                w * 0.10f,

                            h * 0.10f
                        )
                )


            drawRect(

                brush =
                    crownBrush,

                blendMode =
                    BlendMode.SrcAtop
            )
        }
    }
}


/**
 * ============================================================
 * RC 3D
 * ============================================================
 *
 * دوران حول المحور العمودي Y.
 *
 * لا يوجد:
 * - دوران Z
 * - تكبير
 * - تصغير
 */
@Composable
fun RoyalRC3D(
    @DrawableRes drawableRes: Int,
    contentDescription: String? = "RC logo",
    modifier: Modifier = Modifier
) {

    val transition =
        rememberInfiniteTransition(
            label = "royal_rc_3d"
        )


    val rotationY by
        transition.animateFloat(

            initialValue = 0f,

            targetValue = 360f,

            animationSpec =
                infiniteRepeatable(

                    animation =
                        keyframes {

                            durationMillis =
                                RC_FRONT_PAUSE_MS +
                                    RC_TURN_MS

                            /*
                             * أمامي.
                             */
                            0f at 0

                            /*
                             * توقف 5 ثوانٍ.
                             */
                            0f at
                                RC_FRONT_PAUSE_MS

                            /*
                             * دوران 3D كامل.
                             */
                            360f at
                                RC_FRONT_PAUSE_MS +
                                    RC_TURN_MS
                        },

                    repeatMode =
                        RepeatMode.Restart
                ),

            label = "rc_rotation_y"
        )


    Image(

        painter =
            painterResource(
                drawableRes
            ),

        contentDescription =
            contentDescription,

        modifier =
            modifier.graphicsLayer {

                /*
                 * دوران حول المحور العمودي فقط.
                 */
                this.rotationY =
                    rotationY

                /*
                 * منظور 3D.
                 */
                cameraDistance =
                    24f * density
            },

        contentScale =
            ContentScale.Fit
    )
}


/**
 * ============================================================
 * الاستخدام النهائي للشعار
 * ============================================================
 *
 * يعتمد على طبقتي PNG الحقيقيتين:
 *
 * royal_crest_layer.png
 * royal_rc_layer.png
 */
@Composable
fun RoyalAppLogo(
    modifier: Modifier = Modifier
) {

    RoyalLayeredLogoAnimation(

        crownShieldRes =
            R.drawable.royal_crest_layer,

        rcRes =
            R.drawable.royal_rc_layer,

        modifier =
            modifier
    )
}
