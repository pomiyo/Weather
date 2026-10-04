package dev.local.weatherstudy.domain.source.backend

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.source.backend.AuthorityProvider
 *
 * Observed responsibility: supplies the content-provider authority string. It is an
 * interface because the original ships a legacy AccuWeather authority alongside the
 * current one and picks between them at runtime.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyAuthorityProvider {
    fun getUriAuth(): String
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.source.backend.SecureKeyProvider
 *
 * Observed responsibility in the original: hands out the per-backend API credentials
 * that ship inside the APK's bundled `assets/database/backend_ver8.db`.
 *
 * ### This is the credential seam, and it is deliberately empty here.
 *
 * The original declares one accessor per secret (provider API keys, Samsung account
 * client id/secret, consent-history server key, ad-collection keys, Maps key). The
 * *shape* of that interface is architecturally meaningful — it is why the network
 * layer never hard-codes a key, and why a rebuild cannot reach these backends — so
 * the interface is reconstructed. The values are not: no key was read from the
 * bundled database, none is reproduced, and the reconstruction's implementation
 * returns empty strings. See `reports/samsung-bridge-map.md`.
 *
 * Accessor names are kept generic rather than vendor-specific, so this file names no
 * real provider's credential.
 */
interface StudySecureKeyProvider {
    suspend fun getProviderApiKey(providerId: String): String
    suspend fun getProviderSecretKey(providerId: String): String
    suspend fun getMapsApiKey(): String
    suspend fun getAccountClientId(): String
    suspend fun getAccountClientSecret(): String
    suspend fun getConsentHistoryServerKey(): String
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.source.backend.SecureLinkProvider
 *
 * Observed responsibility: per-backend base domains and legal links (terms, privacy,
 * feedback, partner code). Unlike the keys, these are *structure* — the fact that
 * each provider has a domain, a T&C link, a PP link, a feedback link and a partner
 * code is the architecture. The reconstruction returns local fixture URLs.
 *
 * The original has four accessors per provider × five providers plus setters for the
 * three feedback links (they are refreshed from the forecast response). Reconstructed
 * as a parameterised accessor set over [providerId], documented as a merge in
 * `reports/class-mapping.md` §18.
 */
interface StudySecureLinkProvider {
    suspend fun getDomain(providerId: String): String
    suspend fun getTermsLink(providerId: String): String
    suspend fun getPrivacyLink(providerId: String): String
    suspend fun getFeedbackLink(providerId: String): String
    suspend fun getPartnerCode(providerId: String): String
    fun setFeedbackLink(providerId: String, url: String)
}

/** Corresponds conceptually to `…source.backend.LogProvider`. */
interface StudyLogProvider {
    fun isNetworkLogEnabled(): Boolean
    fun isBodyLogEnabled(): Boolean
}
