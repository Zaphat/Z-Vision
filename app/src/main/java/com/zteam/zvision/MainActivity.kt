package com.zteam.zvision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.*
import kotlinx.coroutines.launch
import com.zteam.zvision.ui.screens.MainScreen
import com.zteam.zvision.ui.commons.LanguageChoosingPage
import com.zteam.zvision.ui.theme.ZVisionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZVisionTheme {
                var initMode by remember { mutableStateOf("QR") }
                var initTranslateFromLanguage by remember { mutableStateOf("Tiếng Việt") }
                var initTranslateToLanguage by remember { mutableStateOf("English") }
                var isNavigating by remember { mutableStateOf(false) }
                val navController = rememberNavController()
                val coroutineScope = rememberCoroutineScope()

                fun safePopBack(){
                    if (!navController.popBackStack()) {
                        navController.navigate("main") {
                            popUpTo("main") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "main"
                ) {
                    composable("main") {
                        MainScreen(
                            initMode = initMode,
                            onModeChange = { initMode = it },
                            translateFromLanguage = initTranslateFromLanguage,
                            translateToLanguage = initTranslateToLanguage,
                            onNavigateToLanguageSelection = { isFromLanguage ->
                                if (!isNavigating) {
                                    isNavigating = true
                                    coroutineScope.launch {
                                        try {
                                            navController.navigate("language_selection/$isFromLanguage")
                                        } catch (e: Exception) {
                                            // Handle navigation error
                                        } finally {
                                            isNavigating = false
                                        }
                                    }
                                }
                            }
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
                }
            }
        }
    }
}