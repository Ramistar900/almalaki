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

        // ألوان متوافقة مع بطاقات BottomIdentityBar الرئيسية.
        private const val GOLD = 0xFFD4AF37.toInt()
        private const val PEARL = 0xFFF0E9F5.toInt()
        private const val RED = 0xFFFF3038.toInt()
        private const val CHARCOAL = 0xFF17191F.toInt()
        private const val BLACK = 0xFF050609.toInt()
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
                        ticker.text = latestText
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

            else -> Log.w(TAG, "أمر غير معروف: ${intent.action}")
        }

        return START_NOT_STICKY
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density)
            .toInt()
            .coerceAtLeast(1)

    private fun cardBackground(
        color: Int = CHARCOAL,
        strokeColor: Int = GOLD,
        radiusDp: Int = 7
    ): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(color)
            setStroke(dp(1), strokeColor)
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
        val size = dp(54)

        return FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                size,
                size
            )

            val crest =
                ImageView(this@RoyalTVBroadcastOverlayService).apply {
                    setImageResource(R.drawable.royal_crest_layer)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    contentDescription = "شعار الملكي"
                }

            val rc =
                ImageView(this@RoyalTVBroadcastOverlayService).apply {
                    setImageResource(R.drawable.royal_rc_layer)
                    scaleType = ImageView.ScaleType.FIT_CENTER
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
        }
    }

    private fun buildOverlay(): View {
        // الحاوية الخارجية شفافة: لا مستطيل أخضر ولا إطار خارجي.
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setBackgroundColor(Color.TRANSPARENT)
        }

        // 1. الشعار الأصلي بطبقتَي PNG.
        root.addView(
            buildLogo(),
            LinearLayout.LayoutParams(dp(54), dp(54))
        )

        // 2. الشريط الإخباري: فحمي وحد ذهبي مثل الواجهة الرئيسية.
        tickerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = cardBackground()
            setPadding(dp(8), 0, dp(8), 0)
        }

        tickerView = makeText(
            RoyalTVTickerManager.getText(),
            12f,
            PEARL,
            true
        ).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            ellipsize = android.text.TextUtils.TruncateAt.MARQUEE
            marqueeRepeatLimit = -1
            isSelected = true
            isFocusable = true
            isFocusableInTouchMode = true
        }

        tickerContainer?.addView(
            tickerView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        root.addView(
            tickerContainer,
            LinearLayout.LayoutParams(
                0,
                dp(36),
                1f
            ).apply {
                marginStart = dp(6)
                marginEnd = dp(6)
            }
        )

        // 3. الساعة فقط؛ التاريخ محذوف من الواجهة والتحديث.
        val clockCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = cardBackground()
            setPadding(dp(10), dp(7), dp(10), dp(7))
        }

        timeView = makeText(
            "--:--",
            15f,
            GOLD,
            true
        )

        clockCard.addView(
            timeView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            clockCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(36)
            ).apply {
                marginEnd = dp(6)
            }
        )

        // 4. علامة مباشر والنقطة الحمراء.
        val liveContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = cardBackground(BLACK, GOLD, 20)
            setPadding(dp(8), dp(7), dp(8), dp(7))
        }

        liveDot = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(RED)
            }

            layoutParams = LinearLayout.LayoutParams(
                dp(7),
                dp(7)
            ).apply {
                marginEnd = dp(4)
            }
        }

        liveContainer.addView(liveDot)

        liveContainer.addView(
            makeText(
                "مباشر",
                10f,
                Color.WHITE,
                true
            )
        )

        root.addView(
            liveContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(36)
            )
        )

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
            dp(66),
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
