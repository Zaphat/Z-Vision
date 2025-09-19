package com.zteam.zvision.presentation.ui.screens.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zteam.zvision.presentation.viewmodel.ManageLanguagesViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import java.util.Locale

@Composable
fun ManageLanguagesScreen(
    onBack: () -> Unit,
    viewModel: ManageLanguagesViewModel = hiltViewModel()
) {
    val all = viewModel.all.collectAsState()
    val offline = viewModel.offline.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text(text = "Manage Language Packages", style = MaterialTheme.typography.titleLarge)

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
            items(all.value.toList().sorted()) { iso ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val displayName = try {
                        val name = Locale.forLanguageTag(iso).getDisplayName(Locale.getDefault())
                        if (name.isNullOrBlank()) iso else name
                    } catch (e: Exception) {
                        iso
                    }
                    Text(text = displayName)
                    if (offline.value.contains(iso)) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Downloaded")
                    } else {
                        Button(onClick = { viewModel.download(iso) }) {
                            Text(text = "Download")
                        }
                    }
                }
            }
        }
    }
}
