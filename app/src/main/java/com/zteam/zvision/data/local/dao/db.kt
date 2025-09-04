package com.zteam.zvision.data.local.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.zteam.zvision.data.local.entity.QrModel
import java.util.Base64
import java.util.Date
import java.util.UUID

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

