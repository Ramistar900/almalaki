package com.almalaki.cafe.royaltv

import androidx.compose.ui.graphics.Color

data class RoyalTVKeyboardFocusStyle(
    val backgroundColor: Color,
    val borderColor: Color,
    val textColor: Color,
    val borderWidth: Float,
    val elevation: Float
)

object RoyalTVKeyboardFocusStyleProvider {

    private val normalBackground =
        Color(0xFF111111)

    private val focusedBackground =
        Color(0xFFD4AF37)

    private val normalBorder =
        Color(0xFFD4AF37)

    private val focusedBorder =
        Color(0xFFFFE9A3)

    private val normalText =
        Color(0xFFF5F0E5)

    private val focusedText =
        Color(0xFF050505)

    fun normal():
        RoyalTVKeyboardFocusStyle {

        return RoyalTVKeyboardFocusStyle(
            backgroundColor =
                normalBackground,
            borderColor =
                normalBorder,
            textColor =
                normalText,
            borderWidth = 1f,
            elevation = 0f
        )
    }

    fun focused():
        RoyalTVKeyboardFocusStyle {

        return RoyalTVKeyboardFocusStyle(
            backgroundColor =
                focusedBackground,
            borderColor =
                focusedBorder,
            textColor =
                focusedText,
            borderWidth = 3f,
            elevation = 8f
        )
    }
}
