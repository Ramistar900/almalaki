package com.almalaki.cafe.royaltv

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * WebView الخاص بـ ROYAL TV.
 *
 * مهم:
 * - لا يفتح تطبيق YouTube الخارجي.
 * - يعرض YouTube داخل مساحة ROYAL TV نفسها.
 * - يمكن لاحقًا التحكم به بواسطة Royal Keyboard و Remote.
 * - لا يحتوي على شريط ROYAL TV السفلي؛
 *   الشريط سيبقى مسؤولية شاشة ROYAL TV الرئيسية.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RoyalTVYouTubeWebView(
    modifier: Modifier = Modifier,
    initialUrl: String = "https://www.youtube.com/",
    onPageChanged: ((String) -> Unit)? = null
) {
    val webView = remember {
        WebView(androidx.compose.ui.platform.LocalContext.current).apply {

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

            webChromeClient = WebChromeClient()

            setBackgroundColor(android.graphics.Color.BLACK)

            webViewClient =
                object : android.webkit.WebViewClient() {

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
            // تحديث WebView يتم لاحقًا عبر أوامر ROYAL TV.
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
        }
    }
}
