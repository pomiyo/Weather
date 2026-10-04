package dev.local.weatherstudy.logger.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.local.weatherstudy.logger.StudyAppTracker
import dev.local.weatherstudy.logger.StudyDataTracker
import dev.local.weatherstudy.logger.StudyLogcatAnalytics
import dev.local.weatherstudy.logger.StudyVocTracker
import dev.local.weatherstudy.logger.StudyWeatherAnalytics
import dev.local.weatherstudy.logger.diag.StudyLogcatUserMonitor
import dev.local.weatherstudy.logger.diag.StudyUserMonitorDataSource
import javax.inject.Singleton

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.logger.di.LoggerModule
 * com.sec.android.daemonapp.di.AnalyticsModule
 *
 * Observed responsibility: binds the three tracker surfaces. Note all three bind to the
 * SAME implementation in the reconstruction — in the original they are three different
 * destinations, which is why the interfaces are separate in the first place.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Module
@InstallIn(SingletonComponent::class)
interface StudyLoggerModule {

    @Binds
    @Singleton
    fun bindAnalytics(impl: StudyLogcatAnalytics): StudyWeatherAnalytics

    @Binds
    @Singleton
    fun bindAppTracker(impl: StudyLogcatAnalytics): StudyAppTracker

    @Binds
    @Singleton
    fun bindDataTracker(impl: StudyLogcatAnalytics): StudyDataTracker

    @Binds
    @Singleton
    fun bindVocTracker(impl: StudyLogcatAnalytics): StudyVocTracker

    @Binds
    @Singleton
    fun bindUserMonitor(impl: StudyLogcatUserMonitor): StudyUserMonitorDataSource
}
