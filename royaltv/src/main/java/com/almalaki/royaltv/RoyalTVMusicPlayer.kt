package com.almalaki.royaltv

import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.alpha
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
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

private val MusicGold = Color(0xFFB99A52)
private val MusicPearl = Color(0xFFDCE5EE)
private val MusicBlue = Color(0xFF172B4D)
private val MusicEmerald = Color(0xFF176B60)
private val MusicPanel = Color(0xFF080C12)
private val MusicSpectrum = listOf(
    Color(0xFFFF3B5C),
    Color(0xFFFF9D3D),
    Color(0xFFFFE45E),
    Color(0xFF42F59B),
    Color(0xFF35D9FF),
    Color(0xFF7067FF),
    Color(0xFFE65CFF),
    Color(0xFFFF3B5C)
)

private data class RoyalMusicTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: Uri,
    val duration: Int,
    val albumArt: Uri?
)

private fun queryRoyalMusic(context: Context): List<RoyalMusicTrack> {
    val result = mutableListOf<RoyalMusicTrack>()
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ALBUM_ID
    )

    try {
        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idCol =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationCol =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumCol =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)

                val title = cursor.getString(titleCol)
                    ?.takeIf { it.isNotBlank() }
                    ?: "مقطع صوتي"

                val artist = cursor.getString(artistCol)
                    ?.takeUnless {
                        it == "<unknown>" || it.isBlank()
                    }
                    ?: "فنان غير معروف"

                val duration =
                    cursor.getInt(durationCol).coerceAtLeast(0)

                val albumId = cursor.getLong(albumCol)

                if (duration > 0) {
                    result += RoyalMusicTrack(
                        id = id,
                        title = title,
                        artist = artist,
                        uri = Uri.withAppendedPath(
                            collection,
                            id.toString()
                        ),
                        duration = duration,
                        albumArt = if (albumId > 0L) {
                            Uri.parse(
                                "content://media/external/audio/albumart/$albumId"
                            )
                        } else {
                            null
                        }
                    )
                }
            }
        }
    } catch (_: Exception) {
        // تُعرض رسالة المكتبة بدل إيقاف التطبيق.
    }

    return result
}

private fun musicTime(milliseconds: Int): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

@Composable
fun RoyalTVMusicPlayer(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    tickerText: String = "",
    currentTime: String = "--:--",
    currentDate: String = "",
    liveAlpha: Float = 1f,
    showIdentity: Boolean = false
) {
    val context = LocalContext.current

    val prefs = remember {
        context.getSharedPreferences(
            "royal_music_preferences",
            Context.MODE_PRIVATE
        )
    }

    var tracks by remember {
        mutableStateOf<List<RoyalMusicTrack>>(emptyList())
    }

    var selectedId by remember { mutableStateOf<Long?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }
    var searchText by remember { mutableStateOf("") }

    var favorites by remember {
        mutableStateOf(
            prefs.getStringSet("favorites", emptySet())?.toSet()
                ?: emptySet()
        )
    }

    var shuffleEnabled by remember { mutableStateOf(false) }

    // 0 = إيقاف التكرار، 1 = تكرار الأغنية، 2 = تكرار القائمة
    var repeatMode by remember { mutableIntStateOf(0) }

    var showLibrary by remember { mutableStateOf(false) }
    var libraryTab by remember { mutableIntStateOf(0) }

    var message by remember {
        mutableStateOf("جارٍ تجهيز مكتبة الموسيقى")
    }

    var draggingPosition by remember { mutableStateOf<Float?>(null) }
    var completionCount by remember { mutableIntStateOf(0) }
    var handledCompletion by remember { mutableIntStateOf(0) }

    val permissionName = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    fun refreshLibrary() {
        tracks = queryRoyalMusic(context)

        message = if (tracks.isEmpty()) {
            "لم نعثر على ملفات موسيقى. تحقق من وجود الملفات ومنح الإذن."
        } else {
            "${tracks.size} مقطع صوتي متاح • المكتبة المحلية"
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                refreshLibrary()
            } else {
                message =
                    "يلزم السماح بالوصول إلى الموسيقى لعرض المكتبة."
            }
        }

    fun startTrack(index: Int) {
        if (tracks.isEmpty()) return

        val safeIndex = index.coerceIn(0, tracks.lastIndex)
        val track = tracks[safeIndex]

        try {
            player?.release()
            player = null
            isPlaying = false
            positionMs = 0
            durationMs = track.duration
            selectedId = track.id

            val created = MediaPlayer.create(context, track.uri)

            if (created == null) {
                message = "تعذّر تشغيل ${track.title}"
                return
            }

            created.setOnCompletionListener {
                completionCount++
            }

            created.isLooping = repeatMode == 1
            created.start()

            player = created
            isPlaying = true
            durationMs = created.duration.coerceAtLeast(0)
            message = "يعمل الآن: ${track.title}"
        } catch (_: Exception) {
            try {
                player?.release()
            } catch (_: Exception) {
                // تجاهل خطأ تحرير المشغّل.
            }

            player = null
            isPlaying = false
            message = "تعذّر تشغيل الملف الصوتي."

            Toast.makeText(
                context,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun moveTrack(direction: Int) {
        if (tracks.isEmpty()) return

        val currentIndex =
            tracks.indexOfFirst { it.id == selectedId }

        val nextIndex = if (shuffleEnabled && tracks.size > 1) {
            (
                tracks.indices
                    .filter { it != currentIndex }
                    .ifEmpty { tracks.indices.toList() }
            ).random()
        } else if (currentIndex < 0) {
            0
        } else {
            (currentIndex + direction + tracks.size) % tracks.size
        }

        startTrack(nextIndex)
    }

    fun toggleFavorite() {
        val id = selectedId?.toString() ?: return
        val updated = favorites.toMutableSet()

        if (!updated.add(id)) {
            updated.remove(id)
        }

        favorites = updated.toSet()
        prefs.edit().putStringSet("favorites", updated).apply()
    }

    LaunchedEffect(Unit) {
        if (
            ContextCompat.checkSelfPermission(
                context,
                permissionName
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            refreshLibrary()
        } else {
            permissionLauncher.launch(permissionName)
        }
    }

    LaunchedEffect(player, isPlaying) {
        while (player != null && isPlaying) {
            try {
                val activePlayer = player

                if (activePlayer != null) {
                    positionMs =
                        activePlayer.currentPosition.coerceAtLeast(0)

                    durationMs =
                        activePlayer.duration.coerceAtLeast(0)
                }
            } catch (_: Exception) {
                isPlaying = false
            }

            delay(350L)
        }
    }

    LaunchedEffect(completionCount) {
        if (completionCount > handledCompletion) {
            handledCompletion = completionCount

            if (repeatMode == 1) {
                // عند تفعيل تكرار الأغنية، يتولى MediaPlayer التكرار.
                positionMs = 0
                isPlaying = player != null
            } else {
                val index =
                    tracks.indexOfFirst { it.id == selectedId }

                if (repeatMode == 2 || index < tracks.lastIndex) {
                    moveTrack(1)
                } else {
                    isPlaying = false
                    positionMs = durationMs
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                player?.release()
            } catch (_: Exception) {
                // تجاهل خطأ تحرير المشغّل عند الخروج.
            }

            player = null
        }
    }

    BackHandler {
        if (showLibrary) {
            showLibrary = false
        } else {
            onBack()
        }
    }

    val currentTrack = tracks.firstOrNull {
        it.id == selectedId
    }

    val transition = rememberInfiniteTransition(
        label = "royal-music-orbit"
    )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 7000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc-spin"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
        val spectrumStep =
        ((rotation / 45f).toInt() % (MusicSpectrum.size - 1))
            .coerceIn(0, MusicSpectrum.size - 2)

    val animatedSpectrum =
        MusicSpectrum.drop(spectrumStep) +
            MusicSpectrum.take(spectrumStep)

    val visibleTracks = tracks.filter { track ->
        val matchesText =
            track.title.contains(searchText, ignoreCase = true) ||
                track.artist.contains(searchText, ignoreCase = true)

        val matchesTab = when (libraryTab) {
            1 -> favorites.contains(track.id.toString())
            2 -> searchText.isBlank() ||
                track.artist.contains(searchText, ignoreCase = true)
            else -> true
        }

        matchesText && matchesTab
    }

    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF05070C),
                        Color(0xFF101D32),
                        Color(0xFF0B302C),
                        Color(0xFF070A10)
                    )
                )
            )
            .border(
                width = 3.dp,
                brush = Brush.sweepGradient(
                    colors = animatedSpectrum
                ),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        val compact = maxHeight < 430.dp
Image(
    painter = painterResource(
        id = R.drawable.royal_music_nature
    ),
    contentDescription = "خلفية الطبيعة",
    modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(18.dp)),
    contentScale = ContentScale.Crop
)

Box(
    modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(18.dp))
        .background(Color.Black.copy(alpha = 0.48f))
)
        

Box(
    modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.42f))
)


        if (showLibrary) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 26.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "ROYAL MUSIC / LIBRARY",
                            color = MusicGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )

                        Text(
                            "مكتبة الموسيقى",
                            color = MusicPearl,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    MusicButton(
                        "إغلاق المكتبة",
                        onClick = { showLibrary = false }
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF121D2D))
                        .border(
                            1.dp,
                            MusicGold.copy(alpha = 0.5f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "⌕",
                        color = MusicGold,
                        fontSize = 27.sp
                    )

                    Spacer(Modifier.width(10.dp))

                    Box(Modifier.weight(1f)) {
                        if (searchText.isBlank()) {
                            Text(
                                "ابحث باسم الأغنية أو الفنان...",
                                color = MusicPearl.copy(alpha = 0.5f),
                                fontSize = 16.sp
                            )
                        }

                        BasicTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = MusicPearl,
                                fontSize = 16.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    MusicButton(
                        "مسح",
                        compact = true,
                        onClick = { searchText = "" }
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MusicButton(
                        "كل الأغاني",
                        active = libraryTab == 0,
                        onClick = { libraryTab = 0 }
                    )

                    MusicButton(
                        "المفضلة ♥",
                        active = libraryTab == 1,
                        onClick = { libraryTab = 1 }
                    )

                    MusicButton(
                        "الفنانون",
                        active = libraryTab == 2,
                        onClick = { libraryTab = 2 }
                    )

                    MusicButton(
                        "تحديث",
                        onClick = {
                            if (
                                ContextCompat.checkSelfPermission(
                                    context,
                                    permissionName
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                refreshLibrary()
                            } else {
                                permissionLauncher.launch(permissionName)
                            }
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    message,
                    color = MusicGold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                if (visibleTracks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (libraryTab == 1) {
                                "لا توجد أغانٍ مفضلة بعد"
                            } else {
                                "لا توجد نتائج في المكتبة"
                            },
                            color = MusicPearl.copy(alpha = 0.7f),
                            fontSize = 20.sp
                        )
                    }
                  } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(
                            items = visibleTracks,
                            key = { it.id }
                        ) { track ->
                            val selected = track.id == selectedId
                            val isFavorite =
                                favorites.contains(track.id.toString())

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(
                                        if (selected) {
                                            MusicGold.copy(alpha = 0.15f)
                                        } else {
                                            Color.White.copy(alpha = 0.045f)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) MusicGold
                                        else Color.White.copy(alpha = 0.06f),
                                        RoundedCornerShape(13.dp)
                                    )
                                    .clickable {
                                        val index =
                                            tracks.indexOfFirst {
                                                it.id == track.id
                                            }

                                        if (index >= 0) {
                                            startTrack(index)
                                            showLibrary = false
                                        }
                                    }
                                    .padding(
                                        horizontal = 12.dp,
                                        vertical = 9.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlbumArtwork(
                                    uri = track.albumArt,
                                    modifier = Modifier.size(50.dp),
                                    corner = 10.dp
                                )

                                Spacer(Modifier.width(12.dp))

                                Column(Modifier.weight(1f)) {
                                    Text(
                                        track.title,
                                        color = if (selected) MusicGold
                                        else MusicPearl,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        track.artist,
                                        color = MusicPearl.copy(alpha = 0.65f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    musicTime(track.duration),
                                    color = MusicPearl.copy(alpha = 0.65f),
                                    fontSize = 13.sp
                                )

                                Spacer(Modifier.width(12.dp))

                                MusicButton(
                                    if (isFavorite) "♥" else "♡",
                                    compact = true,
                                    active = isFavorite,
                                    onClick = {
                                        val updated =
                                            favorites.toMutableSet()
                                        val id = track.id.toString()

                                        if (!updated.add(id)) {
                                            updated.remove(id)
                                        }

                                        favorites = updated.toSet()
                                        prefs.edit()
                                            .putStringSet("favorites", updated)
                                            .apply()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 28.dp,
                        vertical = if (compact) 12.dp else 20.dp
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "ROYAL",
                            color = MusicGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 5.sp
                        )

                        Text(
                            "MUSIC SYSTEM",
                            color = MusicPearl,
                            fontSize = if (compact) 20.sp else 27.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MusicButton(
                            "المكتبة  ☷",
                            onClick = { showLibrary = true }
                        )

                        MusicButton(
                              "رجوع",
                            onClick = onBack
                        )
                    }
                }

                Spacer(
                    Modifier.height(if (compact) 8.dp else 16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // القرص الرقمي والغطاء الفني
                    Box(
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF173C48),
                                        Color(0xFF101D32),
                                        Color(0xFF080C12)
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                MusicGold.copy(alpha = 0.4f),
                                RoundedCornerShape(28.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                    
Box(
    modifier = Modifier
        .size(if (compact) 190.dp else 285.dp)
        .rotate(if (isPlaying) rotation else 0f)
        .clip(CircleShape),
    contentAlignment = Alignment.Center
) {
    Image(
        painter = painterResource(
            id = R.drawable.royal_music_disc
        ),
        contentDescription = "قرص الموسيقى الملكي",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )

    Box(
        modifier = Modifier
            .size(if (compact) 100.dp else 148.dp)
            .clip(CircleShape)
            .background(Color(0xFF09111A))
            .border(
                2.dp,
                MusicGold.copy(alpha = 0.85f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (currentTrack?.albumArt != null) {
            AlbumArtwork(
                uri = currentTrack.albumArt,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
                corner = 100.dp
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "♫",
                    color = MusicGold,
                    fontSize = if (compact) 36.sp else 54.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    "ROYAL TV",
                    color = MusicPearl.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
    
                            repeat(6) { ring ->
                                Box(
                                    modifier = Modifier
                                        .size(
                                            (if (compact) 174 else 264).dp -
                                                (ring * (if (compact) 23 else 35)).dp
                                        )
                                        .border(
                                            1.dp,
                                            if (ring % 2 == 0) {
                                                MusicPearl.copy(alpha = 0.17f)
                                            } else {
                                                MusicGold.copy(alpha = 0.18f)
                                            },
                                            CircleShape
                                        )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(if (compact) 105.dp else 155.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF09111A))
                                    .border(
                                        2.dp,
                                        MusicEmerald.copy(alpha = 0.8f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentTrack?.albumArt != null) {
                                    AlbumArtwork(
                                        uri = currentTrack.albumArt,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(7.dp),
                                        corner = 100.dp
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            "♫",
                                            color = MusicGold,
                                            fontSize = if (compact) 36.sp else 54.sp,
                                            fontWeight = FontWeight.Black
                                        )

                                        Text(
                                            "ROYAL",
                                            color = MusicPearl.copy(alpha = 0.7f),
                                            fontSize = 10.sp,
                                            letterSpacing = 2.sp
                                        )
                                    }
                                }
                            }
                        }


                        Text(
                            if (isPlaying) "●  PLAYING" else "◉  READY",
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp),
                            color = if (isPlaying) MusicGold
                            else MusicPearl.copy(alpha = 0.65f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    // معلومات الأغنية وأدوات التحكم
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "NOW PLAYING",
                            color = MusicGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            currentTrack?.title ?: "اختر أغنيتك التالية",
                            color = MusicPearl,
                            fontSize = if (compact) 22.sp else 31.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            currentTrack?.artist ?: "مكتبة الموسيقى المحلية",
                            color = MusicPearl.copy(alpha = 0.65f),
                            fontSize = if (compact) 14.sp else 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(
                            Modifier.height(if (compact) 8.dp else 18.dp)
                        )
Box(
    modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(Color.White.copy(alpha = 0.07f))
        .border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.10f),
            shape = RoundedCornerShape(14.dp)
        )
        .padding(
            horizontal = 12.dp,
            vertical = 5.dp
        )
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Slider(
            value = (
                draggingPosition ?: positionMs.toFloat()
            ).coerceIn(
                0f,
                durationMs.coerceAtLeast(1).toFloat()
            ),
            onValueChange = {
                draggingPosition = it
            },
            onValueChangeFinished = {
                draggingPosition?.let { target ->
                    try {
                        player?.seekTo(target.toInt())
                        positionMs = target.toInt()
                    } catch (_: Exception) {
                        // تجاهل تعذر تغيير الموضع.
                    }
                }

                draggingPosition = null
            },
            valueRange = 0f..durationMs.coerceAtLeast(1).toFloat(),
            enabled = player != null,
    colors = SliderDefaults.colors(
    thumbColor = MusicGold,
    activeTrackColor = MusicGold,
    inactiveTrackColor = Color(0xFFB8C0CC),
    disabledThumbColor = MusicGold.copy(alpha = 0.45f),
    disabledActiveTrackColor = MusicGold.copy(alpha = 0.45f),
    disabledInactiveTrackColor = Color(0xFFB8C0CC)
)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                musicTime(
                    draggingPosition?.toInt() ?: positionMs
                ),
                color = MusicGold,
                fontSize = 12.sp
            )

            Text(
                musicTime(durationMs),
                color = MusicPearl.copy(alpha = 0.75f),
                fontSize = 12.sp
            )
        }
    }
}

 Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    MusicImageButton(
        iconRes = R.drawable.royal_music_previous,
        description = "الأغنية السابقة",
        modifier = Modifier.weight(1f),
        onClick = { moveTrack(-1) }
    )

    MusicImageButton(
        iconRes = R.drawable.royal_music_repeat_one,
        description = "تكرار الأغنية الحالية",
        modifier = Modifier.weight(1f),
        active = repeatMode == 1,
        onClick = {
            repeatMode = if (repeatMode == 1) 0 else 1
            try {
                player?.isLooping = repeatMode == 1
            } catch (_: Exception) {
            }
        }
    )

    MusicImageButton(
        iconRes = if (isPlaying) {
            R.drawable.royal_music_pause
        } else {
            R.drawable.royal_music_play
        },
        description = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
        modifier = Modifier.weight(1.2f),
        primary = true,
        enabled = tracks.isNotEmpty(),
        onClick = {
            try {
                val active = player

                if (active == null) {
                    if (tracks.isNotEmpty()) {
                        val index = if (selectedId == null) {
                            0
                        } else {
                            tracks.indexOfFirst {
                                it.id == selectedId
                            }.coerceAtLeast(0)
                        }
                        startTrack(index)
                    }
                } else if (active.isPlaying) {
                    active.pause()
                    isPlaying = false
                } else {
                    if (
                        durationMs > 0 &&
                        positionMs >= durationMs
                    ) {
                        active.seekTo(0)
                        positionMs = 0
                    }
                    active.start()
                    isPlaying = true
                }
            } catch (_: Exception) {
                message = "تعذّر التحكم بالتشغيل."
            }
        }
    )

    MusicImageButton(
        iconRes = R.drawable.royal_music_next,
        description = "الأغنية التالية",
        modifier = Modifier.weight(1f),
        onClick = { moveTrack(1) }
    )
}

Spacer(Modifier.height(8.dp))

Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    MusicImageButton(
        iconRes = R.drawable.royal_music_shuffle,
        description = "تشغيل عشوائي",
        modifier = Modifier.weight(1f),
        active = shuffleEnabled,
        onClick = {
            shuffleEnabled = !shuffleEnabled
        }
    )

    MusicImageButton(
        iconRes = R.drawable.royal_music_repeat,
        description = "تكرار قائمة الأغاني",
        modifier = Modifier.weight(1f),
        active = repeatMode == 2,
        onClick = {
            repeatMode = if (repeatMode == 2) 0 else 2
            try {
                player?.isLooping = false
            } catch (_: Exception) {
            }
        }
    )

    MusicButton(
        if (
            currentTrack != null &&
            favorites.contains(currentTrack.id.toString())
        ) "♥" else "♡",
        Modifier.weight(1f),
        active = currentTrack != null &&
            favorites.contains(currentTrack.id.toString()),
        onClick = { toggleFavorite() }
    )
}
    

Spacer(Modifier.height(8.dp))

Text(
    message,
    color = MusicPearl.copy(alpha = 0.55f),
    fontSize = 11.sp,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis
)

                    }
                }

                // لا نرسم الهوية السفلية هنا؛ تبقى هوية ROYAL TV
                // الأصلية في شاشة البث الأم ولا تتكرر داخل المشغّل.
            }
        }
    }
}

@Composable
private fun AlbumArtwork(
    uri: Uri?,
    modifier: Modifier = Modifier,
    corner: androidx.compose.ui.unit.Dp = 12.dp
) {
    if (uri == null) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(corner))
                .background(
                    Brush.linearGradient(
                        listOf(MusicBlue, MusicEmerald)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "♫",
                color = MusicGold,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
        }
    } else {
        AndroidView(
            modifier = modifier
                .clip(RoundedCornerShape(corner))
                .background(MusicBlue),
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setImageURI(uri)
                }
            },
            update = { view ->
                if (view.tag != uri.toString()) {
                    view.tag = uri.toString()

                    try {
                        view.setImageURI(uri)
                    } catch (_: Exception) {
                        view.setImageDrawable(null)
                    }
                }
            }
        )
    }
}

@Composable
private fun MusicImageButton(
    iconRes: Int,
    description: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = CircleShape

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(shape)
            .background(
                when {
                    !enabled -> Color.White.copy(alpha = 0.04f)
                    primary -> MusicGold.copy(alpha = 0.20f)
                    active -> MusicEmerald.copy(alpha = 0.55f)
                    else -> Color(0xFF101D32).copy(alpha = 0.80f)
                }
            )
            .border(
                width = if (primary) 2.dp else 1.dp,
                color = when {
                    !enabled -> MusicGold.copy(alpha = 0.20f)
                    primary -> MusicGold
                    active -> MusicGold.copy(alpha = 0.90f)
                    else -> MusicGold.copy(alpha = 0.50f)
                },
                shape = shape
            )
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = description,
            modifier = Modifier
                .size(27.dp)
                .then(
                    if (enabled) Modifier
                    else Modifier.alpha(0.35f)
                ),
            contentScale = ContentScale.Fit
        )
    }
}


@Composable
private fun MusicButton(
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    active: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false,
    round: Boolean = false,
    square: Boolean = false,
    onClick: () -> Unit
) {
    val shape = when {
        round -> CircleShape
        square -> RoundedCornerShape(8.dp)
        else -> RoundedCornerShape(if (compact) 10.dp else 13.dp)
    }
    

    Box(
        modifier = modifier
            .height(if (compact) 34.dp else 44.dp)
            .clip(shape)
            .background(
                when {
                    !enabled -> Color.White.copy(alpha = 0.04f)
                    primary -> MusicGold
                    active -> MusicEmerald.copy(alpha = 0.88f)
                    else -> MusicBlue.copy(alpha = 0.95f)
                }
            )
            .border(
                1.dp,
                if (enabled) {
                    MusicGold.copy(
                        alpha = if (active || primary) 0.9f else 0.45f
                    )
                } else {
                    MusicGold.copy(alpha = 0.15f)
                },
                shape
            )
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = when {
                !enabled -> MusicPearl.copy(alpha = 0.3f)
                primary -> MusicPanel
                else -> MusicPearl
            },
            fontSize = if (compact) 11.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
