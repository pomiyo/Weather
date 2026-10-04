package dev.local.weatherstudy.sync.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.local.weatherstudy.domain.usecase.StudySyncAutoRefresh
import dev.local.weatherstudy.sync.usecase.StudyStartBackgroundRefresh
import dev.local.weatherstudy.sync.usecase.StudyStartCurrentLocationAddition
import dev.local.weatherstudy.sync.usecase.StudyStartForegroundRefresh
import dev.local.weatherstudy.sync.usecase.StudySyncGeofence
import javax.inject.Singleton

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.sync.di.SyncModule
 * com.samsung.android.weather.sync.di.SyncUsecaseModule
 *
 * Observed responsibility: the original splits scheduling bindings from use-case
 * bindings across two modules; both are reconstructed here as one file with two objects
 * to keep the split visible without a second file.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Module
@InstallIn(SingletonComponent::class)
object StudySyncUsecaseModule {

    @Provides
    @Singleton
    fun provideStartBackgroundRefresh(
        @ApplicationContext context: Context,
        syncAutoRefresh: StudySyncAutoRefresh,
    ) = StudyStartBackgroundRefresh(context, syncAutoRefresh)

    @Provides
    @Singleton
    fun provideStartForegroundRefresh(@ApplicationContext context: Context) =
        StudyStartForegroundRefresh(context)

    @Provides
    @Singleton
    fun provideStartCurrentLocationAddition(@ApplicationContext context: Context) =
        StudyStartCurrentLocationAddition(context)

    @Provides
    @Singleton
    fun provideSyncGeofence(@ApplicationContext context: Context) = StudySyncGeofence(context)
}
