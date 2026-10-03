package com.almalaki.cafe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val RoyalGold = Color(0xFFD4AF37)
private val RoyalGoldLight = Color(0xFFFFE9A3)
private val RoyalGoldDark = Color(0xFF8C6B16)
private val RoyalBlack = Color(0xFF050505)
private val RoyalCard = Color(0xFF111111)
private val RoyalCream = Color(0xFFF5F0E5)
private val RoyalDarkText = Color(0xFF171717)
private val RoyalGray = Color(0xFFBDBDBD)

@Composable
fun RoyalCustomerInterface(
    isDarkMode: Boolean = true,
    newsText: String = "أهلاً بكم في Royal Coffee — طعمٌ يستحق التجربة",
    onOwnerLogin: () -> Unit = {},
    onSettings: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )

    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val backgroundColor =
        if (isDarkMode) RoyalBlack else RoyalCream

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor =
                    if (isDarkMode) RoyalCard else Color.White
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                RoyalShinyLogo(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(30.dp))

                RoyalDrawerItem(
                    icon = RoyalIcon.PROFILE,
                    title = "دخول المالك",
                    isDarkMode = isDarkMode
                ) {
                    scope.launch {
                        drawerState.close()
                    }
                    onOwnerLogin()
                }

                RoyalDrawerItem(
                    icon = RoyalIcon.SETTINGS,
                    title = "الإعدادات",
                    isDarkMode = isDarkMode
                ) {
                    scope.launch {
                        drawerState.close()
                    }
                    onSettings()
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Royal Coffee",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 20.dp
                        ),
                    color = RoyalGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            RoyalTopBar(
                isDarkMode = isDarkMode,
                onMenuClick = {
                    scope.launch {
                        drawerState.open()
                    }
                    onMenuClick()
                }
            )

            RoyalTitleFrame(
                isDarkMode = isDarkMode
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content()
            }

            RoyalBottomBar(
                newsText = newsText
            )
        }
    }
}

@Composable
private fun Royal3DAnimatedLogo(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    val context = LocalContext.current

    val logoResId = remember {
        context.resources.getIdentifier(
            "royal_logo",
            "drawable",
            context.packageName
        )
    }

    val transition = rememberInfiniteTransition(
        label = "royal_3d_logo_transition"
    )

    val rotationY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4500
                0f at 0
                360f at 1500
                360f at 4500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "royal_3d_logo_rotation_y"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                this.rotationY = rotationY
                cameraDistance = 12f * density
            },
        contentAlignment = Alignment.Center
    ) {
        if (logoResId != 0) {
            Image(
                painter = painterResource(id = logoResId),
                contentDescription = "Royal Coffee",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = "R 👑",
                color = RoyalGold,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RoyalShinyLogo(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(
        label = "royal_logo_shine"
    )

    val shinePosition by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                3000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "royal_logo_shine_position"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            RoyalGoldDark,
            RoyalGold,
            RoyalGoldLight,
            Color.White,
            RoyalGoldLight,
            RoyalGold,
            RoyalGoldDark
        ),
        start = Offset(
            shinePosition * 500f,
            0f
        ),
        end = Offset(
            shinePosition * 500f + 260f,
            0f
        )
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Royal Coffee",
            style = TextStyle(
                brush = brush,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RoyalTopBar(
    isDarkMode: Boolean,
    onMenuClick: () -> Unit
) {
    val background =
        if (isDarkMode) RoyalCard else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(
                horizontal = 8.dp,
                vertical = 8.dp
            )
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "القائمة",
                tint = RoyalGold,
                modifier = Modifier.size(32.dp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 55.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Royal3DAnimatedLogo(
                size = 42.dp
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            RoyalShinyLogo()
        }
    }
}

@Composable
private fun RoyalTitleFrame(
    isDarkMode: Boolean
) {
    val background =
        if (isDarkMode) RoyalBlack else RoyalCream

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
            .clip(
                RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.5.dp,
                color = RoyalGold,
                shape = RoundedCornerShape(18.dp)
            )
            .background(background)
            .padding(
                horizontal = 16.dp,
                vertical = 13.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "قائمة Royal",
                color = RoyalGold,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "طعمٌ يستحق التجربة",
                color =
                    if (isDarkMode) {
                        RoyalGoldLight
                    } else {
                        RoyalDarkText
                    },
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RoyalDrawerItem(
    icon: RoyalIcon,
    title: String,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val textColor =
        if (isDarkMode) Color.White else RoyalDarkText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 4.dp
            )
            .clip(
                RoundedCornerShape(14.dp)
            )
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 12.dp,
                vertical = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoyalIconButton(
            icon = icon,
            size = 42.dp,
            onClick = onClick
        )

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Text(
            text = title,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RoyalLiveDateTime() {
    var now by remember {
        mutableStateOf(Date())
    }

    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1000)
        }
    }

    val time = remember(now) {
        SimpleDateFormat(
            "h:mm a",
            Locale("ar")
        ).format(now)
    }

    val date = remember(now) {
        SimpleDateFormat(
            "EEEE d/M/yyyy",
            Locale("ar")
        ).format(now)
    }

    Column(
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = time,
            color = RoyalDarkText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = date,
            color = RoyalDarkText,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun RoyalNewsTicker(
    newsText: String,
    modifier: Modifier = Modifier
) {
    var containerWidth by remember {
        mutableStateOf(0)
    }

    var textWidth by remember {
        mutableStateOf(0)
    }

    val offsetX = remember {
        Animatable(0f)
    }

    LaunchedEffect(
        containerWidth,
        textWidth,
        newsText
    ) {
        if (
            containerWidth > 0 &&
            textWidth > 0
        ) {
            while (true) {
                offsetX.snapTo(
                    -textWidth.toFloat()
                )

                offsetX.animateTo(
                    containerWidth.toFloat(),
                    animationSpec = tween(
                        durationMillis = 8000,
                        easing = LinearEasing
                    )
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged {
                containerWidth = it.width
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = newsText,
            color = RoyalGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier
                .onSizeChanged {
                    textWidth = it.width
                }
                .offset {
                    IntOffset(
                        offsetX.value.roundToInt(),
                        0
                    )
                }
        )
    }
}

@Composable
private fun RoyalShinyInfoBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition =
        rememberInfiniteTransition(
            label = "info_shine"
        )

    val shinePosition by
        infiniteTransition.animateFloat(
            initialValue = -1.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2200,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "shine_position"
        )

    val brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF9F295),
            Color(0xFFFFFBD0),
            Color(0xFFF9F295),
            Color(0xFFE6D45A)
        ),
        start = Offset(
            shinePosition * 180f,
            0f
        ),
        end = Offset(
            shinePosition * 180f + 180f,
            0f
        )
    )

    Box(
        modifier = modifier
            .height(34.dp)
            .clip(
                RoundedCornerShape(8.dp)
            )
            .background(brush)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun RoyalBottomBar(
    newsText: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(Color.White)
            .padding(
                horizontal = 10.dp,
                vertical = 6.dp
            )
    ) {

        // الشعار المتحرك فوق صندوق الوقت والتاريخ
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            contentAlignment = Alignment.Center
        ) {
            Royal3DAnimatedLogo(
                size = 42.dp
            )
        }

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        // صندوق الوقت والتاريخ بجانب صندوق الأخبار
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            RoyalShinyInfoBox(
                modifier = Modifier.weight(0.38f)
            ) {
                RoyalLiveDateTime()
            }

            Spacer(
                modifier = Modifier.width(6.dp)
            )

            RoyalShinyInfoBox(
                modifier = Modifier.weight(0.62f)
            ) {
                RoyalNewsTicker(
                    newsText = newsText
                )
            }
        }
    }
}

    
