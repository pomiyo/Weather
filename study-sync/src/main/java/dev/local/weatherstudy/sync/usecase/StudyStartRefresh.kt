package dev.local.weatherstudy.sync.usecase

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dev.local.weatherstudy.domain.type.StudyAutoRefresh
import dev.local.weatherstudy.domain.usecase.StudyActionUsecase
import dev.local.weatherstudy.domain.usecase.StudyPureUsecase
import dev.local.weatherstudy.domain.usecase.StudySyncAutoRefresh
import dev.local.weatherstudy.sync.worker.StudyAddCurrentLocationWorker
import dev.local.weatherstudy.sync.worker.StudyBackgroundRefreshWorker
import dev.local.weatherstudy.sync.worker.StudyForegroundRefreshWorker
import dev.local.weatherstudy.sync.worker.StudyGeofenceCalibrationWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.usecase.StartBackgroundRefresh
 * com.samsung.android.weather.domain.usecase.StartForegroundRefresh
 * com.samsung.android.weather.domain.usecase.StartCurrentLocationAddition
 * com.samsung.android.weather.domain.usecase.SyncGeofence
 * and the 57 classes in `com.samsung.android.weather.sync.usecase`
 *
 * ### Why the enqueue is a use case and not a call to WorkManager
 *
 * The original declares `StartBackgroundRefresh` / `StartForegroundRefresh` in
 * `domain.usecase` — as **interfaces** — and implements them here in `:weather-sync`.
 * So the domain can say "start a refresh" without depending on WorkManager, and the
 * ViewModels, receivers and widgets all go through the same door.
 *
 * Three scheduling details preserved:
 * - **unique work names**, from [dev.local.weatherstudy.domain.type.StudyKeys], so five
 *   widgets ticking at once collapse into one refresh
 * - **`KEEP` for background, `REPLACE` for foreground** — a periodic refresh must not
 *   restart on every trigger, but a user's pull-to-refresh supersedes a pending one
 * - **expedited + `RUN_AS_NON_EXPEDITED_WORK_REQUEST` fallback** on the foreground
 *   worker, which is what makes pull-to-refresh feel immediate while still being work
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyStartBackgroundRefresh @Inject constructor(
    private val context: Context,
    private val syncAutoRefresh: StudySyncAutoRefresh,
) : StudyActionUsecase<Int> {

    override suspend fun invoke(arg: Int) {
        val intervalMillis = syncAutoRefresh()
        val workManager = WorkManager.getInstance(context)

        if (intervalMillis == StudySyncAutoRefresh.CANCEL) {
            // the setting is "manual only": cancel rather than schedule a long interval
            workManager.cancelUniqueWork(StudyBackgroundRefreshWorker.WORK_NAME)
            return
        }

        workManager.enqueueUniquePeriodicWork(
            StudyBackgroundRefreshWorker.WORK_NAME,
            // UPDATE keeps the enqueued work's identity and next-run time but adopts a new
            // interval, so changing the setting takes effect without resetting the schedule
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<StudyBackgroundRefreshWorker>(
                intervalMillis,
                TimeUnit.MILLISECONDS,
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setInputData(workDataOf(StudyBackgroundRefreshWorker.KEY_FROM to arg))
                .build(),
        )
    }
}

/** Corresponds conceptually to `…usecase.StartForegroundRefresh`. */
class StudyStartForegroundRefresh @Inject constructor(
    private val context: Context,
) : StudyActionUsecase<StudyForegroundRefreshRequest> {

    override suspend fun invoke(arg: StudyForegroundRefreshRequest) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            StudyForegroundRefreshWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<StudyForegroundRefreshWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(
                    workDataOf(
                        StudyForegroundRefreshWorker.KEY_FROM to arg.from,
                        StudyForegroundRefreshWorker.KEY_LOCATION to arg.locationKey,
                    ),
                )
                .build(),
        )
    }
}

/** The argument to [StudyStartForegroundRefresh]. */
data class StudyForegroundRefreshRequest(
    val from: Int = StudyAutoRefresh.From.DETAIL,
    val locationKey: String = "",
)

/** Corresponds conceptually to `…usecase.StartCurrentLocationAddition`. */
class StudyStartCurrentLocationAddition @Inject constructor(
    private val context: Context,
) : StudyPureUsecase {
    override suspend fun invoke() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            StudyAddCurrentLocationWorker.WORK_NAME,
            // REPLACE, not KEEP: the user is watching a progress screen, and a run that is
            // sitting in back-off after a failed fix must not swallow their "try again"
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<StudyAddCurrentLocationWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build(),
        )
    }
}

/** Corresponds conceptually to `…usecase.SyncGeofence`. */
class StudySyncGeofence @Inject constructor(
    private val context: Context,
) : StudyPureUsecase {
    override suspend fun invoke() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            StudyGeofenceCalibrationWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<StudyGeofenceCalibrationWorker>().build(),
        )
    }
}

/** Corresponds conceptually to `…usecase.StopAutoRefresh`'s scheduling half. */
class StudyCancelBackgroundRefresh @Inject constructor(
    private val context: Context,
) : StudyPureUsecase {
    override suspend fun invoke() {
        WorkManager.getInstance(context)
            .cancelUniqueWork(StudyBackgroundRefreshWorker.WORK_NAME)
    }
}
