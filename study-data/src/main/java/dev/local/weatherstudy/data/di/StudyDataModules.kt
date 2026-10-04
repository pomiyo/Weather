package dev.local.weatherstudy.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.local.weatherstudy.data.repo.StudyLifeStyleSettingsRepoImpl
import dev.local.weatherstudy.data.repo.StudyProfileRepoImpl
import dev.local.weatherstudy.data.repo.StudySettingsRepoImpl
import dev.local.weatherstudy.data.repo.StudyStatusRepoImpl
import dev.local.weatherstudy.data.repo.StudyWeatherRepoImpl
import dev.local.weatherstudy.data.repo.StudyWidgetRepoImpl
import dev.local.weatherstudy.data.source.local.StudyLifeStyleSettingsLocalDataSourceImpl
import dev.local.weatherstudy.data.source.local.StudyWeatherLocalDataSourceImpl
import dev.local.weatherstudy.data.source.local.StudyWeatherSettingsLocalDataSource
import dev.local.weatherstudy.data.source.local.StudyWidgetLocalDataSourceImpl
import dev.local.weatherstudy.data.source.remote.impl.StudyForecastProviderManagerImpl
import dev.local.weatherstudy.data.source.remote.impl.StudyWeatherRemoteDataSourceImpl
import dev.local.weatherstudy.domain.entity.forecast.StudyForecastProviderManager
import dev.local.weatherstudy.domain.repo.StudyLifeStyleSettingsRepo
import dev.local.weatherstudy.domain.repo.StudyProfileRepo
import dev.local.weatherstudy.domain.repo.StudySettingsRepo
import dev.local.weatherstudy.domain.repo.StudyStatusRepo
import dev.local.weatherstudy.domain.repo.StudyWeatherRepo
import dev.local.weatherstudy.domain.repo.StudyWidgetRepo
import dev.local.weatherstudy.domain.source.local.StudyLifeStyleSettingsLocalDataSource
import dev.local.weatherstudy.domain.source.local.StudySettingsLocalDataSource
import dev.local.weatherstudy.domain.source.local.StudyWeatherLocalDataSource
import dev.local.weatherstudy.domain.source.local.StudyWidgetLocalDataSource
import dev.local.weatherstudy.domain.source.remote.StudyWeatherRemoteDataSource
import javax.inject.Singleton

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.data.di.DataModule
 *
 * Observed responsibility: binds the repository interfaces to their implementations.
 * The APK's generated factory names confirm what this module provides —
 * `DataModule_ProvideWeatherRepoFactory`, `…ProvideWidgetRepoFactory`,
 * `…ProvideSettingsRepoFactory` are all present in the decompiled output.
 *
 * The original splits the data layer's DI across **four** modules, and the split is
 * kept: repositories here, sources in [StudyDataSourceModule], Retrofit services in
 * `:study-network`'s module, use cases in [StudyDataUsecaseModule]. Collapsing them
 * into one `AppModule` would lose the subsystem boundaries the graph is organised by.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Module
@InstallIn(SingletonComponent::class)
interface StudyDataModule {

    @Binds
    @Singleton
    fun bindWeatherRepo(impl: StudyWeatherRepoImpl): StudyWeatherRepo

    @Binds
    @Singleton
    fun bindSettingsRepo(impl: StudySettingsRepoImpl): StudySettingsRepo

    @Binds
    @Singleton
    fun bindWidgetRepo(impl: StudyWidgetRepoImpl): StudyWidgetRepo

    @Binds
    @Singleton
    fun bindStatusRepo(impl: StudyStatusRepoImpl): StudyStatusRepo

    @Binds
    @Singleton
    fun bindLifeStyleSettingsRepo(impl: StudyLifeStyleSettingsRepoImpl): StudyLifeStyleSettingsRepo

    @Binds
    @Singleton
    fun bindProfileRepo(impl: StudyProfileRepoImpl): StudyProfileRepo
}

/**
 * Corresponds conceptually to `com.samsung.android.weather.data.di.DataSourceModule`.
 *
 * ### This module is where the three-tier DAO choice is actually made
 *
 * `StudyWeatherLocalDataSourceImpl` takes a `StudyWeatherDao`, and there are three
 * implementations of that interface — Room, legacy raw-SQLite, in-memory. Which one the
 * app runs on is decided by one `@Binds` in `:study-persistence`'s module, and nothing
 * above this line is aware of it. That is the payoff of the whole DAO family.
 */
@Module
@InstallIn(SingletonComponent::class)
interface StudyDataSourceModule {

    @Binds
    @Singleton
    fun bindWeatherLocalDataSource(impl: StudyWeatherLocalDataSourceImpl): StudyWeatherLocalDataSource

    @Binds
    @Singleton
    fun bindSettingsLocalDataSource(
        impl: StudyWeatherSettingsLocalDataSource,
    ): StudySettingsLocalDataSource

    @Binds
    @Singleton
    fun bindWidgetLocalDataSource(impl: StudyWidgetLocalDataSourceImpl): StudyWidgetLocalDataSource

    @Binds
    @Singleton
    fun bindLifeStyleSettingsLocalDataSource(
        impl: StudyLifeStyleSettingsLocalDataSourceImpl,
    ): StudyLifeStyleSettingsLocalDataSource

    @Binds
    @Singleton
    fun bindWeatherRemoteDataSource(
        impl: StudyWeatherRemoteDataSourceImpl,
    ): StudyWeatherRemoteDataSource

    @Binds
    @Singleton
    fun bindForecastProviderManager(
        impl: StudyForecastProviderManagerImpl,
    ): StudyForecastProviderManager
}
