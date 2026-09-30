package com.almalaki.cafe

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import java.net.URL

@Composable
fun RemoteProductImage(url: String) {
    var bitmap by remember(url) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }

    LaunchedEffect(url) {
        Thread {
            try {
                val stream = URL(url).openStream()
                val loaded = BitmapFactory.decodeStream(stream)
                stream.close()
                bitmap = loaded
            } catch (_: Exception) {
                bitmap = null
            }
        }.start()
    }

    if (bitmap != null) {
        Image(
            bitmap!!.asImageBitmap(),
            "صورة المنتج",
            Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF1C1C1C)),
            Alignment.Center
        ) {
            Text(
                "جاري تحميل الصورة...",
                color = Gold,
                fontSize = 13.sp
            )
        }
    }
}
