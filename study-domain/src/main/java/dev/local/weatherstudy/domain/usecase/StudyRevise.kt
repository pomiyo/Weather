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
     * Illustrative mapping. The shape — (internal condition code, day/night) → icon id —
     * is the original's; the table contents are not Samsung's.
     */
    private fun iconNumOf(internalCode: Int, dayOrNight: Int): Int {
        val night = dayOrNight == dev.local.weatherstudy.domain.entity.weather.StudyForecastTime.NIGHT
        return when (internalCode) {
            in 0..1 -> if (night) ICON_CLEAR_NIGHT else ICON_CLEAR_DAY
            in 2..4 -> if (night) ICON_PARTLY_NIGHT else ICON_PARTLY_DAY
            in 5..8 -> ICON_CLOUDY
            in 9..18 -> ICON_RAIN
            in 19..29 -> ICON_SNOW
            in 30..35 -> ICON_THUNDERSTORM
            in 36..40 -> ICON_SANDSTORM
            else -> ICON_UNKNOWN
        }
    }

    companion object {
        const val ICON_UNKNOWN = 0
        const val ICON_CLEAR_DAY = 1
        const val ICON_CLEAR_NIGHT = 2
        const val ICON_PARTLY_DAY = 3
        const val ICON_PARTLY_NIGHT = 4
        const val ICON_CLOUDY = 5
        const val ICON_RAIN = 6
        const val ICON_SNOW = 7
        const val ICON_THUNDERSTORM = 8
        const val ICON_SANDSTORM = 9
    }
}
