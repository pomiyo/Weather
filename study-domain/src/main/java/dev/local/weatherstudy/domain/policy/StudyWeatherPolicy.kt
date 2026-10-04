package dev.local.weatherstudy.domain.policy

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.policy.WeatherPolicy
 *
 * Observed responsibility — one of the most load-bearing types in the app:
 * a 70-method capability object answering "is feature X available here?". The
 * detail screen does not decide which cards exist; it asks this. So does the widget
 * editor, the settings screen and the refresh pipeline.
 *
 * Two structural details worth keeping:
 *
 * 1. **Most methods have defaults; only 13 are abstract.** The abstract ones are the
 *    questions whose answer genuinely differs per provider — alert, away mode, fixed
 *    refresh interval, label, life style, narrative, precipitation graph, radar,
 *    report-incorrect-info, represent location, SmartThings, today stories, video.
 *    Everything else has a sensible global answer that a region can override. That
 *    split tells you exactly where the five backends actually diverge.
 *
 * 2. **Some answers are composed from others** — `supportContent()` is
 *    `supportRadar() || supportVideo() || supportTodayStories() || supportInsightCard()`,
 *    and `supportInsightCard()` is `V1 || V2`. Preserved, because callers rely on the
 *    composite rather than re-deriving it.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
interface StudyWeatherPolicy {

    // ---- abstract: genuinely provider-dependent ----
    fun supportAlert(): Boolean
    fun supportAwayMode(): Boolean
    fun supportFixedRefreshInterval(): Boolean
    fun supportLabel(): Boolean
    fun supportLifeStyle(): Boolean
    fun supportNarrative(): Boolean
    fun supportPrecipitationGraph(): Boolean
    fun supportRadar(): Boolean
    fun supportReportIncorrectInfo(): Boolean
    fun supportRepresentLocation(): Boolean
    fun supportSmartThings(): Boolean
    fun supportTodayStories(): Boolean
    fun supportVideo(): Boolean

    // ---- measurements / index cards ----
    fun supportAQI(): Boolean = true
    fun supportPM10(): Boolean = true
    fun supportPM25(): Boolean = true
    fun supportPollen(): Boolean = false
    fun supportUV(): Boolean = true
    fun supportHumidity(): Boolean = true
    fun supportPress(): Boolean = true
    fun supportWind(): Boolean = true
    fun supportVisibility(): Boolean = true
    fun supportDewpoint(): Boolean = true
    fun supportFeelsLike(): Boolean = true
    fun supportPrecipitation(): Boolean = true
    fun supportHourlyPrecipitation(): Boolean = true
    fun supportShortTermPrecipitation(): Boolean = false
    fun supportSunCycle(): Boolean = true
    fun supportMoonCycle(): Boolean = false
    fun supportDrivingIndex(): Boolean = false
    fun supportGolf(): Boolean = false
    fun supportRunning(): Boolean = false

    // ---- content ----
    fun supportInsightCardV1(): Boolean = false
    fun supportInsightCardV2(): Boolean = false
    fun supportInsightCard(): Boolean = supportInsightCardV1() || supportInsightCardV2()
    fun supportInsightTips(): Boolean = false
    fun supportNews(): Boolean = false
    fun supportContent(): Boolean =
        supportRadar() || supportVideo() || supportTodayStories() || supportInsightCard()

    // ---- surfaces ----
    fun supportLocations(): Boolean = true
    fun supportSearch(): Boolean = true
    fun supportTextSearch(): Boolean = true
    fun supportMapSearch(): Boolean = false
    fun supportThemeArea(): Boolean = false
    fun supportSetting(): Boolean = true
    fun supportTempScale(): Boolean = true
    fun supportContactUs(): Boolean = false
    fun supportCpSource(): Boolean = true

    // ---- widgets / tiles ----
    fun supportBriefWidget(): Boolean = true
    fun supportCoverWidget(): Boolean = false
    fun supportInsightWidget(): Boolean = false
    fun supportSmartPageWidget(): Boolean = false
    fun supportDefaultWidgetWithInsight(): Boolean = false
    fun supportWeatherTile(): Boolean = false
    fun supportWeatherForecastTile(): Boolean = false

    // ---- refresh / location ----
    fun supportGeofence(): Boolean = false
    fun supportAwayToAwayMode(): Boolean = false
    fun supportAutoRefreshOnTheGo(): Boolean = false
    fun supportAutoRefreshOnTheGoTips(): Boolean = false
    fun supportExpireTime(): Boolean = true
    fun useCurrentLocation(): Boolean = true

    // ---- consent / legal ----
    fun supportLocationAuthority(): Boolean = false
    fun supportPermissionNotice(): Boolean = false
    fun supportPermissionPage(): Boolean = true
    fun supportNetworkCharges(): Boolean = false
    fun restrictWebLink(): Boolean = false

    // ---- notifications ----
    fun supportNoticeOfForecastChange(): Boolean = false
    fun supportNoticeOfNarrative(): Boolean = false
    fun supportSevereWeatherForecast(): Boolean = supportAlert()

    // ---- modes / integrations ----
    fun supportKidsMode(): Boolean = false
    fun supportCustomizationService(): Boolean = false
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.policy.WeatherPolicyManager
 *
 * Observed responsibility: the original declares this as `interface WeatherPolicyManager
 * extends WeatherPolicy` and nothing more — i.e. the manager IS a policy. Callers
 * inject the manager and ask it questions directly; which concrete policy is behind it
 * (per provider, per region, per One UI version) is a DI decision.
 */
interface StudyWeatherPolicyManager : StudyWeatherPolicy

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.policy.ForecastProviderPolicy
 *
 * Observed responsibility: the per-provider base from which the regional policies
 * derive. Reconstructed as an abstract class so the five providers differ only where
 * they genuinely do.
 */
abstract class StudyForecastProviderPolicy : StudyWeatherPolicy {
    abstract val providerId: String
}

/**
 * Corresponds conceptually to `…policy.UserPolicyConsent`.
 *
 * Observed responsibility: the consent *vocabulary*, separate from the data source
 * that stores it. `pnVersion` is the published privacy-notice version; the country
 * decides which notice text applies (see the four regional `EulaTextProvider`s).
 */
interface StudyUserPolicyConsent {
    fun getCountry(): String
    fun getPnVersion(): Int
}

/** Corresponds conceptually to `…policy.UserPolicyConstant`. */
object StudyUserPolicyConstant {
    const val CONSENT_NONE = 0
    const val CONSENT_AGREED = 1
    const val CONSENT_DISAGREED = 2

    const val CONSENT_TYPE_UNKNOWN = 0
    const val CONSENT_TYPE_GLOBAL = 1
    const val CONSENT_TYPE_KOREA = 2
    const val CONSENT_TYPE_JAPAN = 3
    const val CONSENT_TYPE_CHINA = 4
}

/**
 * Reconstruction of `…policy.OrderingPolicyKt`.
 *
 * Observed responsibility: the default order of the detail cards. In the original this
 * is a file-facade function in the policy package, not a constant in the UI — the
 * ORDER IS POLICY, and `GetCardOrder` reads it, then lets the user's life-style
 * settings and the provider's capabilities filter it.
 */
object StudyOrderingPolicy {
    /** Card-type ids, matching `StudyDetailCardType` in `:study-ui-common`. */
    val DEFAULT_DETAIL_CARD_ORDER = listOf(
        "Alert",
        "Insight",
        "Hourly",
        "Precipitation",
        "Daily",
        "AirIndex",
        "Index",
        "SunAndMoon",
        "Sun",
        "Moon",
        "BottomIndex",
        "LifeStyle",
        "LifeTips",
        "Radar",
        "SmartThings",
        "NewsAndVideo",
        "News",
        "Video",
        "TodayStoriesAndVideo",
        "Indicator",
    )
}
