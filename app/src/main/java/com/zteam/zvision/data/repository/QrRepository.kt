package com.zteam.zvision.data.repository

import com.zteam.zvision.data.local.dao.QrDao
import com.zteam.zvision.data.local.entity.QrModel
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class QrRepository @Inject constructor(
    private val dao: QrDao
) {
    suspend fun insertQr(qr: QrModel) = dao.insert(qr)
    suspend fun getQrById(id: UUID) = dao.getById(id)
    suspend fun getAllQrs() = dao.getAll()
    suspend fun deleteQr(qr: QrModel) = dao.delete(qr)
    suspend fun filterSearch(
        name: String? = null,
        createdAt: Date? = null,
        favorite: Boolean? = null
    ) = dao.filterSearch(name, createdAt, favorite)
}
