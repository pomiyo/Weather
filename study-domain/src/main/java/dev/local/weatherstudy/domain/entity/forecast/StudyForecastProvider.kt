package dev.local.weatherstudy.domain.entity.forecast

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.entity.forecast.ForecastProvider
 * com.samsung.android.weather.domain.entity.forecast.ForecastProviderInfo
 *
 * Observed responsibilities:
 * - `ForecastProvider` in the original is an `object` with two functions:
 *   `dispatchByCountryCode(code)` and `isChinaProvider(name)`. That is the whole
 *   region-routing policy: the backend is chosen from the device's country/CSC,
 *   not configured.
 * - `ForecastProviderInfo` carries the chosen provider's display name, description
 *   and logo resource — which is what the Indicator detail card renders at the
 *   bottom of the screen ("Weather data provided by …").
 *
 * The five real backends are reconstructed as [PROVIDER_A]…[PROVIDER_E]; the mapping
 * back to the originals is recorded in `reports/class-mapping.md` §10. Using neutral
 * ids here keeps independently written code from naming real vendors' APIs.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
object StudyForecastProvider {

    const val PROVIDER_A = "provider_a"
    const val PROVIDER_B = "provider_b"
    const val PROVIDER_C = "provider_c"
    const val PROVIDER_D = "provider_d"
    const val PROVIDER_E = "provider_e"

    /**
     * Reconstruction of `ForecastProvider.dispatchByCountryCode`.
     *
     * Observed behaviour: a country/region code selects the backend. The original's
     * table is driven by the bundled backend database and the Samsung CSC feature;
     * the shape — a pure function from region to provider id, with a global default —
     * is what matters architecturally.
     */
    fun dispatchByCountryCode(countryCode: String): String = when (countryCode.uppercase()) {
        "JP" -> PROVIDER_B
        "KR" -> PROVIDER_C
        "CN" -> PROVIDER_D
        "" -> PROVIDER_A
        else -> PROVIDER_A
    }

    /**
     * Reconstruction of `ForecastProvider.isChinaProvider`.
     *
     * Observed behaviour: several features (news, radar, the EULA variant, the AQI
     * scale) branch on this one predicate rather than on the country, because a
     * device sold elsewhere can still be routed to the China backend.
     */
    fun isRegionRestrictedProvider(providerName: String): Boolean = providerName == PROVIDER_D
}

/** Corresponds conceptually to `…entity.forecast.ForecastProviderInfo`. */
data class StudyForecastProviderInfo(
    val name: String,
    val description: String = "",
    val icon: String = "",
    val resourceId: Int = 0,
)

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.ForecastProviderManager
 *
 * Observed responsibility: holds the active provider and distinguishes three answers
 * to "which provider?" — the one currently active, the one the DEVICE shipped with
 * (`getDeviceCpType`), and the one the NETWORK says to use (`getNetworkCpType`).
 * A mismatch between the last two is what raises the "weather provider is changing"
 * popup via `HomeCpChanged` / `ShowCpChangeState`.
 */
interface StudyForecastProviderManager {
    fun getActive(): StudyForecastProviderInfo
    fun setActive(info: StudyForecastProviderInfo)
    fun getInfo(name: String): StudyForecastProviderInfo
    fun getDeviceProviderType(): StudyForecastProviderInfo
    fun getNetworkProviderType(): StudyForecastProviderInfo
}
