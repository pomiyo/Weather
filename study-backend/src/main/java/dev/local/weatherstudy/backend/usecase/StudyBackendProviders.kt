package dev.local.weatherstudy.backend.usecase

import dev.local.weatherstudy.backend.dao.StudyBackendDao
import dev.local.weatherstudy.backend.encryptor.StudyBackendEncryptor
import dev.local.weatherstudy.domain.source.backend.StudyAuthorityProvider
import dev.local.weatherstudy.domain.source.backend.StudyLogProvider
import dev.local.weatherstudy.domain.source.backend.StudySecureKeyProvider
import dev.local.weatherstudy.domain.source.backend.StudySecureLinkProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.backend.usecase.GetBackendDatabase
 * and the implementations behind `…domain.source.backend.{SecureKeyProvider,
 * SecureLinkProvider, AuthorityProvider, LogProvider}`
 *
 * Observed responsibility: turn the untyped backend registry into the typed accessors
 * the rest of the app uses. The original's `SecureKeyProvider` has 16 named getters and
 * `SecureLinkProvider` has 27 — four per provider plus three setters — all reading
 * named rows out of this one key/value table.
 *
 * ### The credential accessors return empty, by design
 *
 * [StudyEmptySecureKeyProvider] is the whole point of the boundary: the interface shape
 * is reconstructed so the architecture reads correctly, and the values are not. Nothing
 * in this project read a credential row, and nothing here can produce one.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyGetBackendValue @Inject constructor(
    private val backendDao: StudyBackendDao,
    private val encryptor: StudyBackendEncryptor,
) {
    suspend operator fun invoke(key: String): String = encryptor.decode(backendDao.getValue(key))
}

/**
 * Corresponds conceptually to the implementation behind `SecureKeyProvider`.
 *
 * Every accessor returns `""`. See the file note — this is the deliberate end of the
 * credential chain, not an unfinished stub.
 */
@Singleton
class StudyEmptySecureKeyProvider @Inject constructor() : StudySecureKeyProvider {
    override suspend fun getProviderApiKey(providerId: String): String = ""
    override suspend fun getProviderSecretKey(providerId: String): String = ""

    /**
     * The original's manifest carries a plaintext Google Maps key. It is redacted in
     * every report in this project and is not reproduced; the map surfaces are stubbed.
     */
    override suspend fun getMapsApiKey(): String = ""

    override suspend fun getAccountClientId(): String = ""
    override suspend fun getAccountClientSecret(): String = ""
    override suspend fun getConsentHistoryServerKey(): String = ""
}

/**
 * Corresponds conceptually to the implementation behind `SecureLinkProvider`.
 *
 * Unlike the keys, the *links* are structure worth reconstructing: every provider has a
 * domain, a terms link, a privacy link, a feedback link and a partner code, and the
 * feedback link is refreshed from the forecast response at runtime (hence the setter).
 * The reconstruction serves local fixture URLs.
 */
@Singleton
class StudyFixtureSecureLinkProvider @Inject constructor() : StudySecureLinkProvider {

    private val feedbackOverrides = mutableMapOf<String, String>()

    override suspend fun getDomain(providerId: String): String = ""

    override suspend fun getTermsLink(providerId: String): String = "$FIXTURE_BASE/$providerId/terms"

    override suspend fun getPrivacyLink(providerId: String): String = "$FIXTURE_BASE/$providerId/privacy"

    override suspend fun getFeedbackLink(providerId: String): String =
        feedbackOverrides[providerId] ?: "$FIXTURE_BASE/$providerId/feedback"

    override suspend fun getPartnerCode(providerId: String): String = "study"

    /** the original refreshes this from the forecast payload, so it is mutable state */
    override fun setFeedbackLink(providerId: String, url: String) {
        feedbackOverrides[providerId] = url
    }

    private companion object {
        const val FIXTURE_BASE = "https://example.invalid/weatherstudy"
    }
}

/**
 * Corresponds conceptually to the implementation behind `AuthorityProvider`.
 *
 * Observed responsibility: the content-provider authority. The original picks between a
 * current authority and a legacy AccuWeather-era one at runtime, which is why this is an
 * interface and not a constant.
 */
@Singleton
class StudyLocalAuthorityProvider @Inject constructor() : StudyAuthorityProvider {
    override fun getUriAuth(): String = AUTHORITY

    companion object {
        const val AUTHORITY = "dev.local.weatherstudy.provider"

        /**
         * The original also answers a legacy authority inherited from an earlier
         * provider era, which is how long-installed companion apps keep working.
         */
        const val LEGACY_AUTHORITY = "dev.local.weatherstudy.provider.legacy"
    }
}

/** Corresponds conceptually to the implementation behind `LogProvider`. */
@Singleton
class StudyDebugLogProvider @Inject constructor() : StudyLogProvider {
    override fun isNetworkLogEnabled(): Boolean = true

    /** body logging stays off: a response body could echo a request header */
    override fun isBodyLogEnabled(): Boolean = false
}
