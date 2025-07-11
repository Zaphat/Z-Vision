package com.zteam.zvision.domain

import com.zteam.zvision.data.model.qrModel
import com.zteam.zvision.data.repository.QrRepository
import java.util.*

class QrUsecase(private val repository: QrRepository) {
    suspend fun insertQr(qr: qrModel) = repository.insertQr(qr)
    suspend fun getQrById(id: UUID) = repository.getQrById(id)
    suspend fun getAllQrs() = repository.getAllQrs()
    suspend fun deleteQr(qr: qrModel) = repository.deleteQr(qr)
    suspend fun filterSearch(
        name: String? = null,
        createdAt: Date? = null,
        favorite: Boolean? = null
    ) = repository.filterSearch(name, createdAt, favorite)
}

