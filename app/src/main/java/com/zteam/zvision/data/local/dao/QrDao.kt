package com.zteam.zvision.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zteam.zvision.data.local.entity.QrModel
import java.util.Date
import java.util.UUID

@Dao
interface QrDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(qr: QrModel)

    @Query("SELECT * FROM qr WHERE id = :id")
    suspend fun getById(id: UUID): QrModel?

    @Query("SELECT * FROM qr")
    suspend fun getAll(): List<QrModel>

    @Delete
    suspend fun delete(qr: QrModel)

    @Query("""
        SELECT * FROM qr
        WHERE (:name IS NULL OR LOWER(name) LIKE '%' || LOWER(:name) || '%')
        AND (:createdAt IS NULL OR createdAt = :createdAt)
        AND (:favorite IS NULL OR favorite = :favorite)
    """)
    suspend fun filterSearch(
        name: String?,
        createdAt: Date?,
        favorite: Boolean?
    ): List<QrModel>
}
