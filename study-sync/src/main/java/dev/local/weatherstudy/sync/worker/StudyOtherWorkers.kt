package dev.local.weatherstudy.sync.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.local.weatherstudy.domain.entity.location.StudyLocationPosition
import dev.local.weatherstudy.domain.entity.weather.StudyAwayModeLocation
import dev.local.weatherstudy.domain.entity.location.StudyWeatherGeofence
import dev.local.weatherstudy.domain.repo.StudySettingsRepo
import dev.local.weatherstudy.domain.repo.StudyStatusRepo
import dev.local.weatherstudy.domain.repo.StudyWeatherRepo
import dev.local.weatherstudy.domain.source.location.StudyLocationProvider
import dev.local.weatherstudy.domain.source.location.StudyWeatherGeofenceProvider
import dev.local.weatherstudy.domain.type.StudyKeys
import dev.local.weatherstudy.domain.type.StudySettingValue
import dev.local.weatherstudy.domain.usecase.StudyAddCurrentLocation
import kotlinx.coroutines.flow.first

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.sync.worker.AddCurrentLocationWorker
 *
 * Observed responsibility: adding the device-location entry is slow and failure-prone —
 * it waits for a fix, then does a network round trip — so it runs as work rather than
 * inline. `GetCurrentFragment` enqueues it and watches the status row, which is why
 * [StudyStatusRepo] is a dependency and the key is `StudyKeys.CURRENT`.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@HiltWorker
class StudyAddCurrentLocationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val locationProvider: StudyLocationProvider,
    private val addCurrentLocation: StudyAddCurrentLocation,
    private val statusRepo: StudyStatusRepo,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        statusRepo.setStatus(StudyKeys.CURRENT, StudySettingValue.RefreshStatus.RUNNING, 0)

        val position = runCatching { locationProvider.getLocation().first() }
            .getOrDefault(StudyLocationPosition.INVALID)

        if (!position.isValid) {
            statusRepo.setStatus(StudyKeys.CURRENT, StudySettingValue.RefreshStatus.FAILED, 0)
            // no fix is a state to report, not one to retry blindly: the screen that started
            // this offers "try again", and the next attempt may need the user to act first
            return Result.failure()
        }

        return addCurrentLocation(position).fold(
            onSuccess = {
                statusRepo.setStatus(StudyKeys.CURRENT, StudySettingValue.RefreshStatus.DONE, 0)
                Result.success()
            },
            onFailure = {
                statusRepo.setStatus(StudyKeys.CURRENT, StudySettingValue.RefreshStatus.FAILED, 0)
                Result.failure()
            },
        )
    }

    companion object {
        const val WORK_NAME = "study_add_current_location"
    }
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.sync.worker.GeofenceCalibrationWorker
 *
 * Observed responsibility: re-centre the single away-mode geofence as the user's home
 * area shifts. It runs as work because geofence registration is rate-limited by the
 * platform and must survive process death.
 */
@HiltWorker
class StudyGeofenceCalibrationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val locationProvider: StudyLocationProvider,
    private val geofenceProvider: StudyWeatherGeofenceProvider,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val position = runCatching { locationProvider.getLocation().first() }
            .getOrDefault(StudyLocationPosition.INVALID)
        if (!position.isValid) return Result.retry()

        geofenceProvider.updateFence(
            StudyWeatherGeofence(
                latitude = position.latitude,
                longitude = position.longitude,
                radius = StudyWeatherGeofence.DEFAULT_RADIUS,
            ),
        )
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "study_geofence_calibration"
    }
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.sync.worker.HomeToAwayModeWorker
 *
 * Observed responsibility: the geofence EXIT/DWELL transition arrives at
 * `WeatherGeofenceReceiver`, which enqueues this. It records the away location against
 * the home one — and note it records the PROVIDER for each side, because home and away
 * can be served by different regional backends when the user travels (see
 * `StudyAwayModeLocationsEntity`).
 */
@HiltWorker
class StudyHomeToAwayModeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val weatherRepo: StudyWeatherRepo,
    private val settingsRepo: StudySettingsRepo,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val awayKey = inputData.getString(KEY_AWAY_LOCATION) ?: return Result.failure()
        val homeKey = settingsRepo.getFavoriteLocation()

        weatherRepo.addAwayLocationKey(
            StudyAwayModeLocation(
                awayLocation = awayKey,
                homeLocation = homeKey,
                enterTime = System.currentTimeMillis(),
                isActive = true,
            ),
        )
        settingsRepo.setIsAwayMode(true)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "study_home_to_away"
        const val KEY_AWAY_LOCATION = "away_location"
    }
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.sync.worker.PersistenceWorker
 *
 * Observed responsibility: deferred writes. The refresh use cases deliberately do not
 * persist (see `StudyRefreshForecast`), so a caller that cannot wait — a widget update,
 * a content-provider write — hands the result here instead of blocking.
 */
@HiltWorker
class StudyPersistenceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val weatherRepo: StudyWeatherRepo,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val keys = inputData.getStringArray(KEY_LOCATIONS)?.toList() ?: return Result.success()
        val stored = keys.mapNotNull { weatherRepo.getLocalWeather(it) }
        if (stored.isNotEmpty()) weatherRepo.updateWeathers(stored)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "study_persistence"
        const val KEY_LOCATIONS = "locations"
    }
}
