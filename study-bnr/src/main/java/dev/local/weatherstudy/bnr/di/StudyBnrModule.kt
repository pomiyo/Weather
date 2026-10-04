package dev.local.weatherstudy.bnr.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.local.weatherstudy.bnr.StudyBnrDataSource
import dev.local.weatherstudy.bnr.StudySamsungCloudBnrBridge
import javax.inject.Singleton

/**
 * Educational reconstruction of `bnr.di.BackupModule` / `BnrUseCaseModule`.
 *
 * Binds the transport to the Samsung Cloud stub — the one line to repoint if a real
 * transport is ever added.
 */
@Module
@InstallIn(SingletonComponent::class)
interface StudyBnrModule {
    @Binds
    @Singleton
    fun bindBnrDataSource(impl: StudySamsungCloudBnrBridge): StudyBnrDataSource
}
