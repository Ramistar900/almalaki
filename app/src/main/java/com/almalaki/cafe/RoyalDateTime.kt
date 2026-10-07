package com.almalaki.cafe

import androidx.compose.foundation.layout.Column
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
    style: TextStyle = TextStyle.Default,

    /*
     * التحكم في الظهور.
     *
     * الاستخدام الافتراضي لا يتغير:
     *
     * الوقت = ظاهر
     * التاريخ = ظاهر
     */
    showTime: Boolean = true,
    showDate: Boolean = true,

    /*
     * يمكن إعطاء الوقت والتاريخ
     * حجمًا مستقلًا.
     *
     * إذا لم يتم تحديدهما،
     * يستخدمان style القديم.
     */
    timeStyle: TextStyle = style,
    dateStyle: TextStyle = style,

    /*
     * Modifier مستقل لكل سطر.
     */
    timeModifier: Modifier = Modifier,
    dateModifier: Modifier = Modifier
) {

    /*
     * ========================================================
     * CURRENT TIME
     * ========================================================
     *
     * تحديث الوقت كل ثانية.
     */
    var currentTime by remember {
        mutableStateOf(Date())
    }

    LaunchedEffect(Unit) {

        while (true) {

            currentTime = Date()

            delay(1000L)
        }
    }

    /*
     * ========================================================
     * LANGUAGE
     * ========================================================
     */

    val isEnglish =
        language.equals(
            "en",
            ignoreCase = true
        ) ||
        language.equals(
            "english",
            ignoreCase = true
        )

    val locale =
        if (isEnglish) {
            Locale.ENGLISH
        } else {
            Locale("ar")
        }

    /*
     * ========================================================
     * DATE FORMAT
     * ========================================================
     *
     * توقيت دمشق.
     */
    val dateFormatter =
        remember(language) {

            SimpleDateFormat(
                "EEEE d/M/yyyy",
                locale
            ).apply {

                timeZone =
                    TimeZone.getTimeZone(
                        "Asia/Damascus"
                    )
            }
        }

    /*
     * ========================================================
     * TIME FORMAT
     * ========================================================
     *
     * توقيت دمشق.
     */
    val timeFormatter =
        remember(language) {

            SimpleDateFormat(
                "h:mm a",
                locale
            ).apply {

                timeZone =
                    TimeZone.getTimeZone(
                        "Asia/Damascus"
                    )
            }
        }

    /*
     * ========================================================
     * TEXT VALUES
     * ========================================================
     */

    val dateText =
        dateFormatter.format(
            currentTime
        )

    val timeText =
        timeFormatter.format(
            currentTime
        )

    /*
     * ========================================================
     * DISPLAY
     * ========================================================
     *
     * لا نضيف أي سطر فارغ.
     *
     * إذا كان التاريخ مخفيًا:
     *
     * الوقت يظهر وحده.
     *
     * وهذا مهم جدًا لشاشة Royal TV
     * حتى لا تبقى مساحة فارغة أسفل الوقت.
     */
    Column(
        modifier = modifier
    ) {

        if (showTime) {

            Text(
                text = timeText,
                modifier = timeModifier,
                style = timeStyle
            )
        }

        if (showDate) {

            Text(
                text = dateText,
                modifier = dateModifier,
                style = dateStyle
            )
        }
    }
}
