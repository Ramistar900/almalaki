package com.almalaki.cafe

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RoyalGold = Color(0xFFD4AF37)
val RoyalGoldLight = Color(0xFFFFD966)
val RoyalGoldDark = Color(0xFF9C7A00)
val RoyalBlack = Color(0xFF000000)
val RoyalCardBlack = Color(0xFF111111)
val RoyalCream = Color(0xFFFFF8E7)

private val RoyalDarkColors = darkColorScheme(
    primary = RoyalGold,
    background = RoyalBlack,
    surface = RoyalBlack,
    onBackground = RoyalCream,
    onSurface = RoyalCream
)

private val RoyalLightColors = lightColorScheme(
    primary = RoyalGoldDark,
    background = Color(0xFFF7F2E8),
    surface = Color(0xFFF7F2E8),
    onBackground = Color(0xFF222222),
    onSurface = Color(0xFF222222)
)

@Composable
fun RoyalTheme(
    darkMode: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkMode) {
            RoyalDarkColors
        } else {
            RoyalLightColors
        },
        content = content
    )
}
