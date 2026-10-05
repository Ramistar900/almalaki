package com.almalaki.cafe

import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalLayoutDirection

private val RoyalGold = Color(0xFFD4AF37)
private val RoyalGoldLight = Color(0xFFFFE9A3)
private val RoyalGoldDark = Color(0xFF8C6B16)
private val RoyalBlack = Color(0xFF050505)
private val RoyalCard = Color(0xFF111111)
private val RoyalCream = Color(0xFFF5F0E5)
private val RoyalDarkText = Color(0xFF171717)
private val RoyalGray = Color(0xFFBDBDBD)

/*
 * مدة توقف الشعارات الإضافية أمام المستخدم.
 *
 * الشعار يبقى أماميًا 5 ثوانٍ
 * ثم يدور حول المحور العمودي.
 */
private const val GENERIC_LOGO_FRONT_PAUSE_MS = 5_000
private const val GENERIC_LOGO_TURN_MS = 2_400

  @Composable
fun RoyalCustomerInterface(
    isDarkMode: Boolean = true,
    newsText: String = "أهلاً بكم في Royal Coffee — طعمٌ يستحق التجربة",
    onOwnerLogin: () -> Unit = {},
    onSettings: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    content: @Composable () -> Unit
) {

    val drawerState =
        rememberDrawerState(DrawerValue.Closed)

    val scope =
        rememberCoroutineScope()

    val backgroundColor =
        if (isDarkMode) {
            RoyalBlack
        } else {
            RoyalCream
        }

    ModalNavigationDrawer(

        drawerState = drawerState,

        drawerContent = {

            ModalDrawerSheet(

                modifier =
                    Modifier.width(300.dp),

                drawerContainerColor =
                    if (isDarkMode) {
                        RoyalCard
                    } else {
                        Color.White
                    }
            ) {

                Spacer(
                    Modifier.height(28.dp)
                )

                RoyalShinyLogo(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 20.dp
                            )
                )

                Spacer(
                    Modifier.height(30.dp)
                )

                RoyalDrawerItem(
                    RoyalIcon.PROFILE,
                    "دخول المالك",
                    isDarkMode
                ) {

                    scope.launch {
                        drawerState.close()
                    }

                    onOwnerLogin()
                }

                RoyalDrawerItem(
                    RoyalIcon.SETTINGS,
                    "الإعدادات",
                    isDarkMode
                ) {

                    scope.launch {
                        drawerState.close()
                    }

                    onSettings()
                }

                Spacer(
                    Modifier.weight(1f)
                )

                Text(
                    text = "Royal Coffee",

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 20.dp,
                                vertical = 20.dp
                            ),

                    color = RoyalGray,

                    fontSize = 12.sp,

                    textAlign =
                        TextAlign.Center
                )
            }
        }
    ) {

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        backgroundColor
                    )
        ) {

            /*
             * =================================================
             * المحتوى الرئيسي
             * =================================================
             *
             * الشريط السفلي أصبح طبقة فوق المنتجات.
             *
             * لذلك المنتجات تستمر خلفه ولا توجد
             * مساحة سوداء منفصلة أسفله.
             */
            Column(
                modifier =
                    Modifier.fillMaxSize()
            ) {

                RoyalTopBar(
                    isDarkMode
                ) {

                    scope.launch {
                        drawerState.open()
                    }

                    onMenuClick()
                }

                RoyalTitleFrame(
                    isDarkMode
                )

                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                ) {

                    content()
                }
            }

            /*
             * =================================================
             * الشريط السفلي
             * =================================================
             *
             * موضوع فوق المحتوى بدل أن يأخذ مساحة مستقلة.
             *
             * لا توجد خلفية هنا.
             */
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
            ) {

                RoyalBottomBar(
                    newsText
                )
            }
        }
    }
}

/*
 * ============================================================
 * الشعار النشط
 * ============================================================
 *
 * هذه الدالة هي نقطة الربط الرئيسية مع مكتبة الشعارات.
 *
 * إذا كان الشعار الأساسي:
 *    RoyalAppLogo
 *
 * إذا كان شعارًا إضافيًا:
 *    ملف PNG مستقل + حركة 3D
 */
@Composable
fun RoyalActiveLogo(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {

    val context =
        LocalContext.current

    val activeLogo =
        remember(context) {
            loadActiveRoyalLogo(context)
        }

    Box(
        modifier =
            modifier
                .size(size),
        contentAlignment = Alignment.Center
    ) {

        if (
            activeLogo.id ==
            ROYAL_DEFAULT_LOGO_ID
        ) {

            /*
             * الشعار الأساسي فقط:
             *
             * التاج والدرع
             * +
             * RC
             */
            RoyalAppLogo(
                modifier = Modifier.fillMaxSize()
            )

        } else {

            /*
             * أي شعار إضافي:
             *
             * PNG مستقل.
             * لا توجد طبقات الشعار الأساسي.
             */
            RoyalGeneric3DLogo(
                filePath = activeLogo.filePath,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/*
 * اسم الدالة القديمة محفوظ للتوافق
 * مع أي ملف آخر ما زال يستدعيها.
 *
 * أصبحت الآن تعرض الشعار النشط
 * بدل royal_logo القديم.
 */
@Composable
fun Royal3DAnimatedLogo(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    RoyalActiveLogo(
        modifier = modifier,
        size = size
    )
}

/*
 * ============================================================
 * حركة 3D للشعارات الإضافية
 * ============================================================
 *
 * الحركة:
 *
 * 5 ثوانٍ أمامية
 * ↓
 * دوران حول المحور العمودي
 * ↓
 * العودة للواجهة
 * ↓
 * توقف 5 ثوانٍ
 */
@Composable
private fun RoyalGeneric3DLogo(
    filePath: String,
    modifier: Modifier = Modifier
) {

    val rotationTransition =
        rememberInfiniteTransition(
            label = "royal_generic_3d_logo"
        )

    val rotationY by
        rotationTransition.animateFloat(

            initialValue = 0f,
            targetValue = 360f,

            animationSpec =
                infiniteRepeatable(

                    animation =
                        keyframes {

                            durationMillis =
                                GENERIC_LOGO_FRONT_PAUSE_MS +
                                    GENERIC_LOGO_TURN_MS

                            0f at 0

                            0f at
                                GENERIC_LOGO_FRONT_PAUSE_MS

                            360f at
                                GENERIC_LOGO_FRONT_PAUSE_MS +
                                    GENERIC_LOGO_TURN_MS
                        },

                    repeatMode =
                        RepeatMode.Restart
                ),

            label = "royal_generic_rotation_y"
        )

    val bitmap =
        remember(filePath) {

            if (
                filePath.isNotBlank()
            ) {

                try {

                    BitmapFactory
                        .decodeFile(filePath)
                        ?.asImageBitmap()

                } catch (_: Exception) {

                    null
                }

            } else {
                null
            }
        }

    Box(
        modifier =
            modifier
                .graphicsLayer {

                    this.rotationY =
                        rotationY

                    /*
                     * لا يوجد تكبير أو تصغير.
                     *
                     * الحركة دوران فقط.
                     */
                    cameraDistance =
                        24f * density
                },

        contentAlignment =
            Alignment.Center
    ) {

        if (bitmap != null) {

            Image(
                bitmap = bitmap,
                contentDescription = "Royal Coffee logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

        } else {

            Text(
                text = "R 👑",
                color = RoyalGold,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/*
 * ============================================================
 * الشعار النصي Royal Coffee
 * ============================================================
 */
@Composable
fun RoyalShinyLogo(
    modifier: Modifier = Modifier
) {

    val transition =
        rememberInfiniteTransition(
            label = "royal_logo_shine"
        )

    val shinePosition by
        transition.animateFloat(

            initialValue = -1f,
            targetValue = 2f,

            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            3000,
                            easing = LinearEasing
                        ),
                    repeatMode =
                        RepeatMode.Restart
                ),

            label = "royal_logo_shine_position"
        )

    val brush =
        Brush.linearGradient(

            colors =
                listOf(
                    RoyalGoldDark,
                    RoyalGold,
                    RoyalGoldLight,
                    Color.White,
                    RoyalGoldLight,
                    RoyalGold,
                    RoyalGoldDark
                ),

            start =
                Offset(
                    shinePosition * 500f,
                    0f
                ),

            end =
                Offset(
                    shinePosition * 500f + 260f,
                    0f
                )
        )

    Text(

        text = "Royal Coffee",

        modifier = modifier,

        style =
            TextStyle(
                brush = brush,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            ),

        textAlign = TextAlign.Center
    )
}

/*
 * ============================================================
 * الشريط العلوي
 * ============================================================
 */
 @Composable
private fun RoyalTopBar(
    isDarkMode: Boolean,
    onMenuClick: () -> Unit
) {
    var isFocused by remember {
        mutableStateOf(false)
    }

    val background =
        if (isDarkMode) {
            RoyalCard
        } else {
            Color.White
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(background)
                .padding(
                    horizontal = 8.dp,
                    vertical = 8.dp
                )
    ) {

        IconButton(
            onClick = onMenuClick,

            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .size(48.dp)
                    .onFocusChanged {
                        isFocused = it.isFocused
                    }
                    .focusable()
                    .then(
                        if (isFocused) {
                            Modifier.border(
                                width = 2.dp,
                                color = RoyalGold,
                                shape = RoundedCornerShape(14.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
        ) {

            Icon(
                Icons.Default.Menu,

                "القائمة",

                tint =
                    if (isFocused) {
                        RoyalGoldLight
                    } else {
                        RoyalGold
                    },

                modifier =
                    Modifier.size(32.dp)
            )
        }

        Row(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 55.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            RoyalActiveLogo(
                size = 42.dp
            )

            Spacer(
                Modifier.width(8.dp)
            )

            RoyalShinyLogo()
        }
    }
}

/*
 * ============================================================
 * إطار قائمة Royal
 * ============================================================
 */
@Composable
private fun RoyalTitleFrame(
    isDarkMode: Boolean
) {

    val background =
        if (isDarkMode) {
            RoyalBlack
        } else {
            RoyalCream
        }

    Box(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                )
                .clip(
                    RoundedCornerShape(18.dp)
                )
                .border(
                    1.5.dp,
                    RoyalGold,
                    RoundedCornerShape(18.dp)
                )
                .background(background)
                .padding(
                    horizontal = 16.dp,
                    vertical = 13.dp
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                "قائمة Royal",
                color = RoyalGold,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                Modifier.height(4.dp)
            )

            Text(
                "طعمٌ يستحق التجربة",
                color =
                    if (isDarkMode) {
                        RoyalGoldLight
                    } else {
                        RoyalDarkText
                    },
                fontSize = 13.sp
            )
        }
    }
}

/*
 * ============================================================
 * عناصر القائمة الجانبية
 * ============================================================
 */
 @Composable
private fun RoyalDrawerItem(
    icon: RoyalIcon,
    title: String,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember {
        mutableStateOf(false)
    }

    val textColor =
        if (isDarkMode) {
            Color.White
        } else {
            RoyalDarkText
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 5.dp
                )
                .clip(
                    RoundedCornerShape(14.dp)
                )
                .focusable()
                .onFocusChanged {
                    isFocused = it.isFocused
                }
                
                .border(
                    width =
                        if (isFocused) {
                            2.dp
                        } else {
                            0.dp
                        },
                    color =
                        if (isFocused) {
                            Color(0xFF6A3D8F)
                        } else {
                            Color.Transparent
                        },
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable(
                    onClick = onClick
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 14.dp
                ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        RoyalIconButton(
            icon = icon,
            size = 42.dp,
            onClick = {}
        )

        Spacer(
            modifier = Modifier.width(14.dp)
        )

        Text(
            text = title,
            color = textColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/*
 * ============================================================
 * التاريخ والوقت
 * ============================================================
 */
@Composable
fun RoyalLiveDateTime(
    now: Date
) {

    val time =
        remember(now) {
            SimpleDateFormat(
                "HH:mm",
                Locale.US
            ).format(now)
        }

    val parts =
        remember(time) {
            Pair(
                time.substringBefore(":"),
                time.substringAfter(":")
            )
        }

    val pulseTransition =
        rememberInfiniteTransition(
            label = "time_colon_pulse"
        )

    val colonAlpha by
        pulseTransition.animateFloat(
            initialValue = 0.30f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            700,
                            easing = LinearEasing
                        ),
                    repeatMode =
                        RepeatMode.Reverse
                ),
            label = "colon_alpha"
        )

    CompositionLocalProvider(
        LocalLayoutDirection provides
            LayoutDirection.Ltr
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.Center,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = parts.first,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text = ":",
                color =
                    Color.White.copy(
                        alpha = colonAlpha
                    ),
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text = parts.second,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

            
/*
 * ============================================================
 * الشريط الإخباري
 * ============================================================
 */
 @Composable
fun RoyalNewsTicker(
    newsText: String,
    modifier: Modifier = Modifier
) {
    /*
     * ============================================================
     * شريط أخبار Royal
     * ============================================================
     *
     * - مستطيل رفيع.
     * - حواف دائرية بالكامل.
     * - بدون زخارف جانبية.
     * - حركة من اليمين إلى اليسار.
     * - مدة الدورة الكاملة: 25 ثانية.
     * - يبدأ النص من خارج الطرف الأيمن.
     * - ينتهي بعد خروج النص بالكامل من الطرف الأيسر.
     * - لا يستخدم while.
     * - لا يستخدم delay.
     * - لا يستخدم isRunning.
     * - لا يستخدم toPx().
     */

    var containerWidth by remember {
        mutableStateOf(0)
    }

    var contentWidth by remember {
        mutableStateOf(0)
    }

    /*
     * يصبح الشريط جاهزًا للحركة فقط بعد معرفة
     * عرض الشريط وعرض النص الحقيقي.
     */
    val tickerReady =
        containerWidth > 0 && contentWidth > 0

    /*
     * عند تغيير الخبر أو اكتمال القياسات،
     * تبدأ دورة جديدة من البداية.
     */
    androidx.compose.runtime.key(
        newsText,
        tickerReady
    ) {

        val infiniteTransition =
            rememberInfiniteTransition(
                label = "RoyalNewsTickerTransition"
            )

        /*
         * قيمة الحركة:
         *
         * 0.0 = بداية الحركة من اليمين.
         * 1.0 = نهاية الحركة عند خروج النص بالكامل من اليسار.
         */
        val progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 25_000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "RoyalNewsTickerProgress"
        )

        /*
         * حساب المسافة الكاملة للحركة.
         *
         * عرض الشريط
         * +
         * عرض النص
         *
         * بهذه الطريقة:
         *
         * البداية:
         * النص خارج الطرف الأيمن.
         *
         * النهاية:
         * النص خارج الطرف الأيسر بالكامل.
         */
        val currentOffset: Int =
            if (tickerReady) {

                val travelDistance =
                    containerWidth.toFloat() +
                        contentWidth.toFloat()

                (
                    containerWidth.toFloat() -
                        (travelDistance * progress)
                    ).roundToInt()

            } else {
                0
            }

        /*
         * الشريط نفسه.
         */
        Box(
            modifier = modifier
                .height(22.dp)
                .clip(
                    RoundedCornerShape(50)
                )
                .background(
                    Color(0xFF111111)
                )
                .border(
                    width = 1.dp,
                    color = RoyalGold,
                    shape = RoundedCornerShape(50)
                )
                .clipToBounds()
                .onSizeChanged { size ->
                    containerWidth = size.width
                }
        ) {

            /*
             * محتوى الأخبار.
             */
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .onSizeChanged { size ->
                        contentWidth = size.width
                    }
                    .offset {
                        IntOffset(
                            x = currentOffset,
                            y = 0
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {

                /*
                 * تقسيم الأخبار باستخدام &
                 *
                 * مثال:
                 *
                 * "خبر الأول&خبر الثاني&خبر الثالث"
                 *
                 * تصبح:
                 *
                 * خبر الأول
                 * الشعار
                 * خبر الثاني
                 * الشعار
                 * خبر الثالث
                 */
                val newsItems =
                    newsText.split("&")

                newsItems.forEachIndexed { index, text ->

                    Text(
                        text = text.trim(),
                        color = Color(0xFFF9F295),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )

                    /*
                     * وضع الشعار بين الأخبار فقط،
                     * وليس بعد آخر خبر.
                     */
                    if (index < newsItems.lastIndex) {
                        RoyalTickerLogoSeparator()
                    }
                }
            }
        }
    }
}


/*
 * ============================================================
 * الفاصل الملكي بين أخبار الشريط
 * ============================================================
 *
 * يستخدم الشعار الملكي الموجود أصلًا في التطبيق.
 *
 * لا يتم إنشاء شعار جديد.
 * لا يتم تغيير تصميم الشعار.
 */
@Composable
private fun RoyalTickerLogoSeparator() {

    RoyalActiveLogo(
        size = 18.dp
    )
}



/*
 * ============================================================
 * صندوق المعلومات اللامع
 * ============================================================
 */
@Composable
fun RoyalShinyInfoBox(
    modifier: Modifier = Modifier,
    transparentBackground: Boolean = false,
    content: @Composable () -> Unit
) {

    val transition =
        rememberInfiniteTransition(
            label = "info_shine"
        )

    val shinePosition by
        transition.animateFloat(

            initialValue = -1.5f,
            targetValue = 1.5f,

            animationSpec =
                infiniteRepeatable(

                    animation =
                        tween(
                            2400,
                            easing = LinearEasing
                        ),

                    repeatMode =
                        RepeatMode.Restart
                ),

            label = "shine_position"
        )

    val backgroundBrush =
        Brush.linearGradient(

            colors =
                listOf(
                    Color(0xFF050505),
                    Color(0xFF16091F),
                    Color(0xFF321044),
                    Color(0xFF12071A),
                    Color(0xFF050505)
                ),

            start =
                Offset(0f, 0f),

            end =
                Offset(500f, 100f)
        )

    val shineBrush =
        Brush.linearGradient(

            colors =
                listOf(
                    Color.Transparent,
                    Color(0x66FFFFFF),
                    Color.Transparent
                ),

            start =
                Offset(
                    shinePosition * 220f,
                    0f
                ),

            end =
                Offset(
                    shinePosition * 220f + 90f,
                    0f
                )
        )

    Box(

        modifier =
            modifier
                .height(40.dp)
                .clip(
                    RoundedCornerShape(7.dp)
                )
                .border(
                    1.dp,
                    RoyalGold,
                    RoundedCornerShape(7.dp)
                )
                .then(
                    if (transparentBackground) {
                        Modifier
                    } else {
                        Modifier.background(
                            backgroundBrush
                        )
                    }
                )

    ) {

        Box(
            Modifier
                .matchParentSize()
                .background(shineBrush)
        )

        Box(

            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 7.dp
                ),

            contentAlignment =
                Alignment.Center
        ) {

            content()
        }
    }
}

/*
 * ============================================================
 * الشريط السفلي
 * ============================================================
 */
@Composable
fun RoyalBottomBar(
    newsText: String
) {

    var now by
        remember {
            mutableStateOf(Date())
        }

    LaunchedEffect(Unit) {

        while (true) {

            now = Date()

            delay(1000)
        }
    }

    val date =
        remember(now) {

            SimpleDateFormat(
                "EEEE d/M/yyyy",
                Locale("ar")
            ).format(now)
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = 8.dp,
                    vertical = 5.dp
                ),

        verticalAlignment =
            Alignment.Bottom
    ) {

        // ─────────────────────────
        // اللوجو
        // ─────────────────────────

        Box(
            modifier =
                Modifier
                    .size(48.dp),

            contentAlignment =
                Alignment.Center
        ) {

            RoyalActiveLogo(
                size = 48.dp
            )
        }

        Spacer(
            Modifier.width(5.dp)
        )

        // ─────────────────────────
        // التاريخ + الساعة
        // ─────────────────────────

        Column(
            modifier =
                Modifier.width(82.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Bottom
        ) {

            Text(
                text = date,

                color = RoyalGold,

                fontSize = 9.sp,

                fontWeight =
                    FontWeight.Medium,

                maxLines = 1
            )

            Spacer(
                Modifier.height(2.dp)
            )

            RoyalShinyInfoBox(
                Modifier
                    .width(68.dp)
                    .height(25.dp),

                transparentBackground = true

            ) {

                RoyalLiveDateTime(
                    now
                )
            }
        }

        Spacer(
            Modifier.width(6.dp)
        )

        // ─────────────────────────
        // الشريط الإخباري
        // ─────────────────────────
        //
        // مهم:
        // لا يوجد RoyalShinyInfoBox هنا.
        //
        // RoyalNewsTicker هو المستطيل نفسه.
        // weight(1f) يجعله يتمدد تلقائيًا
        // حسب المساحة المتبقية في الشاشة.
        // ─────────────────────────

        RoyalNewsTicker(
            newsText = newsText,
            modifier =
                Modifier
                    .weight(1f)
        )
    }
}
