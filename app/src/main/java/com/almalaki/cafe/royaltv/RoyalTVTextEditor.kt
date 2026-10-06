package com.almalaki.cafe.royaltv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * محرر نصوص ROYAL TV.
 *
 * المسؤول عن:
 *
 * - إضافة النصوص.
 * - تعديل النصوص.
 * - حذف النصوص.
 * - إظهار / إخفاء النصوص.
 * - تكبير / تصغير كل نص بشكل مستقل.
 * - اختيار لون مستقل لكل نص.
 *
 * الحفظ يتم بواسطة RoyalTVTextManager.
 */
@Composable
fun RoyalTVTextEditor(
    modifier: Modifier = Modifier
) {

    val texts by RoyalTVTextManager.texts.collectAsState()

    var newText by remember {
        mutableStateOf("")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "نصوص ROYAL TV"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            OutlinedTextField(
                value = newText,
                onValueChange = {
                    newText = it
                },
                modifier = Modifier.weight(1f),
                label = {
                    Text("النص الجديد")
                },
                singleLine = true
            )

            Button(
                onClick = {

                    val cleanText =
                        newText.trim()

                    if (cleanText.isNotBlank()) {

                        RoyalTVTextManager.addText(
                            text = cleanText
                        )

                        newText = ""
                    }
                }
            ) {
                Text("إضافة")
            }
        }

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            items(
                items = texts,
                key = { it.id }
            ) { textItem ->

                RoyalTVTextEditorItem(
                    item = textItem
                )
            }
        }
    }
}

/**
 * عنصر تحرير مستقل لكل نص.
 */
@Composable
private fun RoyalTVTextEditorItem(
    item: RoyalTVText
) {

    var editedText by remember(
        item.id,
        item.text
    ) {
        mutableStateOf(item.text)
    }

    var showColorPicker by remember(
        item.id
    ) {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        OutlinedTextField(
            value = editedText,
            onValueChange = {
                editedText = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("النص")
            }
        )

        /*
         * التحكم بحجم النص.
         */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Button(
                onClick = {

                    val newSize =
                        (item.fontSize - 2f)
                            .coerceAtLeast(8f)

                    RoyalTVTextManager.setFontSize(
                        id = item.id,
                        fontSize = newSize
                    )
                }
            ) {
                Text("−")
            }

            Text(
                text =
                    "الحجم: ${
                        item.fontSize
                            .roundToInt()
                    } sp"
            )

            Button(
                onClick = {

                    val newSize =
                        (item.fontSize + 2f)
                            .coerceAtMost(200f)

                    RoyalTVTextManager.setFontSize(
                        id = item.id,
                        fontSize = newSize
                    )
                }
            ) {
                Text("+")
            }
        }

        /*
         * اللون الحالي.
         */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            Spacer(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = Color(item.color),
                        shape = CircleShape
                    )
            )

            Button(
                onClick = {
                    showColorPicker =
                        !showColorPicker
                }
            ) {
                Text(
                    text =
                        if (showColorPicker)
                            "إخفاء اللون"
                        else
                            "اختيار اللون"
                )
            }
        }

        /*
         * Color Picker لهذا النص فقط.
         */
        if (showColorPicker) {

            RoyalTVColorPicker(
                initialColor = item.color,
                onColorSelected = { color ->

                    RoyalTVTextManager.setColor(
                        id = item.id,
                        color = color
                    )

                    showColorPicker = false
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        /*
         * حفظ / حذف / إظهار.
         */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Button(
                onClick = {

                    RoyalTVTextManager.updateText(
                        id = item.id,
                        text = editedText
                    )
                }
            ) {
                Text("حفظ")
            }

            Button(
                onClick = {

                    RoyalTVTextManager.deleteText(
                        id = item.id
                    )
                }
            ) {
                Text("حذف")
            }

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {

                Text(
                    text =
                        if (item.enabled)
                            "ظاهر"
                        else
                            "مخفي"
                )

                Switch(
                    checked = item.enabled,
                    onCheckedChange = { enabled ->

                        RoyalTVTextManager.setEnabled(
                            id = item.id,
                            enabled = enabled
                        )
                    }
                )
            }
        }
    }
}
