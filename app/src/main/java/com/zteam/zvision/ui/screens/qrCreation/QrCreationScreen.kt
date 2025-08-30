package com.zteam.zvision.ui.screens.qrCreation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.zteam.zvision.appDataStore
import com.zteam.zvision.data.local.AppDatabase
import com.zteam.zvision.data.repository.QrRepository
import com.zteam.zvision.domain.QrUsecase
import com.zteam.zvision.ui.features.qrCreation.QRGenerator
import com.zteam.zvision.ui.features.qrCreation.QrCreationViewModel
import com.zteam.zvision.ui.features.qrCreation.TextQR
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream

private val KEY_QR_DEFAULT_FAVORITE = booleanPreferencesKey("qr_default_favorite")

@Composable
fun QrCreationScreen(
    onNavigateToQrStorage: () -> Unit = {},
) {
    val context = LocalContext.current
    // ViewModel creation (not recommended for prod, but matches Fragment)
    val viewModel = remember {
        val db = AppDatabase.getInstance(context)
        val repo = QrRepository(db.qrDao())
        val usecase = QrUsecase(repo)
        QrCreationViewModel(usecase)
    }
    var name by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var favorite by remember { mutableStateOf(false) }
    var showToast by remember { mutableStateOf<String?>(null) }

    // Logo selection state
    var selectedLogoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedLogoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var finalQrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val qrGenerator = remember { QRGenerator() }

    var showDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    val pickLogoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedLogoUri = uri
        finalQrBitmap = null // reset preview when logo changes
        if (uri != null) {
            selectedLogoBitmap = decodeBitmapFromUri(context, uri)
            showToast = if (selectedLogoBitmap != null) {
                "Logo selected: ${selectedLogoBitmap!!.width}x${selectedLogoBitmap!!.height}"
            } else {
                "Failed to load logo image"
            }
        } else {
            selectedLogoBitmap = null
        }
    }

    // Restore default favorite
    LaunchedEffect(Unit) {
        favorite = context.appDataStore.data.first()[KEY_QR_DEFAULT_FAVORITE] ?: false
    }

    // Persist whenever favorite changes
    LaunchedEffect(favorite) {
        context.appDataStore.edit { it[KEY_QR_DEFAULT_FAVORITE] = favorite }
    }

    // Responsive sizing for preview
    val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenW = with(density) { windowInfo.containerSize.width.toDp() }
    val screenH = with(density) { windowInfo.containerSize.height.toDp() }
    val base = minOf(screenW, screenH)
    val previewSize = (base * 0.5f).coerceIn(180.dp, 320.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create QR Code", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Checkbox(
                checked = favorite, onCheckedChange = { favorite = it })
            Text("Favorite")
        }

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Content") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text, imeAction = ImeAction.Done
            ),
        )

        Spacer(Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { pickLogoLauncher.launch("image/*") },
            ) { Text("Choose Logo") }
            if (selectedLogoUri != null) {
                AssistChip(
                    onClick = {
                        selectedLogoUri = null
                        selectedLogoBitmap = null
                        finalQrBitmap = null
                    },
                    label = { Text("Clear logo") },
                )
            } else {
                AssistChip(
                    onClick = { /* no-op */ },
                    enabled = false,
                    label = { Text("No logo selected") },
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    val qrContent = if (content.isNotBlank()) TextQR(content) else null
                    if (qrContent != null) {
                        if (selectedLogoBitmap != null) {
                            // Validate logo bitmap before generation
                            if (selectedLogoBitmap!!.isRecycled) {
                                showToast = "Logo bitmap is invalid, please select again"
                                selectedLogoBitmap = null
                                return@Button
                            }

                            // Generate QR with logo
                            try {
                                finalQrBitmap = qrGenerator.generateQRCodeWithLogo(
                                    content = qrContent, logoBitmap = selectedLogoBitmap!!
                                )
                                if (finalQrBitmap == null) {
                                    showToast =
                                        "Failed to generate QR with logo, falling back to regular QR"
                                    // Fallback to regular QR
                                    finalQrBitmap = qrGenerator.generateQRCode(content = qrContent)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                showToast =
                                    "Error generating QR with logo, falling back to regular QR"
                                // Fallback to regular QR
                                finalQrBitmap = qrGenerator.generateQRCode(content = qrContent)
                            }
                        } else {
                            // Generate regular QR
                            try {
                                finalQrBitmap = qrGenerator.generateQRCode(content = qrContent)
                                if (finalQrBitmap == null) {
                                    showToast = "Failed to generate QR"
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                showToast = "Error generating QR: ${e.message}"
                            }
                        }
                    } else {
                        showToast = "Invalid content"
                    }
                },
            ) {
                Text("Generate QR")
            }
            Button(
                onClick = {
                    if (finalQrBitmap == null) {
                        showToast = "Generate QR first"
                    } else showDialog = true
                },
            ) {
                Text("Save QR")
            }
        }

        LaunchedEffect(name) {
            val qrContent = if (content.isNotBlank()) TextQR(content) else null
            if (finalQrBitmap != null && name.isNotBlank() && qrContent != null) {
                // Convert bitmap to byte array for storage
                val byteArray = bitmapToByteArray(finalQrBitmap!!)
                viewModel.createAndSaveQrWithImage(name, qrContent, byteArray, favorite)
                showToast = "QR saved!"
            }
        }

        Spacer(Modifier.height(24.dp))

        if (finalQrBitmap != null) {
            Image(
                bitmap = finalQrBitmap!!.asImageBitmap(),
                contentDescription = "Generated QR",
                modifier = Modifier.size(previewSize)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(previewSize)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                    )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateToQrStorage,
        ) {
            Text("Go to storage")
        }

        Spacer(modifier = Modifier.weight(1f))

        if (showToast != null) {
            LaunchedEffect(showToast) {
                kotlinx.coroutines.delay(3000)
                showToast = null
            }
            Snackbar { Text(showToast!!) }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false; inputText = "" },
                title = { Text("Enter QR Name") },
                text = {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("QR Name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (inputText.isNotBlank()) {
                            name = inputText
                            showDialog = false
                            inputText = "" // clear after submit
                        } else showToast = "Enter the QR name"
                    }) {
                        Text("Submit")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        showDialog = false
                        inputText = ""
                    }) {
                        Text("Cancel")
                    }
                })
        }
    }
}

private fun ensureCompatibleBitmap(bitmap: Bitmap): Bitmap {
    return when (bitmap.config) {
        Bitmap.Config.HARDWARE -> {
            val compatibleBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            bitmap.recycle()
            compatibleBitmap
        }

        Bitmap.Config.RGB_565 -> {
            val compatibleBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            bitmap.recycle()
            compatibleBitmap
        }

        else -> bitmap
    }
}

private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

        // Ensure bitmap is in a compatible format
        originalBitmap?.let { ensureCompatibleBitmap(it) }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}
