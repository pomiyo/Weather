package dev.local.weatherstudy.data.source.remote.api.forecast.providerbe

import dev.local.weatherstudy.data.source.remote.api.forecast.providera.sub.StudyProviderAConverter
import dev.local.weatherstudy.domain.entity.content.StudyInsightContent
import dev.local.weatherstudy.domain.entity.content.StudyWebContent
import dev.local.weatherstudy.domain.entity.forecast.StudyForecastProvider
import dev.local.weatherstudy.domain.entity.weather.*
import dev.local.weatherstudy.domain.source.remote.StudyWeatherRemoteDataSource
import dev.local.weatherstudy.network.api.forecast.*
import dev.local.weatherstudy.system.service.StudyLocaleService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow

/**
 * Educational reconstruction of the four remaining provider facades —
 * `WjpApi`, `WkrApi`, `HuaApi`, `SRCApi`.
 *
 * ### The point of having four more
 *
 * Each implements a **different subset** of the eleven capability interfaces. That asymmetry is
 * the reason `:study-domain` splits the remote contract eleven ways instead of declaring one
 * fat interface, and the reason `StudyWeatherPolicy` has 70 `support*()` questions: the UI asks
 * policy before it asks a provider, because the provider may have nothing to answer with.
 *
 * Unsupported capabilities return `emptyFlow()` — the original's answer too.
 *
 * Independently written reconstruction code, not original Samsung source.
 */
abstract class StudyProviderFacade(
    protected val converter: StudyProviderAConverter,
    protected val localeService: StudyLocaleService,
) : StudyWeatherRemoteDataSource {

    abstract val providerId: String

    protected fun language(): String = localeService.getLocale().language

    // ---- capabilities every backend serves ----
    abstract override fun getRemoteWeather(location: StudyLocation): Flow<StudyWeather>
    abstract override fun getSearch(key: String): Flow<List<StudyLocation>>

    // ---- defaults: not served unless a subclass overrides ----
    override fun getRemoteWeather(latitude: Double, longitude: Double): Flow<StudyWeather> = emptyFlow()
    override fun getRemoteWeather(locations: List<StudyLocation>): Flow<List<StudyWeather>> = flow {
        emit(locations.mapNotNull { l -> runCatching { collectFirst(getRemoteWeather(l)) }.getOrNull() })
    }
    override fun getRemoteBriefWeather(location: StudyLocation): Flow<StudyBriefWeather> = emptyFlow()
    override fun getRemoteBriefWeather(latitude: Double, longitude: Double): Flow<StudyBriefWeather> = emptyFlow()
    override fun getRemoteCurrentObservation(weather: StudyWeather): Flow<StudyCurrentObservationWithKey> = emptyFlow()
    override fun getRemoteCurrentObservation(weathers: List<StudyWeather>): Flow<List<StudyCurrentObservationWithKey>> = emptyFlow()
    override fun getInsightContent(placeId: String, links: Map<String, String>): Flow<List<StudyInsightContent>> = emptyFlow()
    override fun getTodayStories(placeId: String): Flow<List<StudyWebContent>> = emptyFlow()
    override fun getRadar(placeId: String, links: Map<String, String>): Flow<StudyWebContent> = emptyFlow()
    override fun getAutoComplete(key: String): Flow<List<StudyLocation>> = emptyFlow()
    override fun getThemeCategories(): Flow<List<StudyTheme>> = emptyFlow()
    override fun getThemeRegions(categoryId: String): Flow<List<StudyTheme>> = emptyFlow()
    override fun getThemePlaces(categoryId: String, regionIds: List<String>): Flow<List<StudyThemePlace>> = emptyFlow()
    override fun getVideoList(placeId: String): Flow<List<StudyWebContent>> = emptyFlow()
    override fun getRepresentWeather(code: String): Flow<StudyWeather> = emptyFlow()
    override fun getRepresentWeather(latitude: Double, longitude: Double): Flow<StudyWeather> = emptyFlow()
    override fun getForecastChange(placeId: String): Flow<StudyForecastChange> = emptyFlow()

    protected suspend fun <T> collectFirst(f: Flow<T>): T? {
        var out: T? = null
        f.collect { out = it }
        return out
    }
}

/**
 * `WjpApi`. Adds forecast-change, today-stories and radar; **no** brief forecast and no
 * autocomplete. The APK bundles Simple XML alongside Moshi because part of this backend's
 * payload is an XML feed.
 */
@Singleton
class StudyProviderBApi @Inject constructor(
    private val service: StudyProviderBRetrofitService,
    converter: StudyProviderAConverter,
    localeService: StudyLocaleService,
) : StudyProviderFacade(converter, localeService) {

    override val providerId = StudyForecastProvider.PROVIDER_B

    override fun getRemoteWeather(location: StudyLocation): Flow<StudyWeather> = flow {
        emit(converter.toWeather(service.getForecast(location.key, language()), location))
    }
    override fun getSearch(key: String): Flow<List<StudyLocation>> = flow {
        emit(converter.toLocations(service.search(key)))
    }
    override fun getForecastChange(placeId: String): Flow<StudyForecastChange> = flow {
        converter.toWeather(service.getForecastChange(placeId), StudyLocation(key = placeId))
            .forecastChange?.let { emit(it) }
    }
    override fun getTodayStories(placeId: String): Flow<List<StudyWebContent>> = flow {
        emit(converter.toVideos(service.getTodayStories(placeId)))
    }
    override fun getRadar(placeId: String, links: Map<String, String>): Flow<StudyWebContent> = flow {
        converter.toVideos(service.getRadar(placeId)).firstOrNull()?.let { emit(it) }
    }
}

/**
 * `WkrApi`. The only backend with the **themed-place catalogue** the map search uses —
 * which is why `StudyThemeApi` is a separate capability and `supportThemeArea()` a policy
 * question. Also the only one with a news feed.
 */
@Singleton
class StudyProviderCApi @Inject constructor(
    private val service: StudyProviderCRetrofitService,
    converter: StudyProviderAConverter,
    localeService: StudyLocaleService,
) : StudyProviderFacade(converter, localeService) {

    override val providerId = StudyForecastProvider.PROVIDER_C

    override fun getRemoteWeather(location: StudyLocation): Flow<StudyWeather> = flow {
        emit(converter.toWeather(service.getForecast(location.key, language()), location))
    }
    override fun getSearch(key: String): Flow<List<StudyLocation>> = flow {
        emit(converter.toLocations(service.search(key)))
    }
    override fun getForecastChange(placeId: String): Flow<StudyForecastChange> = flow {
        converter.toWeather(service.getForecastChange(placeId), StudyLocation(key = placeId))
            .forecastChange?.let { emit(it) }
    }
    override fun getThemeCategories(): Flow<List<StudyTheme>> = flow {
        emit(converter.toLocations(service.getThemeCategories(language()))
            .map { StudyTheme(id = it.key, name = it.cityName) })
    }
    override fun getThemeRegions(categoryId: String): Flow<List<StudyTheme>> = flow {
        emit(converter.toLocations(service.getThemeRegions(categoryId, language()))
            .map { StudyTheme(id = it.key, name = it.cityName, categoryId = categoryId) })
    }
    override fun getThemePlaces(categoryId: String, regionIds: List<String>): Flow<List<StudyThemePlace>> = flow {
        emit(converter.toLocations(service.getThemePlaces(categoryId, regionIds.joinToString(",")))
            .map { StudyThemePlace(key = it.key, name = it.cityName,
                latitude = it.latitude, longitude = it.longitude) })
    }
    override fun getVideoList(placeId: String): Flow<List<StudyWebContent>> = flow {
        emit(converter.toVideos(service.getNews(placeId)))
    }
}

/**
 * `HuaApi`. The narrowest subset — forecast, search, air quality. Its package is the one with
 * a separate `HuaAuth` class on top of the interceptor: a two-step signing flow rather than a
 * single header. `StudyForecastProvider.isRegionRestrictedProvider` keys off this provider.
 */
@Singleton
class StudyProviderDApi @Inject constructor(
    private val service: StudyProviderDRetrofitService,
    converter: StudyProviderAConverter,
    localeService: StudyLocaleService,
) : StudyProviderFacade(converter, localeService) {

    override val providerId = StudyForecastProvider.PROVIDER_D

    override fun getRemoteWeather(location: StudyLocation): Flow<StudyWeather> = flow {
        emit(converter.toWeather(service.getForecast(location.key, language()), location))
    }
    override fun getSearch(key: String): Flow<List<StudyLocation>> = flow {
        emit(converter.toLocations(service.search(key)))
    }
    /** air quality arrives on its own endpoint here, not inside the forecast payload */
    override fun getRemoteCurrentObservation(weather: StudyWeather): Flow<StudyCurrentObservationWithKey> = flow {
        emit(StudyCurrentObservationWithKey(
            key = weather.location.key,
            observation = converter.toCurrentObservation(service.getAirQuality(weather.location.key)),
        ))
    }
}

/**
 * `SRCApi` + `SRCAlertRetrofitService`. The only provider split across **two hosts** —
 * alerts come from a separate base URL, which is why `StudyRegionEndpointResolver` has
 * `resolveAlertBaseUrl`.
 */
@Singleton
class StudyProviderEApi @Inject constructor(
    private val service: StudyProviderERetrofitService,
    private val alertService: StudyProviderEAlertRetrofitService,
    converter: StudyProviderAConverter,
    localeService: StudyLocaleService,
) : StudyProviderFacade(converter, localeService) {

    override val providerId = StudyForecastProvider.PROVIDER_E

    override fun getRemoteWeather(location: StudyLocation): Flow<StudyWeather> = flow {
        val base = converter.toWeather(service.getForecast(location.key, language()), location)
        // alerts live on the second host
        val alerts = runCatching {
            converter.toWeather(alertService.getAlerts(location.key, language()), location).alerts
        }.getOrDefault(emptyList())
        emit(if (alerts.isEmpty()) base else base.copy(alerts = alerts))
    }
    override fun getSearch(key: String): Flow<List<StudyLocation>> = flow {
        emit(converter.toLocations(service.search(key)))
    }
    override fun getInsightContent(placeId: String, links: Map<String, String>): Flow<List<StudyInsightContent>> = flow {
        emit(converter.toInsights(service.getInsight(placeId)))
    }
    override fun getRepresentWeather(code: String): Flow<StudyWeather> = flow {
        emit(converter.toWeather(service.getForecast(code, language()), StudyLocation(key = "", id = code)))
    }
}
