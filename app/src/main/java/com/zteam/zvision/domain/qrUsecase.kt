package com.zteam.zvision.domain

import com.zteam.zvision.data.model.QrModel
import com.zteam.zvision.data.repository.QrRepository
import java.util.*

class QrUsecase(private val repository: QrRepository) {
    suspend fun insertQr(qr: QrModel) = repository.insertQr(qr)
    suspend fun getQrById(id: UUID) = repository.getQrById(id)
    suspend fun getAllQrs() = repository.getAllQrs()
    suspend fun deleteQr(qr: QrModel) = repository.deleteQr(qr)
    suspend fun filterSearch(
        name: String? = null,
        createdAt: Date? = null,
        favorite: Boolean? = null
    ) = repository.filterSearch(name, createdAt, favorite)
}

