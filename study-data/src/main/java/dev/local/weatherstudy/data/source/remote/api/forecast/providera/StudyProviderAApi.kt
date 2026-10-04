package dev.local.weatherstudy.data.source.remote.api.forecast.providera

import dev.local.weatherstudy.data.source.remote.api.forecast.providera.sub.StudyProviderAConverter
import dev.local.weatherstudy.domain.entity.content.StudyInsightContent
import dev.local.weatherstudy.domain.entity.content.StudyWebContent
import dev.local.weatherstudy.domain.entity.weather.StudyBriefWeather
import dev.local.weatherstudy.domain.entity.weather.StudyCurrentObservationWithKey
import dev.local.weatherstudy.domain.entity.weather.StudyForecastChange
import dev.local.weatherstudy.domain.entity.weather.StudyLocation
import dev.local.weatherstudy.domain.entity.weather.StudyTheme
import dev.local.weatherstudy.domain.entity.weather.StudyThemePlace
import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import dev.local.weatherstudy.domain.source.remote.StudyWeatherRemoteDataSource
import dev.local.weatherstudy.network.api.forecast.StudyProviderARetrofitService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.data.source.remote.api.forecast.twc.TwcApi
 * (and its four siblings `WjpApi`, `WkrApi`, `HuaApi`, `SRCApi`)
 *
 * ### The provider facade — one per backend
 *
 * Each of the five providers has a package of the same shape. Recovered from the APK,
 * `…api.forecast.twc/` contains:
 *
 * ```
 * TwcApi                      ← the facade (this file's counterpart)
 * TwcConverter                ← the top-level DTO → domain converter
 * TwcCodeConverter            ← provider condition code → internal code
 * TwcExpansionCodeConverter   ← finer-grained code variants
 * TwcAQIScale                 ← which national AQI scale this backend reports
 * TwcAlertColor               ← severity → colour
 * TwcApiLanguage              ← locale → the backend's language parameter
 * sub/ (17 classes)           ← one converter per payload section
 * model/                      ← provider-local models
 * ```
 *
 * The facade implements the **eleven capability interfaces** from `:study-domain`, and a
 * capability this backend does not serve returns an empty flow rather than throwing —
 * the policy layer is what stops the UI asking in the first place.
 *
 * Reconstructed for provider A in full; the other four are documented in
 * `reports/class-mapping.md` §10 and would differ only in which capabilities they
 * implement and which converters they own.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Singleton
class StudyProviderAApi @Inject constructor(
    private val service: StudyProviderARetrofitService,
    private val converter: StudyProviderAConverter,
    private val language: StudyProviderALanguage,
) : StudyWeatherRemoteDataSource {

    // ---- StudyForecastApi ----

    override fun getRemoteWeather(location: StudyLocation): Flow<StudyWeather> = flow {
        val dto = service.getForecast(location.key, UNITS_METRIC, language.resolve())
        emit(converter.toWeather(dto, location))
    }

    override fun getRemoteWeather(latitude: Double, longitude: Double): Flow<StudyWeather> = flow {
        val dto = service.getForecastByGeocode(
            geocode = "$latitude,$longitude",
            units = UNITS_METRIC,
            language = language.resolve(),
        )
        emit(converter.toWeather(dto, StudyLocation(key = "")))
    }

    override fun getRemoteWeather(locations: List<StudyLocation>): Flow<List<StudyWeather>> = flow {
        // the original batches saved cities into one request per provider call budget;
        // reconstructed as a sequential fan-out over the same single-location endpoint
        emit(
            locations.mapNotNull { location ->
                runCatching {
                    converter.toWeather(
                        service.getForecast(location.key, UNITS_METRIC, language.resolve()),
                        location,
                    )
                }.getOrNull()
            },
        )
    }

    // ---- StudyBriefForecastApi ----

    override fun getRemoteBriefWeather(location: StudyLocation): Flow<StudyBriefWeather> = flow {
        val dto = service.getPartialForecast(location.key, UNITS_METRIC, language.resolve())
        emit(converter.toBriefWeather(dto, location))
    }

    override fun getRemoteBriefWeather(latitude: Double, longitude: Double): Flow<StudyBriefWeather> =
        flow {
            val dto = service.getForecastByGeocode(
                "$latitude,$longitude", UNITS_METRIC, language.resolve(),
            )
            emit(converter.toBriefWeather(dto, StudyLocation(key = "")))
        }

    // ---- StudyCurrentObservationApi ----

    override fun getRemoteCurrentObservation(
        weather: StudyWeather,
    ): Flow<StudyCurrentObservationWithKey> = flow {
        val dto = service.getPartialForecast(
            weather.location.key, UNITS_METRIC, language.resolve(),
        )
        emit(
            StudyCurrentObservationWithKey(
                key = weather.location.key,
                observation = converter.toCurrentObservation(dto),
            ),
        )
    }

    override fun getRemoteCurrentObservation(
        weathers: List<StudyWeather>,
    ): Flow<List<StudyCurrentObservationWithKey>> = flow {
        emit(
            weathers.mapNotNull { weather ->
                runCatching {
                    StudyCurrentObservationWithKey(
                        key = weather.location.key,
                        observation = converter.toCurrentObservation(
                            service.getPartialForecast(
                                weather.location.key, UNITS_METRIC, language.resolve(),
                            ),
                        ),
                    )
                }.getOrNull()
            },
        )
    }

    // ---- StudySearchApi ----

    override fun getSearch(key: String): Flow<List<StudyLocation>> = flow {
        emit(converter.toLocations(service.search(key, language.resolve())))
    }

    override fun getAutoComplete(key: String): Flow<List<StudyLocation>> = flow {
        emit(converter.toLocations(service.autoComplete(key, language.resolve())))
    }

    // ---- StudyInsightApi ----

    override fun getInsightContent(
        placeId: String,
        links: Map<String, String>,
    ): Flow<List<StudyInsightContent>> = flow {
        // the original calls the URL the forecast response supplied in `links`;
        // reconstructed against the same conceptual endpoint
        emit(converter.toInsights(service.getForecast(placeId, UNITS_METRIC, language.resolve())))
    }

    // ---- StudyRadarApi ----

    override fun getRadar(placeId: String, links: Map<String, String>): Flow<StudyWebContent> =
        emptyFlow()

    // ---- StudyVideoApi ----

    override fun getVideoList(placeId: String): Flow<List<StudyWebContent>> = flow {
        emit(converter.toVideos(service.getVideoList(placeId)))
    }

    // ---- capabilities this backend does not serve ----

    /**
     * Provider A has no today-stories feed. The policy layer answers
     * `supportTodayStories() == false`, so the card is never requested — but the
     * capability must still be implemented, and an empty flow is the original's answer.
     */
    override fun getTodayStories(placeId: String): Flow<List<StudyWebContent>> = emptyFlow()

    override fun getThemeCategories(): Flow<List<StudyTheme>> = emptyFlow()

    override fun getThemeRegions(categoryId: String): Flow<List<StudyTheme>> = emptyFlow()

    override fun getThemePlaces(
        categoryId: String,
        regionIds: List<String>,
    ): Flow<List<StudyThemePlace>> = emptyFlow()

    override fun getRepresentWeather(code: String): Flow<StudyWeather> = flow {
        emit(converter.toWeather(service.getRepresentForecast(code), StudyLocation(key = "", id = code)))
    }

    override fun getRepresentWeather(latitude: Double, longitude: Double): Flow<StudyWeather> =
        getRemoteWeather(latitude, longitude)

    override fun getForecastChange(placeId: String): Flow<StudyForecastChange> = emptyFlow()

    private companion object {
        const val UNITS_METRIC = "m"
    }
}

/**
 * Corresponds conceptually to `…api.forecast.twc.TwcApiLanguage`.
 *
 * Observed responsibility: each backend names languages differently — a device locale
 * has to be translated into the backend's own parameter, and an unsupported locale has
 * to fall back rather than fail. One class per provider for exactly that reason.
 */
@Singleton
class StudyProviderALanguage @Inject constructor(
    private val localeService: dev.local.weatherstudy.system.service.StudyLocaleService,
) {
    fun resolve(): String {
        val locale = localeService.getLocale()
        val tag = "${locale.language}-${locale.country}".lowercase()
        return if (tag in SUPPORTED) tag else locale.language.takeIf { it in SUPPORTED_BASE } ?: FALLBACK
    }

    private companion object {
        const val FALLBACK = "en-us"
        val SUPPORTED = setOf("en-us", "en-gb", "ja-jp", "ko-kr", "zh-cn", "de-de", "fr-fr", "es-es")
        val SUPPORTED_BASE = setOf("en", "ja", "ko", "zh", "de", "fr", "es")
    }
}
