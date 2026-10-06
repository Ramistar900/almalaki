package com.almalaki.cafe.royaltv

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.collectLatest

/**
 * طبقة YouTube داخل ROYAL TV.
 *
 * المسؤوليات الحالية:
 *
 * 1. عرض YouTube داخل ROYAL TV.
 * 2. استقبال أوامر RoyalTVManager.
 * 3. استقبال بحث YouTube من ROYAL Remote.
 * 4. استقبال Enter من ROYAL Keyboard وتنفيذ البحث.
 * 5. تجهيز WebView لاستقبال D-Pad / Remote لاحقًا.
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
             * تجهيز WebView للتحكم من ريموت التلفزيون.
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
                     * إعطاء WebView التركيز بعد تحميل الصفحة.
                     * هذا مهم لاحقًا للتعامل مع D-Pad وريموت الجهاز.
                     */
                    view?.requestFocus()

                    if (!url.isNullOrBlank()) {
                        onPageChanged?.invoke(url)
                    }
                }
            }

            loadUrl(initialUrl)
        }
    }

    /*
     * ============================================================
     * 1) أوامر RoyalTVManager
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
     * إذا وصل أمر YOUTUBE_SEARCH من الريموت،
     * يتم فتح نتائج البحث داخل WebView.
     */
    LaunchedEffect(webView) {

        RoyalTVRemoteManager.lastCommand.collectLatest { command ->

            if (command == null) {
                return@collectLatest
            }

            when (command.action) {

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

                RoyalTVRemoteAction.SELECT -> {

                    /*
                     * إبقاء WebView في حالة تركيز.
                     *
                     * النقر الفعلي داخل الصفحة سيبقى
                     * مسؤولية WebView / جهاز الإدخال.
                     */
                    webView.requestFocus()
                }

                RoyalTVRemoteAction.BACK -> {

                    if (webView.canGoBack()) {
                        webView.goBack()
                    }
                }

                else -> {
                    /*
                     * بقية الأوامر ستُربط مع طبقة التحكم
                     * الخاصة بالشاشة في الخطوات القادمة.
                     */
                }
            }
        }
    }

    /*
     * ============================================================
     * 3) ROYAL Keyboard → YouTube
     * ============================================================
     *
     * الحروف نفسها تُدار بواسطة RoyalTVKeyboardManager.
     *
     * عند الضغط على ENTER:
     * النص الحالي يتحول إلى بحث YouTube.
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
                    }
                }

                else -> {
                    /*
                     * CHARACTER / DELETE / CLEAR
                     * تُدار حالياً داخل KeyboardManager.
                     */
                }
            }
        }
    }

    AndroidView(
        factory = {
            webView
        },
        modifier = modifier,
        update = {
            /*
             * WebView يبقى داخل مساحة ROYAL TV.
             */
        }
    )

    DisposableEffect(webView) {

        onDispose {

            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
        }
    }
}
