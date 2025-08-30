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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zteam.zvision.appDataStore
import com.zteam.zvision.data.model.QrModel
import com.zteam.zvision.ui.features.qrCreation.QrCreationViewModel
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Locale

private val KEY_QR_FILTER_NAME = stringPreferencesKey("qr_filter_name")
private val KEY_QR_ONLY_FAV = booleanPreferencesKey("qr_show_only_fav")

@Composable
fun QrStorageScreen(
    viewModel: QrCreationViewModel,
    onBack: () -> Unit = {},
    onNavigateToQrCreation: () -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    // Responsive sizing
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenW = with(density) { windowInfo.containerSize.width.toDp() }
    val screenH = with(density) { windowInfo.containerSize.height.toDp() }
    val base = minOf(screenW, screenH)
    val thumbSize = (screenW * 0.18f).coerceIn(48.dp, 96.dp)
    val navIconSize = (base * 0.09f).coerceIn(36.dp, 56.dp)
    val fabIconSize = (navIconSize * 0.6f).coerceIn(20.dp, 36.dp)
    val listActionIconSize = (base * 0.07f).coerceIn(20.dp, 32.dp)

    val qrList by viewModel.qrList.collectAsState()
    var filterName by remember { mutableStateOf("") }
    var showOnlyFavorite by remember { mutableStateOf(false) }

    // Restore saved filters
    LaunchedEffect(Unit) {
        val prefs = context.appDataStore.data.first()
        filterName = prefs[KEY_QR_FILTER_NAME] ?: ""
        showOnlyFavorite = prefs[KEY_QR_ONLY_FAV] ?: false
    }

    // Persist on change
    LaunchedEffect(filterName) {
        context.appDataStore.edit { it[KEY_QR_FILTER_NAME] = filterName }
    }
    LaunchedEffect(showOnlyFavorite) {
        context.appDataStore.edit { it[KEY_QR_ONLY_FAV] = showOnlyFavorite }
    }

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
                        tint = colorScheme.onSurface
                    )
                },
                trailingIcon = {
                    if (filterName.isNotEmpty()) {
                        IconButton(onClick = { filterName = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear",
                                tint = colorScheme.onSurface
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
                    tint = colorScheme.onSurface
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
                            onDelete = { viewModel.deleteQr(qr) },
                            thumbSize = thumbSize,
                            actionIconSize = listActionIconSize
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
                    modifier = Modifier.size(navIconSize),
                    tint = colorScheme.onSurface
                )
            }

            FloatingActionButton(
                onClick = onNavigateToQrCreation,
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create QR code",
                    modifier = Modifier.size(fabIconSize)
                )
            }
        }
    }
}

@Composable
fun QrStorageItem(
    qr: QrModel,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    thumbSize: Dp,
    actionIconSize: Dp
) {
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
            Box(Modifier.size(thumbSize)) {
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
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "Favorite QR",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(actionIconSize)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete QR",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(actionIconSize)
                )
            }
        }
    }
}
