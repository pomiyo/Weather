package dev.local.weatherstudy.domain.usecase

import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import dev.local.weatherstudy.domain.entity.weather.minContentExpireTime
import dev.local.weatherstudy.domain.entity.weather.minForecastExpireTime
import dev.local.weatherstudy.domain.policy.StudyWeatherPolicyManager
import dev.local.weatherstudy.domain.repo.StudySettingsRepo
import dev.local.weatherstudy.domain.repo.StudyStatusRepo
import dev.local.weatherstudy.domain.type.StudyAutoRefresh
import dev.local.weatherstudy.domain.type.StudySettingValue
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.usecase.ReachToForecastRefreshTime
 * com.samsung.android.weather.domain.usecase.ReachToObservationRefreshTime / …Impl
 * com.samsung.android.weather.domain.usecase.ReachToContentRefreshTime
 * com.samsung.android.weather.domain.usecase.ReachToShortIntervalRefreshTime / …Impl
 *
 * Observed responsibility — there are FOUR independent staleness clocks, not one:
 *
 * | use case | asks | reads |
 * |---|---|---|
 * | `ReachToForecastRefreshTime`           | is the 10-day forecast stale? | the data's own `expireTime` |
 * | `ReachToObservationRefreshTime`        | is "now" stale? | `expireTime` + policy |
 * | `ReachToContentRefreshTime`            | are radar/video/stories stale? | insight `expireTime` |
 * | `ReachToShortIntervalRefreshTime`      | may we refresh again at all yet? | a floor interval |
 *
 * Two details worth preserving:
 * - `ReachToForecastRefreshTime` takes **no dependencies** in the original: staleness is
 *   a property of the data, read off `minForecastExpireTime()`. Nothing global is consulted.
 * - `ReachToObservationRefreshTime` DOES inject the policy manager, because a provider
 *   may declare `supportFixedRefreshInterval()` and override the data's expiry.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyReachToForecastRefreshTime @Inject constructor() :
    StudyUsecase<Boolean, StudyWeather> {
    override suspend fun invoke(arg: StudyWeather): Boolean {
        val expire = arg.minForecastExpireTime()
        return expire <= 0L || expire < System.currentTimeMillis()
    }
}

/** Corresponds conceptually to `…usecase.ReachToObservationRefreshTime` / `…Impl`. */
interface StudyReachToObservationRefreshTime : StudyUsecase<Boolean, StudyWeather>

class StudyReachToObservationRefreshTimeImpl @Inject constructor(
    private val policyManager: StudyWeatherPolicyManager,
) : StudyReachToObservationRefreshTime {
    override suspend fun invoke(arg: StudyWeather): Boolean {
        val now = System.currentTimeMillis()
        if (policyManager.supportFixedRefreshInterval()) {
            return now - arg.currentObservation.time.updateTime >= FIXED_INTERVAL
        }
        if (!policyManager.supportExpireTime()) return true
        val expire = arg.currentObservation.time.expireTime
        return expire <= 0L || expire < now
    }

    private companion object {
        const val FIXED_INTERVAL = 60L * 60L * 1000L
    }
}

/** Corresponds conceptually to `…usecase.ReachToContentRefreshTime`. */
class StudyReachToContentRefreshTime @Inject constructor() : StudyUsecase<Boolean, StudyWeather> {
    override suspend fun invoke(arg: StudyWeather): Boolean {
        val expire = arg.minContentExpireTime()
        return expire == Long.MAX_VALUE || expire < System.currentTimeMillis()
    }
}

/**
 * Corresponds conceptually to `…usecase.ReachToShortIntervalRefreshTime` / `…Impl`.
 *
 * Observed responsibility: the rate limiter. It is what stops five widgets ticking at
 * once from producing five network passes.
 */
interface StudyReachToShortIntervalRefreshTime : StudySingleUsecase<Boolean>

class StudyReachToShortIntervalRefreshTimeImpl @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
) : StudyReachToShortIntervalRefreshTime {
    override suspend fun invoke(): Boolean {
        val next = settingsRepo.getAutoRefreshNextTime()
        return next <= 0L || next < System.currentTimeMillis()
    }
}

/**
 * Corresponds conceptually to `…usecase.ObserveRefreshStatus`.
 *
 * Observed responsibility: a single dependency on [StudyStatusRepo]. The refresh state
 * the detail screen's spinner reflects is shared app state, not ViewModel state — a
 * refresh started by a widget shows up in the UI.
 */
class StudyObserveRefreshStatus @Inject constructor(
    private val statusRepo: StudyStatusRepo,
) : StudyUsecaseK<Int, String> {
    override fun invoke(arg: String): Flow<Int> = statusRepo.getStatus(arg)
}

/** Corresponds conceptually to `…usecase.ObserveMigrateStatus`. */
class StudyObserveMigrateStatus @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
) : StudySingleUsecaseK<Int> {
    override fun invoke(): Flow<Int> = settingsRepo.observeMigrationDone()
}

/**
 * Corresponds conceptually to `…usecase.SyncAutoRefresh`.
 *
 * Observed responsibility: translate the user's interval setting into scheduled work.
 * Note the `NONE` case CANCELS rather than scheduling a very long interval — see
 * [StudySettingValue.AutoRefreshInterval].
 */
class StudySyncAutoRefresh @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
) : StudySingleUsecase<Long> {
    override suspend fun invoke(): Long {
        if (settingsRepo.getAutoRefresh() == StudySettingValue.OFF) return CANCEL
        val interval = settingsRepo.getAutoRefreshInterval()
        if (interval == StudySettingValue.AutoRefreshInterval.NONE) return CANCEL
        return StudySettingValue.AutoRefreshInterval.toMillis(interval)
    }

    companion object {
        const val CANCEL = -1L
    }
}

/** Corresponds conceptually to `…usecase.GetAutoRefresh`. */
class StudyGetAutoRefresh @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
) : StudySingleUsecase<Int> {
    override suspend fun invoke(): Int = settingsRepo.getAutoRefresh()
}

/** Corresponds conceptually to `…usecase.GetAutoRefreshIntervalType`. */
class StudyGetAutoRefreshIntervalType @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
) : StudySingleUsecase<Int> {
    override suspend fun invoke(): Int = settingsRepo.getAutoRefreshInterval()
}

/** Corresponds conceptually to `…usecase.UpdateAutoRefreshInterval`. */
class StudyUpdateAutoRefreshInterval @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
) : StudyActionUsecase<Int> {
    override suspend fun invoke(arg: Int) {
        settingsRepo.setAutoRefreshInterval(arg)
        settingsRepo.setAutoRefreshNextTime(
            System.currentTimeMillis() + StudySettingValue.AutoRefreshInterval.toMillis(arg),
        )
    }
}

/** Corresponds conceptually to `…usecase.StopAutoRefresh`. */
class StudyStopAutoRefresh @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
) : StudyPureUsecase {
    override suspend fun invoke() {
        settingsRepo.setAutoRefresh(StudySettingValue.OFF)
        settingsRepo.setAutoRefreshNextTime(0L)
    }
}

/** Corresponds conceptually to `…usecase.UpdateRefreshTimeWhenFailed`. */
class StudyUpdateRefreshTimeWhenFailed @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
) : StudyActionUsecase<Int> {
    override suspend fun invoke(arg: Int) {
        val backoff = if (arg == StudyAutoRefresh.From.SYSTEM) SYSTEM_BACKOFF else USER_BACKOFF
        settingsRepo.setAutoRefreshNextTime(System.currentTimeMillis() + backoff)
    }

    private companion object {
        const val SYSTEM_BACKOFF = 30L * 60L * 1000L
        const val USER_BACKOFF = 5L * 60L * 1000L
    }
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.usecase.CheckForecastChange / CheckForecastChangeImpl
 *
 * Observed responsibility: compare the freshly fetched aggregate against the stored one
 * and decide whether the change is worth a notification. The point of the use case is
 * that "changed" is a DOMAIN judgement (did the condition code or the day's max/min
 * actually move?), not a data-layer diff.
 */
interface StudyCheckForecastChange : StudyUsecase<Boolean, Pair<StudyWeather, StudyWeather>>

class StudyCheckForecastChangeImpl @Inject constructor() : StudyCheckForecastChange {
    override suspend fun invoke(arg: Pair<StudyWeather, StudyWeather>): Boolean {
        val (old, new) = arg
        val oldDay = old.dailyObservations.firstOrNull() ?: return false
        val newDay = new.dailyObservations.firstOrNull() ?: return false
        return oldDay.dayCondition.internalCode != newDay.dayCondition.internalCode ||
            oldDay.dayCondition.maxTemp != newDay.dayCondition.maxTemp ||
            oldDay.nightCondition.minTemp != newDay.nightCondition.minTemp
    }
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.usecase.CheckSunriseSunsetTime / …Impl
 *
 * Observed responsibility: decide whether "now" is day or night from the sun times,
 * handling the polar cases where there is neither sunrise nor sunset. The themed splash
 * activity and every icon lookup depend on this answer.
 */
interface StudyCheckSunriseSunsetTime : StudyUsecase<Int, StudyWeather>

class StudyCheckSunriseSunsetTimeImpl @Inject constructor() : StudyCheckSunriseSunsetTime {
    override suspend fun invoke(arg: StudyWeather): Int {
        val time = arg.currentObservation.time
        return when (time.arcticNightType) {
            dev.local.weatherstudy.domain.entity.weather.StudyForecastTime.ARCTIC_POLAR_DAY ->
                dev.local.weatherstudy.domain.entity.weather.StudyForecastTime.DAY
            dev.local.weatherstudy.domain.entity.weather.StudyForecastTime.ARCTIC_POLAR_NIGHT ->
                dev.local.weatherstudy.domain.entity.weather.StudyForecastTime.NIGHT
            else -> {
                val now = time.epochTime.takeIf { it > 0 } ?: System.currentTimeMillis()
                if (now in time.sunRiseTime..time.sunSetTime) {
                    dev.local.weatherstudy.domain.entity.weather.StudyForecastTime.DAY
                } else {
                    dev.local.weatherstudy.domain.entity.weather.StudyForecastTime.NIGHT
                }
            }
        }
    }
}

/** Corresponds conceptually to `…usecase.CheckNetwork`. */
interface StudyCheckNetwork : StudySingleUsecase<Boolean>

/** Corresponds conceptually to `…usecase.CheckDozeMode`. */
interface StudyCheckDozeMode : StudySingleUsecase<Boolean>

/**
 * Corresponds conceptually to `…usecase.MigrateData` / `CheckDataMigration`.
 *
 * Observed responsibility: bring whatever an earlier install left behind up to the
 * current shape, then mark the settings row so the condition chain stops routing here.
 * The original has real work to do — 51 schema versions and a legacy raw-SQLite tier.
 * A fresh install of the reconstruction has nothing to convert, so the step seeds the
 * defaults a migrated install would already hold and records that it ran.
 */
class StudyMigrateData @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
) : StudyPureUsecase {
    override suspend fun invoke() {
        if (settingsRepo.whetherMigrationDone() != MIGRATION_PENDING) return
        settingsRepo.setAutoRefresh(StudySettingValue.ON)
        settingsRepo.setAutoRefreshInterval(StudySettingValue.AutoRefreshInterval.EVERY_3HOUR)
        settingsRepo.setMigrationDone(MIGRATION_DONE)
    }

    private companion object {
        const val MIGRATION_PENDING = 0
        const val MIGRATION_DONE = 1
    }
}
