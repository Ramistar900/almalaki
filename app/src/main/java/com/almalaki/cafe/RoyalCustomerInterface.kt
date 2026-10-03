package com.almalaki.cafe

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/*
 * Royal Coffee
 * واجهة العميل
 *
 * هذا الملف مستقل عن:
 * - Supabase
 * - الطلبات
 * - المنتجات
 * - الصوت
 *
 * في هذه المرحلة هو إطار واجهة فقط.
 */

private val RoyalGold = Color(0xFFD4AF37)
private val RoyalGoldLight = Color(0xFFFFE9A3)
private val RoyalBlack = Color(0xFF050505)
private val RoyalCard = Color(0xFF111111)
private val RoyalCream = Color(0xFFF5F0E5)
private val RoyalDarkText = Color(0xFF171717)
private val RoyalGray = Color(0xFFBDBDBD)

/**
 * الواجهة الرئيسية للعميل.
 *
 * content:
 * المحتوى الحالي للتطبيق، مثل قائمة المنتجات والسلة.
 */
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

    val backgroundColor =
        if (isDarkMode) RoyalBlack else RoyalCream

    val textColor =
        if (isDarkMode) Color.White else RoyalDarkText

    ModalNavigationDrawer(

        drawerState = drawerState,

        drawerContent = {

            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = if (isDarkMode) {
                    RoyalCard
                } else {
                    Color.White
                }
            ) {

                Spacer(
                    modifier = Modifier.height(28.dp)
                )

                Text(
                    text = "Royal Coffee",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    color = RoyalGold,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "طعمٌ يستحق التجربة",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    color = if (isDarkMode) {
                        RoyalGoldLight
                    } else {
                        RoyalGold
                    },
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(30.dp)
                )

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

                Spacer(
                    modifier = Modifier.weight(1f)
                )

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

            /*
             * الشريط العلوي
             */
            RoyalTopBar(
                isDarkMode = isDarkMode,
                onMenuClick = {

                    scope.launch {
                        drawerState.open()
                    }

                    onMenuClick()
                }
            )

            /*
             * عنوان القائمة
             */
            RoyalTitleFrame(
                isDarkMode = isDarkMode
            )

            /*
             * محتوى التطبيق الحالي
             */
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content()
            }

            /*
             * الشريط السفلي
             */
            RoyalBottomBar(
                isDarkMode = isDarkMode,
                newsText = newsText
            )
        }
    }
}

/* =========================================================
   الشريط العلوي
   ========================================================= */

@Composable
private fun RoyalTopBar(
    isDarkMode: Boolean,
    onMenuClick: () -> Unit
) {

    val background =
        if (isDarkMode) RoyalCard else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(
                horizontal = 10.dp,
                vertical = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(
            onClick = onMenuClick
        ) {

            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "القائمة",
                tint = RoyalGold,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = "Royal Coffee",
                color = RoyalGold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "طعمٌ يستحق التجربة",
                color = if (isDarkMode) {
                    RoyalGoldLight
                } else {
                    RoyalDarkText
                },
                fontSize = 12.sp
            )
        }

        Text(
            text = "R 👑",
            color = RoyalGold,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/* =========================================================
   إطار قائمة Royal
   ========================================================= */

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
                vertical = 10.dp
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
                vertical = 14.dp
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
                color = if (isDarkMode) {
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

/* =========================================================
   عنصر القائمة الجانبية
   ========================================================= */

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

/* =========================================================
   الشريط السفلي
   ========================================================= */

@Composable
private fun RoyalBottomBar(
    isDarkMode: Boolean,
    newsText: String
) {

    val background =
        if (isDarkMode) RoyalCard else Color.White

    val transition =
        rememberInfiniteTransition(
            label = "royal_news"
        )

    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 7000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "news_offset"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(background)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(RoyalGold)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(
                    horizontal = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            /*
             * التاريخ والساعة
             */
            Text(
                text = "☀  التاريخ والساعة",
                color = if (isDarkMode) {
                    RoyalGoldLight
                } else {
                    RoyalDarkText
                },
                fontSize = 11.sp,
                modifier = Modifier.width(100.dp)
            )

            /*
             * الأخبار المتحركة
             */
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = newsText,
                    color = RoyalGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }

            /*
             * شعار R 👑
             */
            Text(
                text = "R 👑",
                color = RoyalGold,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(58.dp),
                textAlign = TextAlign.End
            )
        }
    }
}
