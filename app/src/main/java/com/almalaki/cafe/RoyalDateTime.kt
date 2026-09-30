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

/**
 * Royal Coffee - Date & Time
 *
 * المرحلة الأولى:
 * ملف مستقل، ولا يحتاج إلى تعديل MainActivity.kt الآن.
 * اللغة:
 * "ar" = العربية
 * "en" = English
 */
@Composable
fun RoyalDateTime(
    language: String = "ar",
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default
) {
    var currentTime by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000L)
        }
    }

    val isEnglish = language.equals("en", ignoreCase = true) ||
            language.equals("english", ignoreCase = true)

    val locale = if (isEnglish) Locale.ENGLISH else Locale("ar")

    val dateText = remember(currentTime, language) {
        SimpleDateFormat("EEEE d/M/yyyy", locale).format(currentTime)
    }

    val timeText = remember(currentTime, language) {
        SimpleDateFormat("h:mm a", locale).format(currentTime)
    }

    Text(
        text = "$dateText — $timeText",
        modifier = modifier,
        style = style
    )
}
