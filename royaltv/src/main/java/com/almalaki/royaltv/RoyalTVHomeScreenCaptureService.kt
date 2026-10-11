package com.almalaki.royaltv

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager

/**
 * ROYAL TV — Home Screen Capture Service
 *
 * يلتقط إطارات الشاشة بعد موافقة Android الرسمية.
 *
 * هذه المرحلة مسؤولة عن الالتقاط والتنظيف الآمن فقط.
 * لا ترمّز الفيديو ولا تنقله إلى جهاز آخر بعد.
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
    private var imageReader: ImageReader? = null
    private var projectionCallback: MediaProjection.Callback? = null

    private var isStopping = false

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
                        "Unable to start screen capture",
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
            description = "إشعار جلسة مشاركة الشاشة"
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

        val manager = getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager

        val mediaProjection = manager.getMediaProjection(
            resultCode,
            resultData
        ) ?: error("Android did not provide MediaProjection")

        projection = mediaProjection

        val metrics = getScreenMetrics()
        val width = metrics.widthPixels.coerceAtLeast(1)
        val height = metrics.heightPixels.coerceAtLeast(1)
        val density = metrics.densityDpi.coerceAtLeast(1)

        val reader = ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            3
        )

        imageReader = reader

        reader.setOnImageAvailableListener(
            { source ->
                try {
                    source.acquireLatestImage()?.use { frame ->
                        Log.d(
                            TAG,
                            "Captured frame: " +
                                "${frame.width}x${frame.height}"
                        )
                    }
                } catch (error: Exception) {
                    if (!isStopping) {
                        Log.e(
                            TAG,
                            "Frame processing error",
                            error
                        )
                    }
                }
            },
            null
        )

        val callback = object : MediaProjection.Callback() {
            override fun onStop() {
                Log.i(
                    TAG,
                    "Android stopped screen projection"
                )

                // Android أوقف الإسقاط بالفعل؛ ننظف الموارد
                // دون استدعاء projection.stop() مرة أخرى.
                stopCapture(stopProjection = false)
                stopForegroundCompat()
                stopSelf()
            }
        }

        projectionCallback = callback

        mediaProjection.registerCallback(
            callback,
            null
        )

        val display = mediaProjection.createVirtualDisplay(
            "ROYAL-TV-Home-Capture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            null
        ) ?: error("Could not create screen virtual display")

        virtualDisplay = display

        Log.i(
            TAG,
            "Screen capture initialized: ${width}x${height}"
        )
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

        // أزل المراجع أولاً لتقليل احتمال إعادة التنظيف.
        projection = null
        projectionCallback = null

        try {
            virtualDisplay?.release()
        } catch (error: Exception) {
            Log.w(TAG, "Virtual display release failed", error)
        } finally {
            virtualDisplay = null
        }

        try {
            imageReader?.setOnImageAvailableListener(null, null)
            imageReader?.close()
        } catch (error: Exception) {
            Log.w(TAG, "ImageReader cleanup failed", error)
        } finally {
            imageReader = null
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

        Log.i(TAG, "Screen capture stopped")
    }

    override fun onDestroy() {
        stopCapture()
        stopForegroundCompat()
        super.onDestroy()
    }
}
