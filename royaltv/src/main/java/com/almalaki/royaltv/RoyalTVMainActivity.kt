
package com.almalaki.royaltv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val RoyalBlue = Color(0xFF172B4D)
private val RoyalGold = Color(0xFFB99A52)
private val RoyalEmerald = Color(0xFF176B60)
private val RoyalBurgundy = Color(0xFF6B3154)
private val RoyalPearl = Color(0xFFDCE5EE)
private val RoyalCharcoal = Color(0xFF252B35)
private val RoyalRed = Color(0xFFFF4545)
private val RoyalBlack = Color(0xFF080C12)

private data class RoyalDemoProduct(
    val name: String,
    val description: String,
    val accent: Color
)

/*
 * بيانات تجريبية فقط.
 * تُستبدل لاحقًا بصور المنتجات الحقيقية من تطبيق الملكي.
 */
private val demoProducts = listOf(
    RoyalDemoProduct(
        "مشروب Royal",
        "جديدنا من الملكي",
        RoyalGold
    ),
    RoyalDemoProduct(
        "قهوة فاخرة",
        "جودة فاخرة",
        RoyalEmerald
    ),
    RoyalDemoProduct(
        "حلوى اليوم",
        "طعم يستحق التجربة",
        RoyalBurgundy
    )
)

class RoyalTVMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor =
            android.graphics.Color.rgb(8, 12, 18)

        window.navigationBarColor =
            android.graphics.Color.rgb(8, 12, 18)

        setContent {
            MaterialTheme {
                RoyalTVChannelScreen()
            }
        }
    }
}

@Composable
private fun RoyalTVChannelScreen() {

    var currentTime by remember {
        mutableStateOf("--:--")
    }

    var currentDate by remember {
        mutableStateOf("----/--/--")
    }

    var productIndex by remember {
        mutableIntStateOf(0)
    }

    var fullscreen by remember {
        mutableStateOf(false)
    }

    /*
     * تحديث الوقت والتاريخ كل ثانية.
     * المنطقة الزمنية: دمشق.
     */
    LaunchedEffect(Unit) {

        val timeFormatter =
            SimpleDateFormat(
                "h:mm a",
                Locale("ar")
            ).apply {
                timeZone =
                    TimeZone.getTimeZone("Asia/Damascus")
            }

        val dateFormatter =
            SimpleDateFormat(
                "EEEE d/M/yyyy",
                Locale("ar")
            ).apply {
                timeZone =
                    TimeZone.getTimeZone("Asia/Damascus")
            }

        while (true) {
            val now = Date()

            currentTime = timeFormatter.format(now)
            currentDate = dateFormatter.format(now)

            delay(1_000L)
        }
    }

    /*
     * تغيير المنتج كل 30 ثانية.
     */
    LaunchedEffect(Unit) {

        while (true) {
            delay(30_000L)

            productIndex =
                (productIndex + 1) % demoProducts.size
        }
    }

    /*
     * نبض مؤشر مباشر.
     */
    val liveTransition =
        rememberInfiniteTransition(
            label = "royal_live_transition"
        )

    val liveAlpha by liveTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "royal_live_alpha"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(RoyalBlack)
    ) {

        if (fullscreen) {

            BroadcastWindow(
                liveAlpha = liveAlpha,
                fullscreen = true,
                onToggleFullscreen = {
                    fullscreen = false
                },
                modifier = Modifier.fillMaxSize()
            )

        } else {

            val compact = maxWidth < 700.dp

            val outerPadding =
                if (compact) 5.dp else 12.dp

            val gap =
                if (compact) 5.dp else 9.dp

            val identityHeight =
                if (compact) 48.dp else 60.dp

            val sideFraction =
                if (compact) 0.34f else 0.29f

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(outerPadding),
                verticalArrangement =
                    Arrangement.spacedBy(gap)
            ) {

                /*
                 * منطقة المحتوى الرئيسية.
                 * البث كبير، والخانات الجانبية بجواره.
                 */
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement =
                        Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.Top
                ) {

                    Box(
                        modifier = Modifier
                            .weight(1f - sideFraction)
                            .fillMaxHeight()
                    ) {

                        BroadcastWindow(
                            liveAlpha = liveAlpha,
                            fullscreen = false,
                            onToggleFullscreen = {
                                fullscreen = true
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(sideFraction)
                            .fillMaxHeight(),
                        verticalArrangement =
                            Arrangement.spacedBy(gap)
                    ) {

                        NewProductsPanel(
                            productIndex = productIndex,
                            modifier = Modifier.weight(2.2f)
                        )

                        InfoPanel(
                            title = "العروض",
                            subtitle = "عروض الملكي",
                            accent = RoyalBurgundy,
                            modifier = Modifier.weight(1f)
                        )

                        InfoPanel(
                            title = "الطلبات",
                            subtitle = "حالة الطلبات",
                            accent = RoyalEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                /*
                 * الهوية السفلية في صف واحد.
                 */
                                BottomIdentityBar(
                    currentTime = currentTime,
                    currentDate = currentDate,
                    compact = compact,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(identityHeight)
                )
            }
        }

        // زر الخيارات في واجهة القناة فقط
        if (!fullscreen) {
            RoyalTVOptionsOverlay(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
    }
}

@Composable
private fun BroadcastWindow(
    liveAlpha: Float,
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        RoyalCharcoal,
                        RoyalBlue,
                        RoyalBlack
                    )
                )
            )
            .border(
                width = 1.dp,
                color = RoyalGold,
                shape = RoundedCornerShape(9.dp)
            )
    ) {

        /*
         * منطقة تجريبية لمشغل الفيديو.
         * يُربط مشغل البث الحقيقي لاحقًا.
         */
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(12.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = "ROYAL TV",
                color = RoyalGold,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "نافذة البث الرئيسية",
                color = RoyalPearl,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "منطقة عرض الفيديو",
                color = RoyalPearl.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        /*
         * مؤشر مباشر داخل نافذة البث.
         */
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(CircleShape)
                .background(RoyalBurgundy)
                .border(
                    1.dp,
                    RoyalGold,
                    CircleShape
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 6.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(liveAlpha)
                    .background(
                        RoyalRed,
                        CircleShape
                    )
            )

            Text(
                text = "مباشر",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        /*
         * زر تكبير قابل للنقر.
         */
        Text(
            text = if (fullscreen) "⛶ رجوع" else "⛶",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(RoyalBlue)
                .border(
                    1.dp,
                    RoyalGold,
                    RoundedCornerShape(7.dp)
                )
                .clickable {
                    onToggleFullscreen()
                }
                .padding(
                    horizontal = 10.dp,
                    vertical = 7.dp
                ),
            color = RoyalPearl,
            fontSize = if (fullscreen) 12.sp else 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun NewProductsPanel(
    productIndex: Int,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(RoyalBlue)
            .border(
                1.dp,
                RoyalGold,
                RoundedCornerShape(9.dp)
            )
            .padding(5.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = "جديدنا",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(5.dp))
                .background(RoyalGold)
                .padding(vertical = 5.dp),
            color = RoyalBlue,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        /*
         * عنصر واحد ظاهر في كل مرة.
         * القديم يخرج يمينًا، والجديد يدخل من اليسار.
         */
        AnimatedContent(
            targetState = productIndex,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            transitionSpec = {

                (
                    slideInHorizontally(
                        animationSpec = tween(650),
                        initialOffsetX = { width ->
                            -width
                        }
                    ) + fadeIn(
                        animationSpec = tween(450)
                    )
                ).togetherWith(
                    slideOutHorizontally(
                        animationSpec = tween(650),
                        targetOffsetX = { width ->
                            width
                        }
                    ) + fadeOut(
                        animationSpec = tween(450)
                    )
                ).using(
                    SizeTransform(clip = true)
                )
            },
            label = "royal_product_transition"
        ) { targetIndex ->

            val product =
                demoProducts[targetIndex]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                product.accent.copy(alpha = 0.85f),
                                RoyalCharcoal,
                                RoyalBlack
                            )
                        )
                    )
                    .border(
                        1.dp,
                        RoyalGold.copy(alpha = 0.7f),
                        RoundedCornerShape(6.dp)
                    )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {

                    /*
                     * مكان صورة طولية واحدة.
                     * يستبدل بمكوّن الصورة الحقيقي عند الربط.
                     */
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                RoyalPearl.copy(alpha = 0.08f)
                            )
                            .border(
                                1.dp,
                                product.accent,
                                RoundedCornerShape(6.dp)
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "صورة\nالمنتج",
                            color = RoyalPearl,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = product.name,
                        color = RoyalGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = product.description,
                        color = RoyalPearl,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoPanel(
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.92f),
                        RoyalCharcoal
                    )
                )
            )
            .border(
                1.dp,
                RoyalGold.copy(alpha = 0.85f),
                RoundedCornerShape(9.dp)
            )
            .padding(
                horizontal = 6.dp,
                vertical = 7.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Spacer(
            modifier = Modifier.height(3.dp)
        )

        Text(
            text = subtitle,
            color = RoyalPearl,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BottomIdentityBar(
    currentTime: String,
    currentDate: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {

    val logoSize =
        if (compact) 14.sp else 20.sp

    val tickerSize =
        if (compact) 10.sp else 13.sp
    
    val timeSize =
        if (compact) 12.sp else 16.sp

    val dateSize =
        if (compact) 8.sp else 10.sp

    Row(
        modifier = modifier,
        horizontalArrangement =
            Arrangement.spacedBy(6.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        /*
         * موضع الشعار.
         * يمكن ربط صورة الشعار الرسمية لاحقًا.
         */
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(RoyalCharcoal)
                .border(
                    1.dp,
                    RoyalGold,
                    RoundedCornerShape(7.dp)
                )
                .padding(
                    horizontal = if (compact) 6.dp else 12.dp,
                    vertical = 5.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = "الملكي",
                color = RoyalGold,
                fontSize = logoSize,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }

        /*
         * شريط متحرك في سطر واحد.
         */
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(7.dp))
                .background(RoyalCharcoal)
                .border(
                    1.dp,
                    RoyalGold.copy(alpha = 0.8f),
                    RoundedCornerShape(7.dp)
                )
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {

            Text(
                text = "جودة فاخرة  •  أهلاً بكم في الملكي  •  طعم يستحق التجربة  •  ",
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(
                        iterations = Int.MAX_VALUE
                    ),
                color = RoyalPearl,
                fontSize = tickerSize,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }

        /*
         * الوقت والتاريخ في مساحة واحدة.
         */
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(RoyalCharcoal)
                .border(
                    1.dp,
                    RoyalGold.copy(alpha = 0.8f),
                    RoundedCornerShape(7.dp)
                )
                .padding(
                    horizontal = if (compact) 5.dp else 10.dp,
                    vertical = 4.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = currentTime,
                color = RoyalGold,
                fontSize = timeSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = currentDate,
                color = RoyalPearl,
                fontSize = dateSize,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}
