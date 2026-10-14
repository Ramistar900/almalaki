package com.almalaki.royaltv

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlin.math.roundToInt

class RoyalTVBroadcastOverlayService :
    Service(),
    LifecycleOwner {

    companion object {
        private const val TAG = "RoyalTVOverlay"

        const val ACTION_START =
            "com.almalaki.royaltv.action.START_BROADCAST_OVERLAY"

        const val ACTION_STOP =
            "com.almalaki.royaltv.action.STOP_BROADCAST_OVERLAY"
    }

    private val serviceLifecycle = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = serviceLifecycle

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "تم إنشاء خدمة الهوية العائمة")

        serviceLifecycle.handleLifecycleEvent(
            Lifecycle.Event.ON_CREATE
        )

        windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        Log.i(TAG, "وصل أمر للخدمة: ${intent?.action}")

        when (intent?.action) {
            ACTION_STOP -> {
                stopOverlay()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START, null -> {
                startOverlay()
            }

            else -> {
                Log.w(TAG, "تم تجاهل أمر غير معروف")
            }
        }

        return START_NOT_STICKY
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            applicationContext,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun startOverlay() {
        if (overlayView != null) {
            Log.i(TAG, "نافذة الاختبار موجودة بالفعل")
            showMessage("ROYAL TV: نافذة الاختبار تعمل بالفعل")
            return
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)
        ) {
            Log.e(TAG, "صلاحية الظهور فوق التطبيقات غير ممنوحة")
            showMessage(
                "ROYAL TV: فعّل صلاحية الظهور فوق التطبيقات"
            )
            stopSelf()
            return
        }

        val manager = windowManager

        if (manager == null) {
            Log.e(TAG, "WindowManager غير متاح")
            showMessage("ROYAL TV: مدير النوافذ غير متاح")
            stopSelf()
            return
        }

        val metrics = resources.displayMetrics
        val density = metrics.density.coerceAtLeast(0.1f)
        val screenWidthDp = metrics.widthPixels / density

        val scale = (screenWidthDp / 360f)
            .coerceIn(0.85f, 2.6f)

        val overlayHeightPx =
            (64f * scale * density).roundToInt()

        // اختبار نافذة Android عادية، بدون Compose.
        val testView = TextView(this).apply {
            text = "ROYAL TV — اختبار النافذة العائمة"
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(
                (12f * density).roundToInt(),
                0,
                (12f * density).roundToInt(),
                0
            )
            setBackgroundColor(
                android.graphics.Color.MAGENTA
            )
            importantForAccessibility =
                View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        val windowType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayHeightPx,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            x = 0
            y = 0
        }

        try {
            manager.addView(testView, layoutParams)
            overlayView = testView

            serviceLifecycle.handleLifecycleEvent(
                Lifecycle.Event.ON_START
            )
            serviceLifecycle.handleLifecycleEvent(
                Lifecycle.Event.ON_RESUME
            )

            Log.i(
                TAG,
                "أضيفت نافذة Android التجريبية، الارتفاع=$overlayHeightPx px"
            )

            showMessage(
                "ROYAL TV: تمت إضافة نافذة الاختبار"
            )
        } catch (error: Exception) {
            Log.e(
                TAG,
                "فشل إنشاء نافذة Android التجريبية",
                error
            )

            overlayView = null

            showMessage(
                "ROYAL TV: فشل الاختبار — ${error.javaClass.simpleName}"
            )

            stopSelf()
        }
    }

    private fun stopOverlay() {
        if (
            serviceLifecycle.currentState
                .isAtLeast(Lifecycle.State.RESUMED)
        ) {
            serviceLifecycle.handleLifecycleEvent(
                Lifecycle.Event.ON_PAUSE
            )
        }

        if (
            serviceLifecycle.currentState
                .isAtLeast(Lifecycle.State.STARTED)
        ) {
            serviceLifecycle.handleLifecycleEvent(
                Lifecycle.Event.ON_STOP
            )
        }

        val view = overlayView
        overlayView = null

        if (view != null) {
            try {
                windowManager?.removeView(view)
                Log.i(TAG, "تمت إزالة نافذة الاختبار")
            } catch (error: Exception) {
                Log.w(
                    TAG,
                    "تعذر إزالة نافذة الاختبار",
                    error
                )
            }
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "تم إيقاف خدمة الهوية العائمة")

        stopOverlay()

        if (
            serviceLifecycle.currentState !=
            Lifecycle.State.DESTROYED
        ) {
            serviceLifecycle.handleLifecycleEvent(
                Lifecycle.Event.ON_DESTROY
            )
        }

        windowManager = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    }
    
