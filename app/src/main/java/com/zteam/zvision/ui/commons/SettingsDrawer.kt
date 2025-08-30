package com.zteam.zvision.ui.commons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zteam.zvision.appDataStore
import com.zteam.zvision.ui.theme.LocalThemeController
import com.zteam.zvision.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val menuItems = listOf(
        "Option 1" to { println("Option 1 clicked") },
        "Option 2" to { println("Option 2 clicked") },
        "Option 3" to { println("Option 3 clicked") }
    )

private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")

@Composable
fun SettingsDrawer(
    drawerState: DrawerState,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val themeController = LocalThemeController.current
    val context = LocalContext.current

    // Restore theme from DataStore on first composition
    LaunchedEffect(Unit) {
        val modeStr = context.appDataStore.data.first()[KEY_THEME_MODE]
        if (modeStr != null) {
            themeController.setMode(if (modeStr == ThemeMode.Dark.name) ThemeMode.Dark else ThemeMode.Light)
        }
    }

    // Force the drawer to appear on the right by switching to RTL only for the drawer container
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer( // tap on scrim dismisses automatically
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                // Keep drawer content LTR so items render normally
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        modifier = Modifier.fillMaxWidth(0.65f),
                        drawerShape = RectangleShape
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        // Appearance section: Dark mode toggle
                        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                            Text(
                                text = "Appearance",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Dark mode",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = themeController.mode == ThemeMode.Dark,
                                    onCheckedChange = { checked ->
                                        val newMode = if (checked) ThemeMode.Dark else ThemeMode.Light
                                        themeController.setMode(newMode)
                                        scope.launch {
                                            context.appDataStore.edit { it[KEY_THEME_MODE] = newMode.name }
                                        }
                                    }
                                )
                            }
                        }
                        // Existing menu items
                        menuItems.forEach { (title, action) ->
                            NavigationDrawerItem(
                                label = { Text(title) },
                                selected = false,
                                onClick = {
                                    action()
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    }
                }
            },
            content = {
                // Keep main content LTR
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    content()
                }
            }
        )
    }
}
