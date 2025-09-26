package com.zteam.zvision.di

import android.content.Context
import androidx.room.Room
import com.zteam.zvision.data.generator.QRGenerator
import com.zteam.zvision.data.local.dao.AppDatabase
import com.zteam.zvision.data.local.dao.QrDao
import com.zteam.zvision.data.repository.QrRepository
import com.zteam.zvision.data.repository.TranslatorRepository
import com.zteam.zvision.domain.usecase.QrUsecase
import com.zteam.zvision.domain.usecase.TranslatorUsecase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "zvision.db").build()

    @Provides
    fun provideQrDao(db: AppDatabase): QrDao = db.qrDao()

    @Provides @Singleton
    fun provideQrRepository(dao: QrDao): QrRepository = QrRepository(dao)

    @Provides @Singleton
    fun provideQrUsecase(repo: QrRepository): QrUsecase = QrUsecase(repo)

    @Provides @Singleton
    fun provideQrGenerator(): QRGenerator = QRGenerator()

    @Provides @Singleton
    fun provideTranslatorRepository(@ApplicationContext context: Context): TranslatorRepository =
        TranslatorRepository(context)

    @Provides @Singleton
    fun provideTranslatorUsecase(repo: TranslatorRepository): TranslatorUsecase =
        TranslatorUsecase(repo)
}
