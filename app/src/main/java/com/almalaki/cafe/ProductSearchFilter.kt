package com.almalaki.cafe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        label = {
            Text(
                text = "بحث عن منتج",
                color = SearchGold
            )
        },
        placeholder = {
            Text(
                text = "اكتب اسم المنتج...",
                color = SearchGoldLight.copy(alpha = 0.55f)
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
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val selected = category == selectedCategory

            Text(
                text = category,
                color = if (selected) SearchBlack else SearchGold,
                fontSize = 13.sp,
                modifier = Modifier
                    .background(
                        color = if (selected) {
                            SearchGold
                        } else {
                            SearchBlack
                        },
                        shape = RoundedCornerShape(50.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = SearchGold.copy(
                            alpha = if (selected) 1f else 0.45f
                        ),
                        shape = RoundedCornerShape(50.dp)
                    )
                    .padding(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    )
            )
        }
    }
}
