
package com.almalaki.royaltv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NoticeGold = Color(0xFFD4AF37)
private val NoticeGoldLight = Color(0xFFFFE9A3)
private val NoticePearl = Color(0xFFF5F0E5)

@Composable
fun RoyalTVNotificationOverlay(
    modifier: Modifier = Modifier
) {
    val notification by
        RoyalTVOverlayManager.activeOverlay.collectAsState()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = notification != null,
            enter = fadeIn(tween(260)) +
                scaleIn(
                    initialScale = 0.76f,
                    animationSpec = tween(360)
                ),
            exit = fadeOut(tween(220)) +
                scaleOut(
                    targetScale = 0.88f,
                    animationSpec = tween(220)
                )
        ) {
            val active = notification

            if (active != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .widthIn(max = 760.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF180D2B),
                                    Color(0xFF142B55),
                                    Color(0xFF76551D),
                                    Color(0xFF050505)
                                )
                            )
                        )
                        .border(
                            2.dp,
                            NoticeGold,
                            RoundedCornerShape(24.dp)
                        )
                        .padding(
                            horizontal = 24.dp,
                            vertical = 22.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RoyalTVNoticeLogo(
                        logoKey = active.logoKey
                    )

                    if (active.title.isNotBlank()) {
                        Text(
                            text = active.title,
                            color = NoticeGoldLight,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (active.message.isNotBlank()) {
                        Text(
                            text = active.message,
                            color = NoticePearl,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoyalTVNoticeLogo(
    logoKey: String
) {
    val context = LocalContext.current

    val logoId = context.resources.getIdentifier(
        logoKey,
        "drawable",
        context.packageName
    )

    if (logoId != 0) {
        Image(
            painter = painterResource(id = logoId),
            contentDescription = "ROYAL TV",
            modifier = Modifier.size(58.dp)
        )
    } else {
        Text(
            text = "ROYAL",
            color = NoticeGold,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .border(
                    1.dp,
                    NoticeGold,
                    RoundedCornerShape(10.dp)
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 8.dp
                )
        )
    }
}
