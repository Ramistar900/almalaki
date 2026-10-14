
package com.almalaki.royaltv

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val MusicGold = Color(0xFFB99A52)
private val MusicPearl = Color(0xFFDCE5EE)
private val MusicBlue = Color(0xFF172B4D)
private val MusicEmerald = Color(0xFF176B60)
private val MusicPanel = Color(0xFF080C12)


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

    var selectedUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var selectedName by remember {
        mutableStateOf("اختر ملفاً صوتياً من جهازك")
    }

    var isPlaying by remember {
        mutableStateOf(false)
    }

    var player by remember {
        mutableStateOf<MediaPlayer?>(null)
    }

    var durationMs by remember {
        mutableIntStateOf(0)
    }

    var positionMs by remember {
        mutableIntStateOf(0)
    }

    var draggingPosition by remember {
        mutableStateOf<Float?>(null)
    }

    BackHandler {
        onBack()
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // بعض مزودي الملفات لا يدعمون الصلاحية الدائمة.
                }

                try {
                    player?.release()
                } catch (_: Exception) {
                }

                player = null
                isPlaying = false
                positionMs = 0
                durationMs = 0

                val newPlayer = MediaPlayer.create(context, uri)

                if (newPlayer == null) {
                    selectedUri = null
                    selectedName = "تعذّر فتح الملف الصوتي"

                    Toast.makeText(
                        context,
                        "تعذّر فتح هذا الملف الصوتي",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    selectedUri = uri

                    selectedName = uri.lastPathSegment
                        ?.substringAfterLast('/')
                        ?.substringAfterLast(':')
                        ?.ifBlank { "ملف صوتي" }
                        ?: "ملف صوتي"

                    player = newPlayer
                    durationMs = newPlayer.duration.coerceAtLeast(0)

                    newPlayer.setOnCompletionListener {
                        isPlaying = false
                        positionMs = durationMs
                    }

                    newPlayer.start()
                    isPlaying = true
                }
            } catch (_: Exception) {
                try {
                    player?.release()
                } catch (_: Exception) {
                }

                player = null
                selectedUri = null
                isPlaying = false

                Toast.makeText(
                    context,
                    "لم يتمكن ROYAL TV من تشغيل الملف",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    LaunchedEffect(player, isPlaying) {
        while (player != null && isPlaying) {
            try {
                val activePlayer = player ?: break

                positionMs =
                    activePlayer.currentPosition.coerceAtLeast(0)

                durationMs =
                    activePlayer.duration.coerceAtLeast(0)
            } catch (_: Exception) {
                isPlaying = false
            }

            delay(500L)
        }
    }

    DisposableEffect(player) {
        val activePlayer = player

        onDispose {
            try {
                activePlayer?.release()
            } catch (_: Exception) {
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF080C12),
                        Color(0xFF101B2D),
                        Color(0xFF102B2A),
                        Color(0xFF080C12)
                    )
                )
            )
    ) {
        val compact = maxHeight < 420.dp
        val sidePadding = if (maxWidth > 900.dp) 42.dp else 20.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = sidePadding,
                    vertical = if (compact) 10.dp else 18.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // رأس واجهة الموسيقى
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ROYAL MUSIC",
                        color = MusicGold,
                        fontSize = if (compact) 12.sp else 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp
                    )

                    Text(
                        text = "مكتبة الموسيقى",
                        color = MusicPearl,
                        fontSize = if (compact) 20.sp else 27.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                MusicAction(
                    label = "‹ مصادر البث",
                    compact = compact
                ) {
                    onBack()
                }
            }

            Spacer(
                Modifier.height(if (compact) 10.dp else 20.dp)
            )

            // مساحة العرض الرئيسية بنسبة مرنة تناسب التلفزيون
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    if (compact) 18.dp else 34.dp
                )
            ) {
                // القرص الفني
                Box(
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MusicEmerald.copy(alpha = 0.35f),
                                    MusicBlue.copy(alpha = 0.55f),
                                    Color(0xFF0A0D13)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            MusicGold.copy(alpha = 0.35f),
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (compact) 128.dp else 220.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF28354A),
                                        Color(0xFF090B10),
                                        Color(0xFF1C2635)
                                    )
                                )
                            )
                            .border(
                                2.dp,
                                MusicGold.copy(alpha = 0.8f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (compact) 46.dp else 68.dp)
                                .clip(CircleShape)
                                .background(MusicGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "♫",
                                color = MusicPanel,
                                fontSize = if (compact) 26.sp else 38.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Text(
                        text = if (isPlaying) "يعمل الآن" else "ROYAL AUDIO",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(
                                bottom = if (compact) 10.dp else 18.dp
                            ),
                        color = MusicGold,
                        fontSize = if (compact) 11.sp else 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                // معلومات المقطع والتحكم
                Column(
                    modifier = Modifier.weight(1.15f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "المقطع الحالي",
                        color = MusicGold,
                        fontSize = if (compact) 12.sp else 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = selectedName,
                        color = MusicPearl,
                        fontSize = if (compact) 18.sp else 25.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(
                        Modifier.height(if (compact) 8.dp else 16.dp)
                    )

                    val sliderValue = (
                        draggingPosition
                            ?: positionMs.toFloat()
                        ).coerceIn(
                            0f,
                            durationMs.coerceAtLeast(1).toFloat()
                        )

                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            draggingPosition = it
                        },
                        onValueChangeFinished = {
                            val target = draggingPosition

                            if (target != null) {
                                try {
                                    val safeTarget = target.toInt()
                                        .coerceIn(
                                            0,
                                            durationMs.coerceAtLeast(0)
                                        )

                                    player?.seekTo(safeTarget)
                                    positionMs = safeTarget
                                } catch (_: Exception) {
                                }
                            }

                            draggingPosition = null
                        },
                        valueRange = 0f..durationMs
                            .coerceAtLeast(1).toFloat(),
                        enabled = selectedUri != null,
                        colors = SliderDefaults.colors(
                            thumbColor = MusicGold,
                            activeTrackColor = MusicGold,
                            inactiveTrackColor =
                                MusicPearl.copy(alpha = 0.22f)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMusicTime(
                                draggingPosition?.toInt() ?: positionMs
                            ),
                            color = MusicPearl.copy(alpha = 0.72f),
                            fontSize = 11.sp
                        )

                        Text(
                            text = formatMusicTime(durationMs),
                            color = MusicPearl.copy(alpha = 0.72f),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(
                        Modifier.height(if (compact) 8.dp else 18.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MusicAction(
                            label = "اختيار ملف",
                            modifier = Modifier.weight(1f),
                            compact = compact
                        ) {
                            audioPicker.launch(arrayOf("audio/*"))
                        }

                        MusicAction(
                            label = if (isPlaying) {
                                "إيقاف مؤقت"
                            } else {
                                "تشغيل"
                            },
                            modifier = Modifier.weight(1f),
                            enabled = selectedUri != null,
                            compact = compact,
                            primary = true
                        ) {
                            val activePlayer = player

                            if (activePlayer != null) {
                                try {
                                    if (activePlayer.isPlaying) {
                                        activePlayer.pause()
                                        isPlaying = false
                                    } else {
                                        if (
                                            durationMs > 0 &&
                                            positionMs >= durationMs
                                        ) {
                                            activePlayer.seekTo(0)
                                            positionMs = 0
                                        }

                                        activePlayer.start()
                                        isPlaying = true
                                    }
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        context,
                                        "تعذّر التحكم بالتشغيل",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }

                        MusicAction(
                            label = "إيقاف",
                            modifier = Modifier.weight(0.8f),
                            enabled = selectedUri != null,
                            compact = compact
                        ) {
                            try {
                                player?.pause()
                                player?.seekTo(0)
                                positionMs = 0
                                isPlaying = false
                            } catch (_: Exception) {
                            }
                        }
                    }

                    Spacer(
                        Modifier.height(if (compact) 8.dp else 14.dp)
                    )

                    Text(
                        text = "تشغيل الملفات المحفوظة على الجهاز متاح دون إنترنت",
                        color = MusicPearl.copy(alpha = 0.65f),
                        fontSize = if (compact) 10.sp else 12.sp
                    )
                }
            }

            Spacer(
                Modifier.height(if (compact) 8.dp else 14.dp)
            )

            // هوية داخلية مؤقتة للمشغّل
            
Row(
    modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(Color(0xFF252B35))
        .border(
            1.dp,
            MusicGold.copy(alpha = 0.75f),
            RoundedCornerShape(10.dp)
        )
        .padding(horizontal = 8.dp, vertical = 7.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    Text(
        text = "الملكي",
        color = MusicGold,
        fontSize = if (compact) 13.sp else 17.sp,
        fontWeight = FontWeight.ExtraBold
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF080C12))
            .padding(horizontal = 7.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = tickerText.ifBlank { "أهلاً بكم في ROYAL TV" },
            color = MusicPearl,
            fontSize = if (compact) 9.sp else 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = currentTime,
            color = MusicGold,
            fontSize = if (compact) 10.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = currentDate,
            color = MusicPearl,
            fontSize = if (compact) 7.sp else 9.sp,
            maxLines = 1
        )
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF401820))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(
                    Color(0xFFFF4545).copy(alpha = liveAlpha)
                )
        )
        Text(
            text = "مباشر",
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
private fun MusicAction(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)

    Text(
        text = label,
        modifier = modifier
            .clip(shape)
            .background(
                when {
                    primary && enabled -> MusicGold
                    enabled -> MusicBlue.copy(alpha = 0.88f)
                    else -> Color.White.copy(alpha = 0.05f)
                }
            )
            .border(
                width = 1.dp,
                color = if (enabled) {
                    MusicGold.copy(alpha = 0.9f)
                } else {
                    MusicGold.copy(alpha = 0.25f)
                },
                shape = shape
            )
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(
                horizontal = 8.dp,
                vertical = if (compact) 10.dp else 14.dp
            ),
        color = when {
            !enabled -> MusicPearl.copy(alpha = 0.35f)
            primary -> MusicPanel
            else -> MusicPearl
        },
        fontSize = if (compact) 11.sp else 13.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private fun formatMusicTime(milliseconds: Int): String {
    val totalSeconds = milliseconds.coerceAtLeast(0) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
}

                    
