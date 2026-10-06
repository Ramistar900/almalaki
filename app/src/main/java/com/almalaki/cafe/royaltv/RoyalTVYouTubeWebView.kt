package com.almalaki.cafe.royaltv

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.collectLatest

/**
 * طبقة YouTube داخل ROYAL TV.
 *
 * المسؤوليات:
 *
 * 1. عرض YouTube داخل ROYAL TV.
 * 2. استقبال أوامر RoyalTVManager.
 * 3. استقبال بحث YouTube من ROYAL Remote.
 * 4. استقبال Enter من ROYAL Keyboard.
 * 5. استقبال D-Pad الحقيقي من Android TV / TV Box / Receiver.
 * 6. تمرير أوامر D-Pad القادمة من ROYAL Remote إلى WebView.
 *
 * لا يفتح تطبيق YouTube الخارجي.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RoyalTVYouTubeWebView(
    modifier: Modifier = Modifier,
    initialUrl: String = "https://www.youtube.com/",
    onPageChanged: ((String) -> Unit)? = null
) {
    val context = LocalContext.current

    val webView = remember(context) {

        WebView(context).apply {

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            /*
             * =====================================================
             * Focus
             * =====================================================
             *
             * WebView يجب أن يمتلك التركيز حتى يستقبل
             * D-Pad / Enter من جهاز Android TV.
             */
            isFocusable = true
            isFocusableInTouchMode = true

            settings.apply {

                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true

                mediaPlaybackRequiresUserGesture = false

                loadWithOverviewMode = true
                useWideViewPort = true

                builtInZoomControls = false
                displayZoomControls = false

                cacheMode = WebSettings.LOAD_DEFAULT

                allowFileAccess = true
                allowContentAccess = true
            }

            setBackgroundColor(Color.BLACK)

            webChromeClient = WebChromeClient()

            webViewClient = object : WebViewClient() {

                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {
                    super.onPageFinished(view, url)

                    /*
                     * بعد كل تحميل:
                     * نعيد التركيز إلى WebView.
                     */
                    view?.requestFocus()

                    if (!url.isNullOrBlank()) {
                        onPageChanged?.invoke(url)
                    }
                }
            }

            /*
             * الصفحة الأولى.
             */
            loadUrl(initialUrl)
        }
    }

    /*
     * ============================================================
     * 1) RoyalTVManager → YouTube
     * ============================================================
     */
    LaunchedEffect(webView) {

        RoyalTVManager.state.collectLatest { state ->

            if (state.source != RoyalTVSource.YOUTUBE) {
                return@collectLatest
            }

            val request =
                RoyalTVYouTubeManager.decode(
                    state.payload
                ) ?: return@collectLatest

            when (request.action) {

                RoyalTVYouTubeManager.Action.OPEN_HOME -> {

                    webView.loadUrl(
                        "https://www.youtube.com/"
                    )
                }

                RoyalTVYouTubeManager.Action.OPEN_SEARCH -> {

                    val searchUrl =
                        RoyalTVYouTubeManager.buildSearchUrl(
                            request.query
                        )

                    webView.loadUrl(searchUrl)
                }

                RoyalTVYouTubeManager.Action.OPEN_VIDEO -> {

                    val videoUrl =
                        RoyalTVYouTubeManager.normalizeVideoUrl(
                            request.url
                        )

                    if (videoUrl.isNotBlank()) {
                        webView.loadUrl(videoUrl)
                    }
                }

                RoyalTVYouTubeManager.Action.CLOSE -> {

                    webView.loadUrl(
                        "about:blank"
                    )
                }
            }
        }
    }

    /*
     * ============================================================
     * 2) ROYAL Remote → YouTube
     * ============================================================
     *
     * أوامر الريموت القادمة من الهاتف / التابلت
     * تتحول إلى KeyEvent حقيقي داخل WebView.
     */
    LaunchedEffect(webView) {

        RoyalTVRemoteManager.lastCommand.collectLatest { command ->

            if (command == null) {
                return@collectLatest
            }

            when (command.action) {

                RoyalTVRemoteAction.NAVIGATE_UP -> {

                    dispatchDpadKey(
                        webView,
                        KeyEvent.KEYCODE_DPAD_UP
                    )
                }

                RoyalTVRemoteAction.NAVIGATE_DOWN -> {

                    dispatchDpadKey(
                        webView,
                        KeyEvent.KEYCODE_DPAD_DOWN
                    )
                }

                RoyalTVRemoteAction.NAVIGATE_LEFT -> {

                    dispatchDpadKey(
                        webView,
                        KeyEvent.KEYCODE_DPAD_LEFT
                    )
                }

                RoyalTVRemoteAction.NAVIGATE_RIGHT -> {

                    dispatchDpadKey(
                        webView,
                        KeyEvent.KEYCODE_DPAD_RIGHT
                    )
                }

                RoyalTVRemoteAction.SELECT -> {

                    dispatchDpadKey(
                        webView,
                        KeyEvent.KEYCODE_DPAD_CENTER
                    )
                }

                RoyalTVRemoteAction.BACK -> {

                    if (webView.canGoBack()) {

                        webView.goBack()

                    } else {

                        webView.requestFocus()
                    }
                }

                RoyalTVRemoteAction.YOUTUBE_SEARCH -> {

                    val query =
                        command.value.trim()

                    if (query.isNotBlank()) {

                        val searchUrl =
                            RoyalTVYouTubeManager.buildSearchUrl(
                                query
                            )

                        webView.loadUrl(searchUrl)
                    }
                }

                else -> {
                    /*
                     * بقية أوامر الريموت لا تخص
                     * التنقل داخل YouTube حاليًا.
                     */
                }
            }
        }
    }

    /*
     * ============================================================
     * 3) Royal Keyboard → YouTube
     * ============================================================
     */
    LaunchedEffect(webView) {

        RoyalTVKeyboardManager.lastEvent.collectLatest { event ->

            if (event == null) {
                return@collectLatest
            }

            when (event.action) {

                RoyalTVKeyboardAction.ENTER -> {

                    val query =
                        event.text.trim()

                    if (query.isNotBlank()) {

                        val searchUrl =
                            RoyalTVYouTubeManager.buildSearchUrl(
                                query
                            )

                        webView.loadUrl(searchUrl)

                        webView.requestFocus()
                    }
                }

                else -> {
                    /*
                     * CHARACTER / DELETE / CLEAR
                     * تتم إدارتها بواسطة KeyboardManager.
                     */
                }
            }
        }
    }

    /*
     * ============================================================
     * 4) Android TV / TV Box / Receiver
     *    Physical Remote → WebView
     * ============================================================
     *
     * هذه هي الطبقة المهمة الجديدة.
     *
     * Android TV يرسل أزرار D-Pad كـ KeyEvent.
     * Compose يلتقط الحدث ثم نمرره إلى WebView.
     */
    AndroidView(

        factory = {
            webView
        },

        modifier =
            modifier
                .focusable()
                .onKeyEvent { keyEvent ->

                    /*
                     * تمرير KeyEvent الأصلي إلى WebView.
                     *
                     * هذا يسمح لـ YouTube/WebView
                     * بمعالجة:
                     *
                     * ↑ ↓ ← →
                     * OK / Enter
                     * وغيرها من أحداث لوحة التحكم.
                     */
                    webView.dispatchKeyEvent(
                        keyEvent.nativeKeyEvent
                    )
                },

        update = {

            /*
             * التأكد من بقاء WebView جاهزًا للريموت.
             */
            if (!webView.hasFocus()) {
                webView.requestFocus()
            }
        }
    )

    /*
     * ============================================================
     * 5) تنظيف WebView
     * ============================================================
     */
    DisposableEffect(webView) {

        onDispose {

            webView.stopLoading()

            webView.loadUrl(
                "about:blank"
            )

            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
        }
    }
}

/**
 * إرسال D-Pad صناعي إلى WebView.
 *
 * يستخدم عندما يأتي الأمر من ROYAL Remote
 * الموجود في الهاتف / التابلت.
 */
private fun dispatchDpadKey(
    webView: WebView,
    keyCode: Int
) {
    webView.requestFocus()

    val downEvent =
        KeyEvent(
            KeyEvent.ACTION_DOWN,
            keyCode
        )

    val upEvent =
        KeyEvent(
            KeyEvent.ACTION_UP,
            keyCode
        )

    webView.dispatchKeyEvent(
        downEvent
    )

    webView.dispatchKeyEvent(
        upEvent
    )
}
