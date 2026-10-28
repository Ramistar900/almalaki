package com.almalaki.cafe

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.paint
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
import androidx.compose.ui.text.font.FontStyle
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
  showCartBanner: Boolean = false,
    cartCount: Int = 0,
    onCartClick: () -> Unit = {},
    onOrdersClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onOwnerLogin: () -> Unit = {},
    onSettings: () -> Unit = {},
    onMenuClick: () -> Unit = {},
  searchQuery: String = "",
onSearchQueryChange: (String) -> Unit = {},
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
        
    val spectrumTransition =
        rememberInfiniteTransition(
            label = "royal_customer_rgb"
        )

    val spectrumShift by
        spectrumTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(
                        durationMillis = 14000,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
            label = "royal_customer_rgb_shift"
        )

    val animatedBackground =
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF08070B),
                Color(0xFF512568),
                Color(0xFF007F76),
                Color(0xFFD4AF37),
                Color(0xFF08070B)
            ),
            start = Offset(
                spectrumShift * 900f,
                0f
            ),
            end = Offset(
                900f - spectrumShift * 600f,
                1200f
            )
        )
        

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

                Spacer(Modifier.height(22.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color(0xFF0A4935), Color(0xFF071F19))
                            )
                        )
                        .border(
                            1.dp,
                            RoyalGold.copy(alpha = 0.75f),
                            RoundedCornerShape(22.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RoyalShinyLogo(
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = "مركز الخيارات",
                        color = Color(0xFFFFE5A0),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "إدارة حسابك وتفضيلات التطبيق",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    text = "الحساب والإدارة",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    color = RoyalGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                RoyalDrawerItem(
                    RoyalIcon.PROFILE,
                    "دخول المالك",
                    isDarkMode,
                    subtitle = "إدارة المطعم والمنتجات والطلبات"
                ) {

                    scope.launch {
                        drawerState.close()
                    }

                    onOwnerLogin()
                }

                RoyalDrawerItem(
                    RoyalIcon.SETTINGS,
                    "الإعدادات",
                    isDarkMode,
                    subtitle = "الخطوط ومشاركة التطبيق"
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
    modifier = Modifier.fillMaxSize()
) {
    Image(
        painter = painterResource(
            id = R.drawable.royal_customer_background
        ),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color.Black.copy(alpha = 0.08f)
            )
    )

    

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
Box(
    modifier = Modifier.fillMaxSize()
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        RoyalTopBar(
            isDarkMode = isDarkMode,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange
        ) {
            scope.launch {
                drawerState.open()
            }
            onMenuClick()
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 102.dp)
        ) {
            // يبقى المحتوى قابلاً للتمرير تحت بنر اسم القائمة.
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }

            // طبقة ثابتة أعلى المحتوى؛ لا تغيّر صورة البنر أو نصوصه.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                RoyalTitleFrame(isDarkMode)
            }
        }
    }

            /*
             * =================================================
             * الشريط السفلي مع البنر الملكي
             * =================================================
             */
          
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(102.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Image(
                    painter = painterResource(
                        id = R.drawable.royal_bottom_banner
                    ),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.FillBounds
                )

                RoyalCustomerNavigationActions(
                    cartCount = cartCount,
                    onCartClick = onCartClick,
                    onOrdersClick = onOrdersClick,
                    onAccountClick = onAccountClick,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 7.dp)
                )

                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    RoyalBottomBar(
                        newsText
                    )
                }
            }
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
                    repeatMode = RepeatMode.Restart
                ),
            label = "royal_generic_rotation_y"
        )

    val shineTransition =
        rememberInfiniteTransition(
            label = "royal_generic_logo_diamond_shine"
        )

    /*
     * اللمعة مستقلة عن الدوران.
     *
     * لذلك تستمر أيضًا أثناء توقف الشعار
     * أماميًا لمدة 5 ثوانٍ.
     */
    val shineProgress by
        shineTransition.animateFloat(
            initialValue = -0.35f,
            targetValue = 1.35f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 4_800,
                            easing = LinearEasing
                        ),
                    repeatMode = RepeatMode.Restart
                ),
            label = "royal_generic_logo_shine_progress"
        )

    val bitmap =
        remember(filePath) {
            if (filePath.isNotBlank()) {
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
                    this.rotationY = rotationY
                    cameraDistance = 24f * density
                }
                .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {

        if (bitmap != null) {

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            compositingStrategy =
                                CompositingStrategy.Offscreen
                        }
            ) {

                Image(
                    bitmap = bitmap,
                    contentDescription = "Royal Coffee logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                /*
                 * لمعان ذهبي ماسي ناعم
                 * يتحرك فوق كامل الشعار.
                 */
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {

                    val shineCenterX =
                        size.width * shineProgress

                    val shineWidth =
                        size.width * 0.18f

                    val shineBrush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color.Transparent,
                                    RoyalGold.copy(
                                        alpha = 0.10f
                                    ),
                                    RoyalGoldLight.copy(
                                        alpha = 0.30f
                                    ),
                                    Color.White.copy(
                                        alpha = 0.82f
                                    ),
                                    RoyalGoldLight.copy(
                                        alpha = 0.30f
                                    ),
                                    RoyalGold.copy(
                                        alpha = 0.10f
                                    ),
                                    Color.Transparent
                                ),
                            start =
                                Offset(
                                    shineCenterX -
                                        shineWidth,
                                    size.height
                                ),
                            end =
                                Offset(
                                    shineCenterX +
                                        shineWidth,
                                    0f
                                )
                        )

                    drawRect(
                        brush = shineBrush,
                        blendMode = BlendMode.SrcAtop
                    )

                    /*
                     * نقطة ضوء صغيرة داخل اللمعة
                     * لتعطي إحساسًا ماسيًا ناعمًا.
                     */
                    drawCircle(
                        color =
                            Color.White.copy(
                                alpha = 0.58f
                            ),
                        radius =
                            (
                                size.minDimension *
                                    0.022f
                            ).coerceAtLeast(1f),
                        center =
                            Offset(
                                shineCenterX,
                                size.height * 0.34f
                            ),
                        blendMode =
                            BlendMode.SrcAtop
                    )
                }
            }

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
    val context = LocalContext.current

    val logoConfig = remember(context) {
        RoyalTextLogoSettings.load(context)
    }

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
        start = Offset(shinePosition * 500f, 0f),
        end = Offset(shinePosition * 500f + 260f, 0f)
    )

    val logoFontFamily = when (logoConfig.style) {
        RoyalTextLogoStyle.CLASSIC_CALLIGRAPHY ->
            RoyalFontFamilies.libreBodoni

        RoyalTextLogoStyle.MODERN_SIGNATURE ->
            RoyalFontFamilies.courgette
    }

    val logoFontStyle = when (logoConfig.style) {
        RoyalTextLogoStyle.CLASSIC_CALLIGRAPHY ->
            FontStyle.Italic

        RoyalTextLogoStyle.MODERN_SIGNATURE ->
            FontStyle.Normal
    }

    Text(
        text = logoConfig.text,
        modifier = modifier,
        style = TextStyle(
            brush = brush,
            fontFamily = logoFontFamily,
            fontStyle = logoFontStyle,
            fontSize = logoConfig.fontSizeSp.sp,
            fontWeight = FontWeight.Normal
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
fun RoyalTopBar(
    isDarkMode: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onMenuClick: () -> Unit
) {
    var isFocused by remember {
        mutableStateOf(false)
    }

    val searchTextColor =
        if (isDarkMode) RoyalCream else RoyalDarkText

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(93.dp)
            .clipToBounds()
            .padding(horizontal = 8.dp)
    ) {
        // صورة البنر العلوي تخص منطقة الشعار فقط ولا تمتد خلف البحث.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(47.dp)
                .paint(
                    painter = painterResource(
                        id = R.drawable.royal_customer_top_banner
                    ),
                    sizeToIntrinsics = false,
                    contentScale = ContentScale.FillBounds
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 2.dp)
                    .size(40.dp)
                    .onFocusChanged {
                        isFocused = it.isFocused
                    }
                    .focusable()
                    .then(
                        if (isFocused) {
                            Modifier.border(
                                2.dp,
                                RoyalGold,
                                RoundedCornerShape(12.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "القائمة",
                    tint = if (isFocused) RoyalGoldLight else RoyalGold,
                    modifier = Modifier.size(27.dp)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(start = 38.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoyalActiveLogo(size = 32.dp)
                Spacer(Modifier.width(6.dp))
                RoyalShinyLogo()
            }
        }

        // نزول بسيط للبحث بين البنر العلوي وبنر القائمة دون تداخل.
        Spacer(Modifier.height(7.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(35.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color(0xFF06452F))
                .background(Color.Black.copy(alpha = 0.06f))
                .border(
                    1.dp,
                    RoyalGold.copy(alpha = 0.90f),
                    RoundedCornerShape(11.dp)
                )
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(
                    color = searchTextColor,
                    fontSize = 12.sp
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(RoyalGold),
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "ابحث عن منتج...",
                                color = Color(0xFFD0D8D1),
                                fontSize = 12.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 3.dp
            )
            // زيادة ارتفاع بنر اسم القائمة حتى تبقى النصوص داخله بوضوح.
            .aspectRatio(2.6f),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(
                id = R.drawable.royal_customer_banner
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(0.62f),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            Text(
                text = "القائمة",
                color = RoyalGoldLight,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(
                Modifier.height(4.dp)
            )

            Text(
                text = "طعمٌ يستحقُ التجربة",
                color = RoyalGoldLight,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
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
    subtitle: String? = null,
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

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = title,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = if (isDarkMode) Color(0xFFBDBDBD) else Color(0xFF777064),
                    fontSize = 11.sp,
                    maxLines = 2
                )
            }
        }

        Text(
            text = "‹",
            color = RoyalGold,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
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
                // خلفية البنر الأصلية ظاهرة خلف النص؛ دون صندوق أو إطار منفصل.
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
private enum class RoyalCustomerNavIcon {
    CART,
    ORDERS,
    ACCOUNT
}

private val RoyalCustomerNavInk = Color(0xFF0C2D24)
private val RoyalCustomerNavBadge = Color(0xFFD92D3A)

/*
 * أزرار التنقل فوق المساحة الكريمية في البنر السفلي.
 * تُرسم الأيقونات بخطوط خارجية متناسقة مع الصورة المرجعية.
 */
@Composable
private fun RoyalCustomerNavigationActions(
    cartCount: Int,
    onCartClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoyalCustomerNavigationAction(
                icon = RoyalCustomerNavIcon.CART,
                label = "السلة",
                cartCount = cartCount,
                modifier = Modifier.weight(1f),
                onClick = onCartClick
            )

            RoyalCustomerNavigationAction(
                icon = RoyalCustomerNavIcon.ORDERS,
                label = "طلباتي",
                modifier = Modifier.weight(1f),
                onClick = onOrdersClick
            )

            RoyalCustomerNavigationAction(
                icon = RoyalCustomerNavIcon.ACCOUNT,
                label = "حسابي",
                modifier = Modifier.weight(1f),
                onClick = onAccountClick
            )
        }
    }
}

@Composable
private fun RoyalCustomerNavigationAction(
    icon: RoyalCustomerNavIcon,
    label: String,
    modifier: Modifier = Modifier,
    cartCount: Int = 0,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(25.dp),
            contentAlignment = Alignment.Center
        ) {
            RoyalCustomerNavigationIcon(
                icon = icon,
                modifier = Modifier.fillMaxSize()
            )

            if (icon == RoyalCustomerNavIcon.CART && cartCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(17.dp)
                        .background(RoyalCustomerNavBadge, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (cartCount > 99) "99+" else cartCount.toString(),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        Text(
            text = label,
            color = RoyalCustomerNavInk,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RoyalCustomerNavigationIcon(
    icon: RoyalCustomerNavIcon,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = size.minDimension * 0.075f
        val outline = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        val centerX = w / 2f
        val centerY = h / 2f

        when (icon) {
            RoyalCustomerNavIcon.CART -> {
                drawLine(
                    color = RoyalCustomerNavInk,
                    start = Offset(w * 0.10f, h * 0.18f),
                    end = Offset(w * 0.23f, h * 0.18f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = RoyalCustomerNavInk,
                    start = Offset(w * 0.22f, h * 0.19f),
                    end = Offset(w * 0.32f, h * 0.70f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                val basket = Path().apply {
                    moveTo(w * 0.28f, h * 0.31f)
                    lineTo(w * 0.88f, h * 0.31f)
                    lineTo(w * 0.78f, h * 0.61f)
                    lineTo(w * 0.36f, h * 0.61f)
                    close()
                }
                drawPath(
                    path = basket,
                    color = RoyalCustomerNavInk,
                    style = outline
                )
                drawLine(
                    color = RoyalCustomerNavInk,
                    start = Offset(w * 0.36f, h * 0.70f),
                    end = Offset(w * 0.78f, h * 0.70f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = RoyalCustomerNavInk,
                    radius = strokeWidth * 0.75f,
                    center = Offset(w * 0.43f, h * 0.86f)
                )
                drawCircle(
                    color = RoyalCustomerNavInk,
                    radius = strokeWidth * 0.75f,
                    center = Offset(w * 0.73f, h * 0.86f)
                )
            }

            RoyalCustomerNavIcon.ORDERS -> {
                drawRoundRect(
                    color = RoyalCustomerNavInk,
                    topLeft = Offset(w * 0.28f, h * 0.22f),
                    size = androidx.compose.ui.geometry.Size(w * 0.44f, h * 0.68f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.035f, w * 0.035f),
                    style = outline
                )
                drawRoundRect(
                    color = RoyalCustomerNavInk,
                    topLeft = Offset(w * 0.41f, h * 0.11f),
                    size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.20f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.035f, w * 0.035f),
                    style = outline
                )
                listOf(0.45f, 0.60f, 0.75f).forEach { y ->
                    drawLine(
                        color = RoyalCustomerNavInk,
                        start = Offset(w * 0.39f, h * y),
                        end = Offset(w * 0.61f, h * y),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            RoyalCustomerNavIcon.ACCOUNT -> {
                drawCircle(
                    color = RoyalCustomerNavInk,
                    radius = w * 0.14f,
                    center = Offset(centerX, h * 0.34f),
                    style = outline
                )
                val shoulders = Path().apply {
                    moveTo(w * 0.22f, h * 0.85f)
                    cubicTo(
                        w * 0.23f, h * 0.62f,
                        w * 0.37f, h * 0.53f,
                        centerX, h * 0.53f
                    )
                    cubicTo(
                        w * 0.63f, h * 0.53f,
                        w * 0.77f, h * 0.62f,
                        w * 0.78f, h * 0.85f
                    )
                }
                drawPath(
                    path = shoulders,
                    color = RoyalCustomerNavInk,
                    style = outline
                )
            }
        }
    }
}

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

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = 8.dp,
                    vertical = 2.dp
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
                    .size(38.dp),

            contentAlignment =
                Alignment.Center
        ) {

            RoyalActiveLogo(
                size = 38.dp
            )
        }

        Spacer(
            Modifier.width(5.dp)
        )

        // الساعة فقط؛ حذف التاريخ نهائيًا مع الإبقاء على تحديث الوقت كل ثانية.
        RoyalShinyInfoBox(
            Modifier
                .width(82.dp)
                .height(30.dp),
            transparentBackground = true
        ) {
            RoyalLiveDateTime(now)
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