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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun RoyalTVEmbeddedWebSource(
    modifier: Modifier = Modifier
) {
    val webView = remember {
        WebView(
            androidx.compose.ui.platform.LocalContext
                .current
        ).apply {
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

            loadUrl("https://www.youtube.com/tv")
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
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}
