package com.almalaki.royaltv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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

private val RoyalBlue = Color(0xFF123B43)
private val RoyalGold = Color(0xFFD4AF37)
private val RoyalEmerald = Color(0xFF087653)
private val RoyalTurquoise = Color(0xFF16858A)
private val RoyalBurgundy = Color(0xFF54245F)
private val RoyalPearl = Color(0xFFF0E9F5)
private val RoyalCharcoal = Color(0xFF17191F)
private val RoyalRed = Color(0xFFFF3038)
private val RoyalBlack = Color(0xFF050609)

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

        /*
         * بدء استقبال أوامر ROYAL TV.
         */
        val royalTvDisplayId =
            RoyalTVDisplayIdAllocator.getDisplayId(
                applicationContext
            )

                RoyalTVTickerManager.initialize(applicationContext)

        RoyalTVRealtime.start(royalTvDisplayId) { command ->
            Log.i(
                "RoyalTVMainActivity",
                "تم استقبال أمر ROYAL TV: ${command.type}"
            )

            val tickerHandled = RoyalTVTickerReceiver.receive(
                applicationContext,
                command
            )

            if (!tickerHandled) {
                RoyalTVDisplayStateManager.executeCommand(command)
            }

            runOnUiThread {
                Toast.makeText(
                    this,
                    "وصل أمر ROYAL TV: ${command.type}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val projectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager

        val screenCapturePermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val permissionData = result.data

                if (
                    result.resultCode == android.app.Activity.RESULT_OK &&
                    permissionData != null
                ) {
                    val captureIntent = Intent(
                        this,
                        RoyalTVHomeScreenCaptureService::class.java
                    ).apply {
                        action =
                            RoyalTVHomeScreenCaptureService.ACTION_START

                        putExtra(
                            RoyalTVHomeScreenCaptureService.EXTRA_RESULT_CODE,
                            result.resultCode
                        )

                        putExtra(
                            RoyalTVHomeScreenCaptureService.EXTRA_RESULT_DATA,
                            permissionData
                        )
                    }

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(captureIntent)
                        } else {
                            startService(captureIntent)
                        }

                        Toast.makeText(
                            this,
                            "بدأ تجهيز التقاط الشاشة",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (error: Exception) {
                        Toast.makeText(
                            this,
                            "تعذر بدء التقاط الشاشة",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "تم إلغاء إذن مشاركة الشاشة",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        window.statusBarColor =
            android.graphics.Color.rgb(8, 12, 18)

        window.navigationBarColor =
            android.graphics.Color.rgb(8, 12, 18)

        setContent {
            MaterialTheme {
                RoyalTVChannelScreen(
                    onRequestHomeScreenBroadcast = {
                        screenCapturePermissionLauncher.launch(
                            projectionManager.createScreenCaptureIntent()
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun RoyalTVChannelScreen(
    onRequestHomeScreenBroadcast: () -> Unit
) {
        val context = LocalContext.current

    val productSection =
        RoyalTVLocalMediaLibrary.Section.NEW_PRODUCTS

    val adsSection =
        RoyalTVLocalMediaLibrary.Section.ADS

    var productImageUris by remember {
        mutableStateOf(
            RoyalTVLocalMediaLibrary.getImages(
                context,
                productSection
            )
        )
    }

    var adsImageUris by remember {
        mutableStateOf(
            RoyalTVLocalMediaLibrary.getImages(
                context,
                adsSection
            )
        )
    }

    var ordersImageUri by remember {
        mutableStateOf(
            RoyalTVLocalMediaLibrary.getImage(
                context,
                RoyalTVLocalMediaLibrary.Section.ORDERS
            )
        )
    }

    var productImageIndex by remember {
        mutableIntStateOf(0)
    }

    var adsImageIndex by remember {
        mutableIntStateOf(0)
    }

    val productImagePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments()
        ) { selectedUris ->
            if (selectedUris.isNotEmpty()) {
                val validUris = mutableListOf<android.net.Uri>()

                selectedUris.forEach { uri ->
                    try {
                        context.contentResolver
                            .takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        validUris.add(uri)
                    } catch (error: Exception) {
                        // نتجاوز الصورة التي تعذّر حفظ صلاحيتها.
                    }
                }

                if (validUris.isNotEmpty()) {
                    val combined =
                        (productImageUris + validUris).distinct()

                    RoyalTVLocalMediaLibrary.saveImages(
                        context,
                        productSection,
                        combined
                    )

                    productImageUris = combined
                    productImageIndex = 0
                } else {
                    Toast.makeText(
                        context,
                        "تعذر الوصول إلى الصور المختارة",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    val adsImagePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments()
        ) { selectedUris ->
            if (selectedUris.isNotEmpty()) {
                val validUris = mutableListOf<android.net.Uri>()

                selectedUris.forEach { uri ->
                    try {
                        context.contentResolver
                            .takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        validUris.add(uri)
                    } catch (error: Exception) {
                        // نتجاوز الصورة التي تعذّر حفظ صلاحيتها.
                    }
                }

                if (validUris.isNotEmpty()) {
                    val combined =
                        (adsImageUris + validUris).distinct()

                    RoyalTVLocalMediaLibrary.saveImages(
                        context,
                        adsSection,
                        combined
                    )

                    adsImageUris = combined
                    adsImageIndex = 0
                } else {
                    Toast.makeText(
                        context,
                        "تعذر الوصول إلى صور العروض",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    val ordersImagePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                try {
                    context.contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                    RoyalTVLocalMediaLibrary.saveImage(
                        context,
                        RoyalTVLocalMediaLibrary.Section.ORDERS,
                        uri
                    )

                    ordersImageUri = uri
                } catch (error: Exception) {
                    Toast.makeText(
                        context,
                        "تعذر حفظ صورة الطلب",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        



        
    

    val displayState by
        RoyalTVDisplayStateManager.state.collectAsState()
        
    val tickerText by
        RoyalTVTickerManager.text.collectAsState()

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
     * تغيير المنتج كل 20 ثانية.
     */
    LaunchedEffect(Unit) {

        while (true) {
            delay(20_000L)

            productIndex =
                (productIndex + 1) % demoProducts.size
        }
    }
    
LaunchedEffect(productImageUris) {
    while (true) {
        delay(20_000L)

        if (productImageUris.isNotEmpty()) {
            productImageIndex =
                (productImageIndex + 1) % productImageUris.size
        }
    }
}

LaunchedEffect(adsImageUris) {
    while (true) {
        delay(20_000L)

        if (adsImageUris.isNotEmpty()) {
            adsImageIndex =
                (adsImageIndex + 1) % adsImageUris.size
        }
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
                displayState = displayState,
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
                            displayState = displayState,
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

                    
                    Box(
                        modifier = Modifier
                            .weight(2.2f)
                            .fillMaxWidth()
                    ) {
                        NewProductsPanel(
                            productIndex = productIndex,
                            imageUri = productImageUris.getOrNull(productImageIndex),
                            onChooseImage = {
                                productImagePicker.launch(arrayOf("image/*"))
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        RoyalPanelFrame(
                            
                    }

                    RoyalOrnamentalDivider()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        InfoPanel(
                            title = "العروض",
                            subtitle = "عروض الملكي",
                            accent = RoyalBurgundy,
                            imageUri = adsImageUris.getOrNull(adsImageIndex),
                            onChooseImage = {
                                adsImagePicker.launch(arrayOf("image/*"))
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        
                    }

                    RoyalOrnamentalDivider()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        InfoPanel(
                            title = "الطلبات",
                            subtitle = "حالة الطلبات",
                            accent = RoyalEmerald,
                            imageUri = ordersImageUri,
                            onChooseImage = {
                                ordersImagePicker.launch(arrayOf("image/*"))
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        
                    }
                    

                    
                    }
                }


                /*
                 * هوية البث الجديدة الخاصة بـ ROYAL TV.
                 */
                RoyalTVBroadcastIdentity(
                    newsText = tickerText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(identityHeight)
                )
                
            }
        }

        // زر الخيارات في واجهة القناة فقط
        if (!fullscreen) {
            RoyalTVOptionsOverlay(
                modifier = Modifier.fillMaxSize(),
                onRequestHomeScreenBroadcast =
                    onRequestHomeScreenBroadcast
            )
        }
    }
}

@Composable
private fun BroadcastWindow(
    liveAlpha: Float,
    fullscreen: Boolean,
    displayState: RoyalTVState,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSource by remember {
        mutableStateOf<String?>(null)
    }

    var selectedApp by remember {
        mutableStateOf<RoyalTVInstalledApp?>(null)
    }
    
    var fullscreenButtonVisible by remember {
        mutableStateOf(true)
    }

    var touchVersion by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(touchVersion) {
        fullscreenButtonVisible = true
        delay(5_000L)
        fullscreenButtonVisible = false
    }
    

    
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(
                        requireUnconsumed = false
                    )
                    touchVersion++
                }
            }
            .clip(RoundedCornerShape(9.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        RoyalCharcoal,
                        RoyalBlue,
                        RoyalBlack
                    )
                )
            )
            
.border(
    2.dp,
    Brush.linearGradient(
        listOf(
            RoyalGold,
            RoyalPearl,
            RoyalBlue,
            RoyalGold
        )
    ),
    RoundedCornerShape(9.dp)
)
.border(
    1.dp,
    RoyalPearl.copy(alpha = 0.85f),
    RoundedCornerShape(9.dp)
)

    ) {
        
        when {
            // عرض صفحة YouTube داخل نافذة البث نفسها.
            selectedSource == "يوتيوب" -> {
                RoyalTVEmbeddedWebSource(
                    url = "https://www.youtube.com/tv",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // قائمة التطبيقات، مع إعادة الاختيار إلى النافذة.
            selectedSource == "التطبيقات" -> {
                RoyalTVAppDiscoveryPanel(
                    onAppSelected = { app ->
                        selectedApp = app
                        selectedSource = "التطبيق المختار"
                    }
                )

                Text(
                    text = "‹ العودة",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(RoyalBlue)
                        .border(
                            1.dp,
                            RoyalGold,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            selectedSource = null
                            selectedApp = null
                        }
                        .padding(
                            horizontal = 14.dp,
                            vertical = 8.dp
                        ),
                    color = RoyalPearl,
                    fontWeight = FontWeight.Bold
                )
            }

        
            
            // فحص طريقة عرض التطبيق داخل نافذة ROYAL TV.
            selectedSource == "التطبيق المختار" -> {
                val context = LocalContext.current
                val app = selectedApp

                val capability = remember(app, context) {
                    app?.let {
                        RoyalTVAppDisplayCapabilityResolver.resolve(
                            context,
                            it
                        )
                    }
                }

                if (
                    capability?.mode ==
                        RoyalTVAppDisplayMode.EMBEDDED_WEB &&
                    !capability.embeddedUrl.isNullOrBlank()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        RoyalTVEmbeddedWebSource(
                            url = capability.embeddedUrl!!,
                            modifier = Modifier.fillMaxSize()
                        )

                        Text(
                            text = "‹ العودة",
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(RoyalBlue)
                                .border(
                                    1.dp,
                                    RoyalGold,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedSource = "التطبيقات"
                                }
                                .padding(
                                    horizontal = 14.dp,
                                    vertical = 8.dp
                                ),
                            color = RoyalPearl,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
} else {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = app?.label ?: "التطبيق المختار",
            color = RoyalGold,
            fontSize = 23.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Text(
            text = app?.packageName.orEmpty(),
            color = RoyalPearl,
            textAlign = TextAlign.Center
        )

        Text(
            text = capability?.message
                ?: "تعذّر تحديد طريقة عرض هذا التطبيق.",
            color = RoyalPearl,
            textAlign = TextAlign.Center
        )

        if (
            app != null &&
            capability?.mode ==
                RoyalTVAppDisplayMode.NATIVE_LAUNCH_ONLY
        ) {
            Text(
                text = "▶ تشغيل التطبيق على الجهاز",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(RoyalBlue)
                    .border(
                        1.dp,
                        RoyalGold,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        val packageManager =
                            context.packageManager

                        val launchIntent =
                            packageManager
                                .getLeanbackLaunchIntentForPackage(
                                    app.packageName
                                )
                                ?: packageManager
                                    .getLaunchIntentForPackage(
                                        app.packageName
                                    )

                        if (launchIntent == null) {
                            Toast.makeText(
                                context,
                                "لا يوجد مدخل تشغيل لهذا التطبيق",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            
try {
    // التحقق من إذن الظهور فوق التطبيقات.
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
        !android.provider.Settings.canDrawOverlays(context)
    ) {
        val permissionIntent = Intent(
            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse(
                "package:${context.packageName}"
            )
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(permissionIntent)

        Toast.makeText(
            context,
            "اسمح لـ ROYAL TV بالظهور فوق التطبيقات، ثم ارجع واضغط تشغيل التطبيق مرة أخرى.",
            Toast.LENGTH_LONG
        ).show()
    } else {
        // تشغيل هوية البث العائمة أولاً.
        val overlayIntent = Intent(
            context,
            RoyalTVBroadcastOverlayService::class.java
        ).setAction(
            RoyalTVBroadcastOverlayService.ACTION_START
        )

        context.startService(overlayIntent)

        // تجهيز قائمة التطبيقات للعودة إليها.
selectedSource = "التطبيقات"
selectedApp = null

// فتح التطبيق ضمن مسار الرجوع الحالي.
context.startActivity(launchIntent)
    }
} catch (_: Exception) {
    Toast.makeText(
        context,
        "تعذّر تشغيل التطبيق أو بدء هوية البث",
        Toast.LENGTH_LONG
    ).show()
}

                        }
                    }
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                color = RoyalPearl,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = "‹ العودة إلى التطبيقات",
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(RoyalBlue)
                .border(
                    1.dp,
                    RoyalGold,
                    RoundedCornerShape(8.dp)
                )
                .clickable {
                    selectedSource = "التطبيقات"
                }
                .padding(
                    horizontal = 16.dp,
                    vertical = 10.dp
                ),
            color = RoyalPearl,
            fontWeight = FontWeight.Bold
        )
    }
                }
                

            }
            
            // المصادر التي لم نربط تشغيلها بعد.
            selectedSource != null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = selectedSource.orEmpty(),
                        color = RoyalGold,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = "هذا المصدر لم يُربط بالتشغيل الداخلي بعد.",
                        color = RoyalPearl,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "‹ العودة إلى مصادر البث",
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RoyalBlue)
                            .border(
                                1.dp,
                                RoyalGold,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedSource = null
                            }
                            .padding(
                                horizontal = 16.dp,
                                vertical = 10.dp
                            ),
                        color = RoyalPearl,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // قائمة المصادر الرئيسية.
            displayState.source == RoyalTVSource.IDLE -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ROYAL TV",
                        color = RoyalGold,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = "اختر مصدر البث",
                        color = RoyalPearl,
                        fontSize = 15.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RoyalTVSourceTile(
                            "التطبيقات",
                            "اكتشاف تطبيقات الجهاز",
                            "▦",
                            Modifier.weight(1f)
                        ) {
                            selectedSource = "التطبيقات"
                        }

                        RoyalTVSourceTile(
                            "يوتيوب",
                            "YouTube Web",
                            "▶",
                            Modifier.weight(1f)
                        ) {
                            selectedSource = "يوتيوب"
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RoyalTVSourceTile(
                            "موسيقى",
                            "مكتبة الموسيقى",
                            "♫",
                            Modifier.weight(1f)
                        ) {
                            selectedSource = "موسيقى"
                        }

                        RoyalTVSourceTile(
                            "الفيديو",
                            "مكتبة الصور والفيديو",
                            "▣",
                            Modifier.weight(1f)
                        ) {
                            selectedSource = "الفيديو"
                        }
                    }
                }
            }

            // عرض حالة المحتوى القادم من نظام ROYAL TV.
            else -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val title = when (displayState.source) {
                        RoyalTVSource.IDLE -> "ROYAL TV"
                        RoyalTVSource.ORDER -> "طلب جديد"
                        RoyalTVSource.YOUTUBE -> "YouTube"
                        RoyalTVSource.MEDIA -> "وسائط ROYAL TV"
                        RoyalTVSource.AD -> "عروض الملكي"
                        RoyalTVSource.CONTENT -> "محتوى ROYAL TV"
                    }

                    Text(
                        text = title,
                        color = RoyalGold,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = if (displayState.isPlaying) {
                            "تم استلام أمر العرض"
                        } else {
                            "المحتوى متوقف"
                        },
                        color = RoyalPearl
                    )

                    Text(
                        text = displayState.payload.ifBlank {
                            "بانتظار المحتوى"
                        },
                        color = RoyalPearl.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "مصادر البث",
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RoyalBlue)
                            .border(
                                1.dp,
                                RoyalGold,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedSource = null
                            }
                            .padding(12.dp),
                        color = RoyalPearl
                    )
                }
            }
        }

        RoyalTVNotificationOverlay(Modifier.fillMaxSize())

        
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.Black.copy(alpha = 0.38f))
                .border(
                    width = 1.dp,
                    color = RoyalGold.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(9.dp)
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            
            RoyalTVBroadcastSignal()
            

            Text(
                text = "مباشر",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        

        
        
        
        if (fullscreenButtonVisible) {
            Text(
                text = if (fullscreen) "↙" else "⛶",
                modifier = Modifier
                    .align(
                        if (fullscreen) {
                            Alignment.BottomStart
                        } else {
                            Alignment.BottomEnd
                        }
                    )
                    .padding(12.dp)
                    .clickable {
                        onToggleFullscreen()
                    }
                    .padding(4.dp),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Normal
            )
        }
        
        
    }
}

@Composable
private fun RoyalTVBroadcastSignal() {
    val transition = rememberInfiniteTransition(
        label = "royal_broadcast_signal"
    )

    val innerAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "signal_inner"
    )

    val middleAlpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, delayMillis = 300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "signal_middle"
    )

    val outerAlpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, delayMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "signal_outer"
    )

    androidx.compose.foundation.Canvas(
        modifier = Modifier.size(24.dp)
    ) {
        val stroke = 2.dp.toPx()
        val cx = size.width / 2f
        val baseY = size.height * 0.88f

        // قاعدة الإشارة
        drawLine(
            color = RoyalRed,
            start = androidx.compose.ui.geometry.Offset(
                cx, baseY
            ),
            end = androidx.compose.ui.geometry.Offset(
                cx, size.height * 0.68f
            ),
            strokeWidth = stroke
        )

        // القوس الداخلي
        drawArc(
            color = RoyalRed.copy(alpha = innerAlpha),
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(
                cx - size.width * 0.16f,
                size.height * 0.48f
            ),
            size = androidx.compose.ui.geometry.Size(
                size.width * 0.32f,
                size.height * 0.32f
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )

        // القوس الأوسط
        drawArc(
            color = RoyalRed.copy(alpha = middleAlpha),
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(
                cx - size.width * 0.31f,
                size.height * 0.29f
            ),
            size = androidx.compose.ui.geometry.Size(
                size.width * 0.62f,
                size.height * 0.62f
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )

        // القوس الخارجي
        drawArc(
            color = RoyalRed.copy(alpha = outerAlpha),
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(
                cx - size.width * 0.46f,
                size.height * 0.08f
            ),
            size = androidx.compose.ui.geometry.Size(
                size.width * 0.92f,
                size.height * 0.92f
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
    }
}



@Composable
private fun RoyalTVSourceTile(
    title: String,
    subtitle: String,
    symbol: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF111722))
            .border(1.dp, RoyalGold, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(symbol, color = RoyalGold, fontSize = 26.sp)
        Text(
            title,
            color = RoyalPearl,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            subtitle,
            color = RoyalPearl.copy(alpha = 0.72f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
private fun NewProductsPanel(
    productIndex: Int,
    imageUri: android.net.Uri?,
    onChooseImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(RoyalBlue)
            .border(
                1.dp,
                RoyalRed,
                RoundedCornerShape(9.dp)
            )
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
         */
        
        /*
         * عرض المنتج الحالي دون AnimatedContent.
         */
        val product = demoProducts[
            productIndex.coerceIn(0, demoProducts.lastIndex)
        ]

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
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
                    contentAlignment = Alignment.Center
                ) {
                                        if (imageUri != null) {
                        AndroidView(
                            factory = { viewContext ->
                                ImageView(viewContext).apply {
                                    scaleType =
                                        ImageView.ScaleType.CENTER_CROP
                                }
                            },
                            update = { imageView ->
                                imageView.setImageURI(imageUri)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    onChooseImage()
                                }
                        )
                    } else {
                        Text(
                            text = "اضغط لاختيار صورة المنتج",
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    onChooseImage()
                                }
                                .padding(6.dp),
                            color = RoyalPearl,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                                        }
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

@Composable
private fun InfoPanel(
    title: String,
    subtitle: String,
    accent: Color,
    imageUri: android.net.Uri?,
    onChooseImage: () -> Unit,
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
                RoyalRed,
                RoundedCornerShape(9.dp)
            )
            .clickable(onClick = onChooseImage)
            .padding(
                horizontal = 6.dp,
                vertical = 7.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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

        if (imageUri != null) {
            AndroidView(
                factory = { viewContext ->
                    ImageView(viewContext).apply {
                        scaleType =
                            ImageView.ScaleType.CENTER_CROP
                    }
                },
                update = { imageView ->
                    imageView.setImageURI(imageUri)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(5.dp))
            )
        } else {
            Text(
                text = "اضغط لإضافة صورة",
                color = RoyalGold,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }

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

        
@Composable
private fun RoyalPanelFrame(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(
            id = R.drawable.royal_panel_frame
        ),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.FillBounds,
        alignment = Alignment.Center
    )
}


@Composable
private fun RoyalOrnamentalDivider(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(
            id = R.drawable.royal_panel_divider
        ),
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth()
            .height(35.dp),
        contentScale = ContentScale.FillBounds
    )
}




@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BottomIdentityBar(
    currentTime: String,
    currentDate: String,
    tickerText: String,
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
         * الشريط المتحرك.
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
                text = tickerText,
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
         * الوقت والتاريخ.
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
