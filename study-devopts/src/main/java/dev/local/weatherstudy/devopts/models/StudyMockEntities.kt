package dev.local.weatherstudy.devopts.models

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the 36 classes in
 * com.samsung.android.weather.devopts.models:
 * `CscFeatureMockEntity`, `FloatingFeatureMockEntity`, `DeviceServiceMockEntity`,
 * `DeviceTelephonyMockEntity`, `DeviceMonitorMockEntity`, `LocationMockEntity`,
 * `ForecastProviderMockEntity`, `WeatherMockEntity`, `SmartThingsMockEntity`,
 * `MockPolicy`, `MockForecastChange`, `MockMonitoring`, `FreeNewsMockEntity`,
 * `WidgetSizeMockEntity`, `WidgetShowLastUpdateMockEntity`, `CustomizationMockEntity`,
 * `MockAppStoreEntity` and their Moshi adapters.
 *
 * Observed responsibility: a mock override per environmental input. One mock entity per
 * thing the app cannot control in production — the CSC feature table, the floating
 * feature flags, the telephony state, the device form factor, the location fix, the
 * active provider, the weather payload itself.
 *
 * This is the reason the architecture is as abstracted as it is. Each
 * `*MockEntity` here pairs with an interface in `:study-system-service` and a
 * `…service.dev` implementation — the mock family is the third implementation of the
 * platform seam, after AOSP and Samsung.
 *
 * A representative set is reconstructed; the full original list is above and in
 * `reports/reconstruction-inventory.md` §3.19.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
data class StudyCscFeatureMockEntity(
    val temperatureUnit: Int = 0,
    val defaultAutoRefreshInterval: Int = 3,
    val enableScreenOnRefresh: Boolean = false,
    val isTaiwan: Boolean = false,
    val isHongKong: Boolean = false,
    val isMEA: Boolean = false,
)

/** Corresponds conceptually to `…models.FloatingFeatureMockEntity`. */
data class StudyFloatingFeatureMockEntity(
    val isFoldDevice: Boolean = false,
    val isFlipDevice: Boolean = false,
    val aodFeature: Boolean = false,
)

/** Corresponds conceptually to `…models.DeviceServiceMockEntity`. */
data class StudyDeviceServiceMockEntity(
    val salesCode: String = "",
    val countryCode: String = "",
    val oneUiVersion: Int = 0,
    val isTablet: Boolean = false,
    val isRetailMode: Boolean = false,
)

/** Corresponds conceptually to `…models.DeviceTelephonyMockEntity`. */
data class StudyDeviceTelephonyMockEntity(
    val mcc: String = "",
    val mnc: String = "",
    val networkCountryCode: String = "",
    val isRoaming: Boolean = false,
)

/** Corresponds conceptually to `…models.DeviceMonitorMockEntity`. */
data class StudyDeviceMonitorMockEntity(
    val isFoldable: Boolean = false,
    val isFolded: Boolean = false,
    val isDesktopMode: Boolean = false,
    val isKidsMode: Boolean = false,
)

/**
 * Corresponds conceptually to `…models.LocationMockEntity`.
 *
 * Observed responsibility: a fixed coordinate, so the location subsystem can be driven
 * without a GPS fix. The original pairs it with `MockLocationProvider` and
 * `MockWeatherGeofenceProvider` in the domain.
 */
data class StudyLocationMockEntity(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val enabled: Boolean = false,
)

/** Corresponds conceptually to `…models.ForecastProviderMockEntity`. */
data class StudyForecastProviderMockEntity(
    val providerId: String = "",
    val countryCode: String = "",
)

/** Corresponds conceptually to `…models.WeatherMockEntity`. */
data class StudyWeatherMockEntity(
    val cityName: String = "",
    val temperature: Double = 0.0,
    val iconNum: Int = 0,
    val dayOrNight: Int = 1,
    val enabled: Boolean = false,
)

/** Corresponds conceptually to `…models.MockPolicy`. */
data class StudyMockPolicyEntity(
    val overrides: Map<String, Boolean> = emptyMap(),
)

/** Corresponds conceptually to `…models.SmartThingsMockEntity`. */
data class StudySmartThingsMockEntity(
    val roomCount: Int = 0,
    val deviceCount: Int = 0,
    val enabled: Boolean = false,
)

/** Corresponds conceptually to `…models.WidgetSizeMockEntity`. */
data class StudyWidgetSizeMockEntity(
    val columns: Int = 4,
    val rows: Int = 2,
    val enabled: Boolean = false,
)

/** Corresponds conceptually to `…models.MockForecastChange`. */
data class StudyMockForecastChangeEntity(
    val title: String = "",
    val description: String = "",
    val enabled: Boolean = false,
)
