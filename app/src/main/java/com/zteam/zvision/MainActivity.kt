package com.zteam.zvision

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.datastore.preferences.preferencesDataStore
import com.zteam.zvision.presentation.ui.navigation.ZVisionNavigation
import com.zteam.zvision.presentation.ui.theme.ZVisionTheme
import dagger.hilt.android.AndroidEntryPoint

// Shared singleton DataStore for the whole app
val Context.appDataStore by preferencesDataStore(name = "zv_prefs")

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZVisionTheme {
                ZVisionNavigation()
            }
        }
    }
}
