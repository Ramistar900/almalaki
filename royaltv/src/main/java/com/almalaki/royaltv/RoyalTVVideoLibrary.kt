package com.almalaki.royaltv

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

    var section by remember {
        mutableStateOf("الرئيسية")
    }

    var selectedImageIndex by remember {
        mutableIntStateOf(0)
    }

    var selectedVideo by remember {
        mutableStateOf<RoyalLocalMedia?>(null)
    }

    var slideshowRunning by remember {
        mutableStateOf(false)
    }

    var slideshowSeconds by remember {
        mutableIntStateOf(5)
    }

    var fitImage by remember {
        mutableStateOf(true)
    }

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
            selectedVideo != null -> {
                selectedVideo = null
            }

            section == "معاينة" -> {
                section = "الصور"
                slideshowRunning = false
            }

            section == "الشرائح" -> {
                slideshowRunning = false
                section = "الرئيسية"
            }

            section != "الرئيسية" -> {
                slideshowRunning = false
                section = "الرئيسية"
            }

            else -> onBack()
        }
    }

    BackHandler {
        goBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VideoBlack)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ROYAL MEDIA",
                color = VideoGold,
                fontWeight = FontWeight.ExtraBold
            )

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
                color = VideoPearl,
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
                        text = "اسمح بالوصول إلى الصور والفيديو الموجودة على الجهاز",
                        color = VideoPearl,
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
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = "مكتبة الصور والفيديو",
                            color = VideoPearl,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            text = "تشغيل الملفات المحلية دون إعادة ضغطها",
                            color = VideoPearl.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )

                        RoyalVideoButton(
                            text = "▧ معرض الصور (${images.size})",
                            onClick = {
                                section = "الصور"
                                selectedImageIndex = 0
                                refreshLibrary()
                            }
                        )

                        RoyalVideoButton(
                            text = "▶ مكتبة الفيديو (${videos.size})",
                            onClick = {
                                section = "قائمة الفيديو"
                                refreshLibrary()
                            }
                        )

                        RoyalVideoButton(
                            text = "▤ عرض شرائح الصور",
                            onClick = {
                                section = "الشرائح"
                                selectedImageIndex = 0
                                slideshowRunning = true
                            }
                        )

                        RoyalVideoButton(
                            text = "↻ تحديث المكتبة",
                            onClick = { refreshLibrary() }
                        )
                    }
                }

                "الصور", "الشرائح" -> {
                    if (!canReadImages) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "يلزم إذن الوصول إلى الصور",
                                    color = VideoPearl,
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
                    } else if (images.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد صور متاحة. حدّث المكتبة للتحقق مجددًا.",
                                color = VideoPearl,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val safeIndex = selectedImageIndex.coerceIn(
                            0,
                            images.lastIndex
                        )

                        val media = images[safeIndex]

                        AnimatedContent(
                            targetState = media.uri,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
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

                                        adjustViewBounds = true

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

                                    if (
                                        imageView.tag != imageUri.toString()
                                    ) {
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
                                .padding(vertical = 5.dp),
                            color = VideoPearl,
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
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RoyalVideoButton(
                                text = if (fitImage) {
                                    "ملء الشاشة"
                                } else {
                                    "احتواء الصورة"
                                },
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    fitImage = !fitImage
                                }
                            )

                            if (section == "الشرائح") {
                                RoyalVideoButton(
                                    text = if (slideshowRunning) {
                                        "إيقاف مؤقت"
                                    } else {
                                        "استئناف"
                                    },
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        slideshowRunning = !slideshowRunning
                                    }
                                )
                            }
                        }

                        if (section == "الشرائح") {
                            Text(
                                text = "مدة عرض الصورة: $slideshowSeconds ثانية",
                                color = VideoGold,
                                modifier = Modifier.padding(top = 5.dp)
                            )

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

                
                "قائمة الفيديو" -> {
                    if (!canReadVideos) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "يلزم إذن الوصول إلى الفيديو",
                                    color = VideoPearl,
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
                    } else if (videos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لم يتم العثور على فيديوهات متاحة.",
                                color = VideoPearl,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        )
                        {items(
    count = videos.size,
    key = { index -> videos[index].id }
) { index ->
    val video = videos[index]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(VideoBlue.copy(alpha = 0.65f))
            .border(
                1.dp,
                VideoGold.copy(alpha = 0.45f),
                RoundedCornerShape(9.dp)
            )
            .clickable {
                selectedVideo = video
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = video.title,
                color = VideoPearl,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = royalDurationLabel(video.durationMs),
                color = VideoGold
            )
        }

        Text(
            text = "▶ تشغيل",
            color = VideoGold,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
                        }
                        
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا يوجد محتوى للعرض",
                          color = VideoPearl
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ROYAL",
                color = VideoGold,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "● مباشر",
                color = VideoPearl,
                fontWeight = FontWeight.SemiBold
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
