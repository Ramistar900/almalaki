package com.almalaki.royaltv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class RoyalTVMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                RoyalTVLaunchScreen()
            }
        }
    }
}

@Composable
private fun RoyalTVLaunchScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF050505)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF050505))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "ROYAL TV",
                    color = Color(0xFFD4AF37),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "قناة الملكي المستقلة",
                    color = Color(0xFFF5F0E5),
                    fontSize = 20.sp
                )

                Text(
                    text = "تم تجهيز نقطة التشغيل",
                    color = Color(0xFFB8A66A),
                    fontSize = 14.sp
                )
            }
        }
    }
}
