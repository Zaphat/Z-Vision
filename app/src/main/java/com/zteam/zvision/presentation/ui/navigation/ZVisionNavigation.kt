package com.zteam.zvision.presentation.ui.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zteam.zvision.appDataStore
import com.zteam.zvision.presentation.ui.screens.MainScreen
import com.zteam.zvision.presentation.ui.screens.browser.InAppBrowserScreen
import com.zteam.zvision.presentation.ui.screens.language.LanguageChoosingPage
import com.zteam.zvision.presentation.ui.screens.qr_create_store.QrCreationScreen
import com.zteam.zvision.presentation.ui.screens.qr_create_store.QrStorageScreen
import kotlinx.coroutines.flow.first

private val KEY_SELECTING_MODE = stringPreferencesKey("selecting_mode")
private val KEY_LANG_FROM = stringPreferencesKey("translate_from_lang")
private val KEY_LANG_TO = stringPreferencesKey("translate_to_lang")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZVisionNavigation() {
    var selectingMode by remember { mutableStateOf("QR") }
    var initTranslateFromLanguage by remember { mutableStateOf("Tiếng Việt") }
    var initTranslateToLanguage by remember { mutableStateOf("English") }
    var isNavigating by remember { mutableStateOf(false) }
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    var browserOpen by remember { mutableStateOf(false) }
    val appContext = LocalContext.current

    val navigationHelper = remember {
        NavigationHelper(
            navController = navController,
            isNavigating = { isNavigating },
            setNavigating = { isNavigating = it },
            coroutineScope = coroutineScope
        )
    }

    fun launchExternalIntent(context: Context, uri: Uri) {
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

    val openInAppBrowser: (String) -> Unit = { url ->
        val uri = runCatching { url.toUri() }.getOrNull()
        val scheme = uri?.scheme?.lowercase()
        if (scheme == "http" || scheme == "https") {
            browserOpen = true
            val encoded = Uri.encode(url)
            navController.navigate("zv_browser?url=$encoded")
        } else if (uri != null) {
            launchExternalIntent(appContext, uri)
        }
    }

    // Restore persisted values
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val prefs = appContext.appDataStore.data.first()
        selectingMode = prefs[KEY_SELECTING_MODE] ?: selectingMode
        initTranslateFromLanguage = prefs[KEY_LANG_FROM] ?: initTranslateFromLanguage
        initTranslateToLanguage = prefs[KEY_LANG_TO] ?: initTranslateToLanguage
    }

    // Persist on change
    androidx.compose.runtime.LaunchedEffect(selectingMode) {
        appContext.appDataStore.edit { it[KEY_SELECTING_MODE] = selectingMode }
    }
    androidx.compose.runtime.LaunchedEffect(initTranslateFromLanguage) {
        appContext.appDataStore.edit { it[KEY_LANG_FROM] = initTranslateFromLanguage }
    }
    androidx.compose.runtime.LaunchedEffect(initTranslateToLanguage) {
        appContext.appDataStore.edit { it[KEY_LANG_TO] = initTranslateToLanguage }
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
                    navigationHelper.navigateToLanguageSelection(isFromLanguage)
                },
                onNavigateToQrStorage = {
                    navigationHelper.navigateToQrStorage()
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
                    navigationHelper.safePopBack()
                },
                onBackPressed = {
                    navigationHelper.safePopBack()
                }
            )
        }

        composable("qr_creation") {
            QrCreationScreen(
                onNavigateToQrStorage = { navigationHelper.navigateToQrStorage() }
            )
        }

        composable("qr_storage") {
            QrStorageScreen(
                onBack = { navigationHelper.safePopBack() },
                onNavigateToQrCreation = { navigationHelper.navigateToQrCreation() }
            )
        }

        composable(
            route = "zv_browser?url={url}",
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { backStackEntry ->
            androidx.compose.runtime.DisposableEffect(Unit) {
                browserOpen = true
                onDispose { browserOpen = false }
            }
            val encodedUrl = backStackEntry.arguments?.getString("url").orEmpty()
            val targetUrl = Uri.decode(encodedUrl)
            InAppBrowserScreen(
                url = targetUrl,
                onBack = {
                    navigationHelper.safePopBack()
                }
            )
        }
    }
}
