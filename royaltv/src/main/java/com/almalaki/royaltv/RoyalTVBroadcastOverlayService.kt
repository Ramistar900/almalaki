package com.almalaki.royaltv

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.text.TextUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class RoyalTVBroadcastOverlayService : Service() {

    companion object {
        private const val TAG = "RoyalTVOverlay"

        const val ACTION_START =
            "com.almalaki.royaltv.action.START_BROADCAST_OVERLAY"

        const val ACTION_STOP =
            "com.almalaki.royaltv.action.STOP_BROADCAST_OVERLAY"

        private const val GOLD = 0xFFD4AF37.toInt()
        private const val PEARL = 0xFFF0E9F5.toInt()
        private const val RED = 0xFFFF3038.toInt()
        private const val TURQUOISE = 0xFF16858A.toInt()
        private const val EMERALD = 0xFF087653.toInt()
    }

    private val handler = Handler(Looper.getMainLooper())

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var tickerView: TextView? = null
    private var tickerContainer: LinearLayout? = null
    private var timeView: TextView? = null
    private var liveDot: View? = null

    private val damascusTime =
        SimpleDateFormat("HH:mm", Locale("ar")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Damascus")
        }

    private val refreshRunnable = object : Runnable {
        override fun run() {
            timeView?.text = damascusTime.format(Date())

            val enabled =
                RoyalTVBroadcastSettings.isTickerEnabled(
                    applicationContext
                )

            tickerContainer?.visibility =
                if (enabled) View.VISIBLE else View.GONE

            if (enabled) {
                val latestText = RoyalTVTickerManager.getText()

                tickerView?.let { ticker ->
                    if (ticker.text.toString() != latestText) {
                        ticker.isSelected = false
                        ticker.text = latestText
                        ticker.post {
                            ticker.isSelected = true
                        }
                    }
                }
            }

            liveDot?.let { dot ->
                dot.alpha =
                    if (dot.alpha > 0.7f) 0.35f else 1f
            }

            handler.postDelayed(this, 1_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()

        windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager

        RoyalTVTickerManager.initialize(applicationContext)
        RoyalTVBroadcastSettings.initialize(applicationContext)

        Log.i(TAG, "تم تجهيز الهوية العائمة")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                removeOverlay()
                stopSelf()
            }

            ACTION_START, null -> startOverlay()

            else -> Log.w(
                TAG,
                "أمر غير معروف: ${intent.action}"
            )
        }

        return START_NOT_STICKY
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density)
            .toInt()
            .coerceAtLeast(1)

    private fun tickerBackground(): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                TURQUOISE,
                0xFF087F78.toInt(),
                EMERALD
            )
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(24).toFloat()
            setStroke(dp(1), GOLD)
        }

    private fun makeText(
        value: String,
        sizeSp: Float,
        color: Int,
        bold: Boolean = false
    ): TextView =
        TextView(this).apply {
            text = value
            textSize = sizeSp
            setTextColor(color)
            gravity = Gravity.CENTER
            includeFontPadding = false
            maxLines = 1
            setSingleLine(true)

            if (bold) {
                setTypeface(typeface, Typeface.BOLD)
            }
        }

    private fun buildLogo(): View {
        val size = dp(72)

        return FrameLayout(this).apply {
            val crest =
                ImageView(
                    this@RoyalTVBroadcastOverlayService
                ).apply {
                    setImageResource(
                        R.drawable.royal_crest_layer
                    )
                    scaleType =
                        ImageView.ScaleType.FIT_CENTER
                    contentDescription = "شعار الملكي"
                }

            val rc =
                ImageView(
                    this@RoyalTVBroadcastOverlayService
                ).apply {
                    setImageResource(
                        R.drawable.royal_rc_layer
                    )
                    scaleType =
                        ImageView.ScaleType.FIT_CENTER
                    contentDescription = "ROYAL"
                }

            addView(
                crest,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            addView(
                rc,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            layoutParams =
                LinearLayout.LayoutParams(size, size)
        }
    }

    private fun buildOverlay(): View {
        // خلفية شفافة بالكامل.
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(2), dp(8), dp(4))
            setBackgroundColor(Color.TRANSPARENT)
            clipChildren = true
            clipToPadding = true
        }

        // مباشر في الزاوية العلوية اليمنى.
        val liveRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0, 0, dp(2), dp(2))
        }

        liveDot = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(RED)
            }

            layoutParams =
                LinearLayout.LayoutParams(dp(8), dp(8)).apply {
                    marginStart = dp(5)
                }
        }

        liveRow.addView(liveDot)

        liveRow.addView(
            makeText("مباشر", 12f, Color.WHITE, true),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(20)
            )
        )

        root.addView(
            liveRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(22)
            )
        )

        // الشعار والوقت والشريط في سطر واحد.
        val mainRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.TRANSPARENT)
        }

        mainRow.addView(
            buildLogo(),
            LinearLayout.LayoutParams(dp(72), dp(72)).apply {
                marginEnd = dp(7)
            }
        )

        // الوقت بلا خلفية أو إطار.
        timeView = makeText(
            "--:--",
            17f,
            GOLD,
            true
        )

        mainRow.addView(
            timeView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(42)
            ).apply {
                marginEnd = dp(9)
            }
        )

        // شريط متدرج فيروزي/أخضر بإطار ذهبي.
        tickerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = tickerBackground()
            clipToOutline = true
            clipChildren = true
            clipToPadding = true
            setPadding(dp(10), 0, dp(10), 0)
        }

        tickerView = makeText(
            RoyalTVTickerManager.getText(),
            14f,
            Color.WHITE,
            true
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
            ellipsize = TextUtils.TruncateAt.MARQUEE
            marqueeRepeatLimit = -1
            isSingleLine = true
            setHorizontallyScrolling(true)
            isFocusable = false
            isFocusableInTouchMode = false
            isSelected = true
        }

        tickerContainer?.addView(
            tickerView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        mainRow.addView(
            tickerContainer,
            LinearLayout.LayoutParams(
                0,
                dp(42),
                1f
            )
        )

        root.addView(
            mainRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(74)
            )
        )

        // تفعيل حركة النص بعد قياس عرضه.
        tickerView?.post {
            tickerView?.isSelected = false
            tickerView?.isSelected = true
        }

        return root
    }

    private fun startOverlay() {
        if (overlayView != null) return

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)
        ) {
            Toast.makeText(
                this,
                "فعّل صلاحية الظهور فوق التطبيقات لـ ROYAL TV",
                Toast.LENGTH_LONG
            ).show()

            stopSelf()
            return
        }

        val manager = windowManager ?: run {
            stopSelf()
            return
        }

        val windowType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dp(102),
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
            val view = buildOverlay()
            manager.addView(view, params)
            overlayView = view

            handler.removeCallbacks(refreshRunnable)
            handler.post(refreshRunnable)

            Log.i(TAG, "ظهرت الهوية العائمة فوق التطبيق الخارجي")
        } catch (error: Exception) {
            Log.e(TAG, "فشل إظهار الهوية العائمة", error)
            removeOverlay()

            Toast.makeText(
                this,
                "تعذر إظهار هوية ROYAL TV",
                Toast.LENGTH_LONG
            ).show()

            stopSelf()
        }
    }

    private fun removeOverlay() {
        handler.removeCallbacks(refreshRunnable)

        val view = overlayView
        overlayView = null

        tickerView = null
        tickerContainer = null
        timeView = null
        liveDot = null

        if (view != null) {
            try {
                windowManager?.removeView(view)
            } catch (error: Exception) {
                Log.w(TAG, "تعذر إزالة الهوية العائمة", error)
            }
        }
    }

    override fun onDestroy() {
        removeOverlay()
        windowManager = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
