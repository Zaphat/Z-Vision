package com.zteam.zvision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
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
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZVisionTheme (dynamicColor = false){
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
                    composable("qr_creation") {
                        com.zteam.zvision.ui.screens.qrCreation.QrCreationScreen(
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
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val viewModel = remember {
                            val db = com.zteam.zvision.data.local.AppDatabase.getInstance(context)
                            val repo = com.zteam.zvision.data.repository.QrRepository(db.qrDao())
                            val usecase = com.zteam.zvision.domain.QrUsecase(repo)
                            com.zteam.zvision.ui.features.qrCreation.QrCreationViewModel(usecase)
                        }
                        com.zteam.zvision.ui.screens.qrCreation.QrStorageScreen(
                            viewModel = viewModel,
                            onBack = { safePopBack() },
                            onNavigateToQrCreation = {
                                if (!isNavigating) {
                                    isNavigating = true
                                    coroutineScope.launch {
                                        try {
                                            navController.navigate("qr_creation")
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
                }
            }
        }
    }
}
