package com.almalaki.cafe.royaltv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * طبقة نصوص ROYAL TV.
 *
 * هذه الطبقة:
 *
 * 1. تقرأ جميع النصوص من RoyalTVTextManager.
 * 2. تعرض النصوص المفعلة فقط.
 * 3. تحسب مكان النص حسب أبعاد المساحة الحالية.
 * 4. تحسب حجم الخط Responsive.
 * 5. تحافظ على اللون والشفافية.
 *
 * لا تحتوي على أدوات تحكم للمالك حاليًا.
 * التحكم سيأتي في طبقة مستقلة لاحقًا.
 */
@Composable
fun RoyalTVTextOverlay(
    modifier: Modifier = Modifier
) {

    val texts by RoyalTVTextManager.texts.collectAsState()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {

        val density = LocalDensity.current

        val widthPx =
            with(density) {
                maxWidth.toPx()
            }

        val heightPx =
            with(density) {
                maxHeight.toPx()
            }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            texts
                .filter { it.enabled }
                .forEach { textItem ->

                    val layout =
                        RoyalTVTextResponsive.calculate(
                            text = textItem,
                            availableWidthPx = widthPx,
                            availableHeightPx = heightPx
                        )

                    val offsetX =
                        layout.x
                            .roundToInt()

                    val offsetY =
                        layout.y
                            .roundToInt()

                    val fontSizeSp =
                        with(density) {
                            layout.fontSizePx
                                .toSp()
                        }

                    Text(
                        text = textItem.text,

                        modifier =
                            Modifier
                                .fillMaxSize()
                                .offset {
                                    IntOffset(
                                        x = offsetX,
                                        y = offsetY
                                    )
                                },

                        color =
                            Color(
                                textItem.color
                            ).copy(
                                alpha = layout.alpha
                            ),

                        fontSize =
                            fontSizeSp,

                        fontWeight =
                            FontWeight.Normal
                    )
                }
        }
    }
}
