
package com.almalaki.royaltv

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
import androidx.compose.foundation.BasicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

private val MusicGold = Color(0xFFB99A52)
private val MusicPearl = Color(0xFFDCE5EE)
private val MusicBlue = Color(0xFF172B4D)
private val MusicEmerald = Color(0xFF176B60)
private val MusicPanel = Color(0xFF080C12)

private data class RoyalMusicTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: Uri,
    val duration: Int
)

private fun queryRoyalMusic(context: Context): List<RoyalMusicTrack> {
    val result = mutableListOf<RoyalMusicTrack>()
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION
    )

    try {
        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "مقطع صوتي"
                val artist = cursor.getString(artistColumn)
                    ?.takeUnless { it == "<unknown>" }
                    ?: "فنان غير معروف"
                val duration = cursor.getInt(durationColumn).coerceAtLeast(0)
                val uri = Uri.withAppendedPath(collection, id.toString())

                if (duration > 0) {
                    result.add(
                        RoyalMusicTrack(
                            id = id,
                            title = title,
                            artist = artist,
                            uri = uri,
                            duration = duration
                        )
                    )
                }
            }
        }
    } catch (_: Exception) {
        // تعالج الواجهة حالة عدم القدرة على قراءة المكتبة.
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
    liveAlpha: Float = 1f
) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("royal_music_preferences", Context.MODE_PRIVATE)
    }

    var tracks by remember { mutableStateOf<List<RoyalMusicTrack>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }
    var searchText by remember { mutableStateOf("") }
    var showFavorites by remember { mutableStateOf(false) }
    var favorites by remember {
        mutableStateOf(preferences.getStringSet("favorites", emptySet()) ?: emptySet())
    }
    var shuffleEnabled by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) }
    var completionCount by remember { mutableIntStateOf(0) }
    var handledCompletion by remember { mutableIntStateOf(0) }
    var draggingPosition by remember { mutableStateOf<Float?>(null) }
    var libraryMessage by remember { mutableStateOf("جارٍ تجهيز مكتبة الموسيقى") }

    val permissionName = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    fun refreshLibrary() {
        tracks = queryRoyalMusic(context)
        libraryMessage = if (tracks.isEmpty()) {
            "لم نعثر على ملفات موسيقى. تأكد من وجود ملفات صوتية ومنح الإذن."
        } else {
            "تم العثور على ${tracks.size} مقطع صوتي"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            refreshLibrary()
        } else {
            libraryMessage = "يلزم السماح بالوصول إلى الموسيقى لعرض المكتبة."
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
            selectedIndex = safeIndex

            val newPlayer = MediaPlayer.create(context, track.uri)
            if (newPlayer == null) {
                libraryMessage = "تعذّر تشغيل: ${track.title}"
                return
            }

            newPlayer.setOnCompletionListener {
                completionCount++
            }
            newPlayer.isLooping = repeatMode == 1
            newPlayer.start()

            player = newPlayer
            isPlaying = true
            durationMs = newPlayer.duration.coerceAtLeast(0)
            libraryMessage = "يعمل الآن: ${track.title}"
        } catch (_: Exception) {
            player = null
            isPlaying = false
            libraryMessage = "تعذّر تشغيل الملف الصوتي."
            Toast.makeText(context, libraryMessage, Toast.LENGTH_SHORT).show()
        }
    }

    fun moveTrack(direction: Int) {
        if (tracks.isEmpty()) return

        val next = when {
            selectedIndex < 0 -> 0
            shuffleEnabled && tracks.size > 1 -> {
                var candidate = selectedIndex
                repeat(4) {
                    if (candidate == selectedIndex) {
                        candidate = (tracks.indices).random()
                    }
                }
                candidate
            }
            else -> {
                val candidate = selectedIndex + direction
                if (candidate < 0) tracks.lastIndex
                else if (candidate > tracks.lastIndex) 0
                else candidate
            }
        }
        startTrack(next)
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, permissionName) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            refreshLibrary()
        } else {
            libraryMessage = "اسمح بالوصول إلى الموسيقى لتحميل المكتبة."
        }
    }

    LaunchedEffect(player, isPlaying) {
        while (player != null && isPlaying) {
            try {
                positionMs = player?.currentPosition?.coerceAtLeast(0) ?: 0
                durationMs = player?.duration?.coerceAtLeast(0) ?: 0
            } catch (_: Exception) {
                isPlaying = false
            }
            delay(400L)
        }
    }

    LaunchedEffect(completionCount) {
        if (completionCount > handledCompletion) {
            handledCompletion = completionCount
            when (repeatMode) {
                1 -> {
                    try {
                        player?.seekTo(0)
                        player?.start()
                        isPlaying = true
                        positionMs = 0
                    } catch (_: Exception) {
                        isPlaying = false
                    }
                }
                2 -> moveTrack(1)
                else -> {
                    if (selectedIndex in 0 until tracks.lastIndex) {
                        moveTrack(1)
                    } else {
                        isPlaying = false
                        positionMs = durationMs
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                player?.release()
            } catch (_: Exception) {
            }
        }
    }

    BackHandler { onBack() }

    val currentTrack = tracks.getOrNull(selectedIndex)
    val filteredTracks = tracks.filter { track ->
        val matchesSearch =
            track.title.contains(searchText, ignoreCase = true) ||
            track.artist.contains(searchText, ignoreCase = true)
        val matchesFavorite = !showFavorites || favorites.contains(track.id.toString())
        matchesSearch && matchesFavorite
    }

    val transition = rememberInfiniteTransition(label = "royal-disc")
    val discRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc-rotation"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF070A10),
                        Color(0xFF14213A),
                        Color(0xFF10332F),
                        Color(0xFF080C12)
                    )
                )
            )
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        val compact = maxHeight < 430.dp

        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "ROYAL MUSIC",
                        color = MusicGold,
                        fontSize = if (compact) 12.sp else 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    )
                    Text(
                        "مكتبة الموسيقى",
                        color = MusicPearl,
                        fontSize = if (compact) 21.sp else 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                MusicButton("رجوع", onClick = onBack)
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // منطقة القرص والمقطع الحالي
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MusicEmerald.copy(alpha = 0.36f),
                                        MusicBlue.copy(alpha = 0.6f),
                                        MusicPanel
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                MusicGold.copy(alpha = 0.45f),
                                RoundedCornerShape(24.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val discSize = if (compact) 145.dp else 210.dp

                        Box(
                            modifier = Modifier
                                .size(discSize)
                                .rotate(if (isPlaying) discRotation else 0f)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFF3D4653),
                                            Color(0xFF0A0D13),
                                            Color(0xFF252D39),
                                            Color(0xFF080A0E)
                                        )
                                    )
                                )
                                .border(2.dp, MusicGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                repeat(5) { ring ->
                                    Box(
                                        Modifier
                                            .padding(vertical = 3.dp)
                                            .size((discSize.value - ring * 19).dp)
                                            .border(
                                                1.dp,
                                                MusicPearl.copy(alpha = 0.10f),
                                                CircleShape
                                            )
                                    )
                                }
                            }

                            Box(
                                Modifier
                                    .size(if (compact) 48.dp else 64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(MusicGold, Color(0xFFE7D29B))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "♫",
                                    color = MusicPanel,
                                    fontSize = if (compact) 25.sp else 34.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Text(
                            if (isPlaying) "يعمل الآن" else "ROYAL AUDIO",
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                            color = MusicGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        currentTrack?.title ?: "اختر أغنية للبدء",
                        color = MusicPearl,
                        fontSize = if (compact) 16.sp else 21.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        currentTrack?.artist ?: "مكتبتك الموسيقية المحلية",
                        color = MusicGold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val sliderValue = (
                        draggingPosition ?: positionMs.toFloat()
                    ).coerceIn(0f, durationMs.coerceAtLeast(1).toFloat())

                    Slider(
                        value = sliderValue,
                        onValueChange = { draggingPosition = it },
                        onValueChangeFinished = {
                            draggingPosition?.let { target ->
                                try {
                                    val safe = target.toInt().coerceIn(
                                        0,
                                        durationMs.coerceAtLeast(0)
                                    )
                                    player?.seekTo(safe)
                                    positionMs = safe
                                } catch (_: Exception) {
                                }
                            }
                            draggingPosition = null
                        },
                        valueRange = 0f..durationMs.coerceAtLeast(1).toFloat(),
                        enabled = player != null,
                        colors = SliderDefaults.colors(
                            thumbColor = MusicGold,
                            activeTrackColor = MusicGold,
                            inactiveTrackColor = MusicPearl.copy(alpha = 0.22f)
                        )
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            musicTime(draggingPosition?.toInt() ?: positionMs),
                            color = MusicPearl.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                        Text(
                            musicTime(durationMs),
                            color = MusicPearl.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MusicButton("السابق", Modifier.weight(1f)) {
                            moveTrack(-1)
                        }
                        MusicButton(
                            if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                            Modifier.weight(1.3f),
                            primary = true,
                            enabled = player != null
                        ) {
                            try {
                                val active = player
                                if (active != null) {
                                    if (active.isPlaying) {
                                        active.pause()
                                        isPlaying = false
                                    } else {
                                        if (positionMs >= durationMs && durationMs > 0) {
                                            active.seekTo(0)
                                            positionMs = 0
                                        }
                                        active.start()
                                        isPlaying = true
                                    }
                                }
                            } catch (_: Exception) {
                            }
                        }
                        MusicButton("التالي", Modifier.weight(1f)) {
                            moveTrack(1)
                        }
                    }

                    Spacer(Modifier.height(5.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MusicButton(
                            if (shuffleEnabled) "عشوائي ✓" else "عشوائي",
                            Modifier.weight(1f),
                            active = shuffleEnabled
                        ) {
                            shuffleEnabled = !shuffleEnabled
                        }

                        MusicButton(
                            when (repeatMode) {
                                1 -> "تكرار الأغنية"
                                2 -> "تكرار القائمة"
                                else -> "التكرار: إيقاف"
                            },
                            Modifier.weight(1.5f),
                            active = repeatMode != 0
                        ) {
                            repeatMode = (repeatMode + 1) % 3
                            try {
                                player?.isLooping = repeatMode == 1
                            } catch (_: Exception) {
                            }
                        }

                        MusicButton(
                            if (currentTrack != null &&
                                favorites.contains(currentTrack.id.toString())
                            ) "♥" else "♡",
                            Modifier.weight(0.6f),
                            active = currentTrack != null &&
                                favorites.contains(currentTrack.id.toString())
                        ) {
                            currentTrack?.let { track ->
                                val id = track.id.toString()
                                val updated = favorites.toMutableSet()
                                if (updated.contains(id)) updated.remove(id)
                                else updated.add(id)
                                favorites = updated
                                preferences.edit()
                                    .putStringSet("favorites", updated)
                                    .apply()
                            }
                        }
                    }
                }

                // مكتبة الأغاني والبحث
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xD90B111D))
                        .border(
                            1.dp,
                            MusicGold.copy(alpha = 0.30f),
                            RoundedCornerShape(22.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "مكتبتك",
                            color = MusicPearl,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        MusicButton("تحديث", compact = true) {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    permissionName
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                refreshLibrary()
                            } else {
                                permissionLauncher.launch(permissionName)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF182337))
                            .border(
                                1.dp,
                                MusicGold.copy(alpha = 0.35f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (searchText.isEmpty()) {
                            Text(
                                "ابحث باسم الأغنية أو الفنان…",
                                color = MusicPearl.copy(alpha = 0.55f),
                                fontSize = 12.sp
                            )
                        }
                        BasicTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = MusicPearl,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MusicButton(
                            "كل الأغاني",
                            Modifier.weight(1f),
                            active = !showFavorites,
                            compact = true
                        ) {
                            showFavorites = false
                        }
                        MusicButton(
                            "المفضلة",
                            Modifier.weight(1f),
                            active = showFavorites,
                            compact = true
                        ) {
                            showFavorites = true
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        libraryMessage,
                        color = MusicGold.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(6.dp))

                    if (filteredTracks.isEmpty()) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (showFavorites) {
                                    "لا توجد أغانٍ مفضلة بعد"
                                } else {
                                    "لا توجد نتائج مطابقة"
                                },
                                color = MusicPearl.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            itemsIndexed(
                                filteredTracks,
                                key = { _, track -> track.id }
                            ) { _, track ->
                                val originalIndex = tracks.indexOfFirst {
                                    it.id == track.id
                                }
                                val selected = selectedIndex >= 0 &&
                                    tracks.getOrNull(selectedIndex)?.id == track.id

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (selected) {
                                                MusicGold.copy(alpha = 0.16f)
                                            } else {
                                                Color.White.copy(alpha = 0.035f)
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) MusicGold.copy(alpha = 0.7f)
                                            else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            if (originalIndex >= 0) {
                                                startTrack(originalIndex)
                                            }
                                        }
                                        .padding(horizontal = 9.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MusicBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (selected && isPlaying) "♫" else "▶",
                                            color = MusicGold,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(Modifier.width(9.dp))

                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            track.title,
                                            color = if (selected) MusicGold else MusicPearl,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            track.artist,
                                            color = MusicPearl.copy(alpha = 0.58f),
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Text(
                                        musicTime(track.duration),
                                        color = MusicPearl.copy(alpha = 0.65f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // هوية ROYAL TV السفلية
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF252B35))
                    .border(
                        1.dp,
                        MusicGold.copy(alpha = 0.7f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    "الملكي",
                    color = MusicGold,
                    fontSize = if (compact) 14.sp else 18.sp,
                    fontWeight = FontWeight.Black
                )

                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MusicPanel)
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        tickerText.ifBlank { "أهلاً بكم في ROYAL TV" },
                        color = MusicPearl,
                        fontSize = if (compact) 9.sp else 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        currentTime,
                        color = MusicGold,
                        fontSize = if (compact) 10.sp else 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        currentDate,
                        color = MusicPearl,
                        fontSize = if (compact) 7.sp else 9.sp,
                        maxLines = 1
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF401820))
                        .padding(horizontal = 7.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF4545).copy(alpha = liveAlpha))
                    )
                    Text(
                        "مباشر",
                        color = Color(0xFFFF6666),
                        fontSize = if (compact) 9.sp else 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
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
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Text(
        text = label,
        modifier = modifier
            .clip(shape)
            .background(
                when {
                    !enabled -> Color.White.copy(alpha = 0.04f)
                    primary -> MusicGold
                    active -> MusicEmerald.copy(alpha = 0.85f)
                    else -> MusicBlue.copy(alpha = 0.92f)
                }
            )
            .border(
                1.dp,
                if (enabled) MusicGold.copy(alpha = 0.65f)
                else MusicGold.copy(alpha = 0.18f),
                shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 7.dp else 10.dp
            ),
        color = when {
            !enabled -> MusicPearl.copy(alpha = 0.3f)
            primary -> MusicPanel
            else -> MusicPearl
        },
        fontSize = if (compact) 10.sp else 12.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
