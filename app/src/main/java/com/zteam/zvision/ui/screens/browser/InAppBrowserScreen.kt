package com.zteam.zvision.ui.screens.browser

import android.content.Intent
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun InAppBrowserScreen(url: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    BackHandler {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            onBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Ensure no persisted state
            try {
                webViewRef?.apply {
                    clearHistory()
                    clearCache(true)
                    clearFormData()
                    loadUrl("about:blank")
                    destroy()
                }
                CookieManager.getInstance().apply {
                    removeAllCookies(null)
                    flush()
                }
                WebStorage.getInstance().deleteAllData()
            } catch (_: Exception) { }
        }
    }

    AndroidView(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        factory = {
            WebView(context).apply webView@ {
                // Privacy-focused settings
                settings.apply {
                    domStorageEnabled = true
                    setSupportMultipleWindows(false)
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    allowFileAccess = false
                    allowContentAccess = false
                    mediaPlaybackRequiresUserGesture = true
                    userAgentString = "$userAgentString ZVisionSafeWebView/1.0"
                    javaScriptEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }

                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(this@webView, false)
                    removeAllCookies(null)
                    flush()
                }

                webViewClient = createSecureWebViewClient()
                webChromeClient = WebChromeClient()

                // Start clean
                clearCache(true)
                clearHistory()
                clearFormData()
                WebStorage.getInstance().deleteAllData()

                loadUrl(url)
                webViewRef = this
            }
        },
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        }
    )
}

private fun createSecureWebViewClient() = object : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: android.webkit.WebResourceRequest?
    ): Boolean {
        val u = request?.url?.toString().orEmpty()
        if (u.startsWith("intent://")) {
            return try {
                val intent = Intent.parseUri(u, Intent.URI_INTENT_SCHEME)
                val fallback = intent.getStringExtra("browser_fallback_url")
                if (!fallback.isNullOrEmpty() && (fallback.startsWith("http://") || fallback.startsWith("https://"))) {
                    view?.loadUrl(fallback)
                }
                true
            } catch (_: Exception) {
                true
            }
        }
        val scheme = request?.url?.scheme.orEmpty()
        return scheme !in listOf("http", "https")
    }

    @Suppress("DEPRECATION")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        val u = url.orEmpty()
        if (u.startsWith("intent://")) {
            return try {
                val intent = Intent.parseUri(u, Intent.URI_INTENT_SCHEME)
                val fallback = intent.getStringExtra("browser_fallback_url")
                if (!fallback.isNullOrEmpty() && (fallback.startsWith("http://") || fallback.startsWith("https://"))) {
                    view?.loadUrl(fallback)
                }
                true
            } catch (_: Exception) {
                true
            }
        }
        if (!(u.startsWith("http://") || u.startsWith("https://"))) return true
        return false
    }
}
