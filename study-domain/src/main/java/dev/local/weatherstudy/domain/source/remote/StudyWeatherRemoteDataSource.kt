package dev.local.weatherstudy.domain.source.remote

import dev.local.weatherstudy.domain.entity.content.StudyInsightContent
import dev.local.weatherstudy.domain.entity.content.StudyWebContent
import dev.local.weatherstudy.domain.entity.weather.StudyBriefWeather
import dev.local.weatherstudy.domain.entity.weather.StudyCurrentObservationWithKey
import dev.local.weatherstudy.domain.entity.weather.StudyForecastChange
import dev.local.weatherstudy.domain.entity.weather.StudyLocation
import dev.local.weatherstudy.domain.entity.weather.StudyTheme
import dev.local.weatherstudy.domain.entity.weather.StudyThemePlace
import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import kotlinx.coroutines.flow.Flow

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the eleven capability interfaces in
 * com.samsung.android.weather.domain.source.remote, and their union
 * com.samsung.android.weather.domain.source.remote.WeatherRemoteDataSource
 *
 * Observed responsibility — and why this is eleven interfaces rather than one:
 * each regional backend supports a DIFFERENT SUBSET. A provider facade
 * (`TwcApi`, `WjpApi`, `WkrApi`, `HuaApi`, `SRCApi`) implements only the
 * capabilities its backend has, and the policy layer (`StudyWeatherPolicy`)
 * is what the UI consults before showing a card. Splitting the contract this
 * finely is what lets one UI serve five backends.
 *
 * Every method returns a `Flow`, not a suspend function. That matches the
 * original: the remote layer emits, the repository decides whether to persist,
 * and the refresh use cases compose the flows.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyForecastApi {
    fun getRemoteWeather(location: StudyLocation): Flow<StudyWeather>
    fun getRemoteWeather(latitude: Double, longitude: Double): Flow<StudyWeather>
    fun getRemoteWeather(locations: List<StudyLocation>): Flow<List<StudyWeather>>
}

/** Corresponds conceptually to `…remote.BriefForecastApi` — the widget/complication projection. */
interface StudyBriefForecastApi {
    fun getRemoteBriefWeather(location: StudyLocation): Flow<StudyBriefWeather>
    fun getRemoteBriefWeather(latitude: Double, longitude: Double): Flow<StudyBriefWeather>
}

/**
 * Corresponds conceptually to `…remote.CurrentObservationApi`.
 *
 * Observed responsibility: refresh only "now" without refetching the whole forecast.
 * The batch overload exists so a widget tick updates every saved city in one call.
 */
interface StudyCurrentObservationApi {
    fun getRemoteCurrentObservation(weather: StudyWeather): Flow<StudyCurrentObservationWithKey>
    fun getRemoteCurrentObservation(weathers: List<StudyWeather>): Flow<List<StudyCurrentObservationWithKey>>
}

/** Corresponds conceptually to `…remote.ForecastChangeApi`. */
interface StudyForecastChangeApi {
    fun getForecastChange(placeId: String): Flow<StudyForecastChange>
}

/**
 * Corresponds conceptually to `…remote.InsightApi`.
 *
 * Note the `links` parameter: the forecast response carries a map of follow-up URLs,
 * and the insight call is made against those rather than against a fixed endpoint.
 * The same applies to [StudyRadarApi].
 */
interface StudyInsightApi {
    fun getInsightContent(placeId: String, links: Map<String, String>): Flow<List<StudyInsightContent>>
}

/** Corresponds conceptually to `…remote.RadarApi`. */
interface StudyRadarApi {
    fun getRadar(placeId: String, links: Map<String, String>): Flow<StudyWebContent>
}

/** Corresponds conceptually to `…remote.RepresentApi` — the "representative location" lookup. */
interface StudyRepresentApi {
    fun getRepresentWeather(code: String): Flow<StudyWeather>
    fun getRepresentWeather(latitude: Double, longitude: Double): Flow<StudyWeather>
}

/** Corresponds conceptually to `…remote.SearchApi`. */
interface StudySearchApi {
    fun getSearch(key: String): Flow<List<StudyLocation>>
    fun getAutoComplete(key: String): Flow<List<StudyLocation>>
}

/** Corresponds conceptually to `…remote.ThemeApi` — map-search themed places. */
interface StudyThemeApi {
    fun getThemeCategories(): Flow<List<StudyTheme>>
    fun getThemeRegions(categoryId: String): Flow<List<StudyTheme>>
    fun getThemePlaces(categoryId: String, regionIds: List<String>): Flow<List<StudyThemePlace>>
}

/** Corresponds conceptually to `…remote.TodayStoriesApi`. */
interface StudyTodayStoriesApi {
    fun getTodayStories(placeId: String): Flow<List<StudyWebContent>>
}

/** Corresponds conceptually to `…remote.VideoApi`. */
interface StudyVideoApi {
    fun getVideoList(placeId: String): Flow<List<StudyWebContent>>
}

/**
 * Corresponds conceptually to `…remote.WeatherRemoteDataSource` — the union of all
 * eleven capabilities. A provider that cannot serve one throws or emits empty;
 * the policy layer is what stops the UI asking.
 */
interface StudyWeatherRemoteDataSource :
    StudyForecastApi,
    StudyBriefForecastApi,
    StudyCurrentObservationApi,
    StudyInsightApi,
    StudyTodayStoriesApi,
    StudyRadarApi,
    StudySearchApi,
    StudyThemeApi,
    StudyVideoApi,
    StudyRepresentApi,
    StudyForecastChangeApi
