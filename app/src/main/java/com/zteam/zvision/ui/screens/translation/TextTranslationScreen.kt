package com.zteam.zvision.ui.screens.translation

import android.content.ClipData
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zteam.zvision.ui.features.recognition.TextRecognizerService

@Composable
fun RecognitionResultScreen(
    imageProxy: ImageProxy
) {
    val context = LocalContext.current
    var recognizedText by remember { mutableStateOf("Processing...") }
    val textRecognizerService = remember { TextRecognizerService(context) }
    val clipboardManager = LocalClipboardManager.current
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageProxy) {
        textRecognizerService.fromImageProxy(
            imageProxy,
            onResult = { visionText ->
                recognizedText = textRecognizerService.prettyText(visionText)
                previewBitmap = imageProxy.toBitmap()
                imageProxy.close()
            },
            onError = { e ->
                recognizedText = "Error: ${e.message}"
                previewBitmap = imageProxy.toBitmap()
                imageProxy.close()
            }
        )
    }

    Scaffold(
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Show preview if available
            previewBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Captured Image",
                    modifier = Modifier
                        .size(120.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Box with recognized text (clickable + selectable)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    SelectionContainer(
                        modifier = Modifier.clickable {
                            val clipData = ClipData.newPlainText("Recognized text", recognizedText)
                            val clipEntry = ClipEntry(clipData)
                            clipboardManager.setClip(clipEntry)
                        }
                    ) {
                        Text(
                            text = recognizedText,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}