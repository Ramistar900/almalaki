package com.almalaki.cafe

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almalaki.royalsounds.RoyalUiSound
import com.almalaki.royalsounds.RoyalUiSounds

private val RoyalAlertGold = Color(0xFFD4AF37)
private val RoyalAlertBlack = Color(0xFF050505)

private val royalOrderAlertHandler = Handler(Looper.getMainLooper())
private var royalOrderAlertContext: Context? = null

private val royalOrderAlertRunnable = object : Runnable {
    override fun run() {
        val context = royalOrderAlertContext ?: return
        RoyalUiSounds.play(context, RoyalUiSound.SYSTEM_NOTIFICATION)
        // Preserve the previous repeating alert behaviour while using the new
        // Royal notification sample instead of the legacy looping audio file.
        royalOrderAlertHandler.postDelayed(this, 3_000L)
    }
}

fun startRoyalOrderAlert(context: Context) {
    if (royalOrderAlertContext != null) return

    royalOrderAlertContext = context.applicationContext
    royalOrderAlertHandler.removeCallbacks(royalOrderAlertRunnable)
    royalOrderAlertRunnable.run()
}

fun stopRoyalOrderAlert() {
    royalOrderAlertHandler.removeCallbacks(royalOrderAlertRunnable)
    royalOrderAlertContext = null
}

@Composable
fun RoyalOrderAlertBanner(
    visible: Boolean,
    message: String = "🔔 يوجد طلب جديد",
    onStop: () -> Unit,
    onOpen: () -> Unit = {}
) {
    if (!visible) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RoyalAlertBlack)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            color = RoyalAlertGold,
            fontSize = 16.sp
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onOpen) {
                Text(
                    text = "عرض الطلب",
                    color = RoyalAlertGold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
