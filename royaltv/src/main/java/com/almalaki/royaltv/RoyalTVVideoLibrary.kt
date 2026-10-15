package com.almalaki.royaltv

import android.graphics.Bitmap
import android.os.CancellationSignal
import android.util.Size
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

private val VideoGold = Color(0xFFB99A52)
private val VideoPearl = Color(0xFFDCE5EE)
private val VideoBlue = Color(0xFF172B4D)
private val VideoBlack = Color(0xFF080C12)

private data class RoyalLocalMedia(
    val id: Long,
    val title: String,
    val uri: Uri,
    val isVideo: Boolean,
    val durationMs: Long = 0L
)

private fun royalImagePermission(): String {
    return if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

private fun royalVideoPermission(): String {
    return if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

private fun royalHasImagePermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        royalImagePermission()
    ) == PackageManager.PERMISSION_GRANTED
}

private fun royalHasVideoPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        royalVideoPermission()
    ) == PackageManager.PERMISSION_GRANTED
}

private fun royalMediaPermissionList(): Array<String> {
    return if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun queryRoyalLocalMedia(
    context: Context,
    videosOnly: Boolean
): List<RoyalLocalMedia> {
    val result = mutableListOf<RoyalLocalMedia>()

    val collection = if (videosOnly) {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    val projection = if (videosOnly) {
        arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        )
    } else {
        arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME
        )
    }

    try {
        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(
                MediaStore.MediaColumns._ID
            )
            val nameColumn = cursor.getColumnIndexOrThrow(
                MediaStore.MediaColumns.DISPLAY_NAME
            )
            val durationColumn = if (videosOnly) {
                cursor.getColumnIndex(
                    MediaStore.Video.Media.DURATION
                )
            } else {
                -1
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)

                val title = cursor.getString(nameColumn)
                    ?.takeIf { it.isNotBlank() }
                    ?: if (videosOnly) "فيديو" else "صورة"

                val uri = Uri.withAppendedPath(
                    collection,
                    id.toString()
                )

                val duration = if (durationColumn >= 0) {
                    cursor.getLong(durationColumn).coerceAtLeast(0L)
                } else {
                    0L
                }

                result.add(
                    RoyalLocalMedia(
                        id = id,
                        title = title,
                        uri = uri,
                        isVideo = videosOnly,
                        durationMs = duration
                    )
                )
            }
        }
    } catch (_: Exception) {
        // نتجنب إيقاف الواجهة إذا تعذرت قراءة المكتبة.
    }

    return result
}

private fun royalDurationLabel(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}


@Composable
fun RoyalTVVideoLibrary(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var canReadImages by remember {
        mutableStateOf(royalHasImagePermission(context))
    }
    var canReadVideos by remember {
        mutableStateOf(royalHasVideoPermission(context))
    }
    var images by remember {
        mutableStateOf<List<RoyalLocalMedia>>(emptyList())
    }
    var videos by remember {
        mutableStateOf<List<RoyalLocalMedia>>(emptyList())
    }
    var section by remember { mutableStateOf("الرئيسية") }
    var slideshowReturnSection by remember {
    mutableStateOf("الرئيسية")
    }
    var selectedImageIndex by remember { mutableIntStateOf(0) }
    var selectedVideo by remember {
        mutableStateOf<RoyalLocalMedia?>(null)
    }
    var slideshowRunning by remember { mutableStateOf(false) }
    var slideshowSeconds by remember { mutableIntStateOf(5) }
    var fitImage by remember { mutableStateOf(true) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            canReadImages = royalHasImagePermission(context)
            canReadVideos = royalHasVideoPermission(context)
        }

    fun refreshLibrary() {
        canReadImages = royalHasImagePermission(context)
        canReadVideos = royalHasVideoPermission(context)

        images = if (canReadImages) {
            queryRoyalLocalMedia(context, false)
        } else {
            emptyList()
        }

        videos = if (canReadVideos) {
            queryRoyalLocalMedia(context, true)
        } else {
            emptyList()
        }
    }

    LaunchedEffect(canReadImages, canReadVideos) {
        refreshLibrary()
    }

    LaunchedEffect(
        slideshowRunning,
        slideshowSeconds,
        section,
        images.size
    ) {
        while (
            slideshowRunning &&
            section == "الشرائح" &&
            images.isNotEmpty()
        ) {
            delay(slideshowSeconds * 1000L)
            selectedImageIndex =
                (selectedImageIndex + 1) % images.size
        }
    }

    
    fun goBack() {
        when {
            selectedVideo != null -> selectedVideo = null

            section == "معاينة" -> {
                section = "شبكة الصور"
                slideshowRunning = false
            }

            section == "الشرائح" -> {
    slideshowRunning = false
    section = slideshowReturnSection
            }

            section != "الرئيسية" -> {
                slideshowRunning = false
                section = "الرئيسية"
            }

            else -> onBack()
        }
    }
    

    BackHandler { goBack() }

    val gold = VideoGold
    val pearl = VideoPearl
    val panel = Color(0xFF111B2B)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VideoBlack)
            .padding(horizontal = 22.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "ROYAL MEDIA",
                    color = gold,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "مكتبة الوسائط المحلية",
                    color = pearl.copy(alpha = 0.68f)
                )
            }

            RoyalVideoButton(
                text = "‹ رجوع",
                onClick = { goBack() }
            )
        }

        if (selectedVideo != null) {
            val media = selectedVideo!!

            keyVideoPlayer(
                media = media,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Text(
                text = media.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                color = pearl,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        } else if (!canReadImages && !canReadVideos) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "اسمح بالوصول إلى الصور والفيديو لعرض ملفات جهازك",
                        color = pearl,
                        textAlign = TextAlign.Center
                    )
                    RoyalVideoButton(
                        text = "منح الإذن",
                        onClick = {
                            permissionLauncher.launch(
                                royalMediaPermissionList()
                            )
                        }
                    )
                }
            }
        } else {
            when (section) {
                "الرئيسية" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "مكتبتك، بأسلوب ملكي",
                            color = pearl,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "اختر محتواك المحلي واستمتع بالمشاهدة على الشاشة الكبيرة.",
                            color = pearl.copy(alpha = 0.68f)
                        )

                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement =
                                Arrangement.spacedBy(16.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(panel)
                                        .border(
                                            1.dp,
                                            gold.copy(alpha = 0.7f),
                                            RoundedCornerShape(18.dp)
                                        )
                                        .clickable {
                                            refreshLibrary()
                                            section = "قائمة الفيديو"
                                        }
                                        .padding(20.dp),
                                    verticalArrangement =
                                        Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "▶",
                                        color = gold,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "مكتبة الفيديو",
                                        color = pearl,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${videos.size} ملف فيديو",
                                        color = gold
                                    )
                                    Text(
                                        text = "فتح المكتبة  →",
                                        color = pearl.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(panel)
                                        .border(
                                            1.dp,
                                            gold.copy(alpha = 0.7f),
                                            RoundedCornerShape(18.dp)
                                        )
                                        
.clickable {
    refreshLibrary()
    section = "شبكة الصور"
    selectedImageIndex = 0
}

                                        .padding(20.dp),
                                    verticalArrangement =
                                        Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "▧",
                                        color = gold,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "معرض الصور",
                                        color = pearl,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${images.size} صورة",
                                        color = gold
                                    )
                                    Text(
                                        text = "فتح المعرض  →",
                                        color = pearl.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RoyalVideoButton(
    text = "▤ عرض الشرائح",
    modifier = Modifier.weight(1f),
    onClick = {
        refreshLibrary()
        selectedImageIndex = 0
        slideshowReturnSection = "الرئيسية"
        section = "الشرائح"
        slideshowRunning = true
    }
)
                            RoyalVideoButton(
                                text = "↻ تحديث المكتبة",
                                modifier = Modifier.weight(1f),
                                onClick = { refreshLibrary() }
                            )
                        }
                    }
                }

                "قائمة الفيديو" -> {
                    if (!canReadVideos) {
                        RoyalMediaEmptyState(
                            message = "يلزم إذن الوصول إلى الفيديو",
                            action = "منح الإذن",
                            onAction = {
                                permissionLauncher.launch(
                                    royalMediaPermissionList()
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else if (videos.isEmpty()) {
                        RoyalMediaEmptyState(
                            message = "لا توجد فيديوهات متاحة على الجهاز",
                            action = "تحديث المكتبة",
                            onAction = { refreshLibrary() },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(
                                minSize = 210.dp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement =
                                Arrangement.spacedBy(12.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                count = videos.size,
                                key = { index -> videos[index].id }
                            ) { index ->
                                val video = videos[index]

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(panel)
                                        .border(
                                            1.dp,
                                            gold.copy(alpha = 0.45f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            selectedVideo = video
                                        }
                                        .padding(14.dp),
                                    verticalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {
                                    
val thumbnail by produceState<Bitmap?>(
    initialValue = null,
    key1 = video.uri
) {
    value = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                context.contentResolver.loadThumbnail(
                    video.uri,
                    Size(640, 360),
                    CancellationSignal()
                )
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }
}

Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(VideoBlue),
    contentAlignment = Alignment.Center
) {
    if (thumbnail != null) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
            },
            update = { imageView ->
                imageView.setImageBitmap(thumbnail)
            }
        )
    } else {
        Text(
            text = "▶",
            color = gold,
            fontWeight = FontWeight.ExtraBold
        )
    }

    Text(
        text = "▶",
        modifier = Modifier
            .background(
                VideoBlack.copy(alpha = 0.65f),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = gold,
        fontWeight = FontWeight.ExtraBold
    )
}


                                    Text(
                                        text = video.title,
                                        color = pearl,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement =
                                            Arrangement.SpaceBetween,
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = royalDurationLabel(
                                                video.durationMs
                                            ),
                                            color = gold
                                        )
                                        Text(
                                            text = "تشغيل  ▶",
                                            color = pearl
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                "شبكة الصور" -> {
                    if (!canReadImages) {
                        RoyalMediaEmptyState(
                            message = "يلزم إذن الوصول إلى الصور",
                            action = "منح الإذن",
                            onAction = {
                                permissionLauncher.launch(
                                    royalMediaPermissionList()
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else if (images.isEmpty()) {
                        RoyalMediaEmptyState(
                            message = "لا توجد صور متاحة على الجهاز",
                            action = "تحديث المكتبة",
                            onAction = { refreshLibrary() },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(
                                minSize = 150.dp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement =
                                Arrangement.spacedBy(10.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                count = images.size,
                                key = { index -> images[index].id }
                            ) { index ->
                                val image = images[index]

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(panel)
                                        .border(
                                            1.dp,
                                            gold.copy(alpha = 0.55f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedImageIndex = index
                                            section = "معاينة"
                                        }
                                        .padding(8.dp)
                                ) {
                                    AndroidView(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(
                                                RoundedCornerShape(8.dp)
                                            )
                                            .background(VideoBlue),
                                        factory = { ctx ->
                                            ImageView(ctx).apply {
                                                scaleType =
                                                    ImageView.ScaleType.CENTER_CROP
                                            }
                                        },
                                        update = { imageView ->
                                            val uriKey =
                                                image.uri.toString()

                                            if (imageView.tag != uriKey) {
                                                imageView.tag = uriKey
                                                imageView.setImageURI(
                                                    image.uri
                                                )
                                            }
                                        }
                                    )

                                    Spacer(
                                        Modifier.height(8.dp)
                                    )

                                    Text(
                                        text = image.title,
                                        color = pearl,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = "عرض الصورة  ◉",
                                        color = gold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
                

                "معاينة", "الشرائح" -> {
                    if (!canReadImages) {
                        RoyalMediaEmptyState(
                            message = "يلزم إذن الوصول إلى الصور",
                            action = "منح الإذن",
                            onAction = {
                                permissionLauncher.launch(
                                    royalMediaPermissionList()
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else if (images.isEmpty()) {
                        RoyalMediaEmptyState(
                            message = "لا توجد صور متاحة على الجهاز",
                            action = "تحديث المكتبة",
                            onAction = { refreshLibrary() },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        val safeIndex = selectedImageIndex.coerceIn(
                            0,
                            images.lastIndex
                        )
                        val media = images[safeIndex]

                        AnimatedContent(
                            targetState = media.uri,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "royal_image_transition"
                        ) { imageUri ->
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx ->
                                    ImageView(ctx).apply {
                                        layoutParams =
                                            ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                        scaleType =
                                            ImageView.ScaleType.FIT_CENTER
                                        setBackgroundColor(
                                            android.graphics.Color.BLACK
                                        )
                                    }
                                },
                                update = { imageView ->
                                    imageView.scaleType =
                                        if (fitImage) {
                                            ImageView.ScaleType.FIT_CENTER
                                        } else {
                                            ImageView.ScaleType.CENTER_CROP
                                        }

                                    if (imageView.tag != imageUri.toString()) {
                                        imageView.tag = imageUri.toString()
                                        imageView.setImageURI(imageUri)
                                    }
                                }
                            )
                        }

                        Text(
                            text = "${safeIndex + 1} / ${images.size} — ${media.title}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            color = pearl,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RoyalVideoButton(
                                text = "السابق",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    selectedImageIndex =
                                        if (safeIndex == 0) {
                                            images.lastIndex
                                        } else {
                                            safeIndex - 1
                                        }
                                }
                            )
                            RoyalVideoButton(
                                text = "التالي",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    selectedImageIndex =
                                        (safeIndex + 1) % images.size
                                }
                            )
                            RoyalVideoButton(
                                text = if (fitImage) "ملء الشاشة" else "احتواء",
                                modifier = Modifier.weight(1f),
                                onClick = { fitImage = !fitImage }
                            )
                        }

                        if (section == "الشرائح") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement =
                                    Arrangement.spacedBy(10.dp)
                            ) {
                                RoyalVideoButton(
                                    text = if (slideshowRunning) "إيقاف" else "استئناف",
                                    onClick = {
                                        slideshowRunning = !slideshowRunning
                                    }
                                )
                                Text(
                                    text = "مدة الصورة: $slideshowSeconds ث",
                                    color = gold
                                )
                            }

                            Slider(
                                value = slideshowSeconds.toFloat(),
                                onValueChange = {
                                    slideshowSeconds = it.toInt()
                                        .coerceIn(3, 15)
                                },
                                valueRange = 3f..15f,
                                steps = 11
                            )
                        }
                    }
                }

                else -> {
                    RoyalMediaEmptyState(
                        message = "لا يوجد محتوى للعرض",
                        action = "العودة للرئيسية",
                        onAction = { section = "الرئيسية" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RoyalMediaEmptyState(
    message: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = message,
                color = VideoPearl,
                textAlign = TextAlign.Center
            )
            RoyalVideoButton(
                text = action,
                onClick = onAction
            )
        }
    }
}



@Composable
private fun keyVideoPlayer(
    media: RoyalLocalMedia,
    modifier: Modifier = Modifier
) {
    var videoView by remember {
        mutableStateOf<VideoView?>(null)
    }

    AndroidView(
        modifier = modifier
            .background(Color.Black),
        factory = { ctx ->
            VideoView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                val controller = MediaController(ctx)
                controller.setAnchorView(this)
                setMediaController(controller)

                videoView = this
            }
        },
        update = { view ->
            if (view.tag != media.uri.toString()) {
                view.tag = media.uri.toString()
                view.setVideoURI(media.uri)

                view.setOnPreparedListener { player ->
                    player.isLooping = false
                    player.start()
                }
            }
        }
    )

    DisposableEffect(media.uri, videoView) {
        onDispose {
            videoView?.stopPlayback()
        }
    }
}

@Composable
private fun RoyalVideoButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(VideoBlue.copy(alpha = 0.85f))
            .border(
                width = 1.dp,
                color = VideoGold.copy(alpha = 0.65f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = 16.dp,
                vertical = 11.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = VideoPearl,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
