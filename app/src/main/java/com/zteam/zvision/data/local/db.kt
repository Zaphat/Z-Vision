package com.zteam.zvision.data.local

import android.content.Context
import androidx.room.*
import com.zteam.zvision.data.model.QrModel
import com.zteam.zvision.data.repository.QrDao
import java.util.*

// TypeConverters for Room
class Converters {
    @TypeConverter
    fun fromUUID(uuid: UUID?): String? = uuid?.toString()
    @TypeConverter
    fun toUUID(uuid: String?): UUID? = uuid?.let { UUID.fromString(it) }

    @TypeConverter
    fun fromDate(date: Date?): Long? = date?.time
    @TypeConverter
    fun toDate(millis: Long?): Date? = millis?.let { Date(it) }

    @TypeConverter
    fun fromByteArray(bytes: ByteArray?): String? = bytes?.let { Base64.getEncoder().encodeToString(it) }
    @TypeConverter
    fun toByteArray(data: String?): ByteArray? = data?.let { Base64.getDecoder().decode(it) }
}

// Room Database
@Database(entities = [QrModel::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun qrDao(): QrDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
            fun getInstance(context: Context): AppDatabase {
                return INSTANCE ?: synchronized(this) {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "zvision.db"
                    ).build()
                    INSTANCE = instance
                    instance
                }
            }
    }
}

