package com.zteam.zvision.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zteam.zvision.domain.usecase.TextRecognitionUsecase
import com.zteam.zvision.domain.usecase.TranslatorUsecase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TranslationViewModel @Inject constructor(
    private val translatorUsecase: TranslatorUsecase,
    private val textRecognitionUsecase: TextRecognitionUsecase
) :
    ViewModel() {

    private val _translatedText = MutableStateFlow<String?>(null)
    val translatedText = _translatedText.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading = _isLoading.asStateFlow()

    fun setLoading(state: Boolean) {
        _isLoading.value = state
    }

    suspend fun identifyLanguage(text: String): String {
        return translatorUsecase.identifyLanguage(text)
    }

    fun getSupportedLanguages(): List<String> {
        return translatorUsecase.getAllSupportedLanguages().toList()
    }

    fun translateFromUri(context: Context, uri: Uri, fromIso: String, toIso: String) {
        viewModelScope.launch {
            try {
                textRecognitionUsecase.fromUri(
                    context = context,
                    uri = uri,
                    onResult = { text ->
                        translate(
                            textRecognitionUsecase.prettyText(text),
                            fromIso,
                            toIso
                        )
                    },
                    onError = { e -> throw e }
                )
            } catch (e: Exception) {
                Log.e("TranslationViewModel", "TranslateFromUri error", e)
            }
        }
    }

    fun translateFromImage(image: ImageProxy, fromIso: String, toIso: String) {
        viewModelScope.launch {
            try {
                textRecognitionUsecase.fromImageProxy(
                    image,
                    onResult = { text ->
                        val fromIsoString = buildString {
                            for (block in text.textBlocks) {
                                for (line in block.lines) {
                                    for (element in line.elements) {
                                        Log.i(
                                            "TranslationViewModel",
                                            "recognizedLang: ${element.recognizedLanguage}, fromIso: $fromIso"
                                        )
                                        if (element.recognizedLanguage == fromIso) append("${element.text} ")
                                    }
                                }
                                appendLine()
                            }
                        }.trim()
                        translate(
                            fromIsoString, fromIso, toIso
                        )
                    },
                    onError = { e -> throw e },
                    onComplete = { image.close() })
            } catch (e: Exception) {
                Log.e("TranslationViewModel", "TranslateFromImage error", e)
            }
        }
    }

    fun translate(text: String, fromIso: String, toIso: String) {
        viewModelScope.launch {
            try {
                if (!translatorUsecase.isModelAvailableOffline(fromIso)) {
                    translatorUsecase.downloadModel(fromIso)
                }

                if (!translatorUsecase.isModelAvailableOffline(toIso)) {
                    translatorUsecase.downloadModel(toIso)
                }

                val result = translatorUsecase.translateText(text, fromIso, toIso)
                _translatedText.value = result
            } catch (e: Exception) {
                Log.e("TranslationViewModel", "Translate error", e)
            }
        }
    }
}
