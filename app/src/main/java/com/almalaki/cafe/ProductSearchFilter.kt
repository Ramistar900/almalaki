package com.almalaki.cafe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SearchGold = Color(0xFFD4AF37)
private val SearchGoldLight = Color(0xFFFFE9A3)
private val SearchBlack = Color(0xFF111111)

@Composable
fun ProductSearchBox(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember {
        mutableStateOf(false)
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,

        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .focusable(),

        singleLine = true,

        label = {
            Text(
                text = "بحث عن منتج",
                color =
                    if (isFocused) {
                        SearchGoldLight
                    } else {
                        SearchGold
                    }
            )
        },

        placeholder = {
            Text(
                text = "اكتب اسم المنتج...",
                color = SearchGoldLight.copy(
                    alpha = 0.55f
                )
            )
        },

        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun ProductCategoryRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(
                rememberScrollState()
            )
            .padding(vertical = 4.dp),

        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        categories.forEach { category ->

            val selected =
                category == selectedCategory

            var isFocused by remember {
                mutableStateOf(false)
            }

            val backgroundColor =
                when {
                    selected -> SearchGold
                    isFocused -> SearchGold.copy(
                        alpha = 0.20f
                    )
                    else -> SearchBlack
                }

            val textColor =
                when {
                    selected -> SearchBlack
                    isFocused -> SearchGoldLight
                    else -> SearchGold
                }

            val borderAlpha =
                when {
                    selected -> 1f
                    isFocused -> 1f
                    else -> 0.45f
                }

            Text(
                text = category,

                color = textColor,

                fontSize = 13.sp,

                modifier = Modifier
                    .background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(50.dp)
                    )
                    .border(
                        width =
                            if (isFocused) {
                                2.dp
                            } else {
                                1.dp
                            },

                        color =
                            SearchGold.copy(
                                alpha = borderAlpha
                            ),

                        shape =
                            RoundedCornerShape(50.dp)
                    )
                    .onFocusChanged {
                        isFocused = it.isFocused
                    }
                    .focusable()
                    .clickable {
                        onCategorySelected(category)
                    }
                    .padding(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    )
            )
        }
    }
}
