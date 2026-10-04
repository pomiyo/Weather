package dev.local.weatherstudy.data.repo

import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentValues
import android.database.Cursor
import dev.local.weatherstudy.domain.policy.StudyForecastProviderPolicy
import dev.local.weatherstudy.domain.policy.StudyUserPolicyConstant
import dev.local.weatherstudy.domain.policy.StudyWeatherPolicyManager
import dev.local.weatherstudy.domain.repo.StudyUserPolicyConsentRepo
import dev.local.weatherstudy.domain.repo.StudyWeatherProviderRepo
import dev.local.weatherstudy.persistence.dao.StudyCursorDao
import dev.local.weatherstudy.persistence.database.dao.StudySettingsRoomDao
import dev.local.weatherstudy.persistence.database.models.StudySettingEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Educational reconstruction — the implementations that complete the DI graph.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.data.cp's provider repository,
 * com.samsung.android.weather.domain.repo.UserPolicyConsentRepo, and the
 * `WeatherPolicyManager` implementation in `…domain.policy.impl`.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Singleton
class StudyWeatherProviderRepoImpl @Inject constructor(
    private val cursorDao: StudyCursorDao,
) : StudyWeatherProviderRepo {

    override fun getAll(): Cursor = cursorDao.getAll()
    override fun getByKey(location: String): Cursor = cursorDao.getByKey(location)
    override fun getHourly(): Cursor = cursorDao.getHourly()
    override fun getHourly(location: String): Cursor = cursorDao.getHourly(location)
    override fun getDaily(): Cursor = cursorDao.getDaily()
    override fun getDaily(location: String): Cursor = cursorDao.getDaily(location)
    override fun getIndex(): Cursor = cursorDao.getIndex()
    override fun getIndex(location: String): Cursor = cursorDao.getIndex(location)
    override fun getSettings(): Cursor = cursorDao.getSettings()

    /** writes through the provider are not reconstructed; reads are the educational part */
    override fun insert(table: String, values: ContentValues): Long = -1L
    override fun update(
        table: String,
        values: ContentValues,
        selection: String?,
        selectionArgs: Array<String>?,
    ) = Unit
    override fun delete(table: String, selection: String?, selectionArgs: Array<String>?) = Unit
    override fun applyBatch(
        provider: String,
        operations: ArrayList<ContentProviderOperation>,
    ): Array<ContentProviderResult> = emptyArray()
}

/**
 * Corresponds conceptually to `…domain.repo.UserPolicyConsentRepo`'s implementation.
 *
 * Observed responsibility: the four consents, with the UCL version pair that drives
 * re-consent. Persisted, as in the original, to the single settings row
 * (`COL_SETTING_SHOW_USE_LOCATION_POPUP` holds the agreement after a column reuse, with
 * `COL_SETTING_PP_VERSION` / `COL_SETTING_PP_GRANT_VERSION` beside it) so a consent given
 * once survives a process restart. The flows mirror the row for observers.
 */
@Singleton
class StudyUserPolicyConsentRepoImpl @Inject constructor(
    private val settingsDao: StudySettingsRoomDao,
) : StudyUserPolicyConsentRepo {

    private val uclAgreement = MutableStateFlow(StudyUserPolicyConstant.CONSENT_NONE)
    private val uclGrantVersion = MutableStateFlow(0)
    private val locationAuthority = MutableStateFlow(StudyUserPolicyConstant.CONSENT_NONE)
    private val permissionNotice = MutableStateFlow(StudyUserPolicyConstant.CONSENT_NONE)
    private val networkCharges = MutableStateFlow(StudyUserPolicyConstant.CONSENT_NONE)
    private var consentType = StudyUserPolicyConstant.CONSENT_TYPE_GLOBAL

    private suspend fun row(): StudySettingEntity =
        (settingsDao.getRow() ?: StudySettingEntity(id = StudySettingsRoomDao.SINGLE_ROW_ID)).also { row ->
            uclAgreement.value = row.privacyPolicyAgreement
            uclGrantVersion.value = row.ppGrantVersion ?: 0
            locationAuthority.value = row.consentToUseWlan
            permissionNotice.value = row.consentToPermissionNotice
            networkCharges.value = row.consentToNetworkCharges
        }

    private suspend fun update(change: (StudySettingEntity) -> StudySettingEntity) {
        settingsDao.upsert(change(row()))
        row()
    }

    override suspend fun getUclAgreement(): Int = row().privacyPolicyAgreement

    override suspend fun setUclAgreement(agreement: Int, version: Int) =
        update { it.copy(privacyPolicyAgreement = agreement, ppVersion = PUBLISHED_UCL_VERSION, ppGrantVersion = version) }

    override fun observeUclAgreement(): Flow<Int> = uclAgreement.asStateFlow()

    override suspend fun getUclVersion(): Int = PUBLISHED_UCL_VERSION

    override suspend fun getUclGrantVersion(): Int = row().ppGrantVersion ?: 0

    override suspend fun setUclGrantVersion(version: Int) = update { it.copy(ppGrantVersion = version) }

    override fun observeUclGrantVersion(): Flow<Int> = uclGrantVersion.asStateFlow()

    override fun getUclConsentType(): Int = consentType

    override fun setUclConsentType(type: Int) { consentType = type }

    override suspend fun getLocationAuthority(): Int = row().consentToUseWlan

    override suspend fun setLocationAuthority(value: Int) = update { it.copy(consentToUseWlan = value) }

    override fun observeLocationAuthority(): Flow<Int> = locationAuthority.asStateFlow()

    override suspend fun getPermissionNotice(): Int = row().consentToPermissionNotice

    override suspend fun setPermissionNotice(value: Int) = update { it.copy(consentToPermissionNotice = value) }

    override fun observePermissionNotice(): Flow<Int> = permissionNotice.asStateFlow()

    override suspend fun getNetworkCharges(): Int = row().consentToNetworkCharges

    override suspend fun setNetworkCharges(value: Int) = update { it.copy(consentToNetworkCharges = value) }

    override fun observeNetworkCharges(): Flow<Int> = networkCharges.asStateFlow()

    private companion object {
        const val PUBLISHED_UCL_VERSION = 1
    }
}

/**
 * Corresponds conceptually to the `WeatherPolicyManager` implementation.
 *
 * Observed responsibility: the original's manager IS a `WeatherPolicy` (the interface
 * extends it and adds nothing), so this answers the 70 capability questions directly.
 * It derives from a per-provider policy, which is the structure
 * [StudyForecastProviderPolicy] exists for.
 *
 * The reconstruction's answers enable the cards it can actually render and disable the
 * ones whose dependencies are stubbed — which is exactly how the policy layer is meant
 * to be used.
 */
@Singleton
class StudyWeatherPolicyManagerImpl @Inject constructor() :
    StudyForecastProviderPolicy(), StudyWeatherPolicyManager {

    override val providerId: String =
        dev.local.weatherstudy.domain.entity.forecast.StudyForecastProvider.PROVIDER_A

    // the 13 genuinely provider-dependent answers
    override fun supportAlert(): Boolean = true
    override fun supportAwayMode(): Boolean = false
    override fun supportFixedRefreshInterval(): Boolean = false
    override fun supportLabel(): Boolean = true
    override fun supportLifeStyle(): Boolean = false
    override fun supportNarrative(): Boolean = true
    override fun supportPrecipitationGraph(): Boolean = true
    override fun supportReportIncorrectInfo(): Boolean = false
    override fun supportRepresentLocation(): Boolean = true
    override fun supportTodayStories(): Boolean = false
    override fun supportVideo(): Boolean = false

    /** stubbed in the reconstruction: needs the Google Maps SDK and a key */
    override fun supportRadar(): Boolean = false

    /** stubbed in the reconstruction: needs the SmartThings platform app */
    override fun supportSmartThings(): Boolean = false

    // the cards the reconstruction can render
    override fun supportInsightCardV1(): Boolean = true
    override fun supportMoonCycle(): Boolean = true
    override fun supportSunCycle(): Boolean = true
}
