
package com.almalaki.royaltv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val RoyalBlack = Color(0xFF050505)
private val RoyalGold = Color(0xFFD4AF37)
private val RoyalCream = Color(0xFFF5F0E5)
private val RoyalMutedGold = Color(0xFFB8A66A)
private val RoyalRed = Color(0xFFE53935)

class RoyalTVMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        setContent {
            MaterialTheme {
                RoyalTVChannelScreen()
            }
        }
    }
}

@Composable
private fun RoyalTVChannelScreen() {
    var currentTime by remember { mutableStateOf("--:--:--") }
    var currentDate by remember { mutableStateOf("----/--/--") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
            ).format(now)

            currentDate = SimpleDateFormat(
                "yyyy/MM/dd",
                Locale.getDefault()
            ).format(now)

            delay(1000)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RoyalBlack
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(RoyalBlack)
        ) {
            val scale = minOf(
                maxWidth.value / 1280f,
                maxHeight.value / 720f
            ).coerceIn(0.5f, 2.2f)

            val compact = maxWidth < 500.dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = (24f * scale).dp,
                        vertical = (20f * scale).dp
                    )
            ) {
                // مساحة المحتوى الرئيسية للقناة.
                // ستُضاف إليها لاحقًا الإعلانات والنصوص الحرة.
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "الملكي",
                        color = RoyalGold,
                        fontSize = (46f * scale).sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height((10f * scale).dp)
                    )

                    Text(
                        text = "ROYAL TV",
                        color = RoyalCream,
                        fontSize = (22f * scale).sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(
                        modifier = Modifier.height((8f * scale).dp)
                    )

                    Text(
                        text = "هويتكم... على شاشة الملكي",
                        color = RoyalMutedGold,
                        fontSize = (15f * scale).sp
                    )
                }

                // الهوية السفلية الخاصة بـ ROYAL TV فقط.
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(
                        (8f * scale).dp
                    )
                ) {
                    // الشريط الإخباري.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((42f * scale).dp)
                            .clip(
                                RoundedCornerShape((12f * scale).dp)
                            )
                            .border(
                                width = (1f * scale).dp,
                                color = RoyalGold.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(
                                    (12f * scale).dp
                                )
                            )
                            .background(Color(0xFF101010))
                            .padding(
                                horizontal = (14f * scale).dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الملكي",
                            color = RoyalGold,
                            fontSize = (14f * scale).sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.width((12f * scale).dp)
                        )

                        Box(
                            modifier = Modifier
                                .width((1f * scale).dp)
                                .height((20f * scale).dp)
                                .background(RoyalGold.copy(alpha = 0.65f))
                        )

                        Spacer(
                            modifier = Modifier.width((12f * scale).dp)
                        )

                        Text(
                            text = "قريباً  ROYAL TV",
                            color = RoyalCream,
                            fontSize = (14f * scale).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // الشعار والوقت والتاريخ ومؤشر البث.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = (4f * scale).dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "الملكي",
                                color = RoyalGold,
                                fontSize = (23f * scale).sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "ROYAL TV",
                                color = RoyalMutedGold,
                                fontSize = (10f * scale).sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (!compact) {
                            Spacer(
                                modifier = Modifier.width((12f * scale).dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currentTime,
                                color = RoyalCream,
                                fontSize = (17f * scale).sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = currentDate,
                                color = RoyalMutedGold,
                                fontSize = (11f * scale).sp
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF241011))
                                .padding(
                                    horizontal = (12f * scale).dp,
                                    vertical = (8f * scale).dp
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(
                                (6f * scale).dp
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .width((8f * scale).dp)
                                    .height((8f * scale).dp)
                                    .clip(CircleShape)
                                    .background(RoyalRed)
                            )

                            Text(
                                text = "مباشر",
                                color = RoyalCream,
                                fontSize = (13f * scale).sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
