package com.almalaki.cafe.royaltv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RoyalKeyboardBlack =
    Color(0xFF050505)

private val RoyalKeyboardCard =
    Color(0xFF111111)

private val RoyalKeyboardGold =
    Color(0xFFD4AF37)

private val RoyalKeyboardGoldLight =
    Color(0xFFFFE9A3)

private val RoyalKeyboardCream =
    Color(0xFFF5F0E5)

@Composable
fun RoyalTVKeyboardScreen(
    modifier: Modifier = Modifier
) {

    val state by
        RoyalTVKeyboardManager.state
            .collectAsState()

    val focus by
        RoyalTVKeyboardNavigation.focus
            .collectAsState()

    if (!state.visible) {
        return
    }

    val characterKeys =
        RoyalTVKeyboardManager
            .getCurrentKeys()

    val characterRows =
        splitKeys(
            characterKeys
        )

    val functionKeys =
        listOf(
            "🌐",
            "مسافة",
            "⌫",
            "مسح",
            "بحث"
        )

    val keyboardRows =
        buildList {

            add(
                RoyalTVKeyboardManager
                    .numberKeys
            )

            addAll(
                characterRows
            )

            add(
                functionKeys
            )
        }

    /*
     * ربط الصفوف الحالية بمحرك D-Pad.
     *
     * لا يوجد هنا أي InputMethod أو EditText.
     * لذلك لا يتم طلب كيبورد Android.
     */
    LaunchedEffect(
        state.language,
        state.visible
    ) {

        if (state.visible) {

            RoyalTVKeyboardNavigation
                .setRows(
                    keyboardRows
                )
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    RoyalKeyboardBlack
                )
                .padding(
                    horizontal = 28.dp,
                    vertical = 20.dp
                ),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        /*
         * صندوق النص.
         *
         * Text فقط، وليس TextField.
         * لذلك لا يظهر كيبورد Android.
         */
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color =
                            RoyalKeyboardGold,
                        shape =
                            RoundedCornerShape(
                                18.dp
                            )
                    )
                    .background(
                        RoyalKeyboardCard,
                        RoundedCornerShape(
                            18.dp
                        )
                    )
                    .padding(
                        horizontal = 22.dp,
                        vertical = 16.dp
                    )
        ) {

            Text(
                text =
                    if (
                        state.text.isBlank()
                    ) {
                        "🔎 ابحث في YouTube"
                    } else {
                        state.text
                    },
                color =
                    if (
                        state.text.isBlank()
                    ) {
                        RoyalKeyboardGoldLight
                    } else {
                        RoyalKeyboardCream
                    },
                fontSize = 25.sp,
                fontWeight =
                    FontWeight.Medium,
                textAlign =
                    TextAlign.Start,
                modifier =
                    Modifier.fillMaxWidth()
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    18.dp
                )
        )

        /*
         * صفوف الكيبورد.
         */
        keyboardRows.forEachIndexed {
            rowIndex,
            rowKeys ->

            KeyboardRow(
                keys = rowKeys,
                focusedRow =
                    focus.row,
                focusedColumn =
                    focus.column,
                rowIndex =
                    rowIndex
            )

            if (
                rowIndex <
                    keyboardRows.lastIndex
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun KeyboardRow(
    keys: List<String>,
    focusedRow: Int,
    focusedColumn: Int,
    rowIndex: Int
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.Center
    ) {

        keys.forEachIndexed {
            columnIndex,
            key ->

            val isFocused =
                focusedRow ==
                    rowIndex &&
                    focusedColumn ==
                    columnIndex

            KeyboardKey(
                text = key,
                focused = isFocused
            ) {
                selectKey(
                    key
                )
            }

            if (
                columnIndex <
                    keys.lastIndex
            ) {

                Spacer(
                    modifier =
                        Modifier.width(
                            7.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun KeyboardKey(
    text: String,
    focused: Boolean,
    onClick: () -> Unit
) {

    val focusStyle =
        if (focused) {
            RoyalTVKeyboardFocusStyleProvider
                .focused()
        } else {
            RoyalTVKeyboardFocusStyleProvider
                .normal()
        }

    Box(
        modifier =
            Modifier
                .width(
                    if (
                        text == "مسافة"
                    ) {
                        150.dp
                    } else if (
                        text == "بحث"
                    ) {
                        86.dp
                    } else {
                        58.dp
                    }
                )
                .height(
                    54.dp
                )
                .border(
                    width =
                        focusStyle
                            .borderWidth
                            .dp,
                    color =
                        focusStyle
                            .borderColor,
                    shape =
                        RoundedCornerShape(
                            10.dp
                        )
                )
                .background(
                    color =
                        focusStyle
                            .backgroundColor,
                    shape =
                        RoundedCornerShape(
                            10.dp
                        )
                )
                .clickable(
                    onClick = onClick
                ),
        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text = text,
            color =
                focusStyle.textColor,
            fontSize =
                when {

                    text == "مسافة" ->
                        17.sp

                    text == "بحث" ->
                        18.sp

                    else ->
                        22.sp
                },
            fontWeight =
                FontWeight.Bold,
            textAlign =
                TextAlign.Center
        )
    }
}

private fun selectKey(
    key: String
) {

    when (key) {

        "🌐" -> {

            RoyalTVKeyboardManager
                .switchLanguage()
        }

        "مسافة" -> {

            RoyalTVKeyboardManager
                .space()
        }

        "⌫" -> {

            RoyalTVKeyboardManager
                .delete()
        }

        "مسح" -> {

            RoyalTVKeyboardManager
                .clear()
        }

        "بحث" -> {

            RoyalTVKeyboardManager
                .enter()
        }

        else -> {

            RoyalTVKeyboardManager
                .typeCharacter(
                    key
                )
        }
    }
}

private fun splitKeys(
    keys: List<String>
): List<List<String>> {

    if (keys.isEmpty()) {
        return emptyList()
    }

    val rows =
        mutableListOf<List<String>>()

    var index = 0

    val rowSizes =
        listOf(
            10,
            10,
            10,
            8
        )

    rowSizes.forEach { size ->

        if (
            index >=
                keys.size
        ) {
            return@forEach
        }

        val end =
            (
                index + size
            ).coerceAtMost(
                keys.size
            )

        rows.add(
            keys.subList(
                index,
                end
            )
        )

        index = end
    }

    if (
        index <
            keys.size
    ) {

        rows.add(
            keys.subList(
                index,
                keys.size
            )
        )
    }

    return rows
}
