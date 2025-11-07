package com.zteam.zvision.presentation.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.media.ExifInterface
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zteam.zvision.domain.usecase.TextRecognitionUsecase
import com.zteam.zvision.domain.usecase.TranslatorUsecase
import com.zteam.zvision.presentation.ui.screens.translation.TextBlock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TranslationOverlayViewModel @Inject constructor(
    private val textRecognitionUsecase: TextRecognitionUsecase,
    private val translatorUsecase: TranslatorUsecase
) : ViewModel() {

    private val _textBlocks = MutableStateFlow<List<TextBlock>>(emptyList())
    val textBlocks = _textBlocks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var imageRotation: Int? = null

    // Hold the captured bitmap
    private var capturedBitmap: Bitmap? = null

    // Cache to avoid re-translating
    private var cachedBlocks: List<TextBlock>? = null

    fun setCapturedBitmap(bitmap: Bitmap) {
        capturedBitmap = bitmap
        // Clear cache when new bitmap is set
        cachedBlocks = null
        _textBlocks.value = emptyList()
    }

    fun setImageRotation(rotation: Int) {
        imageRotation = rotation
    }


    fun getCapturedBitmap(): Bitmap? = capturedBitmap
    fun getImageRotation(): Int? = imageRotation


    fun clearCapturedBitmap() {
        capturedBitmap?.recycle()
        capturedBitmap = null
        cachedBlocks = null
        _textBlocks.value = emptyList()
    }

    fun processImage(context: Context, bitmap: Bitmap, fromLanguage: String, toLanguage: String) {
        // Return cached if available
        if (cachedBlocks != null) {
            _textBlocks.value = cachedBlocks!!
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get language ISO codes
                val fromIso = translatorUsecase.identifyLanguage(fromLanguage)
                val toIso = translatorUsecase.identifyLanguage(toLanguage)

                // Recognize text from bitmap
                textRecognitionUsecase.fromBitmap(
                    bitmap = bitmap,
                    onResult = { visionText ->
                        viewModelScope.launch {
                            val blocks = mutableListOf<TextBlock>()

                            // Process each text block
                            for (textBlock in visionText.textBlocks) {
                                val boundingBox = textBlock.boundingBox
                                if (boundingBox != null) {
                                    // Translate the text
                                    val originalText = textBlock.text
                                    val translatedText = try {
                                        translatorUsecase.translateText(originalText, fromIso, toIso)
                                    } catch (e: Exception) {
                                        Log.e("TranslationOverlay", "Translation failed", e)
                                        originalText // Fallback to original
                                    }

                                    blocks.add(
                                        TextBlock(
                                            text = originalText,
                                            translatedText = translatedText,
                                            x = boundingBox.left.toFloat(),
                                            y = boundingBox.top.toFloat(),
                                            width = boundingBox.width().toFloat(),
                                            height = boundingBox.height().toFloat()
                                        )
                                    )
                                }
                            }

                            // Cache and update
                            cachedBlocks = blocks
                            _textBlocks.value = blocks
                            _isLoading.value = false
                        }
                    },
                    onError = { exception ->
                        Log.e("TranslationOverlay", "Text recognition failed", exception)
                        _isLoading.value = false
                    }
                )
            } catch (e: Exception) {
                Log.e("TranslationOverlay", "Process image failed", e)
                _isLoading.value = false
            }
        }
    }
}
