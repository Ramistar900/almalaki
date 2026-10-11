package com.almalaki.cafe.royaltv

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics

class RoyalTVSenderCaptureService : Service() {

    companion object {
        const val ACTION_START =
            "com.almalaki.cafe.royaltv.action.START_SENDER"

        const val ACTION_STOP =
            "com.almalaki.cafe.royaltv.action.STOP_SENDER"

        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_RECEIVER_HOST = "receiver_host"
        const val EXTRA_RECEIVER_PORT = "receiver_port"

        private const val CHANNEL_ID = "royal_tv_sender"
        private const val NOTIFICATION_ID = 39871
        private const val DEFAULT_VIDEO_PORT = 39872
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var encoder: RoyalTVVideoEncoder? = null
    private var transport: RoyalTVVideoPacketTransport? = null

    @Volatile
    private var stopping = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private val projectionCallback =
        object : MediaProjection.Callback() {
            override fun onStop() {
                stopStreaming()
                stopSelf()
            }
        }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopStreaming()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                startForegroundNotification()

                val resultCode = intent.getIntExtra(
                    EXTRA_RESULT_CODE,
                    0
                )

                val resultData: Intent? =
                    if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(
                            EXTRA_RESULT_DATA,
                            Intent::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(EXTRA_RESULT_DATA)
                    }

                val host = intent.getStringExtra(
                    EXTRA_RECEIVER_HOST
                )?.trim().orEmpty()

                val port = intent.getIntExtra(
                    EXTRA_RECEIVER_PORT,
                    DEFAULT_VIDEO_PORT
                )

                if (
                    resultData == null ||
                    resultCode == 0 ||
                    host.isBlank() ||
                    port !in 1..65535
                ) {
                    stopStreaming()
                    stopSelf()
                    return START_NOT_STICKY
                }

                try {
                    startStreaming(resultCode, resultData, host, port)
                } catch (error: Exception) {
                    android.util.Log.e(
                        "RoyalTVSender",
                        "Failed to start video sender",
                        error
                    )
                    stopStreaming()
                    stopSelf()
                }

                return START_NOT_STICKY
            }

            else -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ROYAL TV — بث الفيديو",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار إرسال فيديو ROYAL TV"
            }

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        ) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle("ROYAL TV")
            .setContentText("خدمة بث الفيديو قيد التشغيل")
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startStreaming(
        resultCode: Int,
        resultData: Intent,
        host: String,
        port: Int
    ) {
        stopping = false

        val manager = getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager

        val activeProjection = manager.getMediaProjection(
            resultCode,
            resultData
        ) ?: error("MediaProjection is unavailable")

        projection = activeProjection

        activeProjection.registerCallback(
            projectionCallback,
            mainHandler
        )

        val metrics: DisplayMetrics = resources.displayMetrics

        val width = (metrics.widthPixels / 2) * 2
        val height = (metrics.heightPixels / 2) * 2
        val densityDpi = metrics.densityDpi

        require(width >= 2 && height >= 2) {
            "Invalid screen dimensions"
        }

        val videoTransport = RoyalTVVideoPacketTransport(
            object : RoyalTVVideoPacketTransport.Listener {

                override fun onConnected() {
                    android.util.Log.i(
                        "RoyalTVSender",
                        "Video transport connected"
                    )
                }

                override fun onDisconnected(reason: String) {
                    android.util.Log.w(
                        "RoyalTVSender",
                        "Video disconnected: $reason"
                    )
                }

                override fun onVideoPacket(
                    data: ByteArray,
                    presentationTimeUs: Long,
                    flags: Int
                ) {
                    // This device sends video; it does not receive it.
                }

                override fun onError(
                    operation: String,
                    reason: String
                ) {
                    android.util.Log.e(
                        "RoyalTVSender",
                        "$operation: $reason"
                    )
                }
            }
        )

        transport = videoTransport
        videoTransport.connect(host, port)

        val videoEncoder = RoyalTVVideoEncoder(
            width = width,
            height = height,
            bitRate = 2_000_000,
            frameRate = 20,
            iFrameIntervalSeconds = 2,
            listener = object : RoyalTVVideoEncoder.Listener {

                override fun onOutputFormatChanged(
                    format: android.media.MediaFormat
                ) {
                    android.util.Log.i(
                        "RoyalTVSender",
                        "H.264 output format: $format"
                    )
                }

                override fun onEncodedFrame(
                    data: ByteArray,
                    presentationTimeUs: Long,
                    flags: Int
                ) {
                    val sent = videoTransport.sendVideoPacket(
                        data,
                        presentationTimeUs,
                        flags
                    )

                    if (!sent) {
                        android.util.Log.w(
                            "RoyalTVSender",
                            "Video frame not sent; connection may not be ready"
                        )
                    }
                }

                override fun onEncoderError(error: Exception) {
                    android.util.Log.e(
                        "RoyalTVSender",
                        "H.264 encoder error",
                        error
                    )
                }
            }
        )

        encoder = videoEncoder

        val inputSurface = videoEncoder.prepare()

        virtualDisplay = activeProjection.createVirtualDisplay(
            "ROYAL-TV-SENDER",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            inputSurface,
            null,
            mainHandler
        ) ?: error("Could not create capture display")

        videoEncoder.start()

        android.util.Log.i(
            "RoyalTVSender",
            "Capture started: ${width}x$height -> $host:$port"
        )
    }

    @Synchronized
    private fun stopStreaming() {
        if (stopping) return
        stopping = true

        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }
        virtualDisplay = null

        try {
            encoder?.stop()
        } catch (_: Exception) {
        }
        encoder = null

        try {
            transport?.close()
        } catch (_: Exception) {
        }
        transport = null

        val activeProjection = projection
        projection = null

        if (activeProjection != null) {
            try {
                activeProjection.unregisterCallback(
                    projectionCallback
                )
            } catch (_: Exception) {
            }

            try {
                activeProjection.stop()
            } catch (_: Exception) {
            }
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopStreaming()
        super.onDestroy()
    }
}
