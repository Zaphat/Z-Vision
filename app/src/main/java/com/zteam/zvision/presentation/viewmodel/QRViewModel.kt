package com.zteam.zvision.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.zteam.zvision.data.decoder.QrDecoder
import com.zteam.zvision.domain.model.QrDetection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

@HiltViewModel
class QRViewModel @Inject constructor() : ViewModel() {
    private val _detection = MutableStateFlow<QrDetection?>(null)
    val detection: StateFlow<QrDetection?> = _detection.asStateFlow()

    private val lastAnalyzedAt = AtomicLong(0L)
    private val lastDetAt = AtomicLong(0L)
    private var throttleMs: Long = 360L
    private val holdMs: Long = 360L

    fun setThrottleMs(value: Long) {
        throttleMs = value.coerceAtLeast(0L)
    }

    @OptIn(ExperimentalGetImage::class)
    fun analyze(imageProxy: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAnalyzedAt.get() < throttleMs) {
            imageProxy.close()
            return
        }
        lastAnalyzedAt.set(now)

        QrDecoder.analyze(imageProxy) { decoded ->
            val det = decoded ?: run {
                val age = SystemClock.elapsedRealtime() - lastDetAt.get()
                if (age <= holdMs) _detection.value else null
            }
            if (det != null) {
                _detection.value = det
                lastDetAt.set(SystemClock.elapsedRealtime())
            }
        }
    }

    suspend fun decodeFromUri(context: Context, uri: Uri): String? =
        withContext(Dispatchers.IO) { QrDecoder.decodeFromUri(context, uri) }

    fun clear() { _detection.value = null }

    override fun onCleared() {
        try { QrDecoder.close() } catch (_: Exception) {}
        super.onCleared()
    }
}
