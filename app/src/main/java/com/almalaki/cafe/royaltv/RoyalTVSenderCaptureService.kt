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
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

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

        private const val TAG = "RoyalTVSender"
        private const val CHANNEL_ID = "royal_tv_sender"
        private const val NOTIFICATION_ID = 39871
        private const val DEFAULT_VIDEO_PORT = 39872

        // يجب أن تتطابق القيم مع مستقبل ROYAL TV.
        private const val STREAM_CONFIG_FLAG = Int.MIN_VALUE
        private const val CONFIG_MAGIC = 0x52545643
        private const val MAX_CONFIG_BYTES = 1024 * 1024
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var encoder: RoyalTVVideoEncoder? = null
    private var transport: RoyalTVVideoPacketTransport? = null

    @Volatile
    private var stopping = false

    @Volatile
    private var codecConfigPacket: ByteArray? = null

    @Volatile
    private var codecConfigSent = false

    private val codecConfigLock = Any()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val reconnectScheduled = AtomicBoolean(false)

    private val projectionCallback =
        object : MediaProjection.Callback() {
            override fun onStop() {
                stopStreaming()
                stopForegroundCompat()
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
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                if (projection != null) {
                    Log.i(TAG, "Video capture is already active")
                    return START_NOT_STICKY
                }

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
                        intent.getParcelableExtra(
                            EXTRA_RESULT_DATA
                        )
                    }

                val host = intent.getStringExtra(
                    EXTRA_RECEIVER_HOST
                )?.trim().orEmpty()

                val port = intent.getIntExtra(
                    EXTRA_RECEIVER_PORT,
                    DEFAULT_VIDEO_PORT
                )

                if (
                    resultCode != android.app.Activity.RESULT_OK ||
                    resultData == null ||
                    host.isBlank() ||
                    port !in 1..65535
                ) {
                    Log.e(
                        TAG,
                        "Invalid screen permission or receiver address"
                    )
                    stopForegroundCompat()
                    stopSelf()
                    return START_NOT_STICKY
                }

                try {
                    startForegroundNotification()
                    startStreaming(
                        resultCode,
                        resultData,
                        host,
                        port
                    )
                } catch (error: Exception) {
                    Log.e(
                        TAG,
                        "Failed to start video sender",
                        error
                    )
                    stopStreaming()
                    stopForegroundCompat()
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

        val builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(this)
            }

        val notification = builder
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
            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun startStreaming(
        resultCode: Int,
        resultData: Intent,
        host: String,
        port: Int
    ) {
        check(projection == null) {
            "A screen projection is already active"
        }

        stopping = false

        synchronized(codecConfigLock) {
            codecConfigPacket = null
            codecConfigSent = false
        }

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

        val metrics = getScreenMetrics()

        val width = evenDimension(metrics.widthPixels)
        val height = evenDimension(metrics.heightPixels)
        val densityDpi = metrics.densityDpi.coerceAtLeast(1)

        val videoTransport = RoyalTVVideoPacketTransport(
            object : RoyalTVVideoPacketTransport.Listener {

                override fun onConnected() {
                    Log.i(
                        TAG,
                        "Video transport connected"
                    )

                    // إذا كانت التهيئة جاهزة، أرسلها فور الاتصال.
                    sendCodecConfigIfReady(transport)
                }

                override fun onDisconnected(reason: String) {
                    synchronized(codecConfigLock) {
                        codecConfigSent = false
                    }

                    Log.w(
                        TAG,
                        "Video disconnected: $reason"
                    )
                    transport?.let { active -> scheduleVideoReconnect(active, host, port) }
                }

                override fun onVideoPacket(
                    data: ByteArray,
                    presentationTimeUs: Long,
                    flags: Int
                ) {
                    // هذا الجهاز يرسل الفيديو ولا يستقبله.
                }

                override fun onError(
                    operation: String,
                    reason: String
                ) {
                    Log.e(
                        TAG,
                        "$operation: $reason"
                    )
                    if (operation == "connect") {
                        scheduleVideoReconnect(videoTransport, host, port)
                    }
                }
            }
        )

        transport = videoTransport

        val videoEncoder = RoyalTVVideoEncoder(
            width = width,
            height = height,
            bitRate = 2_000_000,
            frameRate = 20,
            iFrameIntervalSeconds = 2,
            listener = object : RoyalTVVideoEncoder.Listener {

                override fun onOutputFormatChanged(
                    format: MediaFormat
                ) {
                    try {
                        val actualWidth =
                            if (format.containsKey(MediaFormat.KEY_WIDTH)) {
                                format.getInteger(MediaFormat.KEY_WIDTH)
                            } else {
                                width
                            }

                        val actualHeight =
                            if (format.containsKey(MediaFormat.KEY_HEIGHT)) {
                                format.getInteger(MediaFormat.KEY_HEIGHT)
                            } else {
                                height
                            }

                        require(
                            actualWidth >= 2 &&
                                actualHeight >= 2
                        ) {
                            "Invalid H.264 output dimensions"
                        }

                        val csd0 = format.getByteBuffer("csd-0")
                            ?: error("H.264 csd-0 is missing")

                        val csd1 = format.getByteBuffer("csd-1")
                            ?: error("H.264 csd-1 is missing")

                        val csd0Bytes = copyBuffer(csd0)
                        val csd1Bytes = copyBuffer(csd1)

                        require(
                            csd0Bytes.isNotEmpty() &&
                                csd1Bytes.isNotEmpty()
                        ) {
                            "H.264 codec configuration is empty"
                        }

                        require(
                            csd0Bytes.size <= MAX_CONFIG_BYTES &&
                                csd1Bytes.size <= MAX_CONFIG_BYTES
                        ) {
                            "H.264 codec configuration is too large"
                        }

                        val configPacket =
                            ByteArrayOutputStream().use { bytes ->
                                DataOutputStream(bytes).use { output ->
                                    output.writeInt(CONFIG_MAGIC)
                                    output.writeInt(actualWidth)
                                    output.writeInt(actualHeight)

                                    output.writeInt(csd0Bytes.size)
                                    output.write(csd0Bytes)

                                    output.writeInt(csd1Bytes.size)
                                    output.write(csd1Bytes)
                                }

                                bytes.toByteArray()
                            }

                        require(
                            configPacket.size <=
                                RoyalTVVideoPacketTransport.MAX_PACKET_BYTES
                        ) {
                            "H.264 configuration packet is too large"
                        }

                        synchronized(codecConfigLock) {
                            codecConfigPacket = configPacket
                            codecConfigSent = false
                        }

                        Log.i(
                            TAG,
                            "H.264 config prepared: " +
                                "${actualWidth}x${actualHeight}, " +
                                "csd-0=${csd0Bytes.size}, " +
                                "csd-1=${csd1Bytes.size}"
                        )

                        sendCodecConfigIfReady(transport)

                    } catch (error: Exception) {
                        Log.e(
                            TAG,
                            "Failed to prepare H.264 configuration",
                            error
                        )
                    }
                }

                override fun onEncodedFrame(
                    data: ByteArray,
                    presentationTimeUs: Long,
                    flags: Int
                ) {
                    if (stopping) return

                    val activeTransport = transport ?: return

                    // لا ترسل إطارات قبل نجاح إرسال التهيئة.
                    if (!sendCodecConfigIfReady(activeTransport)) {
                        return
                    }

                    val sent = activeTransport.sendVideoPacket(
                        data,
                        presentationTimeUs,
                        flags
                    )

                    if (!sent) {
                        Log.w(
                            TAG,
                            "Video frame not sent; connection may not be ready"
                        )
                    }
                }

                override fun onEncoderError(error: Exception) {
                    Log.e(
                        TAG,
                        "H.264 encoder error",
                        error
                    )

                    mainHandler.post {
                        if (!stopping) {
                            stopStreaming()
                            stopForegroundCompat()
                            stopSelf()
                        }
                    }
                }
            }
        )

        encoder = videoEncoder

        val inputSurface = videoEncoder.prepare()

        // ابدأ المشفر قبل توجيه الشاشة إلى سطح الإدخال.
        videoEncoder.start()

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

        // ابدأ الاتصال بعد تجهيز مكونات الالتقاط.
        videoTransport.connect(host, port)

        Log.i(
            TAG,
            "Capture started: ${width}x$height -> $host:$port"
        )
    }

    /**
     * يعيد محاولة اتصال المرسل بمستقبل ROYAL TV بعد انقطاع الشبكة.
     * لا ينشئ ناقلًا جديدًا ولا يغيّر بروتوكول الحزم، ويتوقف عند إيقاف البث.
     */
    private fun scheduleVideoReconnect(
        activeTransport: RoyalTVVideoPacketTransport,
        host: String,
        port: Int
    ) {
        if (
            stopping ||
            transport !== activeTransport ||
            !reconnectScheduled.compareAndSet(false, true)
        ) {
            return
        }

        mainHandler.postDelayed({
            reconnectScheduled.set(false)

            if (!stopping && transport === activeTransport) {
                Log.i(TAG, "Retrying video connection to $host:$port")
                activeTransport.connect(host, port)
            }
        }, 2000L)
    }

    private fun copyBuffer(source: ByteBuffer): ByteArray {
        val buffer = source.duplicate()
        return ByteArray(buffer.remaining()).also {
            buffer.get(it)
        }
    }

    /**
     * يرسل حزمة التهيئة مرة واحدة لكل اتصال ناجح.
     * تعاد المحاولة إذا لم يكن الاتصال جاهزًا.
     */
    private fun sendCodecConfigIfReady(
        activeTransport: RoyalTVVideoPacketTransport?
    ): Boolean {
        if (stopping || activeTransport == null) {
            return false
        }

        synchronized(codecConfigLock) {
            if (codecConfigSent) {
                return true
            }

            val packet = codecConfigPacket ?: return false

            val sent = activeTransport.sendVideoPacket(
                packet,
                0L,
                STREAM_CONFIG_FLAG
            )

            if (sent) {
                codecConfigSent = true

                Log.i(
                    TAG,
                    "H.264 configuration sent successfully"
                )
            }

            return sent
        }
    }

    private fun evenDimension(value: Int): Int {
        val safeValue = value.coerceAtLeast(2)
        return safeValue - (safeValue % 2)
    }

    private fun getScreenMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = getSystemService(
                android.view.WindowManager::class.java
            ).currentWindowMetrics.bounds

            metrics.widthPixels = bounds.width()
            metrics.heightPixels = bounds.height()
            metrics.densityDpi = resources.displayMetrics.densityDpi
        } else {
            @Suppress("DEPRECATION")
            val windowManager =
                getSystemService(Context.WINDOW_SERVICE)
                    as android.view.WindowManager

            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
        }

        return metrics
    }

    @Synchronized
    private fun stopStreaming() {
        if (stopping) return
        stopping = true

        val activeDisplay = virtualDisplay
        val activeEncoder = encoder
        val activeTransport = transport
        val activeProjection = projection

        // أزل المراجع أولًا لمنع التنظيف المتكرر.
        virtualDisplay = null
        encoder = null
        transport = null
        projection = null

        synchronized(codecConfigLock) {
            codecConfigPacket = null
            codecConfigSent = false
        }

        try {
            activeDisplay?.release()
        } catch (error: Exception) {
            Log.w(TAG, "Virtual display release failed", error)
        }

        try {
            activeEncoder?.stop()
        } catch (error: Exception) {
            Log.w(TAG, "H.264 encoder cleanup failed", error)
        }

        try {
            activeTransport?.close()
        } catch (error: Exception) {
            Log.w(TAG, "Video transport cleanup failed", error)
        }

        if (activeProjection != null) {
            try {
                activeProjection.unregisterCallback(
                    projectionCallback
                )
            } catch (error: Exception) {
                Log.w(TAG, "Projection callback cleanup failed", error)
            }

            try {
                activeProjection.stop()
            } catch (error: Exception) {
                Log.w(TAG, "Projection stop failed", error)
            }
        }

        Log.i(TAG, "Video streaming stopped")
    }

    override fun onDestroy() {
        stopStreaming()
        stopForegroundCompat()
        super.onDestroy()
    }
}
