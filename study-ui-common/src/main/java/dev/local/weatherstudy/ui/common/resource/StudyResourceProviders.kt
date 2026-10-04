package dev.local.weatherstudy.ui.common.resource

import dev.local.weatherstudy.domain.entity.forecast.StudyForecastProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the 30 classes in
 * com.samsung.android.weather.ui.common.resource and
 * com.samsung.android.weather.app.common.resource:
 * `WeatherIconProvider`, `WeatherAnimIconProvider`, `AnimIconProvider`,
 * `ThemeIconProvider`, `GearIconProvider`, `MoonPhaseImageProvider`,
 * `BackgroundProvider`, `IconProvider`, `UnitProvider`, `TwcAqiProvider`,
 * `TTSInfoProvider`, `TextProvider` (+ Global/Korea/Japan/China),
 * `EulaTextProvider` (+ 4 regional), `EulaLayoutProvider` (+ China/Global),
 * `LocationAuthorityTextProvider`, `PermissionNoticeTextProvider`
 *
 * ### The regional provider families
 *
 * `TextProvider` has **four** implementations — Global, Korea, Japan, China — and so
 * does `EulaTextProvider`. The same string id resolves to different text per region,
 * and `EulaLayoutProvider` goes further: China gets a different *layout*, not just
 * different words, because its consent flow has more checkboxes.
 *
 * That is why these are interfaces with regional subclasses rather than string
 * resources with locale qualifiers: the branch is on the **CSC region**, not the device
 * language. A Korean-language device sold in the US gets the Global consent flow.
 *
 * The reconstruction keeps the interfaces and the four-way split, with neutral English
 * text — Samsung's strings are not reproduced.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyWeatherIconProvider {
    /** static WebP for a condition code */
    fun getIconRes(iconNum: Int, isDay: Boolean): Int

    /** the small variant used in lists and widgets */
    fun getSmallIconRes(iconNum: Int, isDay: Boolean): Int
}

/**
 * Corresponds conceptually to `…resource.WeatherAnimIconProvider` / `AnimIconProvider`.
 *
 * Observed responsibility: the Lottie animation for a condition. The original bundles
 * 69 Lottie JSON files in three sets — `assets/{dark,white,illust}/` — and picks the set
 * from the theme and the header state. The reconstruction keeps the selection logic and
 * ships no artwork.
 */
interface StudyWeatherAnimIconProvider {
    fun getAnimationAsset(iconNum: Int, isDay: Boolean, variant: StudyAnimVariant): String
}

/** The three Lottie asset sets the original bundles. */
enum class StudyAnimVariant {
    /** `assets/dark/` — for light backgrounds */
    DARK,

    /** `assets/white/` — for dark backgrounds */
    WHITE,

    /** `assets/illust/` — the large header illustration */
    ILLUSTRATION,
}

/** Corresponds conceptually to `…resource.MoonPhaseImageProvider`. */
interface StudyMoonPhaseImageProvider {
    fun getPhaseRes(phase: Int): Int
}

/**
 * Corresponds conceptually to `…app.common.resource.BackgroundProvider`.
 *
 * Observed responsibility: the per-condition gradient pair. The same mapping backs the
 * 11 themed splash activities, where it has to be expressed as a window theme instead —
 * see `reports/screen-map.md` §3.
 */
interface StudyBackgroundProvider {
    fun getGradientColors(iconNum: Int, isDay: Boolean): Pair<Int, Int>
    fun getSplashThemeName(iconNum: Int, isDay: Boolean): String
}

/**
 * Corresponds conceptually to `…resource.TextProvider` and its four regional
 * implementations.
 *
 * Observed responsibility: region-dependent wording. See the file note on why this is
 * not a locale qualifier.
 */
interface StudyTextProvider {
    val regionId: String
    fun getConditionText(iconNum: Int, isDay: Boolean): String
    fun getProviderAttribution(providerId: String): String
    fun getFeedbackLabel(): String
}

/** `GlobalTextProvider`. */
@Singleton
class StudyGlobalTextProvider @Inject constructor() : StudyTextProvider {
    override val regionId = REGION_GLOBAL
    override fun getConditionText(iconNum: Int, isDay: Boolean) = ""
    override fun getProviderAttribution(providerId: String) = "Weather data provided by $providerId"
    override fun getFeedbackLabel() = "Report incorrect information"

    companion object {
        const val REGION_GLOBAL = "global"
    }
}

/** `KoreaTextProvider`. */
@Singleton
class StudyKoreaTextProvider @Inject constructor() : StudyTextProvider {
    override val regionId = "korea"
    override fun getConditionText(iconNum: Int, isDay: Boolean) = ""
    override fun getProviderAttribution(providerId: String) = "Provided by $providerId"
    override fun getFeedbackLabel() = "Report an error"
}

/** `JapanTextProvider`. */
@Singleton
class StudyJapanTextProvider @Inject constructor() : StudyTextProvider {
    override val regionId = "japan"
    override fun getConditionText(iconNum: Int, isDay: Boolean) = ""
    override fun getProviderAttribution(providerId: String) = "Source: $providerId"
    override fun getFeedbackLabel() = "Report an error"
}

/** `ChinaTextProvider`. */
@Singleton
class StudyChinaTextProvider @Inject constructor() : StudyTextProvider {
    override val regionId = "china"
    override fun getConditionText(iconNum: Int, isDay: Boolean) = ""
    override fun getProviderAttribution(providerId: String) = "Source: $providerId"
    override fun getFeedbackLabel() = "Feedback"
}

/**
 * Corresponds conceptually to `…resource.pp.eula.EulaTextProvider` and its four
 * regional implementations, plus `EulaLayoutProvider` / `ChinaEulaLayoutProvider` /
 * `GlobalEulaLayoutProvider`.
 *
 * Observed responsibility: the consent flow differs by region in *structure*, not only
 * wording — which is why there is a layout provider as well as a text provider.
 */
interface StudyEulaTextProvider {
    val regionId: String
    fun getTitle(): String
    fun getBody(): String
    fun requiresSeparateLocationConsent(): Boolean
    fun requiresNetworkChargesConsent(): Boolean
    /** China's flow has additional checkboxes; see `ChinaEulaLayoutProvider` */
    fun additionalConsentCount(): Int
}

/** `GlobalEulaTextProvider`. */
@Singleton
class StudyGlobalEulaTextProvider @Inject constructor() : StudyEulaTextProvider {
    override val regionId = StudyGlobalTextProvider.REGION_GLOBAL
    override fun getTitle() = "Terms and conditions"
    override fun getBody() =
        "Weather Study is an educational reconstruction of a weather app's architecture. " +
            "It is not a Samsung product and uses no Samsung service.\n\n" +
            "To show a forecast, the app sends the coordinates of the places you save - and, " +
            "if you allow location access, of where you are - to Open-Meteo.com, a public " +
            "weather service. When you search, it sends the text you type to the same " +
            "service. No account is used and nothing identifies you.\n\n" +
            "Forecasts are stored on this device so they can be shown without a connection. " +
            "Usage events are written to the device log only; nothing is uploaded.\n\n" +
            "Weather data by Open-Meteo.com, licensed CC BY 4.0."
    override fun requiresSeparateLocationConsent() = false
    override fun requiresNetworkChargesConsent() = false
    override fun additionalConsentCount() = 0
}

/** `KoreaEulaTextProvider` — Korea requires a separate location-authority consent. */
@Singleton
class StudyKoreaEulaTextProvider @Inject constructor() : StudyEulaTextProvider {
    override val regionId = "korea"
    override fun getTitle() = "Terms of service"
    override fun getBody() = "Placeholder consent text for the reconstruction."
    override fun requiresSeparateLocationConsent() = true
    override fun requiresNetworkChargesConsent() = false
    override fun additionalConsentCount() = 1
}

/** `JapanEulaTextProvider`. */
@Singleton
class StudyJapanEulaTextProvider @Inject constructor() : StudyEulaTextProvider {
    override val regionId = "japan"
    override fun getTitle() = "Terms of use"
    override fun getBody() = "Placeholder consent text for the reconstruction."
    override fun requiresSeparateLocationConsent() = false
    override fun requiresNetworkChargesConsent() = true
    override fun additionalConsentCount() = 1
}

/** `ChinaEulaTextProvider` — the most elaborate flow; see `ChinaEulaLayoutProvider`. */
@Singleton
class StudyChinaEulaTextProvider @Inject constructor() : StudyEulaTextProvider {
    override val regionId = "china"
    override fun getTitle() = "User agreement"
    override fun getBody() = "Placeholder consent text for the reconstruction."
    override fun requiresSeparateLocationConsent() = true
    override fun requiresNetworkChargesConsent() = true
    override fun additionalConsentCount() = 3
}

/**
 * Corresponds conceptually to `…resource.UnitProvider`.
 *
 * Observed responsibility: which unit each measurement is displayed in, derived from the
 * temperature scale plus the CSC default. It feeds the notation layer.
 */
interface StudyUnitProvider {
    fun getTemperatureScale(): Int
    fun getWindUnit(): Int
    fun getPressureUnit(): Int
    fun getDistanceUnit(): Int
    fun getPrecipitationUnit(): Int
}

/**
 * Corresponds conceptually to `…resource.TTSInfoProvider`.
 *
 * Observed responsibility: the spoken description of a card, which is not the visible
 * text — "twenty-three degrees, partly cloudy" rather than "23° Partly Cloudy". The
 * original has a dedicated provider because TalkBack strings are assembled differently,
 * and `DetailViewModel` exposes `isTalkBackEnabled` to switch on it.
 */
interface StudyTtsInfoProvider {
    fun describeCurrentConditions(temperature: String, conditionText: String): String
    fun describeHourlyRow(timeText: String, temperature: String, conditionText: String): String
    fun describeIndex(titleText: String, valueText: String, levelText: String): String
}

/**
 * Corresponds conceptually to `…resource.TwcAqiProvider` and the four
 * `Get*AqiGraphViewEntity` use cases.
 *
 * Observed responsibility: AQI banding is national. The original has Global, China,
 * Japan and Korea variants because the same pollutant concentration falls in different
 * bands under different national scales — see `StudyIndexLevel.AqiScale`.
 */
interface StudyAqiScaleProvider {
    fun getScaleId(): Int
    fun getBandBoundaries(): List<Float>
    fun getBandLabels(): List<String>
    fun levelFor(index: Int): Int
}

/** Reconstruction helper: the region each provider family is selected by. */
object StudyRegionSelector {
    fun regionIdFor(providerId: String, countryCode: String): String = when {
        StudyForecastProvider.isRegionRestrictedProvider(providerId) -> "china"
        countryCode.equals("KR", ignoreCase = true) -> "korea"
        countryCode.equals("JP", ignoreCase = true) -> "japan"
        else -> StudyGlobalTextProvider.REGION_GLOBAL
    }
}
