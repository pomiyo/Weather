package dev.local.weatherstudy.sync.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.local.weatherstudy.domain.repo.StudySettingsRepo
import dev.local.weatherstudy.domain.repo.StudyStatusRepo
import dev.local.weatherstudy.domain.repo.StudyWeatherRepo
import dev.local.weatherstudy.domain.type.StudyAutoRefresh
import dev.local.weatherstudy.domain.type.StudyKeys
import dev.local.weatherstudy.domain.type.StudySettingValue
import dev.local.weatherstudy.domain.usecase.StudyReachToContentRefreshTime
import dev.local.weatherstudy.domain.usecase.StudyReachToForecastRefreshTime
import dev.local.weatherstudy.domain.usecase.StudyReachToObservationRefreshTime
import dev.local.weatherstudy.domain.usecase.StudyReachToShortIntervalRefreshTime
import dev.local.weatherstudy.domain.usecase.StudyRefreshContent
import dev.local.weatherstudy.domain.usecase.StudyRefreshForecast
import dev.local.weatherstudy.domain.usecase.StudyRefreshObservation
import dev.local.weatherstudy.domain.usecase.StudyUpdateRefreshTimeWhenFailed
import dev.local.weatherstudy.domain.usecase.StudyUpdateWeather
import kotlinx.coroutines.flow.toList

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.sync.worker.BackgroundRefreshWorker
 *
 * ### Observed responsibility and the flow that matters
 *
 * This is the refresh entry point for everything that is not a user gesture: the alarm
 * tick, boot, package replace, a widget's periodic update. Its job is **deciding what
 * actually needs fetching** before fetching anything:
 *
 * ```
 * doWork()
 *   ├─ reachToShortInterval()              rate limit — bail if too soon
 *   ├─ statusRepo.setStatus(RUNNING)       so every surface sees one refresh
 *   ├─ for each saved location:
 *   │    ├─ reachToObservationRefreshTime()  → refreshObservation()
 *   │    ├─ reachToForecastRefreshTime()     → refreshForecast()
 *   │    └─ reachToContentRefreshTime()      → refreshContent()
 *   ├─ updateWeather(results)              persist
 *   └─ statusRepo.setStatus(DONE | FAILED)
 * ```
 *
 * The three independent clocks are the point. A tick that finds the forecast fresh but
 * the observation stale fetches only "now", which is why the app can refresh on a widget
 * cadence without re-pulling ten days of data. The `from` reason is carried into
 * `UpdateRefreshTimeWhenFailed` so a failed system refresh backs off further than a
 * failed user one.
 *
 * `@HiltWorker` + `@AssistedInject` is how the original injects use cases into a Worker;
 * the APK contains `BackgroundRefreshWorker_HiltModule`, confirming it.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@HiltWorker
class StudyBackgroundRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val weatherRepo: StudyWeatherRepo,
    private val settingsRepo: StudySettingsRepo,
    private val statusRepo: StudyStatusRepo,
    private val refreshForecast: StudyRefreshForecast,
    private val refreshObservation: StudyRefreshObservation,
    private val refreshContent: StudyRefreshContent,
    private val reachToForecastRefreshTime: StudyReachToForecastRefreshTime,
    private val reachToObservationRefreshTime: StudyReachToObservationRefreshTime,
    private val reachToContentRefreshTime: StudyReachToContentRefreshTime,
    private val reachToShortIntervalRefreshTime: StudyReachToShortIntervalRefreshTime,
    private val updateWeather: StudyUpdateWeather,
    private val updateRefreshTimeWhenFailed: StudyUpdateRefreshTimeWhenFailed,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val from = inputData.getInt(KEY_FROM, StudyAutoRefresh.From.SYSTEM)

        if (!reachToShortIntervalRefreshTime()) return Result.success()

        statusRepo.setStatus(StudyKeys.REFRESH, StudySettingValue.RefreshStatus.RUNNING, from)

        return runCatching { refreshAll(from) }
            .onSuccess {
                statusRepo.setStatus(StudyKeys.REFRESH, StudySettingValue.RefreshStatus.DONE, from)
            }
            .onFailure {
                statusRepo.setStatus(StudyKeys.REFRESH, StudySettingValue.RefreshStatus.FAILED, from)
                updateRefreshTimeWhenFailed(from)
            }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }

    /** the per-location, per-clock decision — see the class note */
    private suspend fun refreshAll(from: Int) {
        val stored = weatherRepo.getLocalWeathers()
        if (stored.isEmpty()) return

        val needObservation = stored.filter { reachToObservationRefreshTime(it) }
        val needForecast = stored.filter { reachToForecastRefreshTime(it) }
        val needContent = stored.filter { reachToContentRefreshTime(it) }

        val refreshed = buildMap {
            if (needObservation.isNotEmpty()) {
                refreshObservation(needObservation).toList().flatten()
                    .forEach { put(it.location.key, it) }
            }
            if (needForecast.isNotEmpty()) {
                refreshForecast(needForecast.map { it.location }).toList().flatten()
                    .forEach { put(it.location.key, it) }
            }
            if (needContent.isNotEmpty()) {
                val base = needContent.map { get(it.location.key) ?: it }
                refreshContent(base).toList().flatten().forEach { put(it.location.key, it) }
            }
        }

        if (refreshed.isNotEmpty()) {
            updateWeather(refreshed.values.toList())
            settingsRepo.setAutoRefreshNextTime(
                System.currentTimeMillis() + StudyAutoRefresh.Interval.FORECAST,
            )
        }
    }

    companion object {
        const val WORK_NAME = "study_background_refresh"
        const val KEY_FROM = "from"
    }
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.sync.worker.ForegroundRefreshWorker
 *
 * Observed difference from the background worker, and why both exist: this one is
 * **user-visible**. It is enqueued expedited, posts a `ForegroundInfo` notification, and
 * **skips the rate limiter** — a pull-to-refresh must do something. It also refreshes
 * everything rather than consulting the three clocks, because the user has asked.
 */
@HiltWorker
class StudyForegroundRefreshWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val weatherRepo: StudyWeatherRepo,
    private val statusRepo: StudyStatusRepo,
    private val refreshForecast: StudyRefreshForecast,
    private val refreshContent: StudyRefreshContent,
    private val updateWeather: StudyUpdateWeather,
    private val updateRefreshTimeWhenFailed: StudyUpdateRefreshTimeWhenFailed,
) : CoroutineWorker(context, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo =
        StudyRefreshForegroundNotification.create(context, id)

    override suspend fun doWork(): Result {
        val from = inputData.getInt(KEY_FROM, StudyAutoRefresh.From.DETAIL)
        val key = inputData.getString(KEY_LOCATION)

        statusRepo.setStatus(StudyKeys.REFRESH, StudySettingValue.RefreshStatus.RUNNING, from)

        return runCatching {
            val targets = if (key.isNullOrEmpty()) {
                weatherRepo.getLocalWeathers()
            } else {
                listOfNotNull(weatherRepo.getLocalWeather(key))
            }
            if (targets.isEmpty()) return@runCatching

            // no clock check: the user asked
            val forecast = refreshForecast(targets.map { it.location }).toList().flatten()
            val withContent = refreshContent(forecast).toList().flatten()
            updateWeather(withContent)
        }
            .onSuccess {
                statusRepo.setStatus(StudyKeys.REFRESH, StudySettingValue.RefreshStatus.DONE, from)
            }
            .onFailure {
                statusRepo.setStatus(StudyKeys.REFRESH, StudySettingValue.RefreshStatus.FAILED, from)
                updateRefreshTimeWhenFailed(from)
            }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.failure() })
    }

    companion object {
        const val WORK_NAME = "study_foreground_refresh"
        const val KEY_FROM = "from"
        const val KEY_LOCATION = "location_key"
    }
}

/**
 * Corresponds conceptually to the foreground-notification construction inside
 * `ForegroundRefreshWorker`.
 *
 * Split out here because `:study-widget` owns the notification channels in the original;
 * this keeps the worker readable and names the dependency.
 */
internal object StudyRefreshForegroundNotification {
    fun create(context: Context, id: java.util.UUID): ForegroundInfo {
        val channelId = "study_refresh"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as android.app.NotificationManager
        manager.createNotificationChannel(
            android.app.NotificationChannel(
                channelId,
                "Weather refresh",
                android.app.NotificationManager.IMPORTANCE_LOW,
            ),
        )
        val notification = android.app.Notification.Builder(context, channelId)
            .setContentTitle("Refreshing weather")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()
        return ForegroundInfo(id.hashCode(), notification)
    }
}
