package com.almalaki.royaltv

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun RoyalTVEmbeddedWebSource(
    url: String = "https://www.youtube.com/tv",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    key(url) {
        val webView = remember(context) {
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false

                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()

                setBackgroundColor(
                    android.graphics.Color.BLACK
                )

                loadUrl(url)
            }
        }

        DisposableEffect(webView) {
            onDispose {
                webView.stopLoading()
                webView.destroy()
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize()
            )

            Text(
                text = "ROYAL TV",
                color = Color(0xFFD4AF37),
                modifier = Modifier.align(
                    Alignment.BottomEnd
                )
            )
        }
    }
}
