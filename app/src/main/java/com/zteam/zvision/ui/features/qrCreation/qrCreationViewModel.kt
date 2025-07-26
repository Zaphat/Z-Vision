package com.zteam.zvision.ui.features.qrCreation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.zteam.zvision.domain.QrUsecase
import com.zteam.zvision.data.model.QrModel
import com.zteam.zvision.ui.features.qrCreation.QRContent
import com.zteam.zvision.ui.features.qrCreation.QRGenerator
import android.graphics.Bitmap
import java.util.*

class QrCreationViewModel(
    private val usecase: QrUsecase,
    private val qrGenerator: QRGenerator = QRGenerator()
) : ViewModel() {
    private val _qrList = MutableStateFlow<List<QrModel>>(emptyList())
    val qrList: StateFlow<List<QrModel>> = _qrList.asStateFlow()

    private val _selectedQr = MutableStateFlow<QrModel?>(null)
    val selectedQr: StateFlow<QrModel?> = _selectedQr.asStateFlow()

    private val _generatedBitmap = MutableStateFlow<Bitmap?>(null)
    val generatedBitmap: StateFlow<Bitmap?> = _generatedBitmap.asStateFlow()

    fun generateQrBitmap(
        content: QRContent,
        size: Int = 512,
        foregroundColor: Int = android.graphics.Color.BLACK,
        backgroundColor: Int = android.graphics.Color.WHITE
    ) {
        _generatedBitmap.value = qrGenerator.generateQRCode(
            content = content,
            size = size,
            foregroundColor = foregroundColor,
            backgroundColor = backgroundColor
        )
    }

    fun createAndSaveQr(name: String, content: QRContent, favorite: Boolean = false) {
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

    fun loadAllQrs() {
        viewModelScope.launch {
            _qrList.value = usecase.getAllQrs()
        }
    }

    fun filterQrs(name: String? = null, createdAt: Date? = null, favorite: Boolean? = null) {
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
}

