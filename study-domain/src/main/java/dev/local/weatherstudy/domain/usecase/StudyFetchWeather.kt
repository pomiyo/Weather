package dev.local.weatherstudy.domain.usecase

import dev.local.weatherstudy.domain.entity.weather.StudyLocation
import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import dev.local.weatherstudy.domain.entity.weather.isCurrentLocation
import dev.local.weatherstudy.domain.repo.StudyWeatherRepo
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.usecase.FetchWeather  (interface)
 * com.samsung.android.weather.domain.usecase.FetchWeatherImpl
 *
 * Observed responsibility — this is where the network fan-out actually happens, and
 * the structure is the point. The original's impl has three private methods plus
 * `invoke`, and they partition the location list by KIND before calling the API:
 *
 * ```
 * invoke(locations)
 *   ├─ getCurrent(locations)                  → the device-location entry
 *   ├─ getRepresent(locations)                → the "representative location" entry
 *   └─ getForecast(locations, predicate)      → everything else, batched
 *         ↓
 *      assignIconNum(weather)                 → applied to every result
 * ```
 *
 * The three paths exist because they hit different endpoints: the current location is
 * fetched by coordinate, the representative location by region code, and saved cities
 * by provider key — and the saved cities go in ONE batched request. Reducing this to a
 * single `getRemoteWeather(location)` loop would change both the request count and the
 * behaviour when the device has no fix.
 *
 * `assignIconNum` is applied centrally here, not per provider, which is why every
 * provider's converter can leave `iconNum` unset.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyFetchWeather : StudyUsecaseK<List<StudyWeather>, List<StudyLocation>>

class StudyFetchWeatherImpl @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
    private val fetchCurrent: StudyFetchCurrent,
    private val fetchRepresent: StudyFetchRepresent,
    private val assignIconNum: StudyAssignIconNum,
) : StudyFetchWeather {

    override fun invoke(arg: List<StudyLocation>): Flow<List<StudyWeather>> = flow {
        val fetched = buildList {
            getCurrent(arg)?.let { add(it) }
            getRepresent(arg)?.let { add(it) }
            addAll(getForecast(arg) { !it.isCurrentLocation && !it.isRepresent() })
        }
        emit(fetched.map { assignIconNum(it) })
    }

    /** the device-location entry, fetched by coordinate */
    private suspend fun getCurrent(locations: List<StudyLocation>): StudyWeather? {
        val current = locations.firstOrNull { it.isCurrentLocation } ?: return null
        return runCatching { fetchCurrent(current).toList().firstOrNull() }.getOrNull()
    }

    /** the representative-location entry, fetched by region code */
    private suspend fun getRepresent(locations: List<StudyLocation>): StudyWeather? {
        val represent = locations.firstOrNull { it.isRepresent() } ?: return null
        return runCatching { fetchRepresent(represent).toList().firstOrNull() }.getOrNull()
    }

    /** saved cities, batched into one provider call */
    private suspend fun getForecast(
        locations: List<StudyLocation>,
        predicate: (StudyLocation) -> Boolean,
    ): List<StudyWeather> {
        val targets = locations.filter(predicate)
        if (targets.isEmpty()) return emptyList()
        return runCatching {
            weatherRepo.getRemoteWeather(targets).toList().flatten()
        }.getOrDefault(emptyList())
    }

    private fun StudyLocation.isRepresent() = id.isNotEmpty() && key.isEmpty()

    companion object {
        const val TAG = "StudyFetchWeather"
    }
}

/**
 * Corresponds conceptually to `…usecase.FetchCurrent` / `FetchCurrentImpl`.
 *
 * Observed responsibility: fetch by coordinate, then centrally assign the icon number.
 * Same two dependencies as the original.
 */
interface StudyFetchCurrent : StudyUsecaseK<StudyWeather, StudyLocation>

class StudyFetchCurrentImpl @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
    private val assignIconNum: StudyAssignIconNum,
) : StudyFetchCurrent {
    override fun invoke(arg: StudyLocation): Flow<StudyWeather> =
        weatherRepo.getRemoteWeather(arg.latitude, arg.longitude).map { fetched ->
            // a by-coordinate answer knows nothing of the list: the slot, and the label the
            // user gave it, belong to the entry that asked
            assignIconNum(
                fetched.copy(
                    location = fetched.location.copy(
                        priority = arg.priority,
                        label = arg.label,
                        labelType = arg.labelType,
                    ),
                ),
            )
        }
}

/**
 * Corresponds conceptually to `…usecase.FetchRepresent`.
 *
 * Observed responsibility: the "representative location" is a region-level place the
 * app can show before any city is saved and before a location permission is granted.
 * It is fetched by code, not coordinate — which is why it is a separate use case.
 */
class StudyFetchRepresent @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
    private val assignIconNum: StudyAssignIconNum,
) : StudyUsecaseK<StudyWeather, StudyLocation> {
    override fun invoke(arg: StudyLocation): Flow<StudyWeather> =
        if (arg.id.isEmpty()) emptyFlow()
        else weatherRepo.getRepresentWeather(arg.id).map { assignIconNum(it) }
}

/**
 * Corresponds conceptually to `…usecase.FetchCurrentObservation` / `…Impl`.
 *
 * Observed responsibility: refresh only "now". The original's impl has a private
 * `getCurrentObservation(list, predicate)` for exactly the same reason
 * [StudyFetchWeatherImpl] partitions — the batch call is separate from the by-coordinate
 * one.
 */
interface StudyFetchCurrentObservation : StudyUsecaseK<List<StudyWeather>, List<StudyWeather>>

class StudyFetchCurrentObservationImpl @Inject constructor(
    private val weatherRepo: StudyWeatherRepo,
    private val assignIconNum: StudyAssignIconNum,
) : StudyFetchCurrentObservation {

    override fun invoke(arg: List<StudyWeather>): Flow<List<StudyWeather>> = flow {
        emit(getCurrentObservation(arg) { true })
    }

    private suspend fun getCurrentObservation(
        weathers: List<StudyWeather>,
        predicate: (StudyWeather) -> Boolean,
    ): List<StudyWeather> {
        val targets = weathers.filter(predicate)
        if (targets.isEmpty()) return weathers
        val observations = runCatching {
            weatherRepo.getRemoteCurrentObservation(targets).toList().flatten()
        }.getOrDefault(emptyList()).associateBy { it.key }

        return weathers.map { weather ->
            observations[weather.location.key]
                ?.let { assignIconNum(weather.copy(currentObservation = it.observation)) }
                ?: weather
        }
    }
}
