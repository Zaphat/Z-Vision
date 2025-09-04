package com.zteam.zvision.presentation.ui.navigation

import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NavigationHelper(
    private val navController: NavController,
    private val isNavigating: () -> Boolean,
    private val setNavigating: (Boolean) -> Unit,
    private val coroutineScope: CoroutineScope
) {
    
    fun safePopBack() {
        if (!navController.popBackStack()) {
            navController.navigate("main") {
                popUpTo("main") { inclusive = false }
                launchSingleTop = true
            }
        }
    }
    
    private fun safeNavigate(route: String) {
        if (!isNavigating()) {
            setNavigating(true)
            coroutineScope.launch {
                try {
                    navController.navigate(route)
                } catch (_: Exception) {
                    // Handle navigation error silently
                } finally {
                    setNavigating(false)
                }
            }
        }
    }
    
    fun navigateToLanguageSelection(isFromLanguage: Boolean) {
        safeNavigate("language_selection/$isFromLanguage")
    }
    
    fun navigateToQrStorage() {
        safeNavigate("qr_storage")
    }
    
    fun navigateToQrCreation() {
        safeNavigate("qr_creation")
    }
}
