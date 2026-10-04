package dev.local.weatherstudy.app.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.local.weatherstudy.condition.StudyConditionManager
import dev.local.weatherstudy.condition.StudyConditionResult
import dev.local.weatherstudy.condition.StudyConditionUi
import dev.local.weatherstudy.condition.StudyScenario
import dev.local.weatherstudy.domain.repo.StudySettingsRepo
import dev.local.weatherstudy.domain.type.StudyAutoRefresh
import dev.local.weatherstudy.domain.usecase.StudyMigrateData
import dev.local.weatherstudy.sync.usecase.StudyStartBackgroundRefresh
import dev.local.weatherstudy.logger.analytics.tracking.StudyMainTracking
import javax.inject.Inject
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.sec.android.daemonapp.app.main.MainViewModel
 *
 * ### What the start destination actually does
 *
 * This resolves the open question in `reports/FINAL_REPORT.md` §24: the EULA and
 * permission gating runs **as the nav graph's start destination**, not before the
 * NavHost. `MainFragment` has no UI; it runs the condition chain for
 * `Scenario.DetailProcess` and navigates to whatever blocks — or to the detail screen if
 * nothing does.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@HiltViewModel
class StudyMainViewModel @Inject constructor(
    private val conditionManager: StudyConditionManager,
    private val settingsRepo: StudySettingsRepo,
    private val mainTracking: StudyMainTracking,
    private val migrateData: StudyMigrateData,
    private val startBackgroundRefresh: StudyStartBackgroundRefresh,
) : ViewModel(), ContainerHost<StudyMainState, StudyMainSideEffect> {

    override val container: Container<StudyMainState, StudyMainSideEffect> =
        container(StudyMainState()) { resolve() }

    fun dispatch(action: StudyMainAction) = intent {
        when (action) {
            StudyMainAction.Resolve -> resolve()
            is StudyMainAction.ConsentGranted -> if (action.granted) resolve()
            is StudyMainAction.PermissionResult -> if (action.granted) resolve()
        }
    }

    /** run the chain, then navigate to its answer */
    private fun resolve() = intent {
        mainTracking.onLaunch(StudyAutoRefresh.From.SYSTEM)
        reduce { state.copy(isResolving = true) }

        var result = conditionManager.check(StudyScenario.DetailProcess)

        // data migration is the one blocking condition with no screen: it is work, not a
        // question. Do it, then ask the chain again.
        if (result.blockedBy() == StudyConditionUi.DATA_MIGRATION) {
            reduce { state.copy(migrationState = StudyDataMigrationState.Running) }
            migrateData()
            reduce { state.copy(migrationState = StudyDataMigrationState.Done) }
            result = conditionManager.check(StudyScenario.DetailProcess)
        }

        val favourite = runCatching { settingsRepo.getFavoriteLocation() }.getOrDefault("")
        val blockedBy = result.blockedBy()
        val destination = when {
            blockedBy == null ->
                StudyMainDestination.fromConditionUi(StudyConditionUi.NONE, favourite)
            // the location conditions exist to obtain a FIRST location. Someone who has
            // already saved a city declined them on purpose and is not asked on every launch.
            blockedBy in LOCATION_CONDITIONS && favourite.isNotEmpty() ->
                StudyMainDestination.Detail(favourite)
            else -> StudyMainDestination.fromConditionUi(blockedBy, favourite)
        }

        if (destination is StudyMainDestination.Detail) {
            // the periodic refresh is (re)armed on every launch that reaches the forecast
            runCatching { startBackgroundRefresh(StudyAutoRefresh.From.SYSTEM) }
        }

        reduce { state.copy(isResolving = false, destination = destination) }
        postSideEffect(StudyMainSideEffect.Navigate(destination))
    }

    private fun StudyConditionResult.blockedBy(): StudyConditionUi? =
        (this as? StudyConditionResult.Blocked)?.condition?.ui

    private companion object {
        val LOCATION_CONDITIONS = setOf(
            StudyConditionUi.LOCATION_PERMISSION,
            StudyConditionUi.BACKGROUND_LOCATION_PERMISSION,
            StudyConditionUi.TURN_ON_LOCATION,
            StudyConditionUi.LOCATION_AUTHORITY,
            StudyConditionUi.GET_CURRENT_LOCATION,
        )
    }
}
