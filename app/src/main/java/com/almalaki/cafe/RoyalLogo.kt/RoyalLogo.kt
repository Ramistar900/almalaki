package com.almalaki.cafe

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoyalLogo() {
    val transition = rememberInfiniteTransition(label = "royal_shine")

    val shinePosition by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            tween(2600, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "shine_position"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            GoldDark,
            Gold,
            GoldLight,
            Color.White,
            GoldLight,
            Gold,
            GoldDark
        ),
        start = androidx.compose.ui.geometry.Offset(
            shinePosition * 500f,
            0f
        ),
        end = androidx.compose.ui.geometry.Offset(
            shinePosition * 500f + 500f,
            0f
        )
    )

    Box(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        Alignment.Center
    ) {
        Text(
            text = "Royal Coffee",
            style = androidx.compose.ui.text.TextStyle(
                brush = brush,
                fontSize = 39.sp,
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
