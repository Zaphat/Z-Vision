package com.zteam.zvision.ui.screens.browser

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.core.net.toUri

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

                webViewClient = createSecureWebViewClient(context)
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

// +++ helper to launch intents for non-http(s) schemes
private fun launchExternalUri(context: Context, uri: Uri) {
    val scheme = uri.scheme?.lowercase()
    val baseIntent = when (scheme) {
        "mailto" -> Intent(Intent.ACTION_SENDTO, uri)
        "tel" -> Intent(Intent.ACTION_DIAL, uri)
        "sms", "smsto", "mms", "mmsto" -> Intent(Intent.ACTION_SENDTO, uri)
        else -> Intent(Intent.ACTION_VIEW, uri)
    }.apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(Intent.createChooser(baseIntent, null))
    } catch (_: ActivityNotFoundException) {
        // No matching app installed; ignore
    }
}

private fun createSecureWebViewClient(context: Context) = object : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: android.webkit.WebResourceRequest?
    ): Boolean {
        val u = request?.url?.toString().orEmpty()
        if (u.startsWith("intent://")) {
            return try {
                val intent = Intent.parseUri(u, Intent.URI_INTENT_SCHEME).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    component = null
                    selector = null
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(Intent.createChooser(intent, null))
                } catch (_: ActivityNotFoundException) {
                    val fallback = intent.getStringExtra("browser_fallback_url")
                    if (!fallback.isNullOrEmpty() &&
                        (fallback.startsWith("http://") || fallback.startsWith("https://"))
                    ) {
                        view?.loadUrl(fallback)
                    }
                }
                true
            } catch (_: Exception) {
                true
            }
        }
        val scheme = request?.url?.scheme.orEmpty()
        if (scheme !in listOf("http", "https")) {
            request?.url?.let { launchExternalUri(context, it) }
            return true
        }
        return false
    }

    @Suppress("DEPRECATION")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        val u = url.orEmpty()
        if (u.startsWith("intent://")) {
            return try {
                val intent = Intent.parseUri(u, Intent.URI_INTENT_SCHEME).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    component = null
                    selector = null
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(Intent.createChooser(intent, null))
                } catch (_: ActivityNotFoundException) {
                    val fallback = intent.getStringExtra("browser_fallback_url")
                    if (!fallback.isNullOrEmpty() &&
                        (fallback.startsWith("http://") || fallback.startsWith("https://"))
                    ) {
                        view?.loadUrl(fallback)
                    }
                }
                true
            } catch (_: Exception) {
                true
            }
        }
        val uri = try {
            u.toUri() } catch (_: Exception) { null }
        if (uri != null && uri.scheme != null && uri.scheme !in listOf("http", "https")) {
            launchExternalUri(context, uri)
            return true
        }
        return false
    }
}
