// screens/MainScreen.kt
package com.zteam.zvision.ui.screens

import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import com.zteam.zvision.R
import com.zteam.zvision.data.model.QrDetection
import com.zteam.zvision.ui.commons.SettingsPopup
import com.zteam.zvision.ui.components.*
import com.zteam.zvision.ui.features.camera.CameraQRPreview
import com.zteam.zvision.ui.features.image.QRImageDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@ExperimentalMaterial3Api
@Composable
fun MainScreen(
    selectingMode: String,
    onModeChange: (String) -> Unit,
    translateFromLanguage: String,
    translateToLanguage: String,
    onNavigateToLanguageSelection: (Boolean) -> Unit,
    onNavigateToQrStorage: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showResultSheet by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var copyEnabled by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val pickMedia = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.d("PhotoPicker", "Selected URI: $uri")
            scope.launch {
                val decoded = withContext(Dispatchers.Default) {
                    QRImageDecoder.decodeFromUri(context, uri)
                }
                if (decoded != null) {
                    resultText = decoded
                    copyEnabled = true
                    showResultSheet = true
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No QR code found in image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    // Camera permission state for live preview
    val hasCameraPermissionInitial = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    var hasCameraPermission by remember { mutableStateOf(hasCameraPermissionInitial) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission is required for live preview", Toast.LENGTH_SHORT).show()
        }
    }

    // State for overlay and preview size
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    var detection by remember { mutableStateOf<QrDetection?>(null) }
    var lastShownText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Selection state for bottom mode buttons
        val isQRSelected = (selectingMode == "QR")
        val isTranslateSelected = (selectingMode == "Translate")

        if (showResultSheet) {
            QrResultBottomSheet(
                resultText = resultText,
                copyEnabled = copyEnabled,
                onDismiss = { showResultSheet = false },
                sheetState = sheetState
            )
        }

        // Settings header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 8.dp, bottom = 8.dp, top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectingMode == "Translate") {
                Button(
                    onClick = { onNavigateToLanguageSelection(true) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = translateFromLanguage, color = Color.White, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    painter = painterResource(id = R.drawable.arrow_forward_24px),
                    contentDescription = "Translate Arrow",
                    tint = Color.White,
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { onNavigateToLanguageSelection(false) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = translateToLanguage, color = Color.White, fontSize = 16.sp)
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "QR More",
                        tint = Color.White,
                    )
                }

                SettingsPopup(
                    expanded = menuExpanded,
                    onDismiss = { menuExpanded = false }
                )
            }
        }

        // Main content area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { previewSize = it }
                ) {
                    CameraQRPreview(
                        modifier = Modifier.fillMaxSize(),
                        onQrDetected = { det ->
                            detection = det
                            det?.let {
                                // Update the sheet when new content appears or when sheet is hidden
                                if (lastShownText != it.text || !showResultSheet) {
                                    resultText = it.text
                                    copyEnabled = true
                                    showResultSheet = true
                                    lastShownText = it.text
                                }
                            }
                        }
                    )
                    QrBoundingBoxOverlay(
                        viewSize = previewSize,
                        detection = detection
                    )
                }
            } else {
                CameraPermissionRequest(
                    onGrantPermission = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
        }

        // Gallery, Camera Capture, History buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    if (selectingMode != "QR") {
                        Toast.makeText(context, "TODO: Translate from image not implemented yet", Toast.LENGTH_SHORT).show()
                    } else {
                        pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                    }
                },
                modifier = Modifier
                    .padding(start = 35.dp, end = 10.dp)
                    .size(width = 50.dp, height = 50.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.filter_24px),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentDescription = "gallery_icon",
                )
            }
            // Camera capture button
            if (isTranslateSelected) {
                Button(
                    onClick = { openCamera() },
                    modifier = Modifier.size(width = 70.dp, height = 70.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.brightness_1_24px),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        contentDescription = "camera_icon",
                    )
                }
            }

            Button(
                onClick = { viewHistoryQRScans() },
                modifier = Modifier
                    .padding(start = 10.dp, end = 35.dp)
                    .size(width = 50.dp, height = 50.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.history_2_24px),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentDescription = "history_icon",
                )
            }
        }

        // QR, Translate, and Create QR buttons
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(
                onClick = { onModeChange("QR") },
                modifier = Modifier
                    .padding(start = 35.dp, end = 5.dp)
                    .size(width = 100.dp, height = 50.dp),
                contentPadding = PaddingValues(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isQRSelected) MaterialTheme.colorScheme.primary else Color.DarkGray,
                    contentColor = if (isQRSelected) MaterialTheme.colorScheme.onPrimary else Color.White
                ),
                elevation = if (isQRSelected) ButtonDefaults.buttonElevation(defaultElevation = 6.dp) else ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.qr_code_24px),
                    contentDescription = "QR More",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Button(
                onClick = { onModeChange("Translate") },
                modifier = Modifier
                    .padding(start = 5.dp, end = 35.dp)
                    .size(width = 100.dp, height = 50.dp),
                contentPadding = PaddingValues(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTranslateSelected) MaterialTheme.colorScheme.primary else Color.DarkGray,
                    contentColor = if (isTranslateSelected) MaterialTheme.colorScheme.onPrimary else Color.White
                ),
                elevation = if (isTranslateSelected) ButtonDefaults.buttonElevation(defaultElevation = 6.dp) else ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.g_translate_24px),
                    contentDescription = "QR More",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


fun viewHistoryQRScans() {
    // TODO: Implement navigation to history screen
}

fun openCamera() {
    // TODO: Implement camera opening
}
