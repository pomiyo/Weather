package dev.local.weatherstudy.devopts

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.devopts.DevOpts
 * com.samsung.android.weather.devopts.DevOptsMode
 * com.samsung.android.weather.devopts.GetDefaultDevOptsEntity
 * and the 36 mock-entity classes in `com.samsung.android.weather.devopts.models`
 *
 * ### Why this module matters more than its name suggests
 *
 * `DevOpts` is injected into **`DetailViewModel`** — it is one of the 29 constructor
 * parameters. It is not a debug side-channel bolted on; it is a first-class input to
 * production code, and that is what makes the whole app testable on a desk.
 *
 * The 36 `*MockEntity` classes in `devopts.models` are the giveaway. The original ships
 * a mock for nearly every environmental input:
 *
 * ```
 * CscFeatureMockEntity        FloatingFeatureMockEntity   DeviceServiceMockEntity
 * DeviceTelephonyMockEntity   DeviceMonitorMockEntity     LocationMockEntity
 * ForecastProviderMockEntity  WeatherMockEntity           SmartThingsMockEntity
 * MockPolicy                  MockForecastChange          MockMonitoring
 * FreeNewsMockEntity          WidgetSizeMockEntity        MockAppStoreEntity
 * WidgetShowLastUpdateMockEntity  CustomizationMockEntity …
 * ```
 *
 * and the domain has a matching `Mock*` use case for every network-bound one
 * (`MockFetchWeather`, `MockCheckForecastChange`, `MockFetchCurrentObservation`, …).
 *
 * **That is the seam the reconstruction uses instead of inventing a fake layer.** When
 * `devOpts.isMockWeatherEnabled` is on, the real use-case graph runs against mock
 * sources the original already provides — so the reconstruction's "no real backend"
 * mode is the original's own developer mode, not a parallel codebase.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
@Singleton
class StudyDevOpts @Inject constructor() {

    var mode: StudyDevOptsMode = StudyDevOptsMode.NORMAL

    /** when on, the `Mock*` use cases are bound instead of the network ones */
    val isMockWeatherEnabled: Boolean get() = mode == StudyDevOptsMode.MOCK

    /** forces a provider, bypassing region routing */
    var forcedProviderId: String? = null

    /** forces a country code into the policy/region decision */
    var forcedCountryCode: String? = null

    /** overrides the device form factor, so fold/tablet layouts can be seen on a phone */
    var forcedDeviceType: String? = null

    /** forces day or night, which drives the themed splash and every icon lookup */
    var forcedDayOrNight: Int? = null

    /** makes every expiry check report "stale", so a refresh can be triggered on demand */
    var forceStaleData: Boolean = false

    /** turns on the control-point dots in StudyBezierLineGraphItemView */
    var showGraphDebugPoints: Boolean = false

    /** body-level network logging; off by default even here */
    var verboseNetworkLogging: Boolean = false

    fun reset() {
        mode = StudyDevOptsMode.NORMAL
        forcedProviderId = null
        forcedCountryCode = null
        forcedDeviceType = null
        forcedDayOrNight = null
        forceStaleData = false
        showGraphDebugPoints = false
        verboseNetworkLogging = false
    }
}

/**
 * Corresponds conceptually to `…devopts.DevOptsMode`.
 *
 * Observed responsibility: which source family is bound. `MOCK` is what
 * `…system.service.dev` and the domain's `Mock*` use cases exist for.
 */
enum class StudyDevOptsMode {
    /** real sources */
    NORMAL,

    /** the original's mock families — see the note on [StudyDevOpts] */
    MOCK,

    /** mixed: real persistence, mock network */
    MOCK_NETWORK_ONLY,
}

/**
 * Corresponds conceptually to `…devopts.GetDefaultDevOptsEntity`.
 *
 * Observed responsibility: the default mock payload, so turning mock mode on yields
 * something renderable immediately rather than an empty screen.
 */
class StudyGetDefaultDevOptsEntity @Inject constructor() {
    operator fun invoke(): StudyDevOptsEntity = StudyDevOptsEntity()
}

/**
 * Corresponds conceptually to `…devopts.models.DevOptsEntity`.
 *
 * Observed responsibility: the persisted developer-options state. The original serialises
 * it with Moshi (`DevOptsEntityJsonAdapter` is in the APK), which is why it is a flat
 * data class rather than a preferences bundle.
 */
data class StudyDevOptsEntity(
    val mode: String = StudyDevOptsMode.NORMAL.name,
    val forcedProviderId: String = "",
    val forcedCountryCode: String = "",
    val forcedDeviceType: String = "",
    val forcedDayOrNight: Int = -1,
    val forceStaleData: Boolean = false,
    val showGraphDebugPoints: Boolean = false,
    val verboseNetworkLogging: Boolean = false,
)
