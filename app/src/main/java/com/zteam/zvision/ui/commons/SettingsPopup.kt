package com.zteam.zvision.ui.commons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun SettingsPopup(
    expanded: Boolean,
    onDismiss: () -> Unit
) {
    val menuItems = listOf(
        "Option 1" to { println("Option 1 clicked") },
        "Option 2" to { println("Option 2 clicked") },
        "Option 3" to { println("Option 3 clicked") }
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        menuItems.forEach { (title, action) ->
            DropdownMenuItem(
                text = { Text(title, color = Color.White) },
                onClick = {
                    action()
                    onDismiss()
                }
            )
        }
    }
}

