package com.almalaki.cafe

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun RoyalDateTime(
    language: String = "ar",
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default
) {
    var currentTime by remember {
        mutableStateOf(Date())
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000L)
        }
    }

    val isEnglish =
        language.equals("en", ignoreCase = true) ||
        language.equals("english", ignoreCase = true)

    val locale =
        if (isEnglish) Locale.ENGLISH
        else Locale("ar")

    val dateFormatter = remember(language) {
        SimpleDateFormat(
            "EEEE d/M/yyyy",
            locale
        ).apply {
            timeZone = TimeZone.getTimeZone("Asia/Damascus")
        }
    }

    val timeFormatter = remember(language) {
        SimpleDateFormat(
            "h:mm a",
            locale
        ).apply {
            timeZone = TimeZone.getTimeZone("Asia/Damascus")
        }
    }

    val dateText = dateFormatter.format(currentTime)
    val timeText = timeFormatter.format(currentTime)

    Text(
        text = "$timeText\n$dateText",
        modifier = modifier,
        style = style
    )
}
