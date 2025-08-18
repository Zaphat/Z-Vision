package com.zteam.zvision.ui.screens.qrCreation

import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.LineHeightStyle

@Composable
fun QrStorageScreen(
    viewModel: QrCreationViewModel,
    onBack: () -> Unit = {},
    onNavigateToQrCreation: () -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme

    val qrList by viewModel.qrList.collectAsState()
    var filterName by remember { mutableStateOf("") }
    var showOnlyFavorite by remember { mutableStateOf(false) }

    LaunchedEffect(filterName, showOnlyFavorite) {
        viewModel.filterQrs(
            name = if (filterName.isBlank()) null else filterName,
            favorite = if (showOnlyFavorite) true else null
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(8.dp)
            .background(colorScheme.background) // background adapts to dark mode
    ) {
        // Search bar + Favorite
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = filterName,
                onValueChange = { filterName = it },
                placeholder = { Text("Filter by name", color = colorScheme.onSurfaceVariant) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (filterName.isNotEmpty()) {
                        IconButton(onClick = { filterName = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear",
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(50),
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colorScheme.surface,
                    unfocusedContainerColor = colorScheme.surface,
                    focusedTextColor = colorScheme.onSurface,
                    unfocusedTextColor = colorScheme.onSurface,
                    cursorColor = colorScheme.primary
                )
            )
            IconToggleButton(
                checked = showOnlyFavorite,
                onCheckedChange = { showOnlyFavorite = it }
            ) {
                Icon(
                    imageVector = if (showOnlyFavorite) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                    contentDescription = "Favorite",
                    tint = if (showOnlyFavorite) Color(0xFFFFD700) else colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // QR list
        Box(
            Modifier
                .fillMaxHeight(0.85f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (qrList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No saved QR codes.", color = colorScheme.onBackground)
                }
            } else {
                LazyColumn {
                    items(qrList, key = { it.id }) { qr ->
                        QrStorageItem(
                            qr = qr,
                            onFavorite = {},
                            onDelete = { viewModel.deleteQr(qr) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Navigation Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Back icon at start
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(48.dp),
                    tint = colorScheme.onBackground
                )
            }

            FloatingActionButton(
                onClick = onNavigateToQrCreation,
                containerColor = Color.DarkGray,
                contentColor = colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create QR code",
                    modifier = Modifier.size(28.dp) // smaller icon inside circle
                )
            }
        }
    }
}


@Composable
fun QrStorageItem(qr: QrModel,onFavorite: () -> Unit ,onDelete: () -> Unit) {
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
            IconButton(onClick = onFavorite) {
                Icon(Icons.Default.Favorite, contentDescription = "Favorite QR")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete QR")
            }
        }
    }
} 