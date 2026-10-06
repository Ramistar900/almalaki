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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * WebView الخاص بـ ROYAL TV.
 *
 * يعرض YouTube داخل مساحة محتوى ROYAL TV
 * ولا يفتح تطبيق YouTube الخارجي.
 *
 * شريط ROYAL TV السفلي يبقى مسؤولية شاشة ROYAL TV الرئيسية.
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

    AndroidView(
        factory = {
            webView
        },
        modifier = modifier,
        update = {
            // سيتم التحكم بالتنقل لاحقًا
            // بواسطة RoyalTVYouTubeManager
            // وRoyal Keyboard وRoyal Remote.
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
