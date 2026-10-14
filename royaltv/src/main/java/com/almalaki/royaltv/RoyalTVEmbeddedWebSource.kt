package com.almalaki.royaltv

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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

    /*
     * تشغيل الهوية العائمة الجديدة أثناء عرض
     * YouTube Web داخل نافذة ROYAL TV.
     */
    DisposableEffect(context) {
        var overlayStarted = false

        val hasOverlayPermission =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                Settings.canDrawOverlays(context)

        if (hasOverlayPermission) {
            try {
                val startIntent = Intent(
                    context,
                    RoyalTVBroadcastOverlayService::class.java
                ).apply {
                    action =
                        RoyalTVBroadcastOverlayService.ACTION_START
                }

                context.startService(startIntent)
                overlayStarted = true
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    "تعذر تشغيل الهوية العائمة",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                "يجب السماح لـ ROYAL TV بالظهور فوق التطبيقات",
                Toast.LENGTH_LONG
            ).show()
        }

        onDispose {
            if (overlayStarted) {
                try {
                    val stopIntent = Intent(
                        context,
                        RoyalTVBroadcastOverlayService::class.java
                    ).apply {
                        action =
                            RoyalTVBroadcastOverlayService.ACTION_STOP
                    }

                    context.startService(stopIntent)
                } catch (_: Exception) {
                    // نتجاوز تعذر إيقاف الخدمة عند مغادرة العرض.
                }
            }
        }
    }

    key(url) {
        val webView = remember(context, url) {
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

        /*
         * صفحة YouTube فقط داخل مربع البث.
         * لا نضيف شعارًا نصيًا أو إطارًا بديلًا.
         */
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
