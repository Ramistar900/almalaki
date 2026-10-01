package com.almalaki.cafe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RoyalPremiumBlack = Color(0xFF050505)
private val RoyalPremiumPanel = Color(0xFF111111)
private val RoyalPremiumGold = Color(0xFFD4AF37)
private val RoyalPremiumGoldLight = Color(0xFFFFE9A3)

@Composable
fun RoyalPremiumBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(RoyalPremiumBlack)
    ) {
        content()
    }
}

@Composable
fun RoyalPremiumSectionTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            color = RoyalPremiumGold,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = subtitle,
                color = RoyalPremiumGoldLight.copy(alpha = 0.72f),
                fontSize = 12.sp
            )
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    RoyalPremiumGold.copy(alpha = 0.38f)
                )
        )
    }
}

@Composable
fun RoyalPremiumCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = RoyalPremiumPanel,
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = RoyalPremiumGold.copy(alpha = 0.28f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        content()
    }
}

@Composable
fun RoyalPremiumBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = RoyalPremiumGold.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50)
            )
            .border(
                width = 1.dp,
                color = RoyalPremiumGold.copy(alpha = 0.45f),
                shape = RoundedCornerShape(50)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 5.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = RoyalPremiumGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun RoyalPremiumInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = RoyalPremiumGoldLight.copy(alpha = 0.68f),
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            color = RoyalPremiumGold,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
