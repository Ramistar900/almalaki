package com.almalaki.royaltv

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner

/**
 * خدمة الهوية العائمة الخاصة بـ ROYAL TV.
 *
 * تعرض هوية البث فوق التطبيقات الخارجية.
 * لا تعترض اللمسات الموجّهة إلى التطبيق الموجود تحتها.
 */
class RoyalTVBroadcastOverlayService :
    Service(),
    LifecycleOwner {

    companion object {
        const val ACTION_START =
            "com.almalaki.royaltv.action.START_BROADCAST_OVERLAY"

        const val ACTION_STOP =
            "com.almalaki.royaltv.action.STOP_BROADCAST_OVERLAY"
    }

    private val serviceLifecycle =
        LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = serviceLifecycle

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()

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

        when (intent?.action) {
            ACTION_STOP -> {
                stopOverlay()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START, null -> {
                startOverlay()
            }
        }

        return START_NOT_STICKY
    }

    private fun startOverlay() {
        if (overlayView != null) return

        // لا يمكن إظهار النافذة دون موافقة المستخدم.
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)
        ) {
            stopSelf()
            return
        }

        val manager = windowManager ?: run {
            stopSelf()
            return
        }

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
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
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
        } catch (_: Exception) {
            composeView.disposeComposition()
            overlayView = null
            stopSelf()
        }
    }

    private fun stopOverlay() {
        val state = serviceLifecycle.currentState

        if (state.isAtLeast(Lifecycle.State.RESUMED)) {
            serviceLifecycle.handleLifecycleEvent(
                Lifecycle.Event.ON_PAUSE
            )
        }

        if (serviceLifecycle.currentState.isAtLeast(
                Lifecycle.State.STARTED
            )
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
            } catch (_: Exception) {
                // قد تكون النافذة أزيلت مسبقًا من النظام.
            }

            view.disposeComposition()
        }
    }

    override fun onDestroy() {
        stopOverlay()

        if (serviceLifecycle.currentState != Lifecycle.State.DESTROYED) {
            serviceLifecycle.handleLifecycleEvent(
                Lifecycle.Event.ON_DESTROY
            )
        }

        windowManager = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    }
