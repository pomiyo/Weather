package dev.local.weatherstudy.condition.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.local.weatherstudy.condition.IStudyCondition
import dev.local.weatherstudy.condition.IStudyConditionFactory
import dev.local.weatherstudy.condition.StudyScenario
import dev.local.weatherstudy.condition.conditions.StudyAppUpdateCondition
import dev.local.weatherstudy.condition.conditions.StudyAwayModeFirstAccessCondition
import dev.local.weatherstudy.condition.conditions.StudyBackgroundLocationPermissionCondition
import dev.local.weatherstudy.condition.conditions.StudyBackgroundRestrictCondition
import dev.local.weatherstudy.condition.conditions.StudyCompleteCondition
import dev.local.weatherstudy.condition.conditions.StudyDataMigrationCondition
import dev.local.weatherstudy.condition.conditions.StudyEulaCondition
import dev.local.weatherstudy.condition.conditions.StudyGetCurrentLocationCondition
import dev.local.weatherstudy.condition.conditions.StudyLocationAuthorityCondition
import dev.local.weatherstudy.condition.conditions.StudyLocationPermissionCondition
import dev.local.weatherstudy.condition.conditions.StudyPermissionNoticeCondition
import dev.local.weatherstudy.condition.conditions.StudyTurnOnLocationCondition
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.condition.di.ScenarioModule
 * com.samsung.android.weather.condition.ConditionFactory
 *
 * ### This file is the app's startup order, written down
 *
 * Each scenario's list below is the chain in evaluation order. The differences between
 * them are the whole reason the mechanism exists:
 *
 * - **`DetailProcess`** — the full chain. Migration first (half-migrated data is worse
 *   than waiting), then consent, then permissions, then "do you have a location yet".
 * - **`CurrentLocation`** — consent and permissions, plus the location-services check,
 *   because this scenario is specifically about getting a fix.
 * - **`DeepLink`** — consent only. A widget tap must not pop a permission dialog; it
 *   has a location key already and just needs to be allowed to show data.
 * - **`Refresh`** — consent and background-restriction. No UI conditions at all,
 *   because a background refresh has no surface to show one on.
 * - **`RepresentLocation`** — consent only, since the representative location needs no
 *   permission by design; it is the pre-permission fallback.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Singleton
class StudyConditionFactory @Inject constructor(
    private val dataMigration: StudyDataMigrationCondition,
    private val eula: StudyEulaCondition,
    private val locationAuthority: StudyLocationAuthorityCondition,
    private val permissionNotice: StudyPermissionNoticeCondition,
    private val locationPermission: StudyLocationPermissionCondition,
    private val backgroundLocationPermission: StudyBackgroundLocationPermissionCondition,
    private val turnOnLocation: StudyTurnOnLocationCondition,
    private val backgroundRestrict: StudyBackgroundRestrictCondition,
    private val appUpdate: StudyAppUpdateCondition,
    private val awayModeFirstAccess: StudyAwayModeFirstAccessCondition,
    private val getCurrentLocation: StudyGetCurrentLocationCondition,
    private val complete: StudyCompleteCondition,
) : IStudyConditionFactory {

    override fun create(scenario: StudyScenario): List<IStudyCondition> = when (scenario) {
        StudyScenario.DetailProcess -> listOf(
            dataMigration,
            appUpdate,
            eula,
            locationAuthority,
            permissionNotice,
            locationPermission,
            getCurrentLocation,
            complete,
        )

        StudyScenario.CurrentLocation -> listOf(
            eula,
            locationAuthority,
            permissionNotice,
            locationPermission,
            turnOnLocation,
            backgroundLocationPermission,
            complete,
        )

        // a widget tap has a key already: consent only, and never a dialog
        is StudyScenario.DeepLink -> listOf(eula, complete)

        // no surface to show UI on
        StudyScenario.Refresh -> listOf(eula, backgroundRestrict, complete)

        // the pre-permission fallback
        StudyScenario.RepresentLocation -> listOf(eula, complete)
    }
}

/** Binds the factory. */
@Module
@InstallIn(SingletonComponent::class)
interface StudyScenarioModule {

    @Binds
    @Singleton
    fun bindConditionFactory(impl: StudyConditionFactory): IStudyConditionFactory
}
