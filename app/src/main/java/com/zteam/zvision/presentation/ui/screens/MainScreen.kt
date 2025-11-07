// screens/MainScreen.kt
package com.zteam.zvision.presentation.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.zteam.zvision.R
import com.zteam.zvision.domain.model.QrDetection
import com.zteam.zvision.presentation.ui.components.CameraPermissionRequest
import com.zteam.zvision.presentation.ui.components.CameraQRPreview
import com.zteam.zvision.presentation.ui.components.CameraTranslationPreview
import com.zteam.zvision.presentation.ui.components.QrBoundingBoxOverlay
import com.zteam.zvision.presentation.ui.components.QrResultBottomSheet
import com.zteam.zvision.presentation.ui.components.SettingsDrawer
import com.zteam.zvision.presentation.viewmodel.QRViewModel
import com.zteam.zvision.presentation.viewmodel.TranslationViewModel
import com.zteam.zvision.presentation.viewmodel.TranslationOverlayViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@ExperimentalMaterial3Api
@Composable
fun MainScreen(
    selectingMode: String,
    onModeChange: (String) -> Unit,
    translateFromLanguage: String,
    translateToLanguage: String,
    onNavigateToLanguageSelection: (Boolean) -> Unit,
    onManageLanguagePackages: () -> Unit,
    onNavigateToQrStorage: () -> Unit,
    onOpenUrl: (String) -> Unit,
    scanningEnabled: Boolean,
    navController: NavHostController? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val qrViewModel: QRViewModel = hiltViewModel()
    var showResultSheet by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var copyEnabled by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val translationViewModel: TranslationViewModel = hiltViewModel()
    val translationOverlayViewModel: TranslationOverlayViewModel = hiltViewModel()

    val pickMedia = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (!scanningEnabled) return@rememberLauncherForActivityResult
        if (uri != null) {
            Log.d("PhotoPicker", "Selected URI: $uri")
            scope.launch {
                when (selectingMode) {
                    "QR" -> {
                        val decoded = qrViewModel.decodeFromUri(context, uri)
                        if (decoded != null) {
                            resultText = decoded
                            copyEnabled = true
                            showResultSheet = true
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "No QR code found in image",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
                    }

                    "Translate" -> {
                        translationViewModel.setLoading(true)
                        scope.launch {
                            val fromIso =
                                translationViewModel.identifyLanguage(translateFromLanguage)
                            val toIso = translationViewModel.identifyLanguage(translateToLanguage)
                            translationViewModel.translateFromUri(context, uri, fromIso, toIso)
                        }
                    }
                }
            }
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    // Camera permission state for live preview
    val hasCameraPermissionInitial = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    var hasCameraPermission by remember { mutableStateOf(hasCameraPermissionInitial) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(
                context,
                "Camera permission is required for live preview",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // State for overlay and preview size
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    var detection by remember { mutableStateOf<QrDetection?>(null) }
    var lastShownText by remember { mutableStateOf<String?>(null) }

    // State for image capture
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // Track previous scanning state to detect return from overlay
    var wasScanningDisabled by remember { mutableStateOf(false) }

    // Detect when we return from overlay screen and reset imageCapture
    LaunchedEffect(scanningEnabled, selectingMode) {
        if (scanningEnabled && wasScanningDisabled && selectingMode == "Translate") {
            // We just returned from overlay, reset imageCapture
            imageCapture = null
        }
        wasScanningDisabled = !scanningEnabled
    }

    val translatedText by translationViewModel.translatedText.collectAsState()

    LaunchedEffect(translatedText) {
        translatedText?.let { newText ->
            if (lastShownText != newText || !showResultSheet) {
                resultText = newText
                showResultSheet = true
                lastShownText = newText
            }
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Function to capture and navigate
    fun openCamera() {
        val capture = imageCapture ?: return

        // Create output file
        val photoFile = File(
            context.cacheDir,
            "captured_image_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Load bitmap from file and rotate to correct orientation
                    var bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    if (bitmap != null) {
                        // Rotate bitmap to correct orientation based on EXIF data
                        try {
                            val exif = ExifInterface(photoFile.absolutePath)
                            val orientation = exif.getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_NORMAL
                            )

                            translationOverlayViewModel.setImageRotation(orientation)

                            val matrix = Matrix()
                            when (orientation) {
                                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(
                                    -1f,
                                    1f
                                )

                                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                            }

                            if (!matrix.isIdentity) {
                                val rotatedBitmap = Bitmap.createBitmap(
                                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                                )
                                bitmap.recycle()
                                bitmap = rotatedBitmap
                            }
                        } catch (e: Exception) {
                            Log.e("MainScreen", "Failed to read EXIF data", e)
                        }

                        // Store in ViewModel and navigate
                        translationOverlayViewModel.setCapturedBitmap(bitmap)
                        navController?.navigate("translation_overlay")
                    } else {
                        Toast.makeText(context, "Failed to load captured image", Toast.LENGTH_SHORT)
                            .show()
                    }
                    // Clean up temp file
                    photoFile.delete()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("MainScreen", "Photo capture failed", exception)
                    Toast.makeText(context, "Photo capture failed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Responsive sizing (single source of truth)
    val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenW = with(density) { windowInfo.containerSize.width.toDp() }
    val screenH = with(density) { windowInfo.containerSize.height.toDp() }
    val base = minOf(screenW, screenH)
    val horizonPad = (screenW * 0.04f).coerceIn(8.dp, 24.dp)
    val smallBtnSize = (base * 0.08f).coerceIn(44.dp, 72.dp)      // square small buttons
    val mediumBtnSize = (base * 0.10f).coerceIn(56.dp, 96.dp)     // camera capture
    val wideBtnWidth = (screenW * 0.28f).coerceIn(96.dp, 160.dp)  // mode and top buttons width
    val wideBtnHeight = (base * 0.07f).coerceIn(40.dp, 64.dp)
    val iconOnlyPadding = (smallBtnSize * 0.16f)
    val headerIconSize = (base * 0.08f).coerceIn(24.dp, 40.dp)
    val bottomIconPadding = (mediumBtnSize * 0.14f)

    SettingsDrawer(
        drawerState = drawerState,
        onManageLanguages = onManageLanguagePackages
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Selection state for bottom mode buttons
            val isQRSelected = (selectingMode == "QR")
            val isTranslateSelected = (selectingMode == "Translate")

            if (showResultSheet) {
                QrResultBottomSheet(
                    resultText = resultText,
                    copyEnabled = copyEnabled,
                    onDismiss = {
                        showResultSheet = false
                        translationViewModel.setLoading(false)
                    },
                    sheetState = sheetState,
                    onOpenUrl = onOpenUrl
                )
            }

            // Settings header: use SpaceBetween and responsive sizes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizonPad, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (selectingMode) {
                    "QR" -> {
                        Button(
                            onClick = { onNavigateToQrStorage() },
                            modifier = Modifier.size(width = wideBtnWidth, height = wideBtnHeight),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "My QR",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                        }
                    }

                    "Translate" -> {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onNavigateToLanguageSelection(true) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(wideBtnHeight),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text(text = translateFromLanguage, fontSize = 16.sp)
                            }

                            Icon(
                                painter = painterResource(id = R.drawable.arrow_forward_24px),
                                contentDescription = "Translate Arrow",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(headerIconSize)
                            )

                            Button(
                                onClick = { onNavigateToLanguageSelection(false) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(wideBtnHeight),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text(text = translateToLanguage, fontSize = 16.sp)
                            }
                        }
                    }

                    else -> {
                        Spacer(Modifier.weight(1f))
                    }
                }

                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "QR More",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(headerIconSize)
                    )
                }
            }

            // Main content area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPermission && scanningEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { previewSize = it }
                    ) {
                        if (selectingMode == "QR") {
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
                        } else if (selectingMode == "Translate") {
                            CameraTranslationPreview(
                                modifier = Modifier.fillMaxSize(),
                                analyzer = { img ->
                                    // No auto-translate; just close the image
                                    img.close()
                                },
                                onImageCaptureUseCase = { capture ->
                                    imageCapture = capture
                                }
                            )
                        }
                    }
                } else if (!hasCameraPermission) {
                    CameraPermissionRequest(
                        onGrantPermission = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                } else {
                    // Scanning disabled (browser open): show nothing to ensure analyzers are stopped
                    Box(modifier = Modifier.fillMaxSize())
                }
            }

            // Gallery, Camera Capture, History buttons (responsive)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .padding(horizonPad / 2),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        if (scanningEnabled) {
                            pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                        }
                    },
                    modifier = Modifier.size(smallBtnSize),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.filter_24px),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(iconOnlyPadding),
                        contentDescription = "gallery_icon",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isTranslateSelected) {
                    Button(
                        onClick = { openCamera() },
                        modifier = Modifier.size(mediumBtnSize),
                        contentPadding = PaddingValues(0.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.brightness_1_24px),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottomIconPadding),
                            contentDescription = "camera_icon",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Button(
                    onClick = { viewHistoryQRScans() },
                    modifier = Modifier.size(smallBtnSize),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.history_2_24px),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(iconOnlyPadding),
                        contentDescription = "history_icon",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Bottom mode buttons (responsive)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizonPad / 2),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                Button(
                    onClick = { onModeChange("QR") },
                    modifier = Modifier.size(width = wideBtnWidth, height = wideBtnHeight),
                    contentPadding = PaddingValues(iconOnlyPadding),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isQRSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isQRSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
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
                    modifier = Modifier.size(width = wideBtnWidth, height = wideBtnHeight),
                    contentPadding = PaddingValues(iconOnlyPadding),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTranslateSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isTranslateSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = if (isTranslateSelected) ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp
                    ) else ButtonDefaults.buttonElevation(
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
}


fun viewHistoryQRScans() {
    // TODO: Implement navigation to history screen
}
