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
 * تستقبل أوامر RoyalTVManager:
 *
 * OPEN_HOME
 * OPEN_SEARCH
 * OPEN_VIDEO
 * CLOSE
 *
 * ولا تفتح تطبيق YouTube الخارجي.
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

                    if (!url.isNullOrBlank()) {
                        onPageChanged?.invoke(url)
                    }
                }
            }

            loadUrl(initialUrl)
        }
    }

    /*
     * مراقبة حالة ROYAL TV.
     *
     * عندما يصل أمر SHOW_YOUTUBE،
     * يتم تحويله إلى إجراء داخل WebView.
     */
    LaunchedEffect(Unit) {
        RoyalTVManager.state.collectLatest { state ->

            if (state.source != RoyalTVSource.YOUTUBE) {
                return@collectLatest
            }

            val request =
                RoyalTVYouTubeManager.decode(state.payload)
                    ?: return@collectLatest

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

    AndroidView(
        factory = {
            webView
        },
        modifier = modifier,
        update = {
            // أوامر التنقل تتم عبر RoyalTVManager.
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
