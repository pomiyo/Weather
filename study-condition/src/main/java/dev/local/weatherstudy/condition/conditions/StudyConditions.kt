package dev.local.weatherstudy.condition.conditions

import dev.local.weatherstudy.condition.IStudyCondition
import dev.local.weatherstudy.condition.StudyConditionUi
import dev.local.weatherstudy.condition.conditions.checker.StudyCheckLocationPermission
import dev.local.weatherstudy.condition.conditions.checker.StudyCheckLocationProvider
import dev.local.weatherstudy.domain.repo.StudySettingsRepo
import dev.local.weatherstudy.domain.usecase.StudyWhetherToConsentLocationAuthority
import dev.local.weatherstudy.domain.usecase.StudyWhetherToConsentUcl
import dev.local.weatherstudy.domain.usecase.StudyWhetherToNoticePermission
import dev.local.weatherstudy.domain.usecase.StudyWhetherToReconsentUcl
import dev.local.weatherstudy.system.service.StudyConnectivityService
import javax.inject.Inject

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the condition classes in
 * com.samsung.android.weather.condition.conditions:
 * `LocationPermissionCondition`, `BackgroundLocationPermissionCondition`,
 * `LocationAuthorityCondition`, `ActivityRecognitionCondition`,
 * `AwayModeFirstAccessCondition`, `AppUpdateCondition`,
 * `BackgroundRestrictCondition`, `DataMigrationCondition`, `IDLECondition`,
 * `CompleteCondition`
 *
 * Observed responsibility: each condition answers one question and names the UI that
 * resolves it. None of them navigates — [dev.local.weatherstudy.condition.StudyConditionManager]
 * returns the blocking condition and the ViewModel decides.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyEulaCondition @Inject constructor(
    private val whetherToConsentUcl: StudyWhetherToConsentUcl,
    private val whetherToReconsentUcl: StudyWhetherToReconsentUcl,
) : IStudyCondition {
    override val id = "eula"
    override val ui = StudyConditionUi.EULA
    override suspend fun isSatisfied(): Boolean =
        !whetherToConsentUcl() && !whetherToReconsentUcl()
}

/** Corresponds conceptually to `…conditions.LocationAuthorityCondition`. */
class StudyLocationAuthorityCondition @Inject constructor(
    private val whetherToConsent: StudyWhetherToConsentLocationAuthority,
) : IStudyCondition {
    override val id = "location_authority"
    override val ui = StudyConditionUi.LOCATION_AUTHORITY
    override suspend fun isSatisfied(): Boolean = !whetherToConsent()
}

/** Corresponds conceptually to `…conditions.PermissionNoticeCondition`. */
class StudyPermissionNoticeCondition @Inject constructor(
    private val whetherToNotice: StudyWhetherToNoticePermission,
) : IStudyCondition {
    override val id = "permission_notice"
    override val ui = StudyConditionUi.PERMISSION_NOTICE
    override suspend fun isSatisfied(): Boolean = !whetherToNotice()
}

/** Corresponds conceptually to `…conditions.LocationPermissionCondition`. */
class StudyLocationPermissionCondition @Inject constructor(
    private val checkLocationPermission: StudyCheckLocationPermission,
) : IStudyCondition {
    override val id = "location_permission"
    override val ui = StudyConditionUi.LOCATION_PERMISSION
    override suspend fun isSatisfied(): Boolean = checkLocationPermission.hasForeground()
}

/**
 * Corresponds conceptually to `…conditions.BackgroundLocationPermissionCondition`.
 *
 * Observed responsibility: separate from the foreground one because it is a separate
 * grant from Android 10 on, and the app degrades rather than blocks without it — away
 * mode and geofencing stop, the rest keeps working.
 */
class StudyBackgroundLocationPermissionCondition @Inject constructor(
    private val checkLocationPermission: StudyCheckLocationPermission,
) : IStudyCondition {
    override val id = "background_location_permission"
    override val ui = StudyConditionUi.BACKGROUND_LOCATION_PERMISSION
    override suspend fun isSatisfied(): Boolean = checkLocationPermission.hasBackground()
}

/** Corresponds conceptually to `…conditions.checker.CheckLocationProvider`'s condition. */
class StudyTurnOnLocationCondition @Inject constructor(
    private val checkLocationProvider: StudyCheckLocationProvider,
) : IStudyCondition {
    override val id = "turn_on_location"
    override val ui = StudyConditionUi.TURN_ON_LOCATION
    override suspend fun isSatisfied(): Boolean = checkLocationProvider.isEnabled()
}

/**
 * Corresponds conceptually to `…conditions.ActivityRecognitionCondition`.
 *
 * Observed responsibility: the "auto refresh on the go" feature needs activity
 * recognition. It is a condition rather than a silent capability check because the
 * original asks for the permission when the feature is turned on.
 */
class StudyActivityRecognitionCondition @Inject constructor() : IStudyCondition {
    override val id = "activity_recognition"
    override val ui = StudyConditionUi.APP_PERMISSION
    override suspend fun isSatisfied(): Boolean = true
}

/**
 * Corresponds conceptually to `…conditions.BackgroundRestrictCondition`.
 *
 * Observed responsibility: if the OS has background-restricted the app, scheduled
 * refresh will never run — so the user is told rather than left with stale data.
 * Note the three-state answer from [StudyConnectivityService.checkBackgroundRestricted].
 */
class StudyBackgroundRestrictCondition @Inject constructor(
    private val connectivityService: StudyConnectivityService,
) : IStudyCondition {
    override val id = "background_restrict"
    override val ui = StudyConditionUi.BACKGROUND_RESTRICT
    override suspend fun isSatisfied(): Boolean =
        connectivityService.checkBackgroundRestricted() !=
            StudyConnectivityService.BACKGROUND_RESTRICTED
}

/**
 * Corresponds conceptually to `…conditions.DataMigrationCondition`.
 *
 * Observed responsibility: first in the chain in the original's full scenario — a
 * migration in progress blocks everything, because reading half-migrated data would be
 * worse than waiting.
 */
class StudyDataMigrationCondition @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
) : IStudyCondition {
    override val id = "data_migration"
    override val ui = StudyConditionUi.DATA_MIGRATION
    override suspend fun isSatisfied(): Boolean = settingsRepo.whetherMigrationDone() != 0
}

/** Corresponds conceptually to `…conditions.AppUpdateCondition`. */
class StudyAppUpdateCondition @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
) : IStudyCondition {
    override val id = "app_update"
    override val ui = StudyConditionUi.APP_UPDATE
    override suspend fun isSatisfied(): Boolean =
        settingsRepo.getAppUpdateStatus() !=
            dev.local.weatherstudy.domain.type.StudyAppUpdateResult.FORCED_UPDATE
}

/** Corresponds conceptually to `…conditions.AwayModeFirstAccessCondition`. */
class StudyAwayModeFirstAccessCondition @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
) : IStudyCondition {
    override val id = "away_mode_first_access"
    override val ui = StudyConditionUi.AWAY_MODE_NOTICE
    override suspend fun isSatisfied(): Boolean = !settingsRepo.isAwayModeFirstAccess()
}

/**
 * Corresponds conceptually to `…conditions.IDLECondition`.
 *
 * Observed responsibility: a condition that is satisfied when there is nothing to do —
 * used to pad a scenario's chain so the manager's loop needs no empty-list special case.
 */
class StudyIdleCondition @Inject constructor() : IStudyCondition {
    override val id = "idle"
    override val ui = StudyConditionUi.NONE
    override suspend fun isSatisfied(): Boolean = true
}

/**
 * Corresponds conceptually to `…conditions.CompleteCondition`.
 *
 * Observed responsibility: the tail of every chain. It is **always satisfied**, which is
 * what lets `ConditionManager` express "nothing is blocking" as an ordinary chain result
 * instead of a sentinel.
 */
class StudyCompleteCondition @Inject constructor() : IStudyCondition {
    override val id = "complete"
    override val ui = StudyConditionUi.NONE
    override suspend fun isSatisfied(): Boolean = true
}

/**
 * Corresponds conceptually to `…conditions.GetCurrentLocationCondition`.
 *
 * Observed responsibility: with permissions granted but no device-location entry saved,
 * the chain routes to `GetCurrentFragment`. This is the condition that makes first run
 * land on "add your location" rather than an empty detail screen.
 */
class StudyGetCurrentLocationCondition @Inject constructor(
    private val settingsRepo: StudySettingsRepo,
) : IStudyCondition {
    override val id = "get_current_location"
    override val ui = StudyConditionUi.GET_CURRENT_LOCATION
    override suspend fun isSatisfied(): Boolean = settingsRepo.getFavoriteLocation().isNotEmpty()
}
