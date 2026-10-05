package dev.local.weatherstudy.domain.usecase

import dev.local.weatherstudy.domain.entity.content.StudyInsightContent
import dev.local.weatherstudy.domain.entity.content.StudyLifeStyleContent
import dev.local.weatherstudy.domain.entity.content.StudyWebContent
import dev.local.weatherstudy.domain.entity.content.StudyWebMenu
import dev.local.weatherstudy.domain.entity.forecast.StudyForecastProviderManager
import dev.local.weatherstudy.domain.entity.weather.StudyAlert
import dev.local.weatherstudy.domain.entity.weather.StudyCurrentObservation
import dev.local.weatherstudy.domain.entity.weather.StudyDailyObservation
import dev.local.weatherstudy.domain.entity.weather.StudyHourlyObservation
import dev.local.weatherstudy.domain.entity.weather.StudyIndex
import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import dev.local.weatherstudy.domain.policy.StudyWeatherPolicyManager
import dev.local.weatherstudy.domain.type.StudyIndexType
import dev.local.weatherstudy.domain.type.StudyInsightType
import javax.inject.Inject

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.usecase.ReviseWebLink
 *
 * Observed responsibility — and the reason this is one class with nine private methods
 * rather than nine use cases:
 *
 * every collection inside the aggregate carries its own outbound URL, and when links
 * are restricted (consent pending, or the provider policy forbids web links) EVERY ONE
 * must be cleared. The original declares exactly this method set:
 *
 * ```
 * revise(weather)
 *   ├─ reviseCurrentObservation(observation, restrict)
 *   ├─ reviseHourlyObservation(observations, restrict)
 *   ├─ reviseDailyObservation(observations, restrict)
 *   ├─ reviseAlerts(alerts, restrict)
 *   ├─ reviseInsightContent(insight, restrict)
 *   ├─ reviseLifeStyleContent(list, restrict)
 *   ├─ reviseWebContent(contents, restrict)        // videos + today stories
 *   ├─ reviseRadar(radar, restrict)
 *   └─ reviseWebMenus(menus, restrict)
 * ```
 *
 * plus a generic `revise(T, …)` helper the nine share. Missing one of these is exactly
 * the kind of bug this decomposition is designed to prevent, which is why it is kept
 * whole rather than folded into a single `map`.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyReviseWebLink @Inject constructor(
    private val policyManager: StudyWeatherPolicyManager,
) {

    operator fun invoke(weather: StudyWeather, restrict: Boolean = false): StudyWeather {
        val restricted = restrict || policyManager.restrictWebLink()
        return weather.copy(
            currentObservation = reviseCurrentObservation(weather.currentObservation, restricted),
            hourlyObservations = reviseHourlyObservation(weather.hourlyObservations, restricted),
            dailyObservations = reviseDailyObservation(weather.dailyObservations, restricted),
            alerts = reviseAlerts(weather.alerts, restricted),
            insightContent = reviseInsightContent(weather.insightContent, restricted),
            lifeStyleContent = reviseLifeStyleContent(weather.lifeStyleContent, restricted),
            videos = reviseWebContent(weather.videos, restricted),
            todayStories = reviseWebContent(weather.todayStories, restricted),
            radar = reviseRadar(weather.radar, restricted),
            webMenus = reviseWebMenus(weather.webMenus, restricted),
        )
    }

    /** the generic helper the nine revisers share */
    private fun revise(url: String, restrict: Boolean): String = if (restrict) "" else url

    private fun reviseCurrentObservation(o: StudyCurrentObservation, restrict: Boolean) =
        o.copy(webUrl = revise(o.webUrl, restrict), condition = reviseIndexUrls(o, restrict))

    private fun reviseIndexUrls(o: StudyCurrentObservation, restrict: Boolean) =
        o.condition.copy(indexList = o.condition.indexList.map { it.copy(webUrl = revise(it.webUrl, restrict)) })

    private fun reviseHourlyObservation(list: List<StudyHourlyObservation>, restrict: Boolean) =
        list.map { it.copy(webUrl = revise(it.webUrl, restrict)) }

    private fun reviseDailyObservation(list: List<StudyDailyObservation>, restrict: Boolean) =
        list.map { it.copy(webUrl = revise(it.webUrl, restrict)) }

    private fun reviseAlerts(list: List<StudyAlert>, restrict: Boolean) =
        list.map { it.copy(linkURL = revise(it.linkURL, restrict)) }

    private fun reviseInsightContent(list: List<StudyInsightContent>, restrict: Boolean) =
        list.map { it.copy(card = it.card.copy(url = revise(it.card.url, restrict))) }

    private fun reviseLifeStyleContent(list: List<StudyLifeStyleContent>, restrict: Boolean) =
        list.map { it.copy(url = revise(it.url, restrict)) }

    private fun reviseWebContent(list: List<StudyWebContent>, restrict: Boolean) =
        list.map { it.copy(url = revise(it.url, restrict), home = revise(it.home, restrict)) }

    private fun reviseRadar(radar: StudyWebContent?, restrict: Boolean) =
        radar?.copy(url = revise(radar.url, restrict), home = revise(radar.home, restrict))

    private fun reviseWebMenus(list: List<StudyWebMenu>, restrict: Boolean) =
        if (restrict) emptyList() else list
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.usecase.ReviseContent / ReviseContentImpl
 *
 * Observed responsibility: drop content the ACTIVE provider's policy does not support.
 * This runs on READ, not only on write, because a persisted row can outlive a provider
 * change — see [StudyGetWeather].
 */
interface StudyReviseContent {
    operator fun invoke(weather: StudyWeather): StudyWeather
}

class StudyReviseContentImpl @Inject constructor(
    private val policyManager: StudyWeatherPolicyManager,
) : StudyReviseContent {
    override fun invoke(weather: StudyWeather): StudyWeather = weather.copy(
        alerts = if (policyManager.supportAlert()) weather.alerts else emptyList(),
        radar = if (policyManager.supportRadar()) weather.radar else null,
        videos = if (policyManager.supportVideo()) weather.videos else emptyList(),
        todayStories = if (policyManager.supportTodayStories()) weather.todayStories else emptyList(),
        insightContent = if (policyManager.supportInsightCard()) weather.insightContent else emptyList(),
        lifeStyleContent = if (policyManager.supportLifeStyle()) weather.lifeStyleContent else emptyList(),
        forecastChange =
        if (policyManager.supportNoticeOfForecastChange()) weather.forecastChange else null,
    )
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.usecase.ReviseIndex
 *
 * Observed responsibility: filter the index list per policy. The index list is one flat
 * collection carrying UV, humidity, pressure, AQI, pollen and the life indices, and each
 * family has its own `support*()` question — so this is a per-type filter, not a
 * whole-list toggle.
 */
class StudyReviseIndex @Inject constructor(
    private val policyManager: StudyWeatherPolicyManager,
) {
    operator fun invoke(indices: List<StudyIndex>): List<StudyIndex> = indices.filter { supported(it.type) }

    private fun supported(type: Int): Boolean = when (type) {
        StudyIndexType.UV -> policyManager.supportUV()
        StudyIndexType.HUMIDITY -> policyManager.supportHumidity()
        StudyIndexType.PRESSURE -> policyManager.supportPress()
        StudyIndexType.WIND -> policyManager.supportWind()
        StudyIndexType.VISIBILITY -> policyManager.supportVisibility()
        StudyIndexType.DEW_POINT -> policyManager.supportDewpoint()
        StudyIndexType.AQI -> policyManager.supportAQI()
        StudyIndexType.PM10 -> policyManager.supportPM10()
        StudyIndexType.PM2_5 -> policyManager.supportPM25()
        StudyIndexType.POLLEN -> policyManager.supportPollen()
        StudyIndexType.MOON_PHASE, StudyIndexType.MOONRISE, StudyIndexType.MOONSET ->
            policyManager.supportMoonCycle()
        StudyIndexType.SUNRISE, StudyIndexType.SUNSET -> policyManager.supportSunCycle()
        StudyIndexType.GOLF -> policyManager.supportGolf()
        StudyIndexType.JOGGING -> policyManager.supportRunning()
        StudyIndexType.TRAFFIC -> policyManager.supportDrivingIndex()
        else -> true
    }
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.usecase.ReviseYesterday
 *
 * Observed responsibility: the original takes NO dependencies. It back-fills
 * `yesterdayMaxTemp`/`yesterdayMinTemp` on today's condition from the first daily row
 * that is in the past — i.e. yesterday's temperatures are not a separate API field,
 * they are read out of the daily series the provider already sent.
 */
class StudyReviseYesterday @Inject constructor() {
    operator fun invoke(weather: StudyWeather, now: Long = System.currentTimeMillis()): StudyWeather {
        val yesterday = weather.dailyObservations
            .lastOrNull { it.time.epochTime < startOfDay(now) }
            ?: return weather
        val condition = weather.currentObservation.condition.copy(
            yesterdayMaxTemp = yesterday.dayCondition.maxTemp,
            yesterdayMinTemp = yesterday.nightCondition.minTemp,
        )
        return weather.copy(currentObservation = weather.currentObservation.copy(condition = condition))
    }

    private fun startOfDay(now: Long) = now - (now % DAY_MILLIS)

    private companion object {
        const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    }
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.usecase.InsertTwilight
 *
 * Observed responsibility: synthesise the sunrise/sunset/twilight index rows from
 * [StudyForecastTime] and merge them into the insight list. The original's signature is
 * `insertTwilight(weather, insightContentList)` — it takes BOTH, because it reads the
 * sun times off the weather and writes insights into the list.
 *
 * Six twilight types exist (civil, nautical and astronomical dawn/dusk) and they are
 * all derived locally, never fetched.
 */
class StudyInsertTwilight @Inject constructor() :
    StudyUsecase<StudyWeather, Pair<StudyWeather, List<StudyInsightContent>>> {

    override suspend fun invoke(arg: Pair<StudyWeather, List<StudyInsightContent>>): StudyWeather {
        val (weather, remoteInsights) = arg
        val time = weather.currentObservation.time
        if (time.sunRiseTime <= 0 && time.sunSetTime <= 0) {
            return weather.copy(insightContent = remoteInsights)
        }
        val derived = buildList {
            add(twilightIndex(StudyIndexType.SUNRISE, time.sunRiseTime))
            add(twilightIndex(StudyIndexType.SUNSET, time.sunSetTime))
            if (time.moonRiseTime > 0) add(twilightIndex(StudyIndexType.MOONRISE, time.moonRiseTime))
            if (time.moonSetTime > 0) add(twilightIndex(StudyIndexType.MOONSET, time.moonSetTime))
        }
        val condition = weather.currentObservation.condition
        return weather.copy(
            currentObservation = weather.currentObservation.copy(
                condition = condition.copy(indexList = condition.indexList + derived),
            ),
            insightContent = remoteInsights,
        )
    }

    private fun twilightIndex(type: Int, epoch: Long) = StudyIndex(
        type = type,
        category = dev.local.weatherstudy.domain.type.StudyIndexCategory.DETAIL,
        value = epoch.toDouble(),
    )
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.usecase.InsertIndexDescription
 *
 * Observed responsibility: turn plain measurements into the six locally-derived
 * "condition" insights (wind, dew point, pressure, UV, humidity, visibility). The
 * original injects `ForecastProviderManager` because the description text depends on
 * which provider is active — the same UV value gets different wording per backend.
 *
 * This is why [StudyInsightType] has two families; see the note there.
 */
class StudyInsertIndexDescription @Inject constructor(
    private val forecastProviderManager: StudyForecastProviderManager,
) : StudyUsecase<StudyWeather, Pair<StudyWeather, List<StudyInsightContent>>> {

    override suspend fun invoke(arg: Pair<StudyWeather, List<StudyInsightContent>>): StudyWeather {
        val (weather, remoteInsights) = arg
        val provider = forecastProviderManager.getActive().name
        val indices = weather.currentObservation.condition.indexList
        val derived = indices.mapNotNull { index ->
            conditionInsightType(index.type)?.let { insightType ->
                StudyInsightContent(
                    insightType = insightType,
                    order = CONDITION_INSIGHT_ORDER_BASE + index.type,
                    card = dev.local.weatherstudy.domain.entity.content.StudyInsightCard(
                        title = index.levelText,
                        content = index.description,
                        shortContent = index.levelText,
                        defaultContent = describeFallback(provider, index),
                    ),
                    showDetail = true,
                )
            }
        }
        return weather.copy(insightContent = remoteInsights + derived)
    }

    private fun conditionInsightType(indexType: Int): Int? = when (indexType) {
        StudyIndexType.WIND -> StudyInsightType.WIND_CONDITION
        StudyIndexType.DEW_POINT -> StudyInsightType.DEW_POINT_CONDITION
        StudyIndexType.PRESSURE -> StudyInsightType.PRESSURE_CONDITION
        StudyIndexType.UV -> StudyInsightType.UV_CONDITION
        StudyIndexType.HUMIDITY -> StudyInsightType.HUMIDITY_CONDITION
        StudyIndexType.VISIBILITY -> StudyInsightType.VISIBILITY_CONDITION
        else -> null
    }

    private fun describeFallback(provider: String, index: StudyIndex) =
        "$provider:${index.type}:${index.value}"

    private companion object {
        const val CONDITION_INSIGHT_ORDER_BASE = 1000
    }
}

/**
 * Corresponds conceptually to:
 * com.samsung.android.weather.domain.usecase.AssignIconNum
 *
 * Observed responsibility: the original takes NO dependencies and is applied centrally
 * in [StudyFetchWeatherImpl]. It maps each provider's `externalCode` onto Samsung's own
 * `iconNum`, which is the id every surface then uses to pick a Lottie animation, a WebP
 * fallback, a widget drawable and a splash theme.
 *
 * Reconstructed as a pure function with a small illustrative table; the original's full
 * per-provider table is provider-proprietary data and is not reproduced. The
 * architectural point is that the mapping happens ONCE, here, so nothing downstream
 * ever sees a provider code.
 */
class StudyAssignIconNum @Inject constructor() {

    operator fun invoke(weather: StudyWeather): StudyWeather = weather.copy(
        currentObservation = weather.currentObservation.copy(
            condition = assign(weather.currentObservation.condition, weather.currentObservation.time.isDayOrNight),
        ),
        hourlyObservations = weather.hourlyObservations.map {
            it.copy(condition = assign(it.condition, it.time.isDayOrNight))
        },
        dailyObservations = weather.dailyObservations.map {
            it.copy(
                dayCondition = assign(it.dayCondition, dev.local.weatherstudy.domain.entity.weather.StudyForecastTime.DAY),
                nightCondition = assign(it.nightCondition, dev.local.weatherstudy.domain.entity.weather.StudyForecastTime.NIGHT),
            )
        },
    )

    private fun assign(
        condition: dev.local.weatherstudy.domain.entity.weather.StudyCondition,
        dayOrNight: Int,
    ) = if (condition.iconNum != dev.local.weatherstudy.domain.entity.weather.StudyCondition.INVALID_CODE) {
        condition
    } else {
        condition.copy(iconNum = iconNumOf(condition.internalCode, dayOrNight))
    }

    /**
     * Maps an internal condition code and a day/night flag onto the icon vocabulary below.
     *
     * The SHAPE is the original's: one central mapping, applied once, after which nothing
     * downstream ever sees a provider's own code. The BANDS are this project's - the
     * original's per-provider lookup tables are provider-proprietary and not reproduced.
     *
     * ### Only seven input values ever arrive
     *
     * `StudyProviderAConverters.INTERNAL_CODE_RANGES` collapses the gateway's codes onto the
     * START of each band, so `internalCode` is only ever one of
     *
     *     0 clear   2 partly cloudy   5 cloudy   9 rain
     *     19 snow   30 thunderstorm   36 sandstorm      (-1 unknown)
     *
     * This is matched on those values, not on ranges that look plausible. Writing
     * `in 9..11 -> ICON_FOG` was tried and silently re-pointed rain at the fog artwork,
     * because 9 is the rain band's start and nothing else in 9..11 is ever produced - the
     * hourly strip filled with fog glyphs under a "Drizzle" narrative.
     *
     * ### The vocabulary is wider than the data
     *
     * Twenty-three of the thirty codes - fog, shower, hail, hurricane, ice, the three
     * "partly sunny with ..." variants, the four intensity steps - are unreachable from this
     * gateway. They are kept because the ARTWORK is keyed by them: every one has its own
     * Lottie animation, its own pair of vectors and a background mapping, and a richer
     * provider would reach them. The gap is the reconstruction's data source, not its
     * vocabulary.
     */
    private fun iconNumOf(internalCode: Int, dayOrNight: Int): Int {
        val night = dayOrNight == dev.local.weatherstudy.domain.entity.weather.StudyForecastTime.NIGHT
        return when (internalCode) {
            CODE_CLEAR -> if (night) ICON_CLEAR else ICON_SUNNY
            CODE_PARTLY_CLOUDY -> if (night) ICON_PARTLY_CLOUD_NIGHT else ICON_PARTLY_CLOUD
            CODE_CLOUDY -> if (night) ICON_MOSTLY_CLOUDY_NIGHT else ICON_CLOUDY
            CODE_RAIN -> ICON_RAIN
            CODE_SNOW -> ICON_SNOW
            CODE_THUNDERSTORM -> ICON_THUNDERSTORM
            CODE_SANDSTORM -> ICON_SAND_STORM
            // the original's switches end in a default that equals the clear-sky arm
            else -> if (night) ICON_CLEAR else ICON_SUNNY
        }
    }

    /**
     * The icon vocabulary: thirty values, 0..29.
     *
     * Session 3 change. This was previously ten values of this project's own invention
     * (ICON_UNKNOWN..ICON_SANDSTORM), which meant the reconstruction could not address the
     * original's artwork at all - every artwork family in the APK is keyed on this exact
     * numbering:
     *
     *   * the `white` and `dark` Lottie asset sets - 30 entries each, via AnimIconProvider
     *   * the `illust` Lottie asset set            - 30 entries, via DetailIllustrationStateConverter
     *   * R.drawable.weather_ic_*                     - 30 AnimatedVectorDrawables x 2 variants
     *   * detail_bg_gradient_*.png                    - 30 codes collapsed onto 11, via BackgroundProvider
     *
     * The numbering is recovered from those resource tables rather than guessed: code 7 is
     * "shower" because AnimIconProvider maps 7 to shower.json and the illustration converter
     * maps 7 to illust/shower.json. It is a vocabulary, not provider data, so reproducing it
     * is what lets every one of those families be addressed correctly.
     *
     * Note the three near-synonym groups, which are genuinely distinct artwork in the
     * original and are the main reason the vocabulary needs thirty values rather than ten:
     * sunny/mostly sunny/partly cloudy/mostly cloudy/cloudy, and the four rain intensities,
     * and the four snow ones.
     */
    companion object {
        /** the seven band starts StudyProviderAConverters can emit; see iconNumOf */
        private const val CODE_CLEAR = 0
        private const val CODE_PARTLY_CLOUDY = 2
        private const val CODE_CLOUDY = 5
        private const val CODE_RAIN = 9
        private const val CODE_SNOW = 19
        private const val CODE_THUNDERSTORM = 30
        private const val CODE_SANDSTORM = 36

        const val ICON_SUNNY = 0
        const val ICON_CLEAR = 1
        const val ICON_PARTLY_CLOUD = 2
        const val ICON_PARTLY_CLOUD_NIGHT = 3
        const val ICON_CLOUDY = 4
        const val ICON_FOG = 5
        const val ICON_RAIN = 6
        const val ICON_SHOWER = 7
        const val ICON_PARTLY_SUNNY_WITH_SHOWER = 8
        const val ICON_THUNDERSTORM = 9
        const val ICON_PARTLY_SUNNY_WITH_THUNDER = 10
        const val ICON_LIGHT_SNOW = 11
        const val ICON_PARTLY_SUNNY_WITH_FLURRIES = 12
        const val ICON_SNOW = 13
        const val ICON_RAIN_AND_SNOW = 14
        const val ICON_ICE = 15
        const val ICON_HOT = 16
        const val ICON_COLD = 17
        const val ICON_WIND = 18
        const val ICON_RAIN_AND_THUNDER = 19
        const val ICON_HEAVY_RAIN = 20
        const val ICON_SAND_STORM = 21
        const val ICON_HURRICANE = 22
        const val ICON_MOSTLY_SUNNY = 23
        const val ICON_MOSTLY_CLEAR = 24
        const val ICON_MOSTLY_CLOUDY = 25
        const val ICON_MOSTLY_CLOUDY_NIGHT = 26
        const val ICON_HEAVY_SNOW = 27
        const val ICON_RAIN_AND_SLEET = 28
        const val ICON_HAIL = 29
    }
}
