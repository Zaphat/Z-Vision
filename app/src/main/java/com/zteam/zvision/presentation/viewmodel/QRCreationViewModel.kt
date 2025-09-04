package com.zteam.zvision.presentation.viewmodel

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zteam.zvision.data.generator.QRContent
import com.zteam.zvision.data.generator.QRGenerator
import com.zteam.zvision.data.local.entity.QrModel
import com.zteam.zvision.domain.usecase.QrUsecase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class QrCreationViewModel @Inject constructor(
    private val usecase: QrUsecase,
    private val qrGenerator: QRGenerator
) : ViewModel() {
    private val _qrList = MutableStateFlow<List<QrModel>>(emptyList())
    val qrList: StateFlow<List<QrModel>> = _qrList.asStateFlow()

    private val _selectedQr = MutableStateFlow<QrModel?>(null)
    val selectedQr: StateFlow<QrModel?> = _selectedQr.asStateFlow()

    private val _generatedBitmap = MutableStateFlow<Bitmap?>(null)
    val generatedBitmap: StateFlow<Bitmap?> = _generatedBitmap.asStateFlow()

    init {
        loadAllQrs()
    }

    fun generateQrBitmap(
        content: QRContent,
        size: Int = 512,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ) {
        _generatedBitmap.value = qrGenerator.generateQRCode(
            content = content,
            size = size,
            foregroundColor = foregroundColor,
            backgroundColor = backgroundColor
        )
    }

    fun createAndSaveQr(name: String, content: QRContent, favorite: kotlin.Boolean = false) {
        val qr = QrModel(
            id = UUID.randomUUID(),
            name = name,
            createdAt = Date(),
            content = content.toEncodedString().toByteArray(),
            favorite = favorite
        )
        viewModelScope.launch {
            usecase.insertQr(qr)
            loadAllQrs()
        }
    }

    fun createAndSaveQrWithImage(name: String, content: QRContent, imageBytes: ByteArray, favorite: kotlin.Boolean = false) {
        val qr = QrModel(
            id = UUID.randomUUID(),
            name = name,
            createdAt = Date(),
            content = imageBytes, // Store the complete QR image (with logo if present)
            favorite = favorite
        )
        viewModelScope.launch {
            usecase.insertQr(qr)
            loadAllQrs()
        }
    }

    fun loadAllQrs() {
        viewModelScope.launch {
            _qrList.value = usecase.getAllQrs()
        }
    }

    fun filterQrs(name: String? = null, createdAt: Date? = null, favorite: kotlin.Boolean? = null) {
        viewModelScope.launch {
            _qrList.value = usecase.filterSearch(name, createdAt, favorite)
        }
    }

    fun selectQrById(id: UUID) {
        viewModelScope.launch {
            _selectedQr.value = usecase.getQrById(id)
        }
    }

    fun deleteQr(qr: QrModel) {
        viewModelScope.launch {
            usecase.deleteQr(qr)
            loadAllQrs()
        }
    }

    fun toggleFavorite(qr: QrModel) {
        viewModelScope.launch {
            usecase.insertQr(qr.copy(favorite = !qr.favorite))
            loadAllQrs()
        }
    }
}
