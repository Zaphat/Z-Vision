package com.zteam.zvision.ui.features.translation

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class TextTranslatorService(
    private val context: Context,
    sourceLang: String,
    targetLang: String
) {
    private val translator: Translator by lazy {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.VIETNAMESE)
            .build()
        Translation.getClient(options)
    }

    private fun prepareModel(onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        var conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }

    fun translate(
        text: String,
        onResult: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        prepareModel(
            onSuccess = {
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        onResult(translatedText)
                    }
                    .addOnFailureListener { e -> onError(e) }
            },
            onError = { e -> onError(e) }
        )
    }

    fun close() {
        translator.close()
    }
}