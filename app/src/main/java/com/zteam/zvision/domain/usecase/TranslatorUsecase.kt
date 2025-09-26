package com.zteam.zvision.domain.usecase

import com.zteam.zvision.data.repository.TranslatorRepository
import javax.inject.Inject

class TranslatorUsecase @Inject constructor(
    private val repository: TranslatorRepository
) {
    suspend fun identifyLanguage(text: String) = repository.identifyLanguage(text)

    suspend fun translateText(text: String, fromIso: String, toIso: String) =
        repository.translateText(text, fromIso, toIso)

    suspend fun ensureModelDownloaded(iso: String) = repository.ensureModelDownloaded(iso)

    // Added wrappers so ViewModels don't access repository directly
    fun getAllSupportedLanguages() = repository.getAllSupportedLanguages()

    suspend fun getOfflineLanguages() = repository.getOfflineLanguages()

    suspend fun downloadModel(iso: String, requireWifi: Boolean = true) =
        repository.downloadModel(iso, requireWifi)

    suspend fun deleteModel(iso: String) = repository.deleteModel(iso)

    suspend fun isModelAvailableOffline(iso: String) = repository.isModelAvailableOffline(iso)
}
