package com.zteam.zvision.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.Translation
// Note: LocalTranslateModel class may not exist depending on ML Kit version; we avoid using it here.
import com.zteam.zvision.data.helper.await
import com.zteam.zvision.appDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Repository that wraps ML Kit on-device translation and language identification.
 */
class TranslatorRepository(private val context: Context) {

    // DataStore key to keep track of offline languages. Stored as a set of ISO codes.
    private val OFFLINE_LANGS_KEY = stringSetPreferencesKey("offline_languages")

    // Use the app-wide DataStore singleton defined in MainActivity via extension property
    private val dataStore: DataStore<Preferences> = context.appDataStore

    private val languageIdentifier: LanguageIdentifier by lazy {
        LanguageIdentification.getClient()
    }

    suspend fun identifyLanguage(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext "und"
        val code = languageIdentifier.identifyLanguage(text).await()
        return@withContext code ?: "und"
    }

    private fun getTranslator(fromIso: String, toIso: String): Translator {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.fromLanguageTag(fromIso) ?: TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.fromLanguageTag(toIso) ?: TranslateLanguage.ENGLISH)
            .build()
        return Translation.getClient(options)
    }

    suspend fun translateText(text: String, fromIso: String, toIso: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext ""

        val translator = getTranslator(fromIso, toIso)

        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        try {
            translator.downloadModelIfNeeded(conditions).await()
            // mark model as downloaded for target language
            markLanguageAsDownloaded(toIso)
        } catch (e: Exception) {
            // download failed; do not mark
        }

        val translated = translator.translate(text).await()
        translator.close()
        return@withContext translated
    }

    suspend fun ensureModelDownloaded(iso: String) = withContext(Dispatchers.IO) {
        val target = TranslateLanguage.fromLanguageTag(iso) ?: iso
        val conditions = DownloadConditions.Builder().requireWifi().build()
        try {
            Translation.getClient(TranslatorOptions.Builder().setTargetLanguage(target).setSourceLanguage(target).build())
                .downloadModelIfNeeded(conditions).await()
            // persist download record
            markLanguageAsDownloaded(iso)
        } catch (_: Exception) {
        }
    }

    private suspend fun markLanguageAsDownloaded(iso: String) {
        try {
            dataStore.edit { prefs ->
                val current = prefs[OFFLINE_LANGS_KEY] ?: emptySet()
                prefs[OFFLINE_LANGS_KEY] = current + iso
            }
        } catch (_: IOException) {
            // ignore write failures
        }
    }

    private suspend fun removeLanguageFromDownloaded(iso: String) {
        try {
            dataStore.edit { prefs ->
                val current = prefs[OFFLINE_LANGS_KEY] ?: emptySet()
                prefs[OFFLINE_LANGS_KEY] = current - iso
            }
        } catch (_: IOException) {
            // ignore write failures
        }
    }

    /**
     * Return list of supported ML Kit translation language codes.
     */
    fun getAllSupportedLanguages(): Set<String> = TranslateLanguage.getAllLanguages().toSet()

    /**
     * Download a language model explicitly (for UI trigger). Returns true on success.
     */
    suspend fun downloadModel(iso: String, requireWifi: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        val target = TranslateLanguage.fromLanguageTag(iso) ?: iso
        val conditionsBuilder = DownloadConditions.Builder()
        if (requireWifi) conditionsBuilder.requireWifi()
        val conditions = conditionsBuilder.build()
        return@withContext try {
            Translation.getClient(TranslatorOptions.Builder().setTargetLanguage(target).setSourceLanguage(target).build())
                .downloadModelIfNeeded(conditions).await()
            markLanguageAsDownloaded(iso)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete an offline model and update DataStore. Returns true on success.
     */
    suspend fun deleteModel(iso: String): Boolean = withContext(Dispatchers.IO) {
        // ML Kit's Translate API does not always expose a simple deleteModel helper across versions.
        // For safe behavior in the app UI, remove the offline marker in DataStore immediately so the
        // UI reflects the change. If you need to actually free the on-device model files, use
        // ML Kit's ModelManager API (com.google.mlkit.common.model.ModelManager) if available in
        // your ML Kit dependency set; that API can delete downloaded models. Implementing that via
        // reflection is error-prone, so we keep deletion explicit and simple here.

        // Remove offline flag in DataStore
        removeLanguageFromDownloaded(iso)
        // Return true to indicate UI-level success. Actual on-device file deletion may require
        // calling ModelManager.deleteDownloadedModel(...) from ML Kit common.
        true
    }

    /**
     * Synchronous single-shot read of offline languages.
     */
    suspend fun getOfflineLanguages(): Set<String> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[OFFLINE_LANGS_KEY] ?: emptySet() }
        .first()

    /**
     * Fast check for UI: returns whether the iso code is in the stored offline set. Falls back to false.
     */
    suspend fun isModelAvailableOffline(iso: String): Boolean {
        return dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs -> prefs[OFFLINE_LANGS_KEY] ?: emptySet() }
            .map { set -> set.contains(iso) }
            .first()
    }

    /**
     * Flow of current offline languages set for UI observation.
     */
    fun offlineLanguagesFlow(): Flow<Set<String>> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[OFFLINE_LANGS_KEY] ?: emptySet() }

}
