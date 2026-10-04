package dev.local.weatherstudy.condition

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.condition.ConditionManager
 * com.samsung.android.weather.condition.ICondition / Condition
 * com.samsung.android.weather.condition.IConditionFactory / ConditionFactory
 * com.samsung.android.weather.condition.IScenario / Scenario
 * com.samsung.android.weather.condition.ConditionUi
 * com.samsung.android.weather.condition.PermissionCallback / PermissionResultCallback
 *
 * ### What this module answers
 *
 * `reports/FINAL_REPORT.md` §24 and `HANDOFF.md` §6 both list an open question: *the
 * exact order of initialisation inside `App`, and whether EULA/permission gating runs
 * before or after the NavHost is created.* The `condition` package is the answer, and
 * reconstructing it resolves that UNKNOWN.
 *
 * It is a **precondition chain, parameterised by scenario**. Nothing in the app decides
 * for itself whether it may run; it declares a scenario and asks:
 *
 * ```
 * MainFragment / DetailFragment / a widget / a refresh
 *        ↓  declares a Scenario
 * ConditionManager.check(scenario)
 *        ↓  ConditionFactory builds the ordered chain for that scenario
 * [ DataMigrationCondition, LocationAuthorityCondition, LocationPermissionCondition,
 *   BackgroundLocationPermissionCondition, ActivityRecognitionCondition,
 *   BackgroundRestrictCondition, AppUpdateCondition, AwayModeFirstAccessCondition,
 *   IDLECondition, … , CompleteCondition ]
 *        ↓  first condition that is not satisfied wins
 * StudyConditionResult.Blocked(condition)  →  the UI shows that condition's screen
 * StudyConditionResult.Complete            →  proceed
 * ```
 *
 * So the gating runs **as the start destination's first action**, not before the
 * NavHost: `MainFragment` is the nav graph's start destination, has no UI of its own,
 * and its only job is to run the chain and navigate to whatever it returns — the EULA,
 * the permission notice, the "add current location" screen, or the detail screen.
 * `CompleteCondition` at the tail is what makes "nothing is blocking" an ordinary chain
 * result rather than a special case.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Singleton
class StudyConditionManager @Inject constructor(
    private val conditionFactory: IStudyConditionFactory,
) {
    /**
     * Runs the chain for [scenario] and returns the first unsatisfied condition.
     *
     * Reconstruction of the original's ordered-chain evaluation: conditions are checked
     * in order and the first failure short-circuits, so a user without consent is never
     * asked for a location permission.
     */
    suspend fun check(scenario: StudyScenario): StudyConditionResult {
        for (condition in conditionFactory.create(scenario)) {
            if (!condition.isSatisfied()) {
                return StudyConditionResult.Blocked(condition)
            }
        }
        return StudyConditionResult.Complete
    }
}

/**
 * Corresponds conceptually to `…condition.ICondition` / `Condition`.
 *
 * Observed responsibility: a condition knows three things — whether it is satisfied,
 * what UI resolves it, and whether it can be skipped. The [ui] property is why the
 * chain can hand the ViewModel a navigation target without a `when` over condition types.
 */
interface IStudyCondition {
    val id: String
    val ui: StudyConditionUi
    suspend fun isSatisfied(): Boolean
}

/**
 * Corresponds conceptually to `…condition.ConditionUi`.
 *
 * Observed responsibility: names the surface that resolves a condition. The original's
 * `MainDestination` is built from this, which is how one chain drives the whole
 * first-run flow.
 */
enum class StudyConditionUi {
    /** nothing to show */
    NONE,

    /** the EULA / consent screen — `EulaFragment` */
    EULA,

    /** the permission-notice screen — `PermissionNoticeActivity` */
    PERMISSION_NOTICE,

    /** the runtime-permission request */
    LOCATION_PERMISSION,

    /** the background-location upgrade request */
    BACKGROUND_LOCATION_PERMISSION,

    /** the location-authority consent — a separate regional consent */
    LOCATION_AUTHORITY,

    /** the "turn on location services" system settings prompt */
    TURN_ON_LOCATION,

    /** the app-permission list in settings */
    APP_PERMISSION,

    /** a forced-update dialog */
    APP_UPDATE,

    /** the battery/background-restriction settings prompt */
    BACKGROUND_RESTRICT,

    /** the data-migration progress screen */
    DATA_MIGRATION,

    /** the "add current location" screen — `GetCurrentFragment` */
    GET_CURRENT_LOCATION,

    /** the away-mode first-access notice */
    AWAY_MODE_NOTICE,
}

/** The chain's answer. */
sealed interface StudyConditionResult {
    /** every condition passed */
    data object Complete : StudyConditionResult

    /** [condition] is unsatisfied; its [IStudyCondition.ui] is where to go */
    data class Blocked(val condition: IStudyCondition) : StudyConditionResult
}

/**
 * Corresponds conceptually to `…condition.IScenario` / `Scenario` and its five nested
 * variants, each of which the APK shows as an assisted-injected factory
 * (`Scenario_CurrentLocation_Factory_Impl`, `Scenario_DeepLink_Factory_Impl`, …).
 *
 * Observed responsibility: **which conditions apply**. The chains genuinely differ:
 * a deep link into the detail screen must not demand a location permission, and a
 * background refresh must not try to show a dialog at all.
 */
sealed interface StudyScenario {
    /** entering the app normally — the fullest chain */
    data object DetailProcess : StudyScenario

    /** adding the device location — needs the location permissions */
    data object CurrentLocation : StudyScenario

    /** arriving from a widget tap or an external intent — minimal chain, no dialogs */
    data class DeepLink(val locationKey: String) : StudyScenario

    /** a refresh — consent and network only; never blocks on UI */
    data object Refresh : StudyScenario

    /** the representative-location fallback, before any city is saved */
    data object RepresentLocation : StudyScenario
}

/**
 * Corresponds conceptually to `…condition.IConditionFactory` / `ConditionFactory`.
 *
 * Observed responsibility: builds the ordered chain per scenario. This is the single
 * place the startup order is written down.
 */
interface IStudyConditionFactory {
    fun create(scenario: StudyScenario): List<IStudyCondition>
}

/**
 * Corresponds conceptually to `…condition.PermissionCallback` /
 * `PermissionCallbackImpl` / `PermissionResultCallback`.
 *
 * Observed responsibility: the chain runs in a ViewModel but a runtime permission can
 * only be requested from a Fragment or Activity, so the result comes back through this.
 * It is why the condition classes can be UI-free.
 */
interface StudyPermissionCallback {
    fun requestPermissions(permissions: List<String>, callback: StudyPermissionResultCallback)
}

/** Corresponds conceptually to `…condition.PermissionResultCallback`. */
fun interface StudyPermissionResultCallback {
    fun onResult(granted: Map<String, Boolean>)
}
