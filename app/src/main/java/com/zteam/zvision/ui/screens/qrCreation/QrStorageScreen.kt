package com.zteam.zvision.ui.screens.qrCreation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zteam.zvision.ui.features.qrCreation.QrCreationViewModel
import com.zteam.zvision.data.model.QrModel
import com.zteam.zvision.ui.screens.qrCreation.QRImage
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete

@Composable
fun QrStorageScreen(
    viewModel: QrCreationViewModel,
    onBack: () -> Unit = {},
) {
    val qrList by viewModel.qrList.collectAsState()
    var filterName by remember { mutableStateOf("") }
    var filterFavorite by remember { mutableStateOf(false) }
    var showOnlyFavorite by remember { mutableStateOf(false) }

    LaunchedEffect(filterName, showOnlyFavorite) {
        viewModel.filterQrs(
            name = if (filterName.isBlank()) null else filterName,
            favorite = if (showOnlyFavorite) true else null
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = filterName,
                onValueChange = { filterName = it },
                label = { Text("Filter by name") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = showOnlyFavorite,
                onClick = { showOnlyFavorite = !showOnlyFavorite },
                label = { Text("Favorite") }
            )
        }
        Spacer(Modifier.height(16.dp))
        if (qrList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No saved QR codes.")
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(qrList, key = { it.id }) { qr ->
                    QrStorageItem(qr = qr, onDelete = { viewModel.deleteQr(qr) })
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.align(Alignment.End)) {
            Text("Back")
        }
    }
}

@Composable
fun QrStorageItem(qr: QrModel, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(64.dp)) {
                QRImage(content = qr.content.decodeToString().let { com.zteam.zvision.ui.features.qrCreation.TextQR(it) })
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(qr.name, style = MaterialTheme.typography.titleMedium)
                Text(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(qr.createdAt), style = MaterialTheme.typography.bodySmall)
                if (qr.favorite) Text("★ Favorite", color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete QR")
            }
        }
    }
} 