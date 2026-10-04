package dev.local.weatherstudy.domain.usecase

import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import dev.local.weatherstudy.domain.policy.StudyWeatherPolicyManager
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.usecase.RefreshForecast
 *
 * Observed responsibility and flow — this is the pipeline `StudyBackgroundRefreshWorker`
 * and pull-to-refresh both enter. Three dependencies in the original, composed in order:
 *
 * ```
 * invoke(locations)
 *   └─ fetchWeather(locations)          network: forecast + hourly + daily
 *        └─ fetchInsightCard(weathers)  network: insight cards, using the links map
 *             └─ reviseWebLink(weather) strip/rewrite outbound links per policy
 * ```
 *
 * Note what it does NOT do: it does not persist. `RefreshForecast` returns the revised
 * aggregates and the CALLER saves them (`SaveWeather`, or `PersistenceWorker`). That is
 * why a failed save does not cost another network round trip.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyRefreshForecast @Inject constructor(
    private val fetchWeather: StudyFetchWeather,
    private val fetchInsightCard: StudyFetchInsightCard,
    private val reviseWebLink: StudyReviseWebLink,
) : StudyUsecaseK<List<StudyWeather>, List<dev.local.weatherstudy.domain.entity.weather.StudyLocation>> {

    override fun invoke(
        arg: List<dev.local.weatherstudy.domain.entity.weather.StudyLocation>,
    ): Flow<List<StudyWeather>> = flow {
        val fetched = fetchWeather(arg).toList().flatten()
        val withInsight = fetchInsightCard(fetched).toList().flatten()
        emit(withInsight.map { reviseWebLink(it, restrict = false) })
    }

    companion object {
        const val TAG = "StudyRefreshForecast"
    }
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.usecase.RefreshObservation
 *
 * Observed responsibility: the cheap refresh. Same three-stage shape as
 * [StudyRefreshForecast] but the first stage fetches only the current observation —
 * which is what a widget tick or a complication update runs, because re-pulling the
 * 10-day forecast for a 1×1 widget would be wasteful.
 */
class StudyRefreshObservation @Inject constructor(
    private val fetchCurrentObservation: StudyFetchCurrentObservation,
    private val fetchInsightCard: StudyFetchInsightCard,
    private val reviseWebLink: StudyReviseWebLink,
) : StudyUsecaseK<List<StudyWeather>, List<StudyWeather>> {

    override fun invoke(arg: List<StudyWeather>): Flow<List<StudyWeather>> = flow {
        val refreshed = fetchCurrentObservation(arg).toList().flatten()
        val withInsight = fetchInsightCard(refreshed).toList().flatten()
        emit(withInsight.map { reviseWebLink(it, restrict = false) })
    }

    companion object {
        const val TAG = "StudyRefreshObservation"
    }
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.usecase.RefreshContent
 *
 * Observed responsibility: the THIRD refresh clock — radar, videos and today-stories
 * expire on their own schedule (`ReachToContentRefreshTime`), separate from forecast
 * and observation.
 *
 * The original's five dependencies include the policy manager, and its private
 * `fetchContent(list)` is where the policy is consulted: each content family is fetched
 * **only if the active provider supports it**. That gate is the reason one code path
 * serves five backends with different content catalogues.
 */
class StudyRefreshContent @Inject constructor(
    private val fetchRadar: StudyFetchRadar,
    private val fetchVideo: StudyFetchVideo,
    private val fetchTodayStories: StudyFetchTodayStories,
    private val policyManager: StudyWeatherPolicyManager,
    private val reviseWebLink: StudyReviseWebLink,
) : StudyUsecaseK<List<StudyWeather>, List<StudyWeather>> {

    override fun invoke(arg: List<StudyWeather>): Flow<List<StudyWeather>> = flow {
        emit(fetchContent(arg).map { reviseWebLink(it, restrict = false) })
    }

    private suspend fun fetchContent(weathers: List<StudyWeather>): List<StudyWeather> =
        weathers.map { weather ->
            var result = weather
            if (policyManager.supportRadar()) {
                runCatching { fetchRadar(result).toList().firstOrNull() }
                    .getOrNull()?.let { result = it }
            }
            if (policyManager.supportVideo()) {
                runCatching { fetchVideo(result).toList().firstOrNull() }
                    .getOrNull()?.let { result = it }
            }
            if (policyManager.supportTodayStories()) {
                runCatching { fetchTodayStories(result).toList().firstOrNull() }
                    .getOrNull()?.let { result = it }
            }
            result
        }

    companion object {
        const val TAG = "StudyRefreshContent"
    }
}
