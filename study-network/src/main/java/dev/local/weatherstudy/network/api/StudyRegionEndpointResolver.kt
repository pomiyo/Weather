package dev.local.weatherstudy.network.api.forecast

import dev.local.weatherstudy.domain.entity.forecast.StudyForecastProvider
import dev.local.weatherstudy.domain.source.backend.StudySecureLinkProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the region-routing the original performs across
 * com.samsung.android.weather.backend.dao.BackendDao,
 * com.samsung.android.weather.domain.entity.forecast.ForecastProvider and
 * com.samsung.android.weather.domain.source.backend.SecureLinkProvider
 *
 * ### The architecture being preserved
 *
 * Samsung Weather does not have "an API". It has five, and which one a device talks to
 * is decided at runtime from the device's region:
 *
 * ```
 * device country / CSC sales code
 *        ↓   ForecastProvider.dispatchByCountryCode()
 * provider id
 *        ↓   SecureLinkProvider.get<Provider>Domain()
 * base URL   ←── read from the bundled backend database
 *        ↓   Retrofit.Builder().baseUrl(...)
 * <Provider>RetrofitService
 * ```
 *
 * The bundled `assets/database/backend_ver8.db` is the lookup table for the last two
 * steps — it holds the domains *and* the credentials, which is why an independent
 * rebuild cannot reach these backends even with the code. This resolver reconstructs
 * the decision; `StudySecureLinkProvider`'s implementation points it at local fixtures.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Singleton
class StudyRegionEndpointResolver @Inject constructor(
    private val secureLinkProvider: StudySecureLinkProvider,
) {

    /** Reconstruction of the country → provider decision. */
    fun resolveProviderId(countryCode: String): String =
        StudyForecastProvider.dispatchByCountryCode(countryCode)

    /** Reconstruction of the provider → base URL lookup. */
    suspend fun resolveBaseUrl(providerId: String): String =
        secureLinkProvider.getDomain(providerId).ifEmpty { FIXTURE_BASE_URL }

    /**
     * Reconstruction of the second host provider E uses for alerts — the only provider
     * whose service is split across two base URLs.
     */
    suspend fun resolveAlertBaseUrl(providerId: String): String =
        if (providerId == StudyForecastProvider.PROVIDER_E) {
            secureLinkProvider.getDomain("${providerId}_alert").ifEmpty { FIXTURE_BASE_URL }
        } else {
            resolveBaseUrl(providerId)
        }

    companion object {
        /**
         * Where the reconstruction actually points. No Samsung endpoint is contacted;
         * [dev.local.weatherstudy.network.fixture.StudyFixtureInterceptor] serves every
         * request from bundled JSON before it leaves the process.
         */
        const val FIXTURE_BASE_URL = "http://localhost/weatherstudy/"
    }
}
