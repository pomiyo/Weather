package dev.local.weatherstudy.domain.usecase

import dev.local.weatherstudy.domain.policy.StudyUserPolicyConstant
import dev.local.weatherstudy.domain.policy.StudyWeatherPolicyManager
import dev.local.weatherstudy.domain.repo.StudyUserPolicyConsentRepo
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the consent use cases in
 * com.samsung.android.weather.domain.usecase:
 * WhetherToConsentUcl, WhetherToReconsentUcl, AgreeToUcl, DisagreeToUcl,
 * WhetherToConsentLocationAuthority, AgreeToLocationAuthority, DisagreeToLocationAuthority,
 * WhetherToNoticePermission, NoticePermissionCompleted, WhetherToShowNetworkCharges,
 * WhetherToAllowPersonalDataAccess, GetPersonalDataAccessStatus, CheckUserPolicyStatus,
 * CheckUclVersion
 *
 * Observed responsibility — these are what gate the whole app, and the pair
 * `WhetherToConsentUcl` / `WhetherToReconsentUcl` is the structural point:
 *
 * - **consent** — has the user ever agreed?
 * - **re-consent** — did they agree to an OLDER version than the one now published?
 *
 * Both must be false before data is shown, which is why [StudyGetWeather] injects both.
 * Every one of these is also policy-gated: a region that does not require a separate
 * location-authority consent answers `false` without reading storage.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyWhetherToConsentUcl @Inject constructor(
    private val consentRepo: StudyUserPolicyConsentRepo,
) : StudySingleUsecase<Boolean> {
    override suspend fun invoke(): Boolean =
        consentRepo.getUclAgreement() != StudyUserPolicyConstant.CONSENT_AGREED
}

/** Corresponds conceptually to `…usecase.WhetherToReconsentUcl`. */
class StudyWhetherToReconsentUcl @Inject constructor(
    private val consentRepo: StudyUserPolicyConsentRepo,
) : StudySingleUsecase<Boolean> {
    override suspend fun invoke(): Boolean {
        if (consentRepo.getUclAgreement() != StudyUserPolicyConstant.CONSENT_AGREED) return false
        return consentRepo.getUclGrantVersion() < consentRepo.getUclVersion()
    }
}

/** Corresponds conceptually to `…usecase.CheckUclVersion`. */
class StudyCheckUclVersion @Inject constructor(
    private val consentRepo: StudyUserPolicyConsentRepo,
) : StudySingleUsecase<Int> {
    override suspend fun invoke(): Int = consentRepo.getUclVersion()
}

/** Corresponds conceptually to `…usecase.AgreeToUcl`. */
class StudyAgreeToUcl @Inject constructor(
    private val consentRepo: StudyUserPolicyConsentRepo,
) : StudyPureUsecase {
    override suspend fun invoke() {
        val version = consentRepo.getUclVersion()
        consentRepo.setUclAgreement(StudyUserPolicyConstant.CONSENT_AGREED, version)
        consentRepo.setUclGrantVersion(version)
    }
}

/** Corresponds conceptually to `…usecase.DisagreeToUcl`. */
class StudyDisagreeToUcl @Inject constructor(
    private val consentRepo: StudyUserPolicyConsentRepo,
) : StudyPureUsecase {
    override suspend fun invoke() =
        consentRepo.setUclAgreement(StudyUserPolicyConstant.CONSENT_DISAGREED, 0)
}

/** Corresponds conceptually to `…usecase.WhetherToConsentLocationAuthority`. */
class StudyWhetherToConsentLocationAuthority @Inject constructor(
    private val consentRepo: StudyUserPolicyConsentRepo,
    private val policyManager: StudyWeatherPolicyManager,
) : StudySingleUsecase<Boolean> {
    override suspend fun invoke(): Boolean =
        policyManager.supportLocationAuthority() &&
            consentRepo.getLocationAuthority() != StudyUserPolicyConstant.CONSENT_AGREED
}

/** Corresponds conceptually to `…usecase.AgreeToLocationAuthority`. */
class StudyAgreeToLocationAuthority @Inject constructor(
    private val consentRepo: StudyUserPolicyConsentRepo,
) : StudyPureUsecase {
    override suspend fun invoke() =
        consentRepo.setLocationAuthority(StudyUserPolicyConstant.CONSENT_AGREED)
}

/** Corresponds conceptually to `…usecase.DisagreeToLocationAuthority`. */
class StudyDisagreeToLocationAuthority @Inject constructor(
    private val consentRepo: StudyUserPolicyConsentRepo,
) : StudyPureUsecase {
    override suspend fun invoke() =
        consentRepo.setLocationAuthority(StudyUserPolicyConstant.CONSENT_DISAGREED)
}

/** Corresponds conceptually to `…usecase.WhetherToNoticePermission`. */
class StudyWhetherToNoticePermission @Inject constructor(
    private val consentRepo: StudyUserPolicyConsentRepo,
    private val policyManager: StudyWeatherPolicyManager,
) : StudySingleUsecase<Boolean> {
    override suspend fun invoke(): Boolean =
        policyManager.supportPermissionNotice() &&
            consentRepo.getPermissionNotice() != StudyUserPolicyConstant.CONSENT_AGREED
}

/** Corresponds conceptually to `…usecase.NoticePermissionCompleted`. */
class StudyNoticePermissionCompleted @Inject constructor(
    private val consentRepo: StudyUserPolicyConsentRepo,
) : StudyPureUsecase {
    override suspend fun invoke() =
        consentRepo.setPermissionNotice(StudyUserPolicyConstant.CONSENT_AGREED)
}

/** Corresponds conceptually to `…usecase.WhetherToShowNetworkCharges`. */
class StudyWhetherToShowNetworkCharges @Inject constructor(
    private val consentRepo: StudyUserPolicyConsentRepo,
    private val policyManager: StudyWeatherPolicyManager,
) : StudySingleUsecase<Boolean> {
    override suspend fun invoke(): Boolean =
        policyManager.supportNetworkCharges() &&
            consentRepo.getNetworkCharges() != StudyUserPolicyConstant.CONSENT_AGREED
}

/**
 * Corresponds conceptually to `…usecase.CheckUserPolicyStatus`.
 *
 * Observed responsibility: the composite the condition chain asks once, instead of
 * four separate questions. Reconstructed with the same four inputs.
 */
class StudyCheckUserPolicyStatus @Inject constructor(
    private val whetherToConsentUcl: StudyWhetherToConsentUcl,
    private val whetherToReconsentUcl: StudyWhetherToReconsentUcl,
    private val whetherToConsentLocationAuthority: StudyWhetherToConsentLocationAuthority,
    private val whetherToNoticePermission: StudyWhetherToNoticePermission,
) : StudySingleUsecase<StudyUserPolicyStatus> {

    override suspend fun invoke() = StudyUserPolicyStatus(
        needConsent = whetherToConsentUcl(),
        needReconsent = whetherToReconsentUcl(),
        needLocationAuthority = whetherToConsentLocationAuthority(),
        needPermissionNotice = whetherToNoticePermission(),
    )
}

/** The composite answer [StudyCheckUserPolicyStatus] returns. */
data class StudyUserPolicyStatus(
    val needConsent: Boolean,
    val needReconsent: Boolean,
    val needLocationAuthority: Boolean,
    val needPermissionNotice: Boolean,
) {
    val isBlocked: Boolean
        get() = needConsent || needReconsent || needLocationAuthority || needPermissionNotice
}

/** Corresponds conceptually to `…usecase.ObserveUclAgreement`, via the repo's observe half. */
class StudyObserveUclAgreement @Inject constructor(
    private val consentRepo: StudyUserPolicyConsentRepo,
) : StudySingleUsecaseK<Int> {
    override fun invoke(): Flow<Int> = consentRepo.observeUclAgreement()
}
