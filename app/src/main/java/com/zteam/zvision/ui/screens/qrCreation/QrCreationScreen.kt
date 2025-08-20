package com.zteam.zvision.ui.screens.qrCreation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zteam.zvision.ui.features.qrCreation.*
import com.zteam.zvision.data.local.AppDatabase
import com.zteam.zvision.data.repository.QrRepository
import com.zteam.zvision.domain.QrUsecase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.asImageBitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.MediaStore
import java.io.ByteArrayOutputStream

@Composable
fun QrCreationScreen(
    onBack: () -> Unit = {},
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
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("URL") }
    var content by remember { mutableStateOf("") }
    var favorite by remember { mutableStateOf(false) }
    var showToast by remember { mutableStateOf<String?>(null) }
    val generatedBitmap by viewModel.generatedBitmap.collectAsState()

    // Logo selection state
    var selectedLogoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedLogoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var finalQrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val qrGenerator = remember { QRGenerator() }

    val pickLogoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedLogoUri = uri
        finalQrBitmap = null // reset preview when logo changes
        if (uri != null) {
            selectedLogoBitmap = decodeBitmapFromUri(context, uri)
            if (selectedLogoBitmap != null) {
                showToast = "Logo selected: ${selectedLogoBitmap!!.width}x${selectedLogoBitmap!!.height}"
            } else {
                showToast = "Failed to load logo image"
            }
        } else {
            selectedLogoBitmap = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create QR Code", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("QR Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Type:")
            Spacer(Modifier.width(8.dp))
            DropdownMenuBox(type, onTypeChange = { type = it })
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Content") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done)
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = favorite, onCheckedChange = { favorite = it })
            Text("Favorite")
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { pickLogoLauncher.launch("image/*") }) { Text("Choose Logo") }
            Spacer(Modifier.width(8.dp))
            if (selectedLogoUri != null) {
                AssistChip(onClick = { 
                    selectedLogoUri = null; 
                    selectedLogoBitmap = null; 
                    finalQrBitmap = null 
                }, label = { Text("Clear logo") })
            } else {
                AssistChip(onClick = { /* no-op */ }, enabled = false, label = { Text("No logo selected") })
            }
        }
        Spacer(Modifier.height(16.dp))
        Row {
            Button(onClick = {
                val qrContent = when (type) {
                    "URL" -> if (content.isNotBlank()) UrlQR(content) else null
                    "Text" -> if (content.isNotBlank()) TextQR(content) else null
                    else -> null
                }
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
                                content = qrContent,
                                logoBitmap = selectedLogoBitmap!!
                            )
                            if (finalQrBitmap == null) {
                                showToast = "Failed to generate QR with logo, falling back to regular QR"
                                // Fallback to regular QR
                                finalQrBitmap = qrGenerator.generateQRCode(content = qrContent)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            showToast = "Error generating QR with logo, falling back to regular QR"
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
            }) {
                Text("Generate QR")
            }
            Spacer(Modifier.width(16.dp))
            Button(onClick = {
                val qrContent = when (type) {
                    "URL" -> if (content.isNotBlank()) UrlQR(content) else null
                    "Text" -> if (content.isNotBlank()) TextQR(content) else null
                    else -> null
                }
                if (name.isNotBlank() && qrContent != null && finalQrBitmap != null) {
                    // Convert bitmap to byte array for storage
                    val byteArray = bitmapToByteArray(finalQrBitmap!!)
                    viewModel.createAndSaveQrWithImage(name, qrContent, byteArray, favorite)
                    showToast = "QR saved!"
                } else {
                    showToast = "Fill all fields and generate QR first"
                }
            }) {
                Text("Save QR")
            }
        }
        Spacer(Modifier.height(24.dp))
        if (finalQrBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = finalQrBitmap!!.asImageBitmap(),
                contentDescription = "Generated QR",
                modifier = Modifier.size(200.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onNavigateToQrStorage, modifier = Modifier.fillMaxWidth()) {
            Text("Storage")
        }
        if (showToast != null) {
            LaunchedEffect(showToast) {
                kotlinx.coroutines.delay(1500)
                showToast = null
            }
            Snackbar { Text(showToast!!) }
        }
    }
}

@Composable
private fun DropdownMenuBox(selected: String, onTypeChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }) {
            Text(selected)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("URL") }, onClick = {
                onTypeChange("URL"); expanded = false
            })
            DropdownMenuItem(text = { Text("Text") }, onClick = {
                onTypeChange("Text"); expanded = false
            })
        }
    }
}

private fun ensureCompatibleBitmap(bitmap: Bitmap): Bitmap {
    return when (bitmap.config) {
        android.graphics.Bitmap.Config.HARDWARE -> {
            val compatibleBitmap = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
            bitmap.recycle()
            compatibleBitmap
        }
        android.graphics.Bitmap.Config.RGB_565 -> {
            val compatibleBitmap = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
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
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
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