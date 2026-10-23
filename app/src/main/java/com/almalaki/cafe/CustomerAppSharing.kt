
package com.almalaki.cafe

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val ALMALAKI_RELEASE_URL =
    "https://github.com/Ramistar900/almalaki/releases/latest"

@Composable
fun CustomerAppSharing(
    isDarkMode: Boolean
) {
    val context = LocalContext.current

    val gold = Color(0xFFD4AF37)
    val cardColor = if (isDarkMode) {
        Color(0xFF111111)
    } else {
        Color.White
    }
    val textColor = if (isDarkMode) {
        Color.White
    } else {
        Color(0xFF171717)
    }
    val secondaryColor = if (isDarkMode) {
        Color(0xFFBDBDBD)
    } else {
        Color(0xFF555555)
    }

    val qrBitmap = remember {
        runCatching {
            val matrix = QRCodeWriter().encode(
                ALMALAKI_RELEASE_URL,
                BarcodeFormat.QR_CODE,
                512,
                512
            )

            val pixels = IntArray(512 * 512)

            for (y in 0 until 512) {
                for (x in 0 until 512) {
                    pixels[y * 512 + x] =
                        if (matrix[x, y]) {
                            AndroidColor.BLACK
                        } else {
                            AndroidColor.WHITE
                        }
                }
            }

            Bitmap.createBitmap(
                pixels,
                512,
                512,
                Bitmap.Config.ARGB_8888
            )
        }.getOrNull()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                gold.copy(alpha = 0.45f),
                RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "مشاركة تطبيق الملكي",
                color = gold,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "امسح رمز QR للوصول إلى صفحة أحدث إصدار من التطبيق",
                color = secondaryColor,
                fontSize = 13.sp
            )

            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "رمز QR لتنزيل تطبيق الملكي",
                    modifier = Modifier
                        .height(220.dp)
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(8.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = "تعذر إنشاء رمز QR",
                    color = secondaryColor
                )
            }

            Text(
                text = ALMALAKI_RELEASE_URL,
                color = secondaryColor,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Button(
                onClick = {
                    val sendIntent = Intent(
                        Intent.ACTION_SEND
                    ).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "حمّل تطبيق الملكي من هنا:\n$ALMALAKI_RELEASE_URL"
                        )
                    }

                    context.startActivity(
                        Intent.createChooser(
                            sendIntent,
                            "مشاركة تطبيق الملكي"
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = gold,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "مشاركة التطبيق",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
