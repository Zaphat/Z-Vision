package com.zteam.zvision.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import okio.IOException
import javax.inject.Inject

class TextRecognitionUsecase @Inject constructor(
) {
    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    fun fromBitmap(
        bitmap: Bitmap,
        rotationDegrees: Int = 0,
        onResult: (Text) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
        processInputImage(inputImage, onResult, onError)
    }

    fun fromUri(
        context: Context,
        uri: Uri,
        onResult: (Text) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val inputImage = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: IOException) {
            null
        }
        processInputImage(inputImage, onResult, onError)
    }

    @OptIn(ExperimentalGetImage::class)
    fun fromImageProxy(
        imageProxy: ImageProxy,
        onResult: (Text) -> Unit,
        onError: ((Exception) -> Unit)? = null,
        onComplete: (() -> Unit)? = null
    ) {
        val mediaImage = imageProxy.image
        val inputImage = if (mediaImage != null) {
            InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        } else null

        processInputImage(inputImage, onResult, onError, onComplete)
    }

    fun prettyText(text: Text): String {
        return buildString {
            for (block in text.textBlocks) {
                val paragraph = block.lines.joinToString(" ") { it.text }
                appendLine(paragraph)
            }
        }.trim()
    }

    private fun processInputImage(
        inputImage: InputImage?,
        onResult: (Text) -> Unit,
        onError: ((Exception) -> Unit)? = null,
        onComplete: (() -> Unit)? = null
    ) {
        if (inputImage == null) {
            onError?.invoke(Exception("Invalid input image"))
            onComplete?.invoke()
            return
        }

        recognizer
            .process(inputImage)
            .addOnSuccessListener { visionText -> onResult(visionText) }
            .addOnFailureListener { e -> onError?.invoke(e) }
            .addOnCompleteListener { onComplete?.invoke() }
    }
}