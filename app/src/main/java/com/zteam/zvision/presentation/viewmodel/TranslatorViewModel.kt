package com.zteam.zvision.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zteam.zvision.domain.usecase.TranslatorUsecase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TranslationViewModel @Inject constructor(
    private val translatorUsecase: TranslatorUsecase
) : ViewModel() {

    private val _translatedText = MutableStateFlow<String?>(null)
    val translatedText = _translatedText.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    suspend fun identifyLang(text: String): String {
        return translatorUsecase.identifyLanguage(text)
    }

    suspend fun translate(text: String, fromIso: String, toIso: String) {
        _loading.value = true
        _error.value = null
        _translatedText.value = null

        try {
            if (!translatorUsecase.isModelAvailableOffline(fromIso)) {
                translatorUsecase.downloadModel(fromIso)
            }

            if (!translatorUsecase.isModelAvailableOffline(toIso)) {
                translatorUsecase.downloadModel(toIso)
            }

            val result = translatorUsecase.translateText(text, fromIso, toIso)
            Log.i("CameraTranslationPreview", "translate result: $result")
            _translatedText.value = result
        } catch (e: Exception) {
            _error.value = e.message
        } finally {
            _loading.value = false
        }
    }

    fun getSupportedLanguages(): List<String> {
        return translatorUsecase.getAllSupportedLanguages().toList()
    }
}
