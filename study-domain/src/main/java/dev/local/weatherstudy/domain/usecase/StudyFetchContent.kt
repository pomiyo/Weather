package dev.local.weatherstudy.domain.usecase

import dev.local.weatherstudy.domain.entity.content.StudyInsightContent
import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import dev.local.weatherstudy.domain.entity.forecast.StudyForecastProviderManager
import dev.local.weatherstudy.domain.policy.StudyWeatherPolicyManager
import dev.local.weatherstudy.domain.repo.StudyWeatherRepo
import dev.local.weatherstudy.domain.type.StudyContentType
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.usecase.FetchInsightCard / FetchInsightCardImpl
 *
 * Observed responsibility and flow — four dependencies in the original, and the two
 * "insert" use cases run AFTER the network call:
 *
 * ```
 * invoke(weathers)
 *   ├─ policyManager.supportInsightCard()        gate
 *   ├─ weatherRepo.getInsightContent(key, links) network, via the links map
 *   ├─ insertTwilight(weather, insights)         add locally-derived sun/twilight insights
 *   └─ insertIndexDescription(weather, insights) add locally-derived index-condition insights
 * ```
 *
 * That ordering is the finding: **not every insight comes from the server.** Six of the
 * insight types (`WIND_CONDITION`, `DEW_POINT_CONDITION`, `PRESSURE_CONDITION`,
 * `UV_CONDITION`, `HUMIDITY_CONDITION`, `VISIBILITY_CONDITION`) plus the twilight ones
 * are synthesised on-device from the current observation and merged into the same list.
 * The detail card cannot tell the difference, and that is deliberate.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyFetchInsightCard : StudyUsecaseK<List<StudyWeather>, List<StudyWeather>>

class StudyFetchInsightCardImpl @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
    private val policyManager: StudyWeatherPolicyManager,
    private val insertTwilight: StudyInsertTwilight,
    private val insertIndexDescription: StudyInsertIndexDescription,
) : StudyFetchInsightCard {

    override fun invoke(arg: List<StudyWeather>): Flow<List<StudyWeather>> = flow {
        if (!policyManager.supportInsightCard()) {
            emit(arg)
            return@flow
        }
        emit(
            arg.map { weather ->
                val remote = fetchRemoteInsights(weather)
                val withTwilight = insertTwilight(weather to remote)
                insertIndexDescription(withTwilight to remote)
            },
        )
    }

    private suspend fun fetchRemoteInsights(weather: StudyWeather): List<StudyInsightContent> =
        runCatching {
            weatherRepo.getInsightContent(weather.location.key, weather.links).toList().flatten()
        }.getOrDefault(emptyList())
}

/**
 * Corresponds conceptually to `…usecase.FetchRadar`.
 *
 * Note the signature: like [StudyFetchInsightCard] it takes the whole aggregate, because
 * the radar endpoint is reached through the `links` map the forecast response supplied.
 */
class StudyFetchRadar @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
) : StudyUsecaseK<StudyWeather, StudyWeather> {
    override fun invoke(arg: StudyWeather): Flow<StudyWeather> = flow {
        val radar = runCatching {
            weatherRepo.getRadar(arg.location.key, arg.links).toList().firstOrNull()
        }.getOrNull()
        emit(if (radar != null) arg.copy(radar = radar) else arg)
    }
}

/** Corresponds conceptually to `…usecase.FetchVideo`. */
class StudyFetchVideo @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
) : StudyUsecaseK<StudyWeather, StudyWeather> {
    override fun invoke(arg: StudyWeather): Flow<StudyWeather> = flow {
        val videos = runCatching {
            weatherRepo.getVideoList(arg.location.key).toList().flatten()
        }.getOrDefault(emptyList())
        emit(arg.copy(videos = videos.map { it.copy(type = StudyContentType.VIDEO) }))
    }
}

/** Corresponds conceptually to `…usecase.FetchTodayStories`. */
class StudyFetchTodayStories @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
) : StudyUsecaseK<StudyWeather, StudyWeather> {
    override fun invoke(arg: StudyWeather): Flow<StudyWeather> = flow {
        val stories = runCatching {
            weatherRepo.getTodayStories(arg.location.key).toList().flatten()
        }.getOrDefault(emptyList())
        emit(arg.copy(todayStories = stories.map { it.copy(type = StudyContentType.TODAY_STORIES) }))
    }
}

/** Corresponds conceptually to `…usecase.FetchContent` / `FetchContentImpl`. */
interface StudyFetchContent : StudyUsecaseK<List<StudyWeather>, List<StudyWeather>>

class StudyFetchContentImpl @Inject constructor(
    private val refreshContent: StudyRefreshContent,
) : StudyFetchContent {
    override fun invoke(arg: List<StudyWeather>): Flow<List<StudyWeather>> = refreshContent(arg)
}

/** Corresponds conceptually to `…usecase.FetchForecastChange`, via `ForecastChangeApi`. */
class StudyFetchForecastChange @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
    private val forecastProviderManager: StudyForecastProviderManager,
) : StudyUsecaseK<StudyWeather, StudyWeather> {
    override fun invoke(arg: StudyWeather): Flow<StudyWeather> = flow {
        val change = runCatching {
            weatherRepo.getForecastChange(arg.location.key).toList().firstOrNull()
        }.getOrNull()
        emit(if (change != null) arg.copy(forecastChange = change) else arg)
    }
}
