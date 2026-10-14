package com.almalaki.royaltv

import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MusicGold = Color(0xFFD4AF37)
private val MusicPearl = Color(0xFFF0E9F5)
private val MusicBlue = Color(0xFF123B43)
private val MusicPanel = Color(0xFF17191F)

@Composable
fun RoyalTVMusicPlayer(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var selectedUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var selectedName by remember {
        mutableStateOf("لم يتم اختيار ملف صوتي")
    }

    var isPlaying by remember {
        mutableStateOf(false)
    }

    var player by remember {
        mutableStateOf<MediaPlayer?>(null)
    }

    val audioPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                try {
                    player?.release()
                    player = null

                    val newPlayer =
                        MediaPlayer.create(context, uri)

                    if (newPlayer == null) {
                        selectedUri = null
                        isPlaying = false

                        Toast.makeText(
                            context,
                            "تعذّر فتح هذا الملف الصوتي",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        selectedUri = uri

                        selectedName =
                            uri.lastPathSegment
                                ?.substringAfterLast('/')
                                ?.substringAfterLast(':')
                                ?.ifBlank { "ملف صوتي" }
                                ?: "ملف صوتي"

                        player = newPlayer

                        newPlayer.setOnCompletionListener {
                            isPlaying = false
                        }

                        newPlayer.start()
                        isPlaying = true
                    }
                } catch (_: Exception) {
                    player?.release()
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

    DisposableEffect(Unit) {
        onDispose {
            player?.run {
                try {
                    if (isPlaying) pause()
                } catch (_: Exception) {
                }

                try {
                    release()
                } catch (_: Exception) {
                }
            }

            player = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MusicPanel)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "♫  مكتبة الموسيقى",
            color = MusicGold,
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(18.dp))

        Text(
            text = selectedName,
            modifier = Modifier.fillMaxWidth(),
            color = MusicPearl,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp,
                    Alignment.CenterHorizontally
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MusicAction(
                label = "اختيار ملف",
                modifier = Modifier.weight(1f)
            ) {
                audioPicker.launch(arrayOf("audio/*"))
            }

            MusicAction(
                label = if (isPlaying) {
                    "إيقاف مؤقت"
                } else {
                    "تشغيل"
                },
                enabled = selectedUri != null,
                modifier = Modifier.weight(1f)
            ) {
                val activePlayer = player

                if (activePlayer != null) {
                    try {
                        if (activePlayer.isPlaying) {
                            activePlayer.pause()
                            isPlaying = false
                        } else {
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
                enabled = selectedUri != null,
                modifier = Modifier.weight(1f)
            ) {
                try {
                    player?.pause()
                    player?.seekTo(0)
                    isPlaying = false
                } catch (_: Exception) {
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text =
                "يمكن تشغيل الملفات الصوتية المحفوظة على الجهاز دون إنترنت.",
            color = MusicPearl.copy(alpha = 0.78f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(18.dp))

        MusicAction(
            label = "‹ العودة إلى مصادر البث",
            modifier = Modifier.fillMaxWidth()
        ) {
            onBack()
        }
    }
}

@Composable
private fun MusicAction(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Text(
        text = label,
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (enabled) {
                    MusicGold
                } else {
                    MusicGold.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(10.dp)
            )
            .background(
                color = MusicBlue.copy(
                    alpha = if (enabled) 1f else 0.45f
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(
                horizontal = 10.dp,
                vertical = 13.dp
            ),
        color = if (enabled) {
            MusicPearl
        } else {
            MusicPearl.copy(alpha = 0.4f)
        },
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}
