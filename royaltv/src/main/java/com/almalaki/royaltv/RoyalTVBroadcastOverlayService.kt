package com.almalaki.royaltv

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
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
    private var overlayView: ComposeView? = null

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
            Log.i(TAG, "نافذة الهوية موجودة بالفعل")
            showMessage("هوية ROYAL TV تعمل بالفعل")
            return
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)
        ) {
            Log.e(TAG, "صلاحية الظهور فوق التطبيقات غير ممنوحة")
            showMessage(
                "ROYAL TV: صلاحية الظهور فوق التطبيقات غير مفعّلة"
            )
            stopSelf()
            return
        }

        val manager = windowManager
        if (manager == null) {
            Log.e(TAG, "WindowManager غير متاح")
            showMessage("ROYAL TV: تعذر الوصول إلى مدير النوافذ")
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

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindow
            )

            setBackgroundColor(
                android.graphics.Color.TRANSPARENT
            )

            setViewTreeLifecycleOwner(
                this@RoyalTVBroadcastOverlayService
            )

            setContent {
                RoyalTVBroadcastIdentity()
            }
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
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            x = 0
            y = 0
        }

        try {
            manager.addView(composeView, layoutParams)
            overlayView = composeView

            serviceLifecycle.handleLifecycleEvent(
                Lifecycle.Event.ON_START
            )
            serviceLifecycle.handleLifecycleEvent(
                Lifecycle.Event.ON_RESUME
            )

            Log.i(
                TAG,
                "نجحت إضافة نافذة الهوية، الارتفاع=$overlayHeightPx px"
            )

            showMessage("ROYAL TV: تم إنشاء نافذة الهوية")

        } catch (error: Exception) {
            Log.e(
                TAG,
                "فشل إنشاء نافذة الهوية العائمة",
                error
            )

            try {
                composeView.disposeComposition()
            } catch (disposeError: Exception) {
                Log.w(
                    TAG,
                    "تعذر التخلص من محتوى Compose",
                    disposeError
                )
            }

            overlayView = null

            showMessage(
                "ROYAL TV: فشل إنشاء الهوية — ${error.javaClass.simpleName}"
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
                Log.i(TAG, "تمت إزالة نافذة الهوية")
            } catch (error: Exception) {
                Log.w(
                    TAG,
                    "تعذر إزالة النافذة",
                    error
                )
            }

            try {
                view.disposeComposition()
            } catch (error: Exception) {
                Log.w(
                    TAG,
                    "تعذر التخلص من محتوى Compose",
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
    
