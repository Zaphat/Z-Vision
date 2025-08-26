package com.zteam.zvision

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zteam.zvision.data.local.AppDatabase
import com.zteam.zvision.data.repository.QrRepository
import com.zteam.zvision.domain.QrUsecase
import com.zteam.zvision.ui.commons.LanguageChoosingPage
import com.zteam.zvision.ui.features.qrCreation.QrCreationViewModel
import com.zteam.zvision.ui.screens.MainScreen
import com.zteam.zvision.ui.screens.qrCreation.QrCreationScreen
import com.zteam.zvision.ui.screens.qrCreation.QrStorageScreen
import com.zteam.zvision.ui.theme.ZVisionTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZVisionTheme {
                var selectingMode by remember { mutableStateOf("QR") }
                var initTranslateFromLanguage by remember { mutableStateOf("Tiếng Việt") }
                var initTranslateToLanguage by remember { mutableStateOf("English") }
                var isNavigating by remember { mutableStateOf(false) }
                val navController = rememberNavController()
                val coroutineScope = rememberCoroutineScope()
                // Track if in-app browser is active
                var browserOpen by remember { mutableStateOf(false) }

                fun safePopBack(){
                    if (!navController.popBackStack()) {
                        navController.navigate("main") {
                            popUpTo("main") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }

                // Helper to open URL in the in-app browser
                val openInAppBrowser: (String) -> Unit = { url ->
                    browserOpen = true
                    val encoded = Uri.encode(url)
                    navController.navigate("zv_browser?url=$encoded")
                }

                NavHost(
                    navController = navController,
                    startDestination = "main"
                ) {
                    composable("main") {
                        MainScreen(
                            selectingMode = selectingMode,
                            onModeChange = { selectingMode = it },
                            translateFromLanguage = initTranslateFromLanguage,
                            translateToLanguage = initTranslateToLanguage,
                            onNavigateToLanguageSelection = { isFromLanguage ->
                                if (!isNavigating) {
                                    isNavigating = true
                                    coroutineScope.launch {
                                        try {
                                            navController.navigate("language_selection/$isFromLanguage")
                                        } catch (_: Exception) {
                                            // Handle navigation error
                                        } finally {
                                            isNavigating = false
                                        }
                                    }
                                }
                            },
                            onNavigateToQrStorage = {
                                if (!isNavigating) {
                                    isNavigating = true
                                    coroutineScope.launch {
                                        try {
                                            navController.navigate("qr_storage")
                                        } catch (_: Exception) {
                                            // Handle navigation error
                                        } finally {
                                            isNavigating = false
                                        }
                                    }
                                }
                            },
                            onOpenUrl = openInAppBrowser,
                            scanningEnabled = !browserOpen
                        )
                    }
                    composable("language_selection/{isFromLanguage}") { backStackEntry ->
                        val isFromLanguage = backStackEntry.arguments?.getString("isFromLanguage")?.toBoolean() ?: true
                        LanguageChoosingPage(
                            isFromLanguage = isFromLanguage,
                            currentFromLanguage = initTranslateFromLanguage,
                            currentToLanguage = initTranslateToLanguage,
                            onLanguageSelected = { language ->
                                if (isFromLanguage) {
                                    initTranslateFromLanguage = language
                                } else {
                                    initTranslateToLanguage = language
                                }
                                safePopBack()
                            },
                            onBackPressed = {
                                safePopBack()
                            }
                        )
                    }
                    composable("qr_creation") {
                        QrCreationScreen(
                            onBack = { safePopBack() },
                            onNavigateToQrStorage = {
                                if (!isNavigating) {
                                    isNavigating = true
                                    coroutineScope.launch {
                                        try {
                                            navController.navigate("qr_storage")
                                        } catch (_: Exception) {
                                            // Handle navigation error
                                        } finally {
                                            isNavigating = false
                                        }
                                    }
                                }
                            }
                        )
                    }
                    composable("qr_storage") {
                        // Compose ViewModel for Compose screen
                        val context = LocalContext.current
                        val viewModel = remember {
                            val db = AppDatabase.getInstance(context)
                            val repo = QrRepository(db.qrDao())
                            val usecase = QrUsecase(repo)
                            QrCreationViewModel(usecase)
                        }
                        QrStorageScreen(
                            viewModel = viewModel,
                            onBack = { safePopBack() },
                            onNavigateToQrCreation = {
                                if (!isNavigating) {
                                    isNavigating = true
                                    coroutineScope.launch {
                                        try {
                                            navController.navigate("qr_creation")
                                        } catch (_: Exception) {
                                            // Handle navigation error
                                        } finally {
                                            isNavigating = false
                                        }
                                    }
                                }
                            }
                        )
                    }
                    composable(
                        route = "zv_browser?url={url}",
                        arguments = listOf(navArgument("url") { type = NavType.StringType })
                    ) { backStackEntry ->
                        // Mark browser as open while this destination is in composition
                        androidx.compose.runtime.DisposableEffect(Unit) {
                            browserOpen = true
                            onDispose { browserOpen = false }
                        }
                        val encodedUrl = backStackEntry.arguments?.getString("url").orEmpty()
                        val targetUrl = Uri.decode(encodedUrl)
                        InAppBrowserScreen(
                            url = targetUrl,
                            onBack = {
                                // Close the WebView screen
                                if (!navController.popBackStack()) {
                                    navController.navigate("main") {
                                        popUpTo("main") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// Ephemeral, hardened in-app browser
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
                    domStorageEnabled = true             // keep runtime storage, but we purge on dispose
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

                // No persistent cookies (allow session cookies; clear on dispose)
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(this@webView, false)
                    removeAllCookies(null)
                    flush()
                }

                // Keep all navigation's in-app and handle intents
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?
                    ): Boolean {
                        val u = request?.url?.toString().orEmpty()
                        // Handle intent:// deep links by using browser_fallback_url if provided
                        if (u.startsWith("intent://")) {
                            return try {
                                val intent = Intent.parseUri(u, Intent.URI_INTENT_SCHEME)
                                val fallback = intent.getStringExtra("browser_fallback_url")
                                if (!fallback.isNullOrEmpty() && (fallback.startsWith("http://") || fallback.startsWith("https://"))) {
                                    view?.loadUrl(fallback)
                                }
                                true // do not leave the app
                            } catch (_: Exception) {
                                true
                            }
                        }
                        // Block custom schemes (e.g., vnd.youtube:)
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
            // If URL changes, navigate
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        }
    )
}
