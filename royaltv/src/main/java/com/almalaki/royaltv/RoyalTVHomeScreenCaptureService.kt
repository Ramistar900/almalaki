package com.almalaki.royaltv

import android.app.Activity
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
import android.view.Surface
import android.view.WindowManager

/**
 * ROYAL TV — Home Screen Capture Service
 *
 * يلتقط الشاشة ويرسل الإطارات إلى مشفر H.264.
 * النقل إلى التلفزيون غير مفعّل في هذه المرحلة.
 */
class RoyalTVHomeScreenCaptureService : Service() {

    companion object {
        const val ACTION_START =
            "com.almalaki.royaltv.action.START_CAPTURE"

        const val ACTION_STOP =
            "com.almalaki.royaltv.action.STOP_CAPTURE"

        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"

        private const val TAG = "RoyalTVCapture"
        private const val CHANNEL_ID = "royal_tv_capture"
        private const val NOTIFICATION_ID = 7301
    }

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var videoEncoder: RoyalTVVideoEncoder? = null
    private var projectionCallback: MediaProjection.Callback? = null

    @Volatile
    private var isStopping = false

    @Volatile
    private var encodedFrameCount = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopCapture()
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                if (projection != null) {
                    Log.i(TAG, "Capture is already active")
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

                if (
                    resultCode != Activity.RESULT_OK ||
                    resultData == null
                ) {
                    Log.e(
                        TAG,
                        "Screen capture permission data missing or invalid"
                    )
                    stopSelf()
                    return START_NOT_STICKY
                }

                try {
                    startForegroundCompat()
                    startCapture(resultCode, resultData)
                } catch (error: Exception) {
                    Log.e(
                        TAG,
                        "Unable to start H.264 screen capture",
                        error
                    )
                    stopCapture()
                    stopForegroundCompat()
                    stopSelf()
                }
            }

            else -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        createNotificationChannel()

        val notification = Notification.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle("ROYAL TV")
            .setContentText("جلسة التقاط الشاشة نشطة")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
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
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "ROYAL TV Screen Capture",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "إشعار جلسة التقاط الشاشة"
        }

        val manager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        manager.createNotificationChannel(channel)
    }

    private fun startCapture(
        resultCode: Int,
        resultData: Intent
    ) {
        check(projection == null) {
            "A screen projection is already active"
        }

        isStopping = false
        encodedFrameCount = 0

        val manager = getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager

        val mediaProjection = manager.getMediaProjection(
            resultCode,
            resultData
        ) ?: error("Android did not provide MediaProjection")

        projection = mediaProjection

        /*
         * اجعل أبعاد الفيديو أعدادًا زوجية،
         * لأن مشفر H.264 يحتاج أبعادًا مناسبة.
         */
        val metrics = getScreenMetrics()
        val width = evenDimension(metrics.widthPixels)
        val height = evenDimension(metrics.heightPixels)
        val density = metrics.densityDpi.coerceAtLeast(1)

        /*
         * سجّل callback قبل إنشاء VirtualDisplay
         * للتوافق مع متطلبات MediaProjection الحديثة.
         */
        val callback = object : MediaProjection.Callback() {
            override fun onStop() {
                Log.i(TAG, "Android stopped screen projection")

                Handler(Looper.getMainLooper()).post {
                    stopCapture(stopProjection = false)
                    stopForegroundCompat()
                    stopSelf()
                }
            }
        }

        projectionCallback = callback
        mediaProjection.registerCallback(callback, null)

        /*
         * جهّز مشفر H.264 وأنشئ سطح الإدخال.
         */
        val encoder = RoyalTVVideoEncoder(
            width = width,
            height = height,
            bitRate = 2_000_000,
            frameRate = 20,
            iFrameIntervalSeconds = 2,
            listener = object : RoyalTVVideoEncoder.Listener {

                override fun onOutputFormatChanged(
                    format: MediaFormat
                ) {
                    Log.i(
                        TAG,
                        "H.264 output format ready: $format"
                    )
                }

                override fun onEncodedFrame(
                    data: ByteArray,
                    presentationTimeUs: Long,
                    flags: Int
                ) {
                    encodedFrameCount++

                    if (encodedFrameCount == 1) {
                        Log.i(
                            TAG,
                            "First H.264 packet produced: " +
                                "${data.size} bytes"
                        )
                    }

                    /*
                     * النقل الشبكي غير مربوط بعد.
                     * البيانات المشفرة تُستقبل هنا فقط.
                     */
                }

                override fun onEncoderError(error: Exception) {
                    Log.e(TAG, "H.264 encoder error", error)

                    Handler(Looper.getMainLooper()).post {
                        if (!isStopping) {
                            stopCapture()
                            stopForegroundCompat()
                            stopSelf()
                        }
                    }
                }
            }
        )

        videoEncoder = encoder

        val inputSurface: Surface = encoder.prepare()

        /*
         * شغّل المشفر قبل توجيه الشاشة إلى سطحه.
         */
        encoder.start()

        /*
         * بدل ImageReader، أصبحت الشاشة تدخل مباشرة
         * إلى سطح مشفر H.264.
         */
        val display = mediaProjection.createVirtualDisplay(
            "ROYAL-TV-Home-Capture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            inputSurface,
            null,
            null
        ) ?: error("Could not create screen virtual display")

        virtualDisplay = display

        Log.i(
            TAG,
            "H.264 screen capture initialized: ${width}x${height}"
        )
    }

    private fun evenDimension(value: Int): Int {
        val safeValue = value.coerceAtLeast(2)
        return safeValue - (safeValue % 2)
    }

    private fun getScreenMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()

        val windowManager = getSystemService(
            Context.WINDOW_SERVICE
        ) as WindowManager

        if (Build.VERSION.SDK_INT >= 30) {
            val bounds = windowManager
                .currentWindowMetrics
                .bounds

            metrics.widthPixels = bounds.width()
            metrics.heightPixels = bounds.height()
            metrics.densityDpi =
                resources.displayMetrics.densityDpi
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
        }

        return metrics
    }

    private fun stopCapture(
        stopProjection: Boolean = true
    ) {
        if (isStopping) return
        isStopping = true

        val activeProjection = projection
        val callback = projectionCallback
        val activeEncoder = videoEncoder
        val activeDisplay = virtualDisplay

        /*
         * أزل المراجع أولًا لمنع التنظيف المتكرر.
         */
        projection = null
        projectionCallback = null
        videoEncoder = null
        virtualDisplay = null

        /*
         * أوقف الشاشة الافتراضية قبل تحرير سطح المشفر.
         */
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
            if (activeProjection != null && callback != null) {
                activeProjection.unregisterCallback(callback)
            }
        } catch (error: Exception) {
            Log.w(TAG, "Projection callback cleanup failed", error)
        }

        if (stopProjection) {
            try {
                activeProjection?.stop()
            } catch (error: Exception) {
                Log.w(TAG, "Projection stop failed", error)
            }
        }

        Log.i(
            TAG,
            "Capture stopped; encoded packets: $encodedFrameCount"
        )
    }

    override fun onDestroy() {
        stopCapture()
        stopForegroundCompat()
        super.onDestroy()
    }
}
