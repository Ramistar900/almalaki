package com.almalaki.cafe

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val RoyalGold = Color(0xFFD4AF37)
private val RoyalGoldLight = Color(0xFFFFE9A3)
private val RoyalBlack = Color(0xFF050505)
private val RoyalCard = Color(0xFF111111)
private val RoyalCream = Color(0xFFF5F0E5)

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

    val scope = rememberCoroutineScope()

    val background =
        if (isDarkMode) RoyalBlack else RoyalCream

    val textColor =
        if (isDarkMode) Color.White else Color(0xFF171717)

    ModalNavigationDrawer(
        drawerState = drawerState,

        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor =
                    if (isDarkMode) RoyalCard else Color.White
            ) {

                Spacer(
                    modifier = Modifier.height(28.dp)
                )

                Text(
                    text = "Royal Coffee",
                    color = RoyalGold,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "طعمٌ يستحق التجربة",
                    color =
                        if (isDarkMode)
                            RoyalGoldLight
                        else
                            RoyalGold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 5.dp,
                            bottom = 24.dp
                        )
                )

                NavigationDrawerItem(
                    label = {
                        Text(
                            text = "👑  دخول المالك",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                        }

                        onOwnerLogin()
                    }
                )

                NavigationDrawerItem(
                    label = {
                        Text(
                            text = "⚙  الإعدادات",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                        }

                        onSettings()
                    }
                )
            }
        }
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
        ) {

            RoyalTopBar(
                textColor = textColor,
                onMenuClick = {
                    onMenuClick()

                    scope.launch {
                        drawerState.open()
                    }
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
                isDarkMode = isDarkMode,
                newsText = newsText
            )
        }
    }
}

@Composable
private fun RoyalTopBar(
    textColor: Color,
    onMenuClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 14
