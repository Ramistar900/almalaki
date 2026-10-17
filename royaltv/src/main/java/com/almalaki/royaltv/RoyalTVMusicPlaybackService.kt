package com.almalaki.royaltv

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
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

        const val EXTRA_URIS = "music_uris"
        const val EXTRA_TITLES = "music_titles"
        const val EXTRA_ARTISTS = "music_artists"
        const val EXTRA_INDEX = "music_index"

        private const val CHANNEL_ID = "royal_music_playback"
        private const val NOTIFICATION_ID = 7412
    }

    private var player: MediaPlayer? = null
    private var uris = arrayListOf<String>()
    private var titles = arrayListOf<String>()
    private var artists = arrayListOf<String>()
    private var currentIndex = 0

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

                currentIndex = intent.getIntExtra(EXTRA_INDEX, 0)
                    .coerceIn(0, (uris.size - 1).coerceAtLeast(0))

                startForeground(
                    NOTIFICATION_ID,
                    buildNotification()
                )
                playCurrent()
            }

            ACTION_TOGGLE -> togglePlayback()
            ACTION_NEXT -> moveTrack(1)
            ACTION_PREVIOUS -> moveTrack(-1)

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
                updateNotification()
                return
            }

            player = created

            created.setOnCompletionListener {
                if (currentIndex < uris.lastIndex) {
                    currentIndex++
                    playCurrent()
                } else {
                    updateNotification()
                }
            }

            created.start()
            updateNotification()
        } catch (_: Exception) {
            releasePlayer()
            updateNotification()
        }
    }

    private fun togglePlayback() {
        try {
            val active = player

            if (active == null) {
                playCurrent()
            } else if (active.isPlaying) {
                active.pause()
            } else {
                active.start()
            }
        } catch (_: Exception) {
            playCurrent()
        }

        updateNotification()
    }

    private fun moveTrack(direction: Int) {
        if (uris.isEmpty()) return

        currentIndex =
            (currentIndex + direction + uris.size) % uris.size

        playCurrent()
    }

    private fun releasePlayer() {
        try {
            player?.setOnCompletionListener(null)
            player?.release()
        } catch (_: Exception) {
        }

        player = null
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

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val title =
            titles.getOrNull(currentIndex) ?: "ROYAL MUSIC"
        val artist =
            artists.getOrNull(currentIndex)
                ?: "الموسيقى تعمل في الخلفية"

        fun pendingAction(
            action: String,
            requestCode: Int
        ): PendingIntent {
            val intent = Intent(
                this,
                RoyalTVMusicPlaybackService::class.java
            ).setAction(action)

            return PendingIntent.getService(
                this,
                requestCode,
                intent,
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
                if (playing) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play,
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
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
