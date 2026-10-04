package dev.local.weatherstudy.domain.source.policy

import kotlinx.coroutines.flow.Flow

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.source.policy.UserPolicyConsentDataSource
 *
 * Observed responsibilities:
 * - the four consents the app gates itself on: the UCL (user consent / EULA),
 *   the location authority, the permission notice, and network-charges acceptance
 * - each has a get, a set and an observe — the observe half is what the condition
 *   chain (`StudyConditionManager`) watches so a consent granted in Settings
 *   immediately unblocks a pending scenario
 * - the UCL carries a VERSION: `getUclVersion` vs `getUclGrantVersion`. When the
 *   published version moves past the granted one, `WhetherToReconsentUcl` fires.
 *   That re-consent mechanism is the reason this is not a single boolean.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyUserPolicyConsentDataSource {
    suspend fun getUclAgreement(): Int
    suspend fun setUclAgreement(agreement: Int, version: Int)
    fun observeUclAgreement(): Flow<Int>

    suspend fun getUclVersion(): Int
    suspend fun getUclGrantVersion(): Int
    suspend fun setUclGrantVersion(version: Int)
    fun observeUclGrantVersion(): Flow<Int>

    fun getUclConsentType(): Int
    fun setUclConsentType(type: Int)

    suspend fun getLocationAuthority(): Int
    suspend fun setLocationAuthority(value: Int)
    fun observeLocationAuthority(): Flow<Int>

    suspend fun getPermissionNotice(): Int
    suspend fun setPermissionNotice(value: Int)
    fun observePermissionNotice(): Flow<Int>

    suspend fun getNetworkCharges(): Int
    suspend fun setNetworkCharges(value: Int)
    fun observeNetworkCharges(): Flow<Int>
}
