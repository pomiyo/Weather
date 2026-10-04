package dev.local.weatherstudy.data.source.remote.impl

import dev.local.weatherstudy.data.source.remote.api.forecast.providera.StudyProviderAApi
import dev.local.weatherstudy.domain.entity.content.StudyInsightContent
import dev.local.weatherstudy.domain.entity.content.StudyWebContent
import dev.local.weatherstudy.domain.entity.forecast.StudyForecastProviderManager
import dev.local.weatherstudy.domain.entity.weather.StudyBriefWeather
import dev.local.weatherstudy.domain.entity.weather.StudyCurrentObservationWithKey
import dev.local.weatherstudy.domain.entity.weather.StudyForecastChange
import dev.local.weatherstudy.domain.entity.weather.StudyLocation
import dev.local.weatherstudy.domain.entity.weather.StudyTheme
import dev.local.weatherstudy.domain.entity.weather.StudyThemePlace
import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import dev.local.weatherstudy.domain.source.remote.StudyWeatherRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.data.source.remote.impl.WeatherRemoteDataSourceImpl
 *
 * ### Observed responsibility: the provider switch
 *
 * This is the one class in the data layer that knows there are five backends. It
 * implements the eleven-capability union and, for every call, dispatches to whichever
 * provider facade is currently active:
 *
 * ```
 * StudyWeatherRepoImpl  (delegates blindly)
 *        ↓
 * StudyWeatherRemoteDataSourceImpl   ← asks the provider manager, then dispatches
 *        ↓
 * StudyProviderAApi / ProviderBApi / … ProviderEApi
 *        ↓
 * Study<Provider>RetrofitService  +  Study<Provider>Converter
 * ```
 *
 * Everything above it — repository, use cases, ViewModels — is provider-agnostic, and
 * everything below is provider-specific. Putting the switch anywhere else would leak
 * the provider identity upward; that is why `ForecastProviderManager` is injected here
 * and not into the repository.
 *
 * All five facades are wired. Each implements a different subset of the eleven capabilities —
 * see `StudyProviderFacade` — which is why the contract is split eleven ways.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Singleton
class StudyWeatherRemoteDataSourceImpl @Inject constructor(
    private val forecastProviderManager: StudyForecastProviderManager,
    private val providerA: StudyProviderAApi,
    private val providerB: dev.local.weatherstudy.data.source.remote.api.forecast.providerbe.StudyProviderBApi,
    private val providerC: dev.local.weatherstudy.data.source.remote.api.forecast.providerbe.StudyProviderCApi,
    private val providerD: dev.local.weatherstudy.data.source.remote.api.forecast.providerbe.StudyProviderDApi,
    private val providerE: dev.local.weatherstudy.data.source.remote.api.forecast.providerbe.StudyProviderEApi,
) : StudyWeatherRemoteDataSource {

    /**
     * Reconstruction of the provider switch. The original selects among five; the
     * reconstruction has one facade built, so every branch resolves to it — but the
     * decision point is preserved, because that is the architecture.
     */
    private fun active(): StudyWeatherRemoteDataSource {
        val providerId = forecastProviderManager.getActive().name
        val p = dev.local.weatherstudy.domain.entity.forecast.StudyForecastProvider
        return when (providerId) {
            p.PROVIDER_B -> providerB
            p.PROVIDER_C -> providerC
            p.PROVIDER_D -> providerD
            p.PROVIDER_E -> providerE
            else -> providerA
        }
    }

    override fun getRemoteWeather(location: StudyLocation): Flow<StudyWeather> =
        active().getRemoteWeather(location)

    override fun getRemoteWeather(latitude: Double, longitude: Double): Flow<StudyWeather> =
        active().getRemoteWeather(latitude, longitude)

    override fun getRemoteWeather(locations: List<StudyLocation>): Flow<List<StudyWeather>> =
        active().getRemoteWeather(locations)

    override fun getRemoteBriefWeather(location: StudyLocation): Flow<StudyBriefWeather> =
        active().getRemoteBriefWeather(location)

    override fun getRemoteBriefWeather(latitude: Double, longitude: Double): Flow<StudyBriefWeather> =
        active().getRemoteBriefWeather(latitude, longitude)

    override fun getRemoteCurrentObservation(
        weather: StudyWeather,
    ): Flow<StudyCurrentObservationWithKey> = active().getRemoteCurrentObservation(weather)

    override fun getRemoteCurrentObservation(
        weathers: List<StudyWeather>,
    ): Flow<List<StudyCurrentObservationWithKey>> = active().getRemoteCurrentObservation(weathers)

    override fun getInsightContent(
        placeId: String,
        links: Map<String, String>,
    ): Flow<List<StudyInsightContent>> = active().getInsightContent(placeId, links)

    override fun getTodayStories(placeId: String): Flow<List<StudyWebContent>> =
        active().getTodayStories(placeId)

    override fun getRadar(placeId: String, links: Map<String, String>): Flow<StudyWebContent> =
        active().getRadar(placeId, links)

    override fun getSearch(key: String): Flow<List<StudyLocation>> = active().getSearch(key)

    override fun getAutoComplete(key: String): Flow<List<StudyLocation>> =
        active().getAutoComplete(key)

    override fun getThemeCategories(): Flow<List<StudyTheme>> = active().getThemeCategories()

    override fun getThemeRegions(categoryId: String): Flow<List<StudyTheme>> =
        active().getThemeRegions(categoryId)

    override fun getThemePlaces(
        categoryId: String,
        regionIds: List<String>,
    ): Flow<List<StudyThemePlace>> = active().getThemePlaces(categoryId, regionIds)

    override fun getVideoList(placeId: String): Flow<List<StudyWebContent>> =
        active().getVideoList(placeId)

    override fun getRepresentWeather(code: String): Flow<StudyWeather> =
        active().getRepresentWeather(code)

    override fun getRepresentWeather(latitude: Double, longitude: Double): Flow<StudyWeather> =
        active().getRepresentWeather(latitude, longitude)

    override fun getForecastChange(placeId: String): Flow<StudyForecastChange> =
        active().getForecastChange(placeId)
}

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.ForecastProviderManager's implementation
 *
 * Observed responsibility: holds the active provider and distinguishes three answers to
 * "which provider?" — active, what the DEVICE shipped with, and what the NETWORK says.
 * A mismatch between the last two is what raises the provider-change popup
 * (`HomeCpChanged` → `ShowCpChangeState` → `MainState`).
 */
@Singleton
class StudyForecastProviderManagerImpl @Inject constructor(
    private val deviceService: dev.local.weatherstudy.system.service.StudyDeviceService,
    private val devOpts: dev.local.weatherstudy.devopts.StudyDevOpts,
) : StudyForecastProviderManager {

    private var activeInfo: dev.local.weatherstudy.domain.entity.forecast.StudyForecastProviderInfo? = null

    override fun getActive(): dev.local.weatherstudy.domain.entity.forecast.StudyForecastProviderInfo =
        activeInfo ?: getDeviceProviderType().also { activeInfo = it }

    override fun setActive(info: dev.local.weatherstudy.domain.entity.forecast.StudyForecastProviderInfo) {
        activeInfo = info
    }

    override fun getInfo(name: String) =
        dev.local.weatherstudy.domain.entity.forecast.StudyForecastProviderInfo(name = name)

    /**
     * What the device shipped with: the region decision, from the CSC country code.
     * Developer options can force it, which is how a non-Samsung build reaches any
     * provider's code path at all.
     */
    override fun getDeviceProviderType() = getInfo(
        devOpts.forcedProviderId
            ?: dev.local.weatherstudy.domain.entity.forecast.StudyForecastProvider
                .dispatchByCountryCode(
                    devOpts.forcedCountryCode ?: runCatching { deviceService.getCountryCode() }
                        .getOrDefault(""),
                ),
    )

    /**
     * What the network says. In the original this comes from the backend registry; the
     * reconstruction has an empty registry, so it agrees with the device and the popup
     * never fires.
     */
    override fun getNetworkProviderType() = getDeviceProviderType()
}
