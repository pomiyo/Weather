package dev.local.weatherstudy.backend.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.local.weatherstudy.backend.BACKEND_DATABASE_NAME
import dev.local.weatherstudy.backend.StudyBackendDatabase
import dev.local.weatherstudy.backend.cache.StudyBackendInMemoryDao
import dev.local.weatherstudy.backend.dao.StudyBackendDao
import dev.local.weatherstudy.backend.dao.StudyBackendRoomDao
import dev.local.weatherstudy.backend.usecase.StudyDebugLogProvider
import dev.local.weatherstudy.backend.usecase.StudyEmptySecureKeyProvider
import dev.local.weatherstudy.backend.usecase.StudyFixtureSecureLinkProvider
import dev.local.weatherstudy.backend.usecase.StudyLocalAuthorityProvider
import dev.local.weatherstudy.domain.source.backend.StudyAuthorityProvider
import dev.local.weatherstudy.domain.source.backend.StudyLogProvider
import dev.local.weatherstudy.domain.source.backend.StudySecureKeyProvider
import dev.local.weatherstudy.domain.source.backend.StudySecureLinkProvider
import javax.inject.Singleton

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.backend.di.BackendModule
 *
 * Observed responsibility: provides the backend database and its DAO. The original's
 * factory names recovered from the APK are `BackendModule_ProvideBackendDatabaseFactory`
 * and `BackendModule_ProvideBackendDaoFactory` — two bindings, matching this.
 *
 * The reconstruction binds the **in-memory** DAO as the registry, because there is no
 * pre-populated database to read. The Room one is still provided so the tier is visible.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Module
@InstallIn(SingletonComponent::class)
object StudyBackendModule {

    @Provides
    @Singleton
    fun provideBackendDatabase(@ApplicationContext context: Context): StudyBackendDatabase =
        Room.databaseBuilder(context, StudyBackendDatabase::class.java, BACKEND_DATABASE_NAME)
            // the original opens a pre-packaged asset here with createFromAsset(...);
            // the reconstruction ships none, so the database starts empty
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideBackendRoomDao(database: StudyBackendDatabase): StudyBackendRoomDao =
        database.backendDao()
}

/** The interface bindings for the backend module. */
@Module
@InstallIn(SingletonComponent::class)
interface StudyBackendBindingModule {

    @Binds
    @Singleton
    fun bindBackendDao(impl: StudyBackendInMemoryDao): StudyBackendDao

    @Binds
    @Singleton
    fun bindSecureKeyProvider(impl: StudyEmptySecureKeyProvider): StudySecureKeyProvider

    @Binds
    @Singleton
    fun bindSecureLinkProvider(impl: StudyFixtureSecureLinkProvider): StudySecureLinkProvider

    @Binds
    @Singleton
    fun bindAuthorityProvider(impl: StudyLocalAuthorityProvider): StudyAuthorityProvider

    @Binds
    @Singleton
    fun bindLogProvider(impl: StudyDebugLogProvider): StudyLogProvider
}
