package com.almalaki.cafe

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

private var royalOrderAlertPlayer: MediaPlayer? = null

private val RoyalAlertGold = Color(0xFFD4AF37)
private val RoyalAlertBlack = Color(0xFF050505)

fun startRoyalOrderAlert(context: Context) {
    if (royalOrderAlertPlayer?.isPlaying == true) return

    stopRoyalOrderAlert()
val uri = Uri.parse(
    "android.resource://${context.packageName}/${R.raw.royal_order_alert}"
)
    royalOrderAlertPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        setDataSource(context.applicationContext, uri)
        isLooping = true
        prepare()
        start()
    }
}

fun stopRoyalOrderAlert() {
    royalOrderAlertPlayer?.let { player ->
        try {
            if (player.isPlaying) player.stop()
        } catch (_: Exception) {
        }

        try {
            player.release()
        } catch (_: Exception) {
        }
    }

    royalOrderAlertPlayer = null
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

        Row(
    verticalAlignment = Alignment.CenterVertically
) {
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
