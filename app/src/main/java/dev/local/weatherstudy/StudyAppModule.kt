package dev.local.weatherstudy

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.local.weatherstudy.app.detail.usecase.*
import dev.local.weatherstudy.data.repo.*
import dev.local.weatherstudy.domain.policy.StudyWeatherPolicyManager
import dev.local.weatherstudy.domain.repo.StudyUserPolicyConsentRepo
import dev.local.weatherstudy.domain.repo.StudyWeatherProviderRepo
import dev.local.weatherstudy.domain.source.location.StudyLocationProvider
import dev.local.weatherstudy.domain.source.location.StudyRepresentLocationProvider
import dev.local.weatherstudy.domain.source.location.StudyWeatherGeofenceProvider
import dev.local.weatherstudy.domain.usecase.*
import dev.local.weatherstudy.system.location.*
import dev.local.weatherstudy.system.service.*
import dev.local.weatherstudy.system.service.android.impl.*
import javax.inject.Singleton

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.di.AppModule
 * com.sec.android.daemonapp.di.AppUsecaseModule
 * com.sec.android.daemonapp.di.WeatherUsecaseModule
 *
 * ### This is where the platform family is chosen
 *
 * The single most instructive binding in the project is [StudyPlatformModule]: it binds
 * the **AOSP** implementations of the 20 system-service interfaces. Point those `@Binds`
 * at `…service.samsung.impl.*` instead and the app targets Samsung firmware — nothing
 * above `:study-system-service` changes, and every Samsung stub starts throwing with the
 * name of the API it needs.
 *
 * The original splits its app-level graph across three modules; the split is kept.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Module
@InstallIn(SingletonComponent::class)
object StudyAppContextModule {

    /** several reconstruction classes take a plain `Context`; the original does the same */
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context
}

/** The platform seam. See the file note — this is the family selector. */
@Module
@InstallIn(SingletonComponent::class)
interface StudyPlatformModule {

    @Binds @Singleton fun bindSystemService(i: StudyAndroidSystemService): StudySystemService
    @Binds @Singleton fun bindDeviceService(i: StudyAndroidDeviceService): StudyDeviceService
    @Binds @Singleton fun bindCscFeature(i: StudyAndroidCscFeature): StudyCscFeature
    @Binds @Singleton fun bindFloatingFeature(i: StudyAndroidFloatingFeature): StudyFloatingFeature
    @Binds @Singleton fun bindConnectivity(i: StudyAndroidConnectivityService): StudyConnectivityService
    @Binds @Singleton fun bindLocaleService(i: StudyAndroidLocaleService): StudyLocaleService
    @Binds @Singleton fun bindTelephony(i: StudyAndroidTelephonyService): StudyTelephonyService
    @Binds @Singleton fun bindSensitive(i: StudyAndroidSensitiveService): StudySensitiveService
    @Binds @Singleton fun bindDesktop(i: StudyAndroidDesktopService): StudyDesktopService
    @Binds @Singleton fun bindViewService(i: StudyAndroidViewService): StudyViewService
    @Binds @Singleton fun bindWindowService(i: StudyAndroidWindowService): StudyWindowService
    @Binds @Singleton fun bindListViewService(i: StudyAndroidListViewService): StudyListViewService
    @Binds @Singleton fun bindRemoteViews(i: StudyAndroidRemoteViewsService): StudyRemoteViewsService
    @Binds @Singleton fun bindActivityService(i: StudyAndroidActivityService): StudyActivityService
    @Binds @Singleton fun bindFoldState(i: StudyAndroidFoldStateService): StudyFoldStateService
    @Binds @Singleton fun bindEdgeManager(i: StudyAndroidEdgeManager): StudyEdgeManager
    @Binds @Singleton fun bindWidgetService(i: StudyAndroidWidgetService): StudyWidgetService
    @Binds @Singleton fun bindPackageService(i: StudyAndroidPackageService): StudyPackageService
    @Binds @Singleton fun bindShortcutService(i: StudyAndroidShortcutService): StudyShortcutService
    @Binds @Singleton fun bindSipService(i: StudyAndroidSipService): StudySipService
    @Binds @Singleton fun bindSmartTipService(i: StudyAndroidSmartTipService): StudySmartTipService

    // location: the delegating chooser, not a single API
    @Binds @Singleton fun bindLocationProvider(i: StudyDelegationLocationSource): StudyLocationProvider
    @Binds @Singleton fun bindRepresentLocation(
        i: StudyRepresentLocationService,
    ): StudyRepresentLocationProvider
    @Binds @Singleton fun bindGeofenceProvider(
        i: StudyAndroidGeofenceSource,
    ): StudyWeatherGeofenceProvider
}

/** Interface → impl for the domain's use cases and the remaining repositories. */
@Module
@InstallIn(SingletonComponent::class)
interface StudyAppUsecaseModule {

    @Binds @Singleton fun bindPolicyManager(i: StudyWeatherPolicyManagerImpl): StudyWeatherPolicyManager
    @Binds @Singleton fun bindProviderRepo(i: StudyWeatherProviderRepoImpl): StudyWeatherProviderRepo
    @Binds @Singleton fun bindConsentRepo(i: StudyUserPolicyConsentRepoImpl): StudyUserPolicyConsentRepo

    @Binds fun bindFetchWeather(i: StudyFetchWeatherImpl): StudyFetchWeather
    @Binds fun bindFetchCurrent(i: StudyFetchCurrentImpl): StudyFetchCurrent
    @Binds fun bindFetchCurrentObservation(
        i: StudyFetchCurrentObservationImpl,
    ): StudyFetchCurrentObservation
    @Binds fun bindFetchInsightCard(i: StudyFetchInsightCardImpl): StudyFetchInsightCard
    @Binds fun bindFetchContent(i: StudyFetchContentImpl): StudyFetchContent
    @Binds fun bindReviseContent(i: StudyReviseContentImpl): StudyReviseContent
    @Binds fun bindReachToObservation(
        i: StudyReachToObservationRefreshTimeImpl,
    ): StudyReachToObservationRefreshTime
    @Binds fun bindReachToShortInterval(
        i: StudyReachToShortIntervalRefreshTimeImpl,
    ): StudyReachToShortIntervalRefreshTime
    @Binds fun bindCheckForecastChange(i: StudyCheckForecastChangeImpl): StudyCheckForecastChange
    @Binds fun bindCheckSunriseSunset(i: StudyCheckSunriseSunsetTimeImpl): StudyCheckSunriseSunsetTime

    // the detail screen's layout use cases
    @Binds fun bindGetColumnSize(i: StudyGetColumnSizeImpl): StudyGetColumnSize
    @Binds fun bindGetContentAreaWidth(i: StudyGetContentAreaWidthImpl): StudyGetContentAreaWidth
    @Binds fun bindGetCardOrder(i: StudyGetCardOrderImpl): StudyGetCardOrder
}
