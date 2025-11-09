package com.zteam.zvision.presentation.ui.screens.translation

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zteam.zvision.presentation.viewmodel.TranslationOverlayViewModel
import com.zteam.zvision.utils.getDevicePhysicalOrientation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TextBlock(
    val text: String,
    val translatedText: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

@Composable
fun TranslationOverlayScreen(
    bitmap: Bitmap,
    onBack: () -> Unit,
    fromLanguage: String,
    toLanguage: String,
    viewModel: TranslationOverlayViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var showTranslations by remember { mutableStateOf(true) }
    val textBlocks by viewModel.textBlocks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var isSaving by remember { mutableStateOf(false) }
    var isLandscape by remember { mutableStateOf(false) }
    // Graphics layer to capture the image + translations Box
    val graphicsLayer = rememberGraphicsLayer()

    // Process image on first load
    LaunchedEffect(bitmap) {
        getDevicePhysicalOrientation(context) { degree ->
            viewModel.processImage(bitmap, 0, fromLanguage, toLanguage)
            isLandscape = degree == 90 || degree == 270
        }
    }

    // Function to save image to Pictures
    fun saveImageToGallery() {
        scope.launch {
            isSaving = true
            try {
                // Capture the rendered Box as bitmap on main thread
                val finalBitmap = if (showTranslations && textBlocks.isNotEmpty()) {
                    // Use the graphicsLayer to create bitmap
                    graphicsLayer.toImageBitmap().asAndroidBitmap()
                } else {
                    bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
                }

                val result = withContext(Dispatchers.IO) {
                    // Save to gallery
                    saveBitmapToMediaStore(context, finalBitmap)
                }

                withContext(Dispatchers.Main) {
                    if (result) {
                        Toast.makeText(context, "Image saved to Pictures", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            } finally {
                isSaving = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
    ) {
        // Box for image + translations that we'll capture
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    // Record the content into graphics layer
                    onDrawWithContent {
                        graphicsLayer.record {
                            this@onDrawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer)
                    }
                }
        ) {
            // Display the captured image without rotation
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Captured photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Overlay translations with proper scaling
            if (showTranslations && textBlocks.isNotEmpty()) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val screenWidth = constraints.maxWidth.toFloat()
                    val screenHeight = constraints.maxHeight.toFloat()
                    val imageWidth = bitmap.width.toFloat()
                    val imageHeight = bitmap.height.toFloat()

                    // Calculate scale to fit image in screen (ContentScale.Fit logic)
                    val scale = minOf(screenWidth / imageWidth, screenHeight / imageHeight)
                    val scaledImageWidth = imageWidth * scale
                    val scaledImageHeight = imageHeight * scale

                    // Calculate offset to center image
                    val offsetX = (screenWidth - scaledImageWidth) / 2f
                    val offsetY = (screenHeight - scaledImageHeight) / 2f

                    val fontSize = (12 * scale * 0.85f).coerceAtLeast(4f).coerceAtMost(12f)

                    textBlocks.forEach { block ->
                        // Scale and position the text block relative to the scaled image
                        val scaledX = ((block.x * scale) + offsetX) / density.density
                        val scaledY = ((block.y * scale) + offsetY) / density.density
                        val scaledWidth = (block.width * scale) / density.density
                        val scaledHeight = (block.height * scale) / density.density

                        Box(
                            modifier = Modifier
                                .offset(
                                    x = if (!isLandscape) scaledX.dp else (scaledX + scaledWidth).dp,
                                    y = scaledY.dp
                                )
                                .width(if (!isLandscape) scaledWidth.dp else scaledHeight.dp)
                                .height(if (!isLandscape) scaledHeight.dp else scaledWidth.dp)
                                .graphicsLayer {
                                    if (isLandscape) {
                                        rotationZ = 90f
                                        transformOrigin =
                                            TransformOrigin(0f, 0f) // rotate from top-left
                                    }
                                }
                                .background(Color.White.copy(alpha = 0.9f))
                                .padding(4.dp)
                        ) {
                            Text(
                                text = block.translatedText,
                                color = Color.Black,
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize * 1.1f).sp,
                                textAlign = TextAlign.Start,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // End of capturable Box
        }

        // Loading indicator (outside the captured Box)
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Top left back button (outside the captured Box)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.White.copy(alpha = 0.7f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Back",
                tint = Color.Black
            )
        }

        // Top right save button
        IconButton(
            onClick = { saveImageToGallery() },
            enabled = !isSaving && !isLoading,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.White.copy(alpha = 0.7f), CircleShape)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Color.Black
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Save to gallery",
                    tint = Color.Black
                )
            }
        }

        // Bottom toggle button
        Button(
            onClick = { showTranslations = !showTranslations },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text = if (showTranslations) "Hide Translations" else "Show Translations")
        }
    }
}

/**
 * Create a bitmap with translations overlaid
 */
//private fun createBitmapWithTranslations(
//    context: Context,
//    originalBitmap: Bitmap,
//    textBlocks: List<TextBlock>,
//    screenWidth: Float,
//    screenHeight: Float,
//    isLandscape: Boolean
//): Bitmap {
//    val resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
//    val canvas = Canvas(resultBitmap)
//
//    val imageWidth = originalBitmap.width.toFloat()
//    val imageHeight = originalBitmap.height.toFloat()
//    // Compute same scale/offset as in Compose (ContentScale.Fit)
//    val scale = minOf(screenWidth / imageWidth, screenHeight / imageHeight)
//    val scaledImageWidth = imageWidth * scale
//    val scaledImageHeight = imageHeight * scale
//    val offsetX = (screenWidth - scaledImageWidth) / 2f
//    val offsetY = (screenHeight - scaledImageHeight) / 2f
//
//    val backgroundPaint = Paint().apply {
//        color = android.graphics.Color.WHITE
//        style = Paint.Style.FILL
//        alpha = (0.9f * 255).toInt()
//    }
//
//    textBlocks.forEach { block ->
//        val padding = 8f
//
//        // Match scaling and translation from Compose
//        val scaledX = (block.x * scale) + offsetX
//        val scaledY = (block.y * scale) + offsetY
//        val scaledWidth = block.width * scale
//        val scaledHeight = block.height * scale
//
//        val fontSize = (12 * scale).coerceAtLeast(6f).coerceAtMost(12f) * 2.5f
//
//        val textPaint = TextPaint().apply {
//            color = android.graphics.Color.BLACK
//            textSize = fontSize
//            isAntiAlias = true
//        }
//
//        val staticLayout = StaticLayout.Builder.obtain(
//            block.translatedText,
//            0,
//            block.translatedText.length,
//            textPaint,
//            scaledWidth.toInt().coerceAtLeast(1)
//        )
//            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
//            .setLineSpacing(fontSize * 0.2f, 1.0f)
//            .setEllipsize(android.text.TextUtils.TruncateAt.END)
//            .build()
//
//        val textHeight = staticLayout.height.toFloat()
//        val actualBoxHeight = textHeight + (padding * 2)
//
//        if (isLandscape) {
//            // Rotate around the center of the text block
//            val centerX = scaledX + scaledWidth / 2f
//            val centerY = scaledY + scaledHeight / 2f
//
//            canvas.withRotation(90f, centerX, centerY) {
//                // Draw background
//                drawRect(
//                    centerX - actualBoxHeight / 2f,
//                    centerY - scaledWidth / 2f,
//                    centerX + actualBoxHeight / 2f,
//                    centerY + scaledWidth / 2f,
//                    backgroundPaint
//                )
//
//                // Draw text
//                withTranslation(
//                    centerX - actualBoxHeight / 2f + padding,
//                    centerY - scaledWidth / 2f + padding
//                ) {
//                    staticLayout.draw(this)
//                }
//
//            }
//        } else {
//            // Portrait or no rotation
//            canvas.drawRect(
//                scaledX,
//                scaledY,
//                scaledX + scaledWidth,
//                scaledY + actualBoxHeight,
//                backgroundPaint
//            )
//            canvas.withTranslation(scaledX + padding, scaledY + padding) {
//                staticLayout.draw(canvas)
//            }
//        }
//    }
//    return resultBitmap
//}

/**
 * Save bitmap to MediaStore (Pictures directory)
 */
private fun saveBitmapToMediaStore(context: Context, bitmap: Bitmap): Boolean {
    val filename = "translated_${System.currentTimeMillis()}.jpg"

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        ?: return false

    return try {
        resolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        }

        contentValues.clear()
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)

        true
    } catch (e: Exception) {
        resolver.delete(uri, null, null)
        false
    }
}
