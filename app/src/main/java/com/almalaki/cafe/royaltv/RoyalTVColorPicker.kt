package com.almalaki.cafe.royaltv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Color Picker مستقل لـ ROYAL TV.
 *
 * يدعم:
 *
 * - Hue
 * - Saturation
 * - Brightness
 * - ألوان جاهزة
 * - معاينة اللون
 * - إعادة اللون النهائي كـ Long
 *
 * لا يقوم هذا الملف بحفظ اللون بنفسه.
 * الحفظ والربط مع النص سيتم في طبقة المحرر.
 */
@Composable
fun RoyalTVColorPicker(
    initialColor: Long = 0xFFFFFFFF,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {

    val initialHsv =
        remember(initialColor) {
            val hsv = FloatArray(3)

            Color(initialColor)
                .getHsv(hsv)

            hsv
        }

    var hue by remember(initialColor) {
        mutableFloatStateOf(
            initialHsv[0]
        )
    }

    var saturation by remember(initialColor) {
        mutableFloatStateOf(
            initialHsv[1]
        )
    }

    var brightness by remember(initialColor) {
        mutableFloatStateOf(
            initialHsv[2]
        )
    }

    var selectedColor by remember(initialColor) {
        mutableStateOf(
            Color.hsv(
                hue = hue,
                saturation = saturation,
                value = brightness
            )
        )
    }

    fun updateColor(
        newHue: Float = hue,
        newSaturation: Float = saturation,
        newBrightness: Float = brightness
    ) {

        hue = newHue.coerceIn(0f, 360f)
        saturation =
            newSaturation.coerceIn(0f, 1f)
        brightness =
            newBrightness.coerceIn(0f, 1f)

        selectedColor =
            Color.hsv(
                hue = hue,
                saturation = saturation,
                value = brightness
            )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = "اختيار لون النص",
            fontSize = 18.sp
        )

        /*
         * معاينة اللون الحالي.
         */
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .background(
                    color = selectedColor,
                    shape = CircleShape
                )
        )

        Text(
            text =
                "Hue: ${hue.roundToInt()}°"
        )

        Slider(
            value = hue,
            onValueChange = {
                updateColor(
                    newHue = it
                )
            },
            valueRange = 0f..360f
        )

        Text(
            text =
                "التشبع: ${
                    (saturation * 100f)
                        .roundToInt()
                }%"
        )

        Slider(
            value = saturation,
            onValueChange = {
                updateColor(
                    newSaturation = it
                )
            },
            valueRange = 0f..1f
        )

        Text(
            text =
                "الإضاءة: ${
                    (brightness * 100f)
                        .roundToInt()
                }%"
        )

        Slider(
            value = brightness,
            onValueChange = {
                updateColor(
                    newBrightness = it
                )
            },
            valueRange = 0f..1f
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = "ألوان سريعة"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceEvenly
        ) {

            RoyalTVPresetColor(
                color = Color.Red
            ) {
                updateColor(
                    newHue = 0f,
                    newSaturation = 1f,
                    newBrightness = 1f
                )
            }

            RoyalTVPresetColor(
                color = Color.Green
            ) {
                updateColor(
                    newHue = 120f,
                    newSaturation = 1f,
                    newBrightness = 1f
                )
            }

            RoyalTVPresetColor(
                color = Color.Yellow
            ) {
                updateColor(
                    newHue = 60f,
                    newSaturation = 1f,
                    newBrightness = 1f
                )
            }

            RoyalTVPresetColor(
                color = Color.Blue
            ) {
                updateColor(
                    newHue = 240f,
                    newSaturation = 1f,
                    newBrightness = 1f
                )
            }

            RoyalTVPresetColor(
                color = Color.Magenta
            ) {
                updateColor(
                    newHue = 300f,
                    newSaturation = 1f,
                    newBrightness = 1f
                )
            }

            RoyalTVPresetColor(
                color = Color.White
            ) {
                updateColor(
                    newHue = hue,
                    newSaturation = 0f,
                    newBrightness = 1f
                )
            }
        }

        Button(
            onClick = {
                onColorSelected(
                    selectedColor.value
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "تطبيق اللون"
            )
        }
    }
}

/**
 * دائرة لون جاهز.
 */
@Composable
private fun RoyalTVPresetColor(
    color: Color,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        modifier = Modifier.size(42.dp),
        shape = CircleShape
    ) {
        Spacer(
            modifier = Modifier
                .size(18.dp)
                .background(
                    color = color,
                    shape = CircleShape
                )
        )
    }
}
