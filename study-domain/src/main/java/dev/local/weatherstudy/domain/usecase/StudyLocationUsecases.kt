package dev.local.weatherstudy.domain.usecase

import dev.local.weatherstudy.domain.entity.weather.StudyLocation
import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import dev.local.weatherstudy.domain.entity.weather.isCurrentLocation
import dev.local.weatherstudy.domain.repo.StudySettingsRepo
import dev.local.weatherstudy.domain.repo.StudyWeatherRepo
import dev.local.weatherstudy.domain.repo.StudyWidgetRepo
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.usecase.AddLocation
 *
 * Observed responsibility and flow — the original injects **ten** dependencies for this
 * one operation, and that is the finding. Adding a city is not an insert:
 *
 * ```
 * invoke(location)
 *   ├─ hasLocation(key)                   already saved? → bail
 *   ├─ exceedNumOfLocation()              at the cap? → remove the oldest first
 *   ├─ fetchWeather([location])           network: forecast
 *   ├─ fetchInsightCard(weathers)         network: insights
 *   ├─ fetchContent(weathers)             network: radar / video / stories
 *   ├─ reviseYesterday(weather)           back-fill yesterday's temps
 *   ├─ reviseContent(weather)             drop unsupported content
 *   ├─ reviseWebLink(weather)             strip restricted links
 *   ├─ saveWeather(weather)               persist
 *   └─ widgetRepo.updateWeatherKey(…)     re-bind any widget pointing at a gap
 * ```
 *
 * The last step is the one that is easy to miss: a widget whose location was removed
 * must be re-pointed, so the widget repository is a dependency of *adding a location*.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyAddLocation @Inject constructor(
    private val hasLocation: StudyHasLocation,
    private val removeLocations: StudyRemoveLocations,
    private val fetchWeather: StudyFetchWeather,
    private val fetchInsightCard: StudyFetchInsightCard,
    private val fetchContent: StudyFetchContent,
    private val reviseYesterday: StudyReviseYesterday,
    private val reviseContent: StudyReviseContent,
    private val reviseWebLink: StudyReviseWebLink,
    private val saveWeather: StudySaveWeather,
    private val widgetRepo: StudyWidgetRepo,
    private val weatherRepo: StudyWeatherRepo,
) : StudyUsecase<Result<StudyWeather>, StudyLocation> {

    override suspend fun invoke(arg: StudyLocation): Result<StudyWeather> {
        if (hasLocation(arg.key)) return Result.failure(StudyAddLocationException.AlreadyExists(arg.key))

        val target = arg.withListPosition()
        val fetched = runCatching { fetchWeather(listOf(target)).toList().flatten() }
            .getOrElse { return Result.failure(StudyAddLocationException.FetchFailed(it)) }
            .firstOrNull()
            ?: return Result.failure(StudyAddLocationException.NotFound(arg.key))

        val withInsight = fetchInsightCard(listOf(fetched)).toList().flatten().firstOrNull() ?: fetched
        val withContent = fetchContent(listOf(withInsight)).toList().flatten().firstOrNull() ?: withInsight
        val revised = reviseWebLink(reviseContent(reviseYesterday(withContent)))

        saveWeather(revised)
        rebindOrphanWidgets(revised.location.key)
        return Result.success(revised)
    }

    /** a newly added city goes to the end of the list; the device location keeps slot 0 */
    private suspend fun StudyLocation.withListPosition(): StudyLocation {
        if (priority != StudyLocation.DEFAULT_PRIORITY) return this
        val last = weatherRepo.getLocalWeathers().maxOfOrNull { it.location.priority }
            ?: StudyLocation.CURRENT_LOCATION_PRIORITY
        return copy(priority = last.coerceAtLeast(StudyLocation.CURRENT_LOCATION_PRIORITY) + 1)
    }

    private suspend fun rebindOrphanWidgets(key: String) {
        widgetRepo.getWidgetInfoList()
            .filter { it.weatherKey.isEmpty() }
            .forEach { widgetRepo.updateWeatherKey(it.widgetId, key) }
    }

    companion object {
        const val TAG = "StudyAddLocation"
    }
}

/** Corresponds conceptually to `…usecase.AddLocationException`. */
sealed class StudyAddLocationException(message: String) : Exception(message) {
    class AlreadyExists(key: String) : StudyAddLocationException("already saved: $key")
    class NotFound(key: String) : StudyAddLocationException("no forecast for: $key")
    class FetchFailed(cause: Throwable) : StudyAddLocationException("fetch failed: ${cause.message}")
    data object ExceedLimit : StudyAddLocationException("location limit reached")
}

/**
 * Corresponds conceptually to `…usecase.AddCurrentLocation`.
 *
 * Observed responsibility: the device-location entry is added by coordinate and pinned
 * to [StudyLocation.CURRENT_LOCATION_PRIORITY], which is what keeps it first in the list.
 */
class StudyAddCurrentLocation @Inject constructor(
    private val addLocation: StudyAddLocation,
    private val settingsRepo: StudySettingsRepo,
    private val weatherRepo: StudyWeatherRepo,
) : StudyUsecase<Result<StudyWeather>, dev.local.weatherstudy.domain.entity.location.StudyLocationPosition> {

    override suspend fun invoke(
        arg: dev.local.weatherstudy.domain.entity.location.StudyLocationPosition,
    ): Result<StudyWeather> {
        if (!arg.isValid) return Result.failure(StudyAddLocationException.NotFound("no fix"))
        val placeholder = StudyLocation(
            key = CURRENT_LOCATION_KEY,
            latitude = arg.latitude,
            longitude = arg.longitude,
            priority = StudyLocation.CURRENT_LOCATION_PRIORITY,
        )
        return addLocation(placeholder).onSuccess { saved ->
            // there is one device-location entry: an earlier one under another key is stale
            val stale = weatherRepo.getLocalWeathers()
                .filter { it.location.isCurrentLocation && it.location.key != saved.location.key }
                .map { it.location.key }
            if (stale.isNotEmpty()) weatherRepo.deleteWeathersByKey(stale)
            settingsRepo.setSuccessOnLocation(1)
            settingsRepo.setFavoriteLocation(saved.location.key)
        }
    }

    companion object {
        const val CURRENT_LOCATION_KEY = "CURRENT"
    }
}

/**
 * Corresponds conceptually to `…usecase.RemoveLocations`.
 *
 * Observed responsibility: two dependencies, because deleting a city may invalidate the
 * favourite-location setting — the widgets and complications read that setting, so it
 * has to be repaired in the same operation.
 */
class StudyRemoveLocations @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
    private val settingsRepo: StudySettingsRepo,
) : StudyUsecase<Int, List<String>> {

    override suspend fun invoke(arg: List<String>): Int {
        val removed = weatherRepo.deleteWeathersByKey(arg)
        val favourite = settingsRepo.getFavoriteLocation()
        if (favourite in arg) {
            val next = weatherRepo.getLocalWeathers().minByOrNull { it.location.priority }
            settingsRepo.setFavoriteLocation(next?.location?.key ?: "")
        }
        return removed
    }
}

/**
 * Corresponds conceptually to `…usecase.ReorderLocations`.
 *
 * Observed responsibility: the drag-to-reorder in the locations list. Note the current
 * location is excluded from reordering — it is pinned by priority, not by position.
 */
class StudyReorderLocations @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
    private val settingsRepo: StudySettingsRepo,
) : StudyUsecase<Int, List<String>> {

    override suspend fun invoke(arg: List<String>): Int {
        val pinned = weatherRepo.getLocalWeathers()
            .filter { it.location.isCurrentLocation }
            .map { it.location.key }
        val ordered = pinned + arg.filterNot { it in pinned }
        val result = weatherRepo.updateOrder(ordered)
        ordered.firstOrNull()?.let { settingsRepo.setFavoriteLocation(it) }
        return result
    }
}

/** Corresponds conceptually to `…usecase.RemoveAllLocations`. */
class StudyRemoveAllLocations @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
    private val settingsRepo: StudySettingsRepo,
) : StudySingleUsecase<Int> {
    override suspend fun invoke(): Int {
        val removed = weatherRepo.deleteAll()
        settingsRepo.setFavoriteLocation("")
        return removed
    }
}

/** Corresponds conceptually to `…usecase.ReplaceLocations`. */
class StudyReplaceLocations @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
) : StudyUsecase<Int, List<StudyWeather>> {
    override suspend fun invoke(arg: List<StudyWeather>): Int = weatherRepo.replaceWeathers(arg)
}

/** Corresponds conceptually to `…usecase.HasLocation`. */
class StudyHasLocation @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
) : StudyUsecase<Boolean, String> {
    override suspend fun invoke(arg: String): Boolean = weatherRepo.isExist(arg)
}

/** Corresponds conceptually to `…usecase.GetLocationCount`. */
class StudyGetLocationCount @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
) : StudySingleUsecase<Int> {
    override suspend fun invoke(): Int = weatherRepo.getLocalWeathers().size
}

/**
 * Corresponds conceptually to `…usecase.GetUserSavedLocationCount`.
 *
 * Observed responsibility: the count EXCLUDING the device-location entry. The detail
 * screen uses this (not the raw count) to decide whether to show the "add a city"
 * affordance, because a user with only the current location has saved nothing.
 */
class StudyGetUserSavedLocationCount @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
) : StudySingleUsecase<Int> {
    override suspend fun invoke(): Int =
        weatherRepo.getLocalWeathers().count { !it.location.isCurrentLocation }
}

/**
 * Corresponds conceptually to `…usecase.ExceedNumOfLocation`.
 *
 * Observed responsibility: the saved-location cap. Two dependencies in the original.
 */
class StudyExceedNumOfLocation @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
    private val getLocationCount: StudyGetLocationCount,
) : StudySingleUsecase<Boolean> {
    override suspend fun invoke(): Boolean = getLocationCount() >= MAX_LOCATION_COUNT

    companion object {
        const val MAX_LOCATION_COUNT = 10
    }
}

/**
 * Corresponds conceptually to `…usecase.GetFavoriteLocation`.
 *
 * Observed responsibility: resolve the favourite key to a whole aggregate, falling back
 * to the first saved location. Widgets, complications and the edge panel all start here.
 */
class StudyGetFavoriteLocation @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
    private val settingsRepo: StudySettingsRepo,
) : StudySingleUsecase<StudyWeather?> {
    override suspend fun invoke(): StudyWeather? {
        val key = settingsRepo.getFavoriteLocation()
        return weatherRepo.getLocalWeather(key)
            ?: weatherRepo.getLocalWeathers().minByOrNull { it.location.priority }
    }
}

/**
 * Corresponds conceptually to `…usecase.SearchLocations`.
 *
 * Observed responsibility: the original holds a compiled `specialCharactersRegex` as a
 * field and strips it from the query before calling the provider — a search-term
 * sanitiser, kept here because it changes results, not just hygiene.
 */
class StudySearchLocations @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
) : StudyUsecaseK<List<StudyLocation>, String> {

    override fun invoke(arg: String): Flow<List<StudyLocation>> =
        weatherRepo.getSearch(sanitise(arg))

    private fun sanitise(query: String) = query.replace(SPECIAL_CHARACTERS, " ").trim()

    private companion object {
        val SPECIAL_CHARACTERS = Regex("""[\\/:*?"<>|\[\]{}()!@#$%^&=+~`;]""")
    }
}

/** Corresponds conceptually to `…usecase.SearchAutocompletedLocations`. */
class StudySearchAutocompletedLocations @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
) : StudyUsecaseK<List<StudyLocation>, String> {
    override fun invoke(arg: String): Flow<List<StudyLocation>> = weatherRepo.getAutoComplete(arg)
}

/** Corresponds conceptually to `…usecase.SearchLocationsException`. */
class StudySearchLocationsException(message: String) : Exception(message)

/**
 * Corresponds conceptually to `…usecase.SaveWeather`.
 *
 * Observed responsibility: two dependencies — it persists AND maintains the favourite
 * setting, so the first location ever saved becomes the favourite without a separate step.
 */
class StudySaveWeather @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
    private val settingsRepo: StudySettingsRepo,
) : StudyActionUsecase<StudyWeather> {
    override suspend fun invoke(arg: StudyWeather) {
        weatherRepo.saveWeather(arg)
        if (settingsRepo.getFavoriteLocation().isEmpty()) {
            settingsRepo.setFavoriteLocation(arg.location.key)
        }
    }
}

/** Corresponds conceptually to `…usecase.UpdateWeather`. */
class StudyUpdateWeather @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
    private val settingsRepo: StudySettingsRepo,
) : StudyActionUsecase<List<StudyWeather>> {
    override suspend fun invoke(arg: List<StudyWeather>) {
        // the device-location entry is refetched by coordinate, so its key follows the device;
        // when it moves, the row under the old key has to go
        val movedTo = arg.firstOrNull { it.location.isCurrentLocation }?.location?.key
        val stale = if (movedTo == null) {
            emptyList()
        } else {
            weatherRepo.getLocalWeathers()
                .filter { it.location.isCurrentLocation && it.location.key != movedTo }
                .map { it.location.key }
        }
        weatherRepo.updateWeathers(arg)
        if (movedTo != null && stale.isNotEmpty()) {
            weatherRepo.deleteWeathersByKey(stale)
            if (settingsRepo.getFavoriteLocation() in stale) settingsRepo.setFavoriteLocation(movedTo)
        }
    }
}

/**
 * Corresponds conceptually to `…usecase.ObserveWeatherChange`.
 *
 * Observed responsibility: the hot stream every screen renders from. Three dependencies
 * in the original — it folds the consent check in, so a user who has not agreed sees an
 * empty list rather than data.
 */
class StudyObserveWeatherChange @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
    private val settingsRepo: StudySettingsRepo,
    private val whetherToConsentUcl: StudyWhetherToConsentUcl,
) : StudySingleUsecaseK<List<StudyWeather>> {

    override fun invoke(): Flow<List<StudyWeather>> =
        kotlinx.coroutines.flow.combine(
            weatherRepo.observeWeathers(),
            settingsRepo.observeFavoriteLocation(),
        ) { weathers, favourite ->
            // the favourite decides which page OPENS, not where the page sits: reordering on
            // every selection would move pages under the user's finger
            if (whetherToConsentUcl()) emptyList()
            else weathers.sortedBy { it.location.priority }
        }
}

/** Corresponds conceptually to `…usecase.ObserveTempScale`. */
class StudyObserveTempScale @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
) : StudySingleUsecaseK<Int> {
    override fun invoke(): Flow<Int> = settingsRepo.observeTempScale()
}

/** Corresponds conceptually to `…usecase.UpdateTempScale`. */
class StudyUpdateTempScale @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
) : StudyActionUsecase<Int> {
    override suspend fun invoke(arg: Int) = settingsRepo.setTempScale(arg)
}

/** Corresponds conceptually to `…usecase.ObserveSuccessOnLocation`. */
class StudyObserveSuccessOnLocation @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
) : StudySingleUsecaseK<Int> {
    override fun invoke(): Flow<Int> = settingsRepo.observeSuccessOnLocation()
}

/** Corresponds conceptually to `…usecase.GetSavedLocationCount`. */
class StudyGetSavedLocationCount @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
) : StudySingleUsecase<Int> {
    override suspend fun invoke(): Int = weatherRepo.getLocalWeathers().size
}

/** Corresponds conceptually to `…usecase.GetRepresentCode`. */
class StudyGetRepresentCode @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
) : StudySingleUsecase<String> {
    override suspend fun invoke(): String = settingsRepo.getActiveCpType()
}
