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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
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

    if (!state.visible) {
        return
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
         * مربع البحث.
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
         * صف الأرقام.
         */
        KeyboardRow(
            keys =
                RoyalTVKeyboardManager
                    .numberKeys
        )

        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )

        /*
         * صفوف الحروف.
         */
        val keys =
            RoyalTVKeyboardManager
                .getCurrentKeys()

        val rows =
            splitKeys(
                keys
            )

        rows.forEach { rowKeys ->

            KeyboardRow(
                keys = rowKeys
            )

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )
        }

        /*
         * أزرار الوظائف.
         */
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            KeyboardActionKey(
                text = "🌐",
                modifier =
                    Modifier.weight(1f)
            ) {
                RoyalTVKeyboardManager
                    .switchLanguage()
            }

            KeyboardActionKey(
                text = "مسافة",
                modifier =
                    Modifier.weight(2.2f)
            ) {
                RoyalTVKeyboardManager
                    .space()
            }

            KeyboardActionKey(
                text = "⌫",
                modifier =
                    Modifier.weight(1f)
            ) {
                RoyalTVKeyboardManager
                    .delete()
            }

            KeyboardActionKey(
                text = "مسح",
                modifier =
                    Modifier.weight(1f)
            ) {
                RoyalTVKeyboardManager
                    .clear()
            }

            KeyboardActionKey(
                text = "بحث",
                modifier =
                    Modifier.weight(1.2f)
            ) {
                RoyalTVKeyboardManager
                    .enter()
            }
        }
    }
}

@Composable
private fun KeyboardRow(
    keys: List<String>
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.Center
    ) {

        keys.forEach { key ->

            KeyboardCharacterKey(
                text = key
            ) {
                RoyalTVKeyboardManager
                    .typeCharacter(
                        key
                    )
            }

            Spacer(
                modifier =
                    Modifier.width(
                        7.dp
                    )
            )
        }
    }
}

@Composable
private fun KeyboardCharacterKey(
    text: String,
    onClick: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .width(
                    58.dp
                )
                .height(
                    54.dp
                )
                .border(
                    width = 1.dp,
                    color =
                        RoyalKeyboardGold,
                    shape =
                        RoundedCornerShape(
                            10.dp
                        )
                )
                .background(
                    RoyalKeyboardCard,
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
                RoyalKeyboardCream,
            fontSize = 22.sp,
            fontWeight =
                FontWeight.Bold,
            textAlign =
                TextAlign.Center
        )
    }
}

@Composable
private fun KeyboardActionKey(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Box(
        modifier =
            modifier
                .height(
                    54.dp
                )
                .border(
                    width = 1.5.dp,
                    color =
                        RoyalKeyboardGold,
                    shape =
                        RoundedCornerShape(
                            10.dp
                        )
                )
                .background(
                    RoyalKeyboardBlack,
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
                RoyalKeyboardGoldLight,
            fontSize = 18.sp,
            fontWeight =
                FontWeight.Bold,
            textAlign =
                TextAlign.Center
        )
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

        if (index >= keys.size) {
            return@forEach
        }

        val end =
            (index + size)
                .coerceAtMost(
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

    if (index < keys.size) {

        rows.add(
            keys.subList(
                index,
                keys.size
            )
        )
    }

    return rows
}
