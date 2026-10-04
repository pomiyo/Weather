package dev.local.weatherstudy.ui.common.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.local.weatherstudy.ui.common.resource.StudyBackgroundProvider
import dev.local.weatherstudy.ui.common.resource.StudyBackgroundProviderImpl
import dev.local.weatherstudy.ui.common.resource.StudyWeatherIconProvider
import dev.local.weatherstudy.ui.common.resource.StudyWeatherIconProviderImpl
import javax.inject.Singleton

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.ui.common.di.ResourceModule
 *
 * Observed responsibility: interface -> implementation for the resource providers, so the
 * state providers and the widgets depend on "an icon for this number" rather than on a
 * drawable set. The original binds a different set per build flavour here.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Module
@InstallIn(SingletonComponent::class)
interface StudyUiCommonModule {

    @Binds
    @Singleton
    fun bindWeatherIconProvider(impl: StudyWeatherIconProviderImpl): StudyWeatherIconProvider

    @Binds
    @Singleton
    fun bindBackgroundProvider(impl: StudyBackgroundProviderImpl): StudyBackgroundProvider
}
