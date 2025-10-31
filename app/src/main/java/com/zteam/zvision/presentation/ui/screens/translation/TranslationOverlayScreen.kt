package com.zteam.zvision.presentation.ui.screens.translation

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.withTranslation
import androidx.hilt.navigation.compose.hiltViewModel
import com.zteam.zvision.presentation.viewmodel.TranslationOverlayViewModel
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
    viewModel: TranslationOverlayViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var showTranslations by remember { mutableStateOf(true) }
    val textBlocks by viewModel.textBlocks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var isSaving by remember { mutableStateOf(false) }

    // Process image on first load
    LaunchedEffect(bitmap) {
        viewModel.processImage(context, bitmap, fromLanguage, toLanguage)
    }

    // Function to save image to Pictures
    fun saveImageToGallery() {
        scope.launch {
            isSaving = true
            try {
                val result = withContext(Dispatchers.IO) {
                    // Create bitmap with or without translations
                    val finalBitmap = if (showTranslations && textBlocks.isNotEmpty()) {
                        createBitmapWithTranslations(bitmap, textBlocks)
                    } else {
                        bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
                    }

                    // Save to gallery
                    saveBitmapToMediaStore(context, finalBitmap)
                }

                withContext(Dispatchers.Main) {
                    if (result) {
                        Toast.makeText(context, "Image saved to Pictures", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
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
                
                textBlocks.forEach { block ->
                    // Scale and position the text block relative to the scaled image
                    val scaledX = (block.x * scale) + offsetX
                    val scaledY = (block.y * scale) + offsetY
                    val scaledWidth = block.width * scale
                    val scaledHeight = block.height * scale
                    
                    // Calculate font size based on original text height to match detected text size
                    val fontSize = (block.height * scale * 0.7f).coerceAtLeast(10f).coerceAtMost(32f)
                    
                    Box(
                        modifier = Modifier
                            .offset(x = (scaledX / density.density).dp, y = (scaledY / density.density).dp)
                            .width((scaledWidth / density.density).dp)
                            .wrapContentHeight()
                            .background(Color.White.copy(alpha = 0.9f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = block.translatedText,
                            color = Color.Black,
                            fontSize = fontSize.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            textAlign = TextAlign.Start,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = (fontSize * 1.2f).sp
                        )
                    }
                }
            }
        }

        // Loading indicator
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Top left back button
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
                    imageVector = Icons.Default.KeyboardArrowDown,
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
private fun createBitmapWithTranslations(originalBitmap: Bitmap, textBlocks: List<TextBlock>): Bitmap {
    val resultBitmap = originalBitmap.copy(originalBitmap.config ?: Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(resultBitmap)
    
    val backgroundPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
        alpha = (0.9f * 255).toInt()
    }
    
    textBlocks.forEach { block ->
        val padding = 8f
        val boxWidth = block.width - (padding * 2)
        
        // Calculate font size based on original text height to match detected text size
        val fontSize = (block.height * 0.7f).coerceAtLeast(10f).coerceAtMost(32f)
        
        val textPaint = TextPaint().apply {
            color = android.graphics.Color.BLACK
            textSize = fontSize
            isAntiAlias = true
        }
        
        // Create StaticLayout for proper text wrapping
        val staticLayout = StaticLayout.Builder.obtain(
            block.translatedText,
            0,
            block.translatedText.length,
            textPaint,
            boxWidth.toInt().coerceAtLeast(1)
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(fontSize * 0.2f, 1.0f)
            .setMaxLines(5)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()
        
        // Calculate actual text height needed
        val textHeight = staticLayout.height.toFloat()
        val actualBoxHeight = textHeight + (padding * 2)
        
        // Draw white background
        canvas.drawRect(
            block.x,
            block.y,
            block.x + block.width,
            block.y + actualBoxHeight,
            backgroundPaint
        )
        
        // Draw text with StaticLayout
        canvas.withTranslation(block.x + padding, block.y + padding) {
            staticLayout.draw(canvas)
        }
    }
    
    return resultBitmap
}

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
