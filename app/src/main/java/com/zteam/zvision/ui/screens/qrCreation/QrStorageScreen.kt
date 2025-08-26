package com.zteam.zvision.ui.screens.qrCreation

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.zteam.zvision.data.model.QrModel
import com.zteam.zvision.ui.features.qrCreation.QrCreationViewModel
import java.text.SimpleDateFormat
import java.util.Locale

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
            name = filterName.ifBlank { null },
            favorite = if (showOnlyFavorite) true else null
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                // Display the stored QR image directly from the database
                val bitmap = BitmapFactory.decodeByteArray(qr.content, 0, qr.content.size)
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Stored QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback: try to display as text-based QR if bitmap decoding fails
                    QRImage(content = qr.content.decodeToString().let { com.zteam.zvision.ui.features.qrCreation.TextQR(it) })
                }
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
