package com.almalaki.royaltv

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class RoyalTVMusicPlaybackService : Service() {

    companion object {
        const val ACTION_START_PLAYLIST =
            "com.almalaki.royaltv.music.START_PLAYLIST"
        const val ACTION_TOGGLE =
            "com.almalaki.royaltv.music.TOGGLE"
        const val ACTION_NEXT =
            "com.almalaki.royaltv.music.NEXT"
        const val ACTION_PREVIOUS =
            "com.almalaki.royaltv.music.PREVIOUS"
        const val ACTION_STOP =
            "com.almalaki.royaltv.music.STOP"
        const val ACTION_SET_REPEAT =
            "com.almalaki.royaltv.music.SET_REPEAT"
        const val ACTION_SET_REPEAT_MODE =
            "com.almalaki.royaltv.music.SET_REPEAT_MODE"
        const val ACTION_SEEK =
            "com.almalaki.royaltv.music.SEEK"
        const val ACTION_STATE =
            "com.almalaki.royaltv.music.STATE"
        const val ACTION_SPECTRUM =
            "com.almalaki.royaltv.music.SPECTRUM"
        const val EXTRA_SPECTRUM = "music_spectrum"

        const val EXTRA_URIS = "music_uris"
        const val EXTRA_TITLES = "music_titles"
        const val EXTRA_ARTISTS = "music_artists"
        const val EXTRA_INDEX = "music_index"
        const val EXTRA_REPEAT_ONE = "music_repeat_one"
        const val EXTRA_REPEAT_MODE = "music_repeat_mode"
        const val EXTRA_SEEK_POSITION = "music_seek_position"
        const val EXTRA_STATE_INDEX = "music_state_index"
        const val EXTRA_STATE_POSITION = "music_state_position"
        const val EXTRA_STATE_DURATION = "music_state_duration"
        const val EXTRA_STATE_PLAYING = "music_state_playing"
        const val EXTRA_STATE_REPEAT_MODE = "music_state_repeat_mode"

        private const val CHANNEL_ID = "royal_music_playback"
        private const val NOTIFICATION_ID = 7412
    }

    private var player: MediaPlayer? = null
    private var visualizer: Visualizer? = null
    private var uris = arrayListOf<String>()
    private var titles = arrayListOf<String>()
    private var artists = arrayListOf<String>()
    private var currentIndex = 0

    // 0 = بدون تكرار، 1 = تكرار الأغنية، 2 = تكرار القائمة
    private var repeatMode = 0

    private val handler = Handler(Looper.getMainLooper())

    private val stateTicker = object : Runnable {
        override fun run() {
            broadcastState()
            if (player != null) {
                handler.postDelayed(this, 500L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            ACTION_START_PLAYLIST -> {
                uris = intent.getStringArrayListExtra(EXTRA_URIS)
                    ?: arrayListOf()
                titles = intent.getStringArrayListExtra(EXTRA_TITLES)
                    ?: arrayListOf()
                artists = intent.getStringArrayListExtra(EXTRA_ARTISTS)
                    ?: arrayListOf()

                if (uris.isEmpty()) {
                    releasePlayer()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }

                currentIndex = intent.getIntExtra(EXTRA_INDEX, 0)
                    .coerceIn(0, uris.lastIndex)

                startForeground(NOTIFICATION_ID, buildNotification())
                playCurrent()
            }

            ACTION_TOGGLE -> togglePlayback()
            ACTION_NEXT -> moveTrack(1)
            ACTION_PREVIOUS -> moveTrack(-1)

            ACTION_SET_REPEAT -> {
                val enabled = intent.getBooleanExtra(
                    EXTRA_REPEAT_ONE,
                    false
                )
                repeatMode = if (enabled) 1 else 0
                applyRepeatMode()
            }

            ACTION_SET_REPEAT_MODE -> {
                repeatMode = intent.getIntExtra(
                    EXTRA_REPEAT_MODE,
                    0
                ).coerceIn(0, 2)
                applyRepeatMode()
            }

            ACTION_SEEK -> {
                val target = intent.getIntExtra(
                    EXTRA_SEEK_POSITION,
                    0
                ).coerceAtLeast(0)

                try {
                    val active = player
                    if (active != null) {
                        active.seekTo(
                            target.coerceAtMost(
                                active.duration.coerceAtLeast(0)
                            )
                        )
                    }
                } catch (_: Exception) {
                }
                broadcastState()
            }

            ACTION_STOP -> {
                releasePlayer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun playCurrent() {
        if (uris.isEmpty() || currentIndex !in uris.indices) {
            broadcastState()
            updateNotification()
            return
        }

        releasePlayer()

        try {
            val created = MediaPlayer.create(
                this,
                Uri.parse(uris[currentIndex])
            )

            if (created == null) {
                broadcastState()
                updateNotification()
                return
            }

            player = created
            created.isLooping = repeatMode == 1

            created.setOnCompletionListener {
                when {
                    repeatMode == 1 -> {
                        // التكرار الفردي يتولاه MediaPlayer.
                        broadcastState()
                    }

                    currentIndex < uris.lastIndex -> {
                        currentIndex++
                        playCurrent()
                    }

                    repeatMode == 2 -> {
                        currentIndex = 0
                        playCurrent()
                    }

                    else -> {
                        broadcastState()
                        updateNotification()
                    }
                }
            }

            created.start()
            startVisualizer(created.audioSessionId)
            scheduleStateBroadcast()
            broadcastState()
            updateNotification()
        } catch (_: Exception) {
            releasePlayer()
            broadcastState()
            updateNotification()
        }
    }

    private fun togglePlayback() {
        try {
            val active = player
            if (active == null) {
                if (uris.isNotEmpty()) playCurrent()
                return
            }

            if (active.isPlaying) {
                active.pause()
            } else {
                if (
                    active.duration > 0 &&
                    active.currentPosition >= active.duration
                ) {
                    active.seekTo(0)
                }
                active.start()
                scheduleStateBroadcast()
            }
        } catch (_: Exception) {
            if (uris.isNotEmpty()) playCurrent()
        }

        broadcastState()
        updateNotification()
    }

    private fun moveTrack(direction: Int) {
        if (uris.isEmpty()) return

        currentIndex = (
            currentIndex + direction + uris.size
        ) % uris.size

        playCurrent()
    }

    private fun applyRepeatMode() {
        try {
            player?.isLooping = repeatMode == 1
        } catch (_: Exception) {
        }
        broadcastState()
        updateNotification()
    }

    private fun scheduleStateBroadcast() {
        handler.removeCallbacks(stateTicker)
        handler.post(stateTicker)
    }

    private fun broadcastState() {
        val active = player
        val stateIntent = Intent(ACTION_STATE).apply {
            setPackage(packageName)
            putExtra(EXTRA_STATE_INDEX, currentIndex)
            putExtra(
                EXTRA_STATE_POSITION,
                try {
                    active?.currentPosition ?: 0
                } catch (_: Exception) {
                    0
                }
            )
            putExtra(
                EXTRA_STATE_DURATION,
                try {
                    active?.duration ?: 0
                } catch (_: Exception) {
                    0
                }
            )
            putExtra(
                EXTRA_STATE_PLAYING,
                try {
                    active?.isPlaying == true
                } catch (_: Exception) {
                    false
                }
            )
            putExtra(EXTRA_STATE_REPEAT_MODE, repeatMode)
        }
        sendBroadcast(stateIntent)
    }

    
    private fun startVisualizer(audioSessionId: Int) {
        stopVisualizer()

        if (
            checkSelfPermission(
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            val effect = Visualizer(audioSessionId)
            effect.captureSize = Visualizer.getCaptureSizeRange()[1]

            effect.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) = Unit

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        if (fft == null) return

                        val intent = Intent(ACTION_SPECTRUM).apply {
                            setPackage(packageName)
                            putExtra(EXTRA_SPECTRUM, fft)
                        }

                        sendBroadcast(intent)
                    }
                },
                (Visualizer.getMaxCaptureRate() / 2).coerceAtLeast(1),
                false,
                true
            )

            visualizer = effect
            effect.enabled = true
        } catch (_: Exception) {
            stopVisualizer()
        }
    }

    private fun stopVisualizer() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {
        }

        visualizer = null
    }

    private fun releasePlayer() {
        stopVisualizer()
        handler.removeCallbacks(stateTicker)

        try {
            player?.setOnCompletionListener(null)
            player?.release()
        } catch (_: Exception) {
        }

        player = null
        broadcastState()
    }
    


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ROYAL TV Music",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "تشغيل الموسيقى في الخلفية"
                setShowBadge(false)
            }

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val title = titles.getOrNull(currentIndex) ?: "ROYAL MUSIC"
        val artist = artists.getOrNull(currentIndex)
            ?: "الموسيقى تعمل في الخلفية"

        fun pendingAction(
            action: String,
            requestCode: Int
        ): PendingIntent {
            val serviceIntent = Intent(
                this,
                RoyalTVMusicPlaybackService::class.java
            ).setAction(action)

            return PendingIntent.getService(
                this,
                requestCode,
                serviceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )
        }

        val playing = try {
            player?.isPlaying == true
        } catch (_: Exception) {
            false
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(artist)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(playing)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_media_previous,
                "السابق",
                pendingAction(ACTION_PREVIOUS, 7414)
            )
            .addAction(
                if (playing) {
                    android.R.drawable.ic_media_pause
                } else {
                    android.R.drawable.ic_media_play
                },
                if (playing) "إيقاف مؤقت" else "تشغيل",
                pendingAction(ACTION_TOGGLE, 7413)
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "التالي",
                pendingAction(ACTION_NEXT, 7415)
            )
            .build()
    }

    private fun updateNotification() {
        try {
            getSystemService(
                NotificationManager::class.java
            ).notify(NOTIFICATION_ID, buildNotification())
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(stateTicker)
        releasePlayer()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
