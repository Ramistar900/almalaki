package com.almalaki.royaltv

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.alpha
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.BroadcastReceiver
import android.content.IntentFilter
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke

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

@OptIn(ExperimentalMaterial3Api::class)
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
    var spectrumData by remember { mutableStateOf(ByteArray(0)) }
    var pendingSpectrumTrackIndex by remember {
    mutableIntStateOf(-1)
}

var spectrumPermissionResolved by remember {
    mutableStateOf(
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    )
}

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
        val spectrumPermissionLauncher =
    rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        spectrumPermissionResolved = true
    }
        
fun startTrack(index: Int) {
    if (tracks.isEmpty()) return

    val safeIndex = index.coerceIn(0, tracks.lastIndex)

if (!spectrumPermissionResolved) {
    pendingSpectrumTrackIndex = safeIndex
    spectrumPermissionLauncher.launch(
        Manifest.permission.RECORD_AUDIO
    )
    return
}

val track = tracks[safeIndex]

    try {
        val intent = Intent(
            context,
            RoyalTVMusicPlaybackService::class.java
        ).apply {
            action =
                RoyalTVMusicPlaybackService.ACTION_START_PLAYLIST

            putStringArrayListExtra(
                RoyalTVMusicPlaybackService.EXTRA_URIS,
                ArrayList(tracks.map { it.uri.toString() })
            )

            putStringArrayListExtra(
                RoyalTVMusicPlaybackService.EXTRA_TITLES,
                ArrayList(tracks.map { it.title })
            )

            putStringArrayListExtra(
                RoyalTVMusicPlaybackService.EXTRA_ARTISTS,
                ArrayList(tracks.map { it.artist })
            )

            putExtra(
                RoyalTVMusicPlaybackService.EXTRA_INDEX,
                safeIndex
            )
        }

        androidx.core.content.ContextCompat.startForegroundService(
            context,
            intent
        )

        selectedId = track.id
        positionMs = 0
        durationMs = track.duration
        isPlaying = true
        message = "يعمل الآن: ${track.title}"
    } catch (_: Exception) {
        isPlaying = false
        message = "تعذّر تشغيل الملف الصوتي."

        Toast.makeText(
            context,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
LaunchedEffect(
    pendingSpectrumTrackIndex,
    spectrumPermissionResolved
) {
    val pendingIndex = pendingSpectrumTrackIndex

    if (pendingIndex >= 0 && spectrumPermissionResolved) {
        pendingSpectrumTrackIndex = -1
        startTrack(pendingIndex)
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

    DisposableEffect(context, tracks) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                                if (intent == null) return

                if (
                    intent.action ==
                    RoyalTVMusicPlaybackService.ACTION_SPECTRUM
                ) {
                    spectrumData =
                        intent.getByteArrayExtra(
                            RoyalTVMusicPlaybackService.EXTRA_SPECTRUM
                        )?.copyOf() ?: ByteArray(0)
                    return
                }

                if (
                    intent.action !=
                    RoyalTVMusicPlaybackService.ACTION_STATE
                ) return

                val index = intent.getIntExtra(
                    RoyalTVMusicPlaybackService.EXTRA_STATE_INDEX,
                    -1
                )

                selectedId = tracks.getOrNull(index)?.id

                positionMs = intent.getIntExtra(
                    RoyalTVMusicPlaybackService.EXTRA_STATE_POSITION,
                    0
                ).coerceAtLeast(0)

                durationMs = intent.getIntExtra(
                    RoyalTVMusicPlaybackService.EXTRA_STATE_DURATION,
                    0
                ).coerceAtLeast(0)

                isPlaying = intent.getBooleanExtra(
                    RoyalTVMusicPlaybackService.EXTRA_STATE_PLAYING,
                    false
                )

                repeatMode = intent.getIntExtra(
                    RoyalTVMusicPlaybackService.EXTRA_STATE_REPEAT_MODE,
                    repeatMode
                ).coerceIn(0, 2)

                tracks.getOrNull(index)?.let { track ->
                    message = "يعمل الآن: ${track.title}"
                }
            }
        }

        
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter().apply {
                addAction(
                    RoyalTVMusicPlaybackService.ACTION_STATE
                )
                addAction(
                    RoyalTVMusicPlaybackService.ACTION_SPECTRUM
                )
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
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
        id = R.drawable.royal_video_library_bg
    ),
    contentDescription = null,
    modifier = Modifier.fillMaxSize(),
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
                            .fillMaxHeight(),
                        
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

                        Text(
    if (isPlaying) "●  PLAYING" else "◉  READY",
    modifier = Modifier
        .fillMaxWidth()
        .padding(top = 16.dp),
    color = if (isPlaying) MusicGold
    else MusicPearl.copy(alpha = 0.65f),
    fontSize = 11.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 2.sp,
    
    textAlign = TextAlign.Center
)

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .height(38.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        val bars = 32
                        val gap = 3.dp.toPx()
                        val barWidth =
                            (size.width - gap * (bars - 1)) / bars

                        val binCount =
    if (spectrumData.size > 2) {
        (spectrumData.size - 2) / 2
    } else {
        0
    }

for (i in 0 until bars) {
    val raw =
        if (binCount > 0 && spectrumData.isNotEmpty()) {
            val maxBin = binCount.coerceAtLeast(1)

            val startBin = Math.pow(
                maxBin.toDouble(),
                i.toDouble() / bars
            ).toInt().coerceIn(1, maxBin)

            val endBin = Math.ceil(
                Math.pow(
                    maxBin.toDouble(),
                    (i + 1).toDouble() / bars
                )
            ).toInt().coerceIn(startBin, maxBin)

            var peak = 0f

            for (bin in startBin..endBin) {
                val realIndex = bin * 2
                val imaginaryIndex = realIndex + 1

                if (imaginaryIndex < spectrumData.size) {
                    val magnitude = kotlin.math.hypot(
                        spectrumData[realIndex].toDouble(),
                        spectrumData[imaginaryIndex].toDouble()
                    ).toFloat() / 128f

                    if (magnitude > peak) {
                        peak = magnitude
                    }
                }
            }

            kotlin.math.sqrt(peak.coerceIn(0f, 1f))
        } else {
            0f
        }

    val amplitude =
        if (isPlaying && spectrumData.isNotEmpty()) {
            (raw * 0.85f + 0.12f).coerceIn(0.12f, 1f)
        } else {
            0.08f
        }

    val barHeight = size.height * amplitude
    val x = i * (barWidth + gap)
    val y = (size.height - barHeight) / 2f

    drawRoundRect(
        color = MusicSpectrum[i % MusicSpectrum.size].copy(
            alpha = if (isPlaying) 0.95f else 0.25f
        ),
        topLeft = androidx.compose.ui.geometry.Offset(x, y),
        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            barWidth / 2f
        )
    )
}

                    }
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
        BoxWithConstraints(
    modifier = Modifier.fillMaxWidth()
) {
    val progressThumbSize = (maxWidth * 0.018f)
        .coerceIn(12.dp, 24.dp)

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
                    val intent = Intent(
                        context,
                        RoyalTVMusicPlaybackService::class.java
                    ).apply {
                        action =
                            RoyalTVMusicPlaybackService.ACTION_SEEK
                        putExtra(
                            RoyalTVMusicPlaybackService.EXTRA_SEEK_POSITION,
                            target.toInt().coerceAtLeast(0)
                        )
                    }

                    context.startService(intent)
                    positionMs = target.toInt()
                } catch (_: Exception) {
                    message = "تعذّر تغيير موضع الأغنية."
                }
            }

            draggingPosition = null
        },
        valueRange = 0f..durationMs.coerceAtLeast(1).toFloat(),
        enabled = durationMs > 0,
        thumb = {
            Box(
                modifier = Modifier
                    .size(progressThumbSize)
                    .clip(CircleShape)
                    .background(MusicGold)
            )
        },
        colors = SliderDefaults.colors(
            thumbColor = MusicGold,
            activeTrackColor = MusicGold,
            inactiveTrackColor = Color(0xFFB8C0CC),
            disabledThumbColor = MusicGold.copy(alpha = 0.45f),
            disabledActiveTrackColor = MusicGold.copy(alpha = 0.45f),
            disabledInactiveTrackColor = Color(0xFFB8C0CC)
        )
    )
        }

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
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    // السابق: اتجاه السهمين إلى اليمين
    MusicImageButton(
        iconRes = R.drawable.royal_music_next,
        description = "الأغنية السابقة",
        modifier = Modifier.weight(1f),
        onClick = { moveTrack(-1) }
    )

    // تشغيل وإيقاف
    MusicImageButton(
        iconRes = if (isPlaying) {
            R.drawable.royal_music_pause
        } else {
            R.drawable.royal_music_play
        },
        description = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
        modifier = Modifier.weight(1f),
        primary = true,
        enabled = tracks.isNotEmpty(),
        onClick = {
    if (selectedId == null) {
        if (tracks.isNotEmpty()) {
            startTrack(0)
        }
    } else {
        val intent = Intent(
            context,
            RoyalTVMusicPlaybackService::class.java
        ).apply {
            action =
                RoyalTVMusicPlaybackService.ACTION_TOGGLE
        }

        try {
            context.startService(intent)
        } catch (_: Exception) {
            message = "تعذّر التحكم بالتشغيل."
        }
    }
        }
        )

    // التالي: اتجاه السهمين إلى اليسار
    MusicImageButton(
        iconRes = R.drawable.royal_music_previous,
        description = "الأغنية التالية",
        modifier = Modifier.weight(1f),
        onClick = { moveTrack(1) }
    )
}

Spacer(Modifier.height(8.dp))

Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
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
    val nextMode = if (repeatMode == 2) 0 else 2

    val intent = Intent(
        context,
        RoyalTVMusicPlaybackService::class.java
    ).apply {
        action =
            RoyalTVMusicPlaybackService.ACTION_SET_REPEAT_MODE

        putExtra(
            RoyalTVMusicPlaybackService.EXTRA_REPEAT_MODE,
            nextMode
        )
    }

    try {
        context.startService(intent)
        repeatMode = nextMode
    } catch (_: Exception) {
        message = "تعذّر تغيير وضع التكرار."
    }
        }
    )

MusicImageButton(
    iconRes = R.drawable.royal_music_repeat_one,
    description = "تكرار الأغنية الحالية",
    modifier = Modifier.weight(1f),
    active = repeatMode == 1,
    onClick = {
    val nextMode = if (repeatMode == 1) 0 else 1

    val intent = Intent(
        context,
        RoyalTVMusicPlaybackService::class.java
    ).apply {
        action =
            RoyalTVMusicPlaybackService.ACTION_SET_REPEAT_MODE

        putExtra(
            RoyalTVMusicPlaybackService.EXTRA_REPEAT_MODE,
            nextMode
        )
    }

    try {
        context.startService(intent)
        repeatMode = nextMode
    } catch (_: Exception) {
        message = "تعذّر تغيير وضع التكرار."
    }
    }
    )


    MusicButton(
        if (
            currentTrack != null &&
            favorites.contains(currentTrack.id.toString())
        ) "♥" else "♡",
        modifier = Modifier.weight(1f),
        active = currentTrack != null &&
            favorites.contains(currentTrack.id.toString()),
        round = true,
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

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        
        // حجم متكيف مع مساحة الزر المتاحة.
        val availableSize = maxWidth.coerceAtMost(maxHeight)
        val buttonSize = (availableSize * 0.82f)
            .coerceIn(32.dp, 88.dp)
            

        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(shape)
                .background(
                    when {
                        !enabled ->
                            Color.White.copy(alpha = 0.04f)

                        primary ->
                            MusicGold.copy(alpha = 0.18f)

                        active ->
                            MusicEmerald.copy(alpha = 0.35f)

                        else ->
                            Color.Transparent
                    }
                )
                
                .then(
                    if (active && !primary) {
                        Modifier.border(
                            width = 1.5.dp,
                            color = MusicGold.copy(alpha = 0.95f),
                            shape = shape
                        )
                    } else {
                        Modifier
                    }
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
                    .fillMaxSize(0.70f)
                    .then(
                        if (enabled) Modifier
                        else Modifier.alpha(0.35f)
                    ),
                contentScale = ContentScale.Fit
            )
        }
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
