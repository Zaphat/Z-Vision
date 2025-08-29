package com.zteam.zvision.ui.features.recognition

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

class TextRecognizerService(private val context: Context) {
    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    fun fromBitmap(bitmap: Bitmap, onResult: (String) -> Unit, onError: (Exception) -> Unit) {
        val inputImage = inputImageFromBitmap(bitmap)
        processInputImage(inputImage, onResult, onError)
    }

    fun fromUri(uri: Uri, onResult: (String) -> Unit, onError: (Exception) -> Unit) {
        val inputImage = inputImageFromUri(context, uri)
        processInputImage(inputImage, onResult, onError)
    }

    @OptIn(ExperimentalGetImage::class)
    fun fromImageProxy(
        imageProxy: ImageProxy,
        onResult: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val inputImage = inputImageFromImageProxy(imageProxy)
        processInputImage(inputImage, onResult, onError)
    }

    fun prettyText(text: Text): String {
        val formattedText = buildString {
            for (block in text.textBlocks) {
                val paragraph = block.lines.joinToString(" ") { it.text }
                appendLine(paragraph) // adds newline after each block
            }
        }
        return formattedText.trim()
    }

    private fun processInputImage(
        inputImage: InputImage?,
        onResult: (String) -> Unit,
        onError: ((Exception) -> Unit)? = null,
        onComplete: (() -> Unit)? = null
    ) {
        if (inputImage == null) {
            onError?.invoke(Exception("Invalid input image"))
            return
        }

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                onResult(visionText.text)
            }
            .addOnFailureListener { e ->
                onError?.invoke(e)
            }
            .addOnCompleteListener {
                onComplete?.invoke()
            }
    }

    private fun inputImageFromBitmap(bitmap: Bitmap): InputImage? {
        return InputImage.fromBitmap(bitmap, 0)
    }

    private fun inputImageFromUri(context: Context, uri: Uri): InputImage? {
        return try {
            InputImage.fromFilePath(context, uri)
        } catch (e: IOException) {
            null
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun inputImageFromImageProxy(imageProxy: ImageProxy): InputImage? {
        val mediaImage = imageProxy.image
        var inputImage: InputImage? = null
        if (mediaImage != null) {
            inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        }
        return inputImage
    }
}