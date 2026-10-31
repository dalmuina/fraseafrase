package com.dalmuina.fraseafrase.di

import android.content.Context
import com.dalmuina.fraseafrase.core.data.catalog.AssetsCatalogDataSource
import com.dalmuina.fraseafrase.core.data.di.IoDispatcher
import com.dalmuina.fraseafrase.core.data.practice.PracticeDatabase
import com.dalmuina.fraseafrase.core.data.practice.RoomPracticeDataSource
import com.dalmuina.fraseafrase.core.data.practice.createPracticeDatabase
import com.dalmuina.fraseafrase.core.data.settings.DataStoreSettingsDataSource
import com.dalmuina.fraseafrase.core.domain.catalog.CatalogRepository
import com.dalmuina.fraseafrase.core.domain.practice.PracticeRepository
import com.dalmuina.fraseafrase.core.domain.settings.SettingsRepository
import com.dalmuina.fraseafrase.core.domain.time.Clock
import com.dalmuina.fraseafrase.core.platform.DeviceClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Cableado de la persistencia (ADR-0010: solo :app conoce las implementaciones). Cada puerto del dominio se enlaza con
 * su única fuente; la base de datos y el dispatcher de E/S son singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PersistenceModule {

    @Binds
    abstract fun bindCatalogRepository(source: AssetsCatalogDataSource): CatalogRepository

    @Binds
    abstract fun bindSettingsRepository(source: DataStoreSettingsDataSource): SettingsRepository

    @Binds
    abstract fun bindPracticeRepository(source: RoomPracticeDataSource): PracticeRepository

    @Binds
    abstract fun bindClock(clock: DeviceClock): Clock

    companion object {

        @Provides
        @Singleton
        fun providePracticeDatabase(@ApplicationContext context: Context): PracticeDatabase =
            createPracticeDatabase(context)

        @Provides
        @IoDispatcher
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    }
}
