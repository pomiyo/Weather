package dev.local.weatherstudy.app.main

import dev.local.weatherstudy.condition.StudyConditionUi

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the 17 classes in
 * com.sec.android.daemonapp.app.main.state:
 * `MainState`, `MainIntent`, `MainSideEffect`, `MainStateProvider`, `MainDestination`,
 * `MainActionDispatcher`, `CpState`, `PpState`, `BnRState`, `DataMigrationState`,
 * `AppUpdateState`, `ShowCpChangeState`, `RefreshReason`
 *
 * Observed responsibility: `MainState` is a fold of five sub-states — content provider,
 * privacy policy, backup/restore, data migration and app update. `MainStateProvider`
 * combines them, and the result is a [StudyMainDestination]. That is the whole of the
 * first-run flow, expressed as data.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
data class StudyMainState(
    val isResolving: Boolean = true,
    val destination: StudyMainDestination = StudyMainDestination.None,
    val cpState: StudyCpState = StudyCpState.Unknown,
    val ppState: StudyPpState = StudyPpState.Unknown,
    val bnrState: StudyBnRState = StudyBnRState.Idle,
    val migrationState: StudyDataMigrationState = StudyDataMigrationState.Unknown,
    val appUpdateState: StudyAppUpdateState = StudyAppUpdateState.None,
)

/**
 * Corresponds conceptually to `…main.state.MainDestination`.
 *
 * Built from the condition chain's answer — see [dev.local.weatherstudy.condition.StudyConditionUi].
 */
sealed interface StudyMainDestination {
    data object None : StudyMainDestination
    data object Eula : StudyMainDestination
    data object PermissionNotice : StudyMainDestination
    data object LocationPermission : StudyMainDestination
    data object AppPermission : StudyMainDestination
    data object GetCurrentLocation : StudyMainDestination
    data object DataMigration : StudyMainDestination
    data object AppUpdate : StudyMainDestination
    data class Detail(val locationKey: String) : StudyMainDestination
    data object Locations : StudyMainDestination

    companion object {
        /** the mapping the original performs in `MainStateProvider` */
        fun fromConditionUi(ui: StudyConditionUi, locationKey: String = ""): StudyMainDestination =
            when (ui) {
                StudyConditionUi.EULA -> Eula
                StudyConditionUi.PERMISSION_NOTICE -> PermissionNotice
                StudyConditionUi.LOCATION_PERMISSION,
                StudyConditionUi.BACKGROUND_LOCATION_PERMISSION,
                StudyConditionUi.TURN_ON_LOCATION,
                StudyConditionUi.LOCATION_AUTHORITY,
                -> LocationPermission
                StudyConditionUi.APP_PERMISSION -> AppPermission
                StudyConditionUi.GET_CURRENT_LOCATION -> GetCurrentLocation
                StudyConditionUi.DATA_MIGRATION -> DataMigration
                StudyConditionUi.APP_UPDATE -> AppUpdate
                StudyConditionUi.BACKGROUND_RESTRICT,
                StudyConditionUi.AWAY_MODE_NOTICE,
                StudyConditionUi.NONE,
                -> Detail(locationKey)
            }
    }
}

/** `…main.state.CpState` — which weather provider is in force, and whether it changed. */
sealed interface StudyCpState {
    data object Unknown : StudyCpState
    data class Settled(val providerId: String) : StudyCpState
    data class Changing(val from: String, val to: String) : StudyCpState
}

/** `…main.state.PpState` — privacy-policy consent. */
sealed interface StudyPpState {
    data object Unknown : StudyPpState
    data object NeedConsent : StudyPpState
    data object NeedReconsent : StudyPpState
    data object Agreed : StudyPpState
}

/** `…main.state.BnRState` — backup/restore in progress. */
sealed interface StudyBnRState {
    data object Idle : StudyBnRState
    data object Restoring : StudyBnRState
    data object Done : StudyBnRState
}

/** `…main.state.DataMigrationState`. */
sealed interface StudyDataMigrationState {
    data object Unknown : StudyDataMigrationState
    data object Running : StudyDataMigrationState
    data object Done : StudyDataMigrationState
}

/** `…main.state.AppUpdateState`. */
sealed interface StudyAppUpdateState {
    data object None : StudyAppUpdateState
    data object Available : StudyAppUpdateState
    data object Forced : StudyAppUpdateState
}

/** `…main.state.MainIntent` — the actions the routing fragment can take. */
sealed interface StudyMainAction {
    data object Resolve : StudyMainAction
    data class ConsentGranted(val granted: Boolean) : StudyMainAction
    data class PermissionResult(val granted: Boolean) : StudyMainAction
}

/** `…main.state.MainSideEffect`. */
sealed interface StudyMainSideEffect {
    data class Navigate(val destination: StudyMainDestination) : StudyMainSideEffect
    data object RequestLocationPermission : StudyMainSideEffect
    data object Finish : StudyMainSideEffect
}

/** `…main.state.RefreshReason`. */
enum class StudyRefreshReason { LAUNCH, RESUME, PULL, WIDGET, SYSTEM }
