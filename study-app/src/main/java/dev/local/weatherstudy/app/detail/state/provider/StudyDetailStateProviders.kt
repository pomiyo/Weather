package dev.local.weatherstudy.app.detail.state.provider

import dev.local.weatherstudy.domain.entity.weather.StudyAlert
import dev.local.weatherstudy.domain.entity.weather.StudyCondition
import dev.local.weatherstudy.domain.entity.weather.StudyForecastTime
import dev.local.weatherstudy.domain.entity.weather.StudyIndex
import dev.local.weatherstudy.domain.entity.weather.StudyWeather
import dev.local.weatherstudy.domain.entity.weather.displayName
import dev.local.weatherstudy.domain.entity.weather.isCurrentLocation
import dev.local.weatherstudy.domain.policy.StudyOrderingPolicy
import dev.local.weatherstudy.domain.policy.StudyWeatherPolicyManager
import dev.local.weatherstudy.domain.type.StudyIndexCategory
import dev.local.weatherstudy.domain.type.StudyIndexLevel
import dev.local.weatherstudy.domain.type.StudyIndexType
import dev.local.weatherstudy.domain.type.StudyInsightType
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailAirIndexCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailAirIndexItemState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailAlertCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailAlertItemState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailBackgroundState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailCardType
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailConfiguration
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailDailyCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailDailyItemState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailHourlyCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailHourlyItemState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailIndexCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailIndexItemState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailIndicatorCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailIndicatorState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailInsightCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailInsightItemState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailItemState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailMoonCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailPrecipitationCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailPrecipitationItemState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailScreenState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailSunCardState
import dev.local.weatherstudy.ui.common.detail.state.StudyDetailTopInfoState
import dev.local.weatherstudy.ui.common.detail.state.StudyIndexGraphViewEntity
import dev.local.weatherstudy.ui.common.resource.StudyBackgroundProvider
import dev.local.weatherstudy.ui.common.usecase.notation.StudyIndexNotation
import dev.local.weatherstudy.ui.common.usecase.notation.StudyLevelNotation
import dev.local.weatherstudy.ui.common.usecase.notation.StudySunProgressNotation
import dev.local.weatherstudy.ui.common.usecase.notation.StudyTemperatureNotation
import dev.local.weatherstudy.ui.common.usecase.notation.StudyTimeNotation
import dev.local.weatherstudy.ui.common.usecase.notation.StudyWindNotation
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Educational reconstruction.
 *
 * Corresponds conceptually to the 26 classes in
 * com.sec.android.daemonapp.app.detail.state.provider:
 * `DetailStateProvider`, `DetailItemStateListProvider`, `DetailTopInfoStateProvider`,
 * `DetailBackgroundStateProvider`, `DetailIndicatorStateProvider`,
 * `DetailHourlyCardStateProvider`, `DetailDailyCardStateProvider`,
 * `DetailPrecipitationCardStateProvider`, `DetailIndexCardStateProvider`,
 * `DetailAirIndexCardStateProvider`, `DetailSunCardStateProvider`,
 * `DetailMoonCardStateProvider`, `DetailInsightCardStateProvider`,
 * `DetailAlertCardStateProvider`, … and their `Impl`s
 *
 * ### One provider per card — and everything formatted HERE
 *
 * Each provider turns the domain aggregate into one card's state. All unit conversion,
 * normalisation, time-zone handling and text assembly happens in this layer, so the view
 * holders bind pre-formatted values and the Canvas views receive ratios in 0..1. That is
 * why a temperature-unit change re-renders the whole screen from one place.
 *
 * Times are formatted in the LOCATION's zone, carried on the current observation: a
 * forecast for another city reads in that city's clock, not the device's.
 *
 * This is independently written reconstruction code, not original Samsung source.
 */
class StudyDetailHourlyCardStateProvider @Inject constructor(
    private val temperatureNotation: StudyTemperatureNotation,
    private val timeNotation: StudyTimeNotation,
    private val indexNotation: StudyIndexNotation,
    private val windNotation: StudyWindNotation,
    private val policyManager: StudyWeatherPolicyManager,
) {
    operator fun invoke(weather: StudyWeather, tempScale: Int): StudyDetailHourlyCardState {
        val zone = weather.currentObservation.time.ianaTimeZone
        val hours = weather.hourlyObservations.take(HOURS_SHOWN)
        if (hours.isEmpty()) return StudyDetailHourlyCardState(isVisible = false)

        // normalise the whole series ONCE: each item view then draws only its own slice
        val temps = hours.map { it.condition.temperature }
            .filter { it != StudyCondition.INVALID_TEMPERATURE }
        val min = temps.minOrNull() ?: 0.0
        val max = temps.maxOrNull() ?: 1.0
        val span = (max - min).takeIf { it > 0.0 } ?: 1.0
        fun ratio(value: Double) =
            if (value == StudyCondition.INVALID_TEMPERATURE) Float.NaN
            else ((value - min) / span).toFloat()

        // the tangent at each hour is decided HERE, once, from the hour's two neighbours.
        // An item view only sees three values, so if it estimated its neighbour's tangent
        // itself, two adjacent items would draw two different curves for the segment they share.
        val ratios = hours.map { ratio(it.condition.temperature) }
        fun slopeAt(index: Int): Float {
            val current = ratios.getOrNull(index)?.takeUnless { it.isNaN() } ?: return 0f
            val before = ratios.getOrNull(index - 1)?.takeUnless { it.isNaN() }
            val after = ratios.getOrNull(index + 1)?.takeUnless { it.isNaN() }
            return when {
                before != null && after != null -> (after - before) / 2f
                after != null -> after - current
                before != null -> current - before
                else -> 0f
            }
        }

        val items = hours.mapIndexed { index, hour ->
            val wind = hour.condition.find(StudyIndexType.WIND)
            val chance = hour.condition.find(StudyIndexType.PRECIPITATION_PROBABILITY)?.value ?: 0.0
            StudyDetailHourlyItemState(
                timeText = if (index == 0) NOW else timeNotation.formatHour(hour.time.epochTime, false, zone),
                iconNum = hour.condition.iconNum,
                temperatureText = temperatureNotation.format(hour.condition.temperature, tempScale),
                temperatureRatio = ratio(hour.condition.temperature),
                previousRatio = hours.getOrNull(index - 1)
                    ?.let { ratio(it.condition.temperature) } ?: Float.NaN,
                nextRatio = hours.getOrNull(index + 1)
                    ?.let { ratio(it.condition.temperature) } ?: Float.NaN,
                slope = slopeAt(index),
                previousSlope = slopeAt(index - 1),
                nextSlope = slopeAt(index + 1),
                precipitationText = if (chance >= MIN_CHANCE_SHOWN) indexNotation.formatPercent(chance) else "",
                windText = wind?.let { windNotation.formatSpeed(it.value, StudyWindNotation.UNIT_KPH) }.orEmpty(),
                windDirectionDegree = windNotation.toDegree(wind?.levelText.orEmpty()),
                isNow = index == 0,
                isDay = hour.time.isDayOrNight == StudyForecastTime.DAY,
            )
        }
        return StudyDetailHourlyCardState(
            isVisible = true,
            items = items,
            supportWind = policyManager.supportWind(),
        )
    }

    private companion object {
        const val HOURS_SHOWN = 24
        const val MIN_CHANCE_SHOWN = 10.0
        const val NOW = "Now"
    }
}

/** Corresponds conceptually to `…state.provider.DetailDailyCardStateProvider`. */
class StudyDetailDailyCardStateProvider @Inject constructor(
    private val temperatureNotation: StudyTemperatureNotation,
    private val timeNotation: StudyTimeNotation,
    private val indexNotation: StudyIndexNotation,
) {
    operator fun invoke(
        weather: StudyWeather,
        tempScale: Int,
        now: Long = System.currentTimeMillis(),
    ): StudyDetailDailyCardState {
        val zone = weather.currentObservation.time.ianaTimeZone
        // the aggregate carries yesterday for the "compared with yesterday" figures;
        // the card starts at today
        val days = weather.dailyObservations.filter { it.time.epochTime + DAY_MILLIS > now }
        if (days.isEmpty()) return StudyDetailDailyCardState(isVisible = false)

        val highs = days.map { it.dayCondition.maxTemp }
            .filter { it != StudyCondition.INVALID_TEMPERATURE }
        val lows = days.map { it.nightCondition.minTemp }
            .filter { it != StudyCondition.INVALID_TEMPERATURE }
        val min = lows.minOrNull() ?: 0.0
        val max = highs.maxOrNull() ?: 1.0
        val span = (max - min).takeIf { it > 0.0 } ?: 1.0
        fun ratio(value: Double) =
            if (value == StudyCondition.INVALID_TEMPERATURE) 0f else ((value - min) / span).toFloat()

        return StudyDetailDailyCardState(
            isVisible = true,
            items = days.mapIndexed { index, day ->
                val chance = day.dayCondition.find(StudyIndexType.PRECIPITATION_PROBABILITY)?.value ?: 0.0
                StudyDetailDailyItemState(
                    dayText = if (index == 0) TODAY else timeNotation.formatDayOfWeek(day.time.epochTime, zone),
                    dateText = timeNotation.formatDate(day.time.epochTime, zone),
                    iconNum = day.dayCondition.iconNum,
                    highText = temperatureNotation.format(day.dayCondition.maxTemp, tempScale),
                    lowText = temperatureNotation.format(day.nightCondition.minTemp, tempScale),
                    highRatio = ratio(day.dayCondition.maxTemp),
                    lowRatio = ratio(day.nightCondition.minTemp),
                    precipitationText = if (chance >= MIN_CHANCE_SHOWN) indexNotation.formatPercent(chance) else "",
                    isToday = index == 0,
                )
            },
        )
    }

    private companion object {
        const val DAY_MILLIS = 24L * 60L * 60L * 1000L
        const val MIN_CHANCE_SHOWN = 10.0
        const val TODAY = "Today"
    }
}

/**
 * Corresponds conceptually to `…state.provider.DetailPrecipitationCardStateProvider`.
 *
 * Observed responsibility: the hourly amount bars. Shown only when the provider supports
 * the graph AND something is actually going to fall — a row of empty bars is not a card.
 */
class StudyDetailPrecipitationCardStateProvider @Inject constructor(
    private val timeNotation: StudyTimeNotation,
    private val indexNotation: StudyIndexNotation,
    private val policyManager: StudyWeatherPolicyManager,
) {
    operator fun invoke(weather: StudyWeather): StudyDetailPrecipitationCardState {
        if (!policyManager.supportPrecipitationGraph()) return StudyDetailPrecipitationCardState()
        val zone = weather.currentObservation.time.ianaTimeZone
        val hours = weather.hourlyObservations.take(HOURS_SHOWN)
        val amounts = hours.map { it.condition.find(StudyIndexType.PRECIPITATION_AMOUNT)?.value ?: 0.0 }
        val peak = amounts.maxOrNull() ?: 0.0
        if (peak <= 0.0) return StudyDetailPrecipitationCardState()

        return StudyDetailPrecipitationCardState(
            isVisible = true,
            summaryText = "${indexNotation.formatPrecipitationAmount(amounts.sum(), StudyIndexNotation.PRECIP_MM)} " +
                "expected in the next ${hours.size} hours",
            unitText = "mm",
            items = hours.mapIndexed { index, hour ->
                val amount = amounts[index]
                StudyDetailPrecipitationItemState(
                    timeText = if (index == 0) "Now" else timeNotation.formatHour(hour.time.epochTime, false, zone),
                    amountText = if (amount > 0.0) "%.1f".format(amount) else "",
                    amountRatio = (amount / peak).toFloat(),
                    probabilityText = hour.condition.find(StudyIndexType.PRECIPITATION_PROBABILITY)
                        ?.let { indexNotation.formatPercent(it.value) }.orEmpty(),
                    precipitationType = hour.condition.find(StudyIndexType.PRECIPITATION_AMOUNT)?.level ?: 0,
                )
            },
        )
    }

    private companion object {
        const val HOURS_SHOWN = 24
    }
}

/** Corresponds conceptually to `…state.provider.DetailIndexCardStateProvider`. */
class StudyDetailIndexCardStateProvider @Inject constructor(
    private val indexNotation: StudyIndexNotation,
    private val levelNotation: StudyLevelNotation,
    private val windNotation: StudyWindNotation,
    private val temperatureNotation: StudyTemperatureNotation,
    private val policyManager: StudyWeatherPolicyManager,
) {
    operator fun invoke(weather: StudyWeather, tempScale: Int): StudyDetailIndexCardState {
        val indices = weather.currentObservation.condition.indexList
        val items = StudyIndexType.INDEX_CARD_TYPES.filter(::supported).mapNotNull { type ->
            val index = indices.firstOrNull { it.type == type && it.category == StudyIndexCategory.DETAIL }
                ?: return@mapNotNull null
            if (index.value == StudyIndex.INVALID_VALUE) return@mapNotNull null
            StudyDetailIndexItemState(
                indexType = type,
                titleText = titleFor(type),
                valueText = valueFor(type, index.value, tempScale),
                levelText = levelFor(type, index),
                descriptionText = index.description,
                graphValue = normalise(type, index.value),
                graphEntity = if (type == StudyIndexType.UV) UV_BANDS else StudyIndexGraphViewEntity(),
                directionDegree = index.extra.toFloatOrNull() ?: windNotation.toDegree(index.levelText),
                webUrl = index.webUrl,
            )
        }
        return StudyDetailIndexCardState(isVisible = items.isNotEmpty(), items = items)
    }

    private fun supported(type: Int) = when (type) {
        StudyIndexType.UV -> policyManager.supportUV()
        StudyIndexType.HUMIDITY -> policyManager.supportHumidity()
        StudyIndexType.PRESSURE -> policyManager.supportPress()
        StudyIndexType.WIND -> policyManager.supportWind()
        StudyIndexType.VISIBILITY -> policyManager.supportVisibility()
        StudyIndexType.DEW_POINT -> policyManager.supportDewpoint()
        StudyIndexType.PRECIPITATION_AMOUNT -> policyManager.supportPrecipitation()
        else -> true
    }

    private fun titleFor(type: Int) = when (type) {
        StudyIndexType.UV -> "UV index"
        StudyIndexType.HUMIDITY -> "Humidity"
        StudyIndexType.PRESSURE -> "Pressure"
        StudyIndexType.WIND -> "Wind"
        StudyIndexType.VISIBILITY -> "Visibility"
        StudyIndexType.DEW_POINT -> "Dew point"
        StudyIndexType.PRECIPITATION_AMOUNT -> "Precipitation"
        else -> ""
    }

    private fun valueFor(type: Int, value: Double, tempScale: Int) = when (type) {
        StudyIndexType.UV -> indexNotation.formatUvIndex(value)
        StudyIndexType.HUMIDITY -> indexNotation.formatPercent(value)
        StudyIndexType.PRESSURE -> indexNotation.formatPressure(value, StudyIndexNotation.PRESSURE_HPA)
        StudyIndexType.WIND -> windNotation.formatSpeed(value, StudyWindNotation.UNIT_KPH)
        StudyIndexType.VISIBILITY -> indexNotation.formatDistance(value, StudyIndexNotation.DISTANCE_KM)
        StudyIndexType.DEW_POINT -> temperatureNotation.format(value, tempScale)
        StudyIndexType.PRECIPITATION_AMOUNT ->
            indexNotation.formatPrecipitationAmount(value, StudyIndexNotation.PRECIP_MM)
        else -> value.toString()
    }

    /** the provider's own wording wins; the notation layer is the fallback */
    private fun levelFor(type: Int, index: StudyIndex): String = when {
        type == StudyIndexType.PRECIPITATION_AMOUNT -> "Today"
        index.levelText.isNotEmpty() -> index.levelText
        type == StudyIndexType.UV -> levelNotation.formatUvLevel(index.level)
        type == StudyIndexType.PRESSURE -> levelNotation.formatPressureTendency(index.level)
        else -> ""
    }

    private fun normalise(type: Int, value: Double): Float = when (type) {
        StudyIndexType.UV -> (value / UV_MAX).toFloat().coerceIn(0f, 1f)
        StudyIndexType.HUMIDITY -> (value / PERCENT_MAX).toFloat().coerceIn(0f, 1f)
        StudyIndexType.PRESSURE ->
            ((value - PRESSURE_MIN) / (PRESSURE_MAX - PRESSURE_MIN)).toFloat().coerceIn(0f, 1f)
        StudyIndexType.WIND -> (value / WIND_MAX).toFloat().coerceIn(0f, 1f)
        else -> 0f
    }

    private companion object {
        const val UV_MAX = 11.0
        const val PERCENT_MAX = 100.0
        const val PRESSURE_MIN = 950.0
        const val PRESSURE_MAX = 1050.0
        const val WIND_MAX = 60.0

        /** WHO exposure categories: low, moderate, high, very high, extreme */
        val UV_BANDS = StudyIndexGraphViewEntity(
            minValue = 0f,
            maxValue = UV_MAX.toFloat(),
            bandBoundaries = listOf(3f, 6f, 8f, 10.5f, 11f).map { it / UV_MAX.toFloat() },
            bandColors = listOf(
                0xFF66BB6A.toInt(), 0xFFFFEE58.toInt(), 0xFFFFA726.toInt(),
                0xFFEF5350.toInt(), 0xFFAB47BC.toInt(),
            ),
        )
    }
}

/**
 * Corresponds conceptually to `…state.provider.DetailAirIndexCardStateProvider`.
 *
 * Observed responsibility: the AQI headline, its banded bar and the pollutant rows. The
 * banding is a national scale — the index carries which one in `extra` — so the bands
 * are chosen here from the scale id, never hard-coded in the view.
 */
class StudyDetailAirIndexCardStateProvider @Inject constructor(
    private val levelNotation: StudyLevelNotation,
    private val policyManager: StudyWeatherPolicyManager,
) {
    operator fun invoke(weather: StudyWeather): StudyDetailAirIndexCardState {
        if (!policyManager.supportAQI()) return StudyDetailAirIndexCardState(isVisible = false)
        val air = weather.currentObservation.condition.indexList
            .filter { it.category == StudyIndexCategory.AIR }
        val aqi = air.firstOrNull { it.type == StudyIndexType.AQI && it.value != StudyIndex.INVALID_VALUE }
            ?: return StudyDetailAirIndexCardState(isVisible = false)

        return StudyDetailAirIndexCardState(
            isVisible = true,
            aqiText = aqi.value.roundToInt().toString(),
            levelText = aqi.levelText.ifEmpty { levelNotation.formatAirLevel(aqi.level) },
            scaleName = scaleName(aqi.extra.toIntOrNull()),
            graphEntity = EPA_BANDS,
            graphValue = (aqi.value / EPA_MAX).toFloat().coerceIn(0f, 1f),
            pollutants = listOf(StudyIndexType.PM2_5 to "PM2.5", StudyIndexType.PM10 to "PM10")
                .mapNotNull { (type, name) ->
                    val pollutant = air.firstOrNull { it.type == type && it.value != StudyIndex.INVALID_VALUE }
                        ?: return@mapNotNull null
                    StudyDetailAirIndexItemState(
                        nameText = name,
                        valueText = "${pollutant.value.roundToInt()} µg/m³",
                    )
                },
        )
    }

    private fun scaleName(scale: Int?) = when (scale) {
        StudyIndexLevel.AqiScale.EPA -> "US AQI"
        StudyIndexLevel.AqiScale.CAQI -> "CAQI"
        StudyIndexLevel.AqiScale.DAQI -> "DAQI"
        else -> "AQI"
    }

    private companion object {
        const val EPA_MAX = 300.0

        /** the upper bound of each EPA category, drawn out to 300 */
        val EPA_BANDS = StudyIndexGraphViewEntity(
            minValue = 0f,
            maxValue = EPA_MAX.toFloat(),
            bandBoundaries = listOf(50f, 100f, 150f, 200f, 300f).map { it / EPA_MAX.toFloat() },
            bandColors = listOf(
                0xFF66BB6A.toInt(), 0xFFFFEE58.toInt(), 0xFFFFA726.toInt(),
                0xFFEF5350.toInt(), 0xFFAB47BC.toInt(),
            ),
        )
    }
}

/** Corresponds conceptually to `…state.provider.DetailSunCardStateProvider`. */
class StudyDetailSunCardStateProvider @Inject constructor(
    private val timeNotation: StudyTimeNotation,
    private val sunProgress: StudySunProgressNotation,
    private val policyManager: StudyWeatherPolicyManager,
) {
    operator fun invoke(weather: StudyWeather, now: Long = System.currentTimeMillis()): StudyDetailSunCardState {
        val time = weather.currentObservation.time
        return StudyDetailSunCardState(
            isVisible = policyManager.supportSunCycle() &&
                (time.sunRiseTime > 0 || time.arcticNightType != StudyForecastTime.ARCTIC_NONE),
            sunriseText = timeNotation.formatClock(time.sunRiseTime, time.ianaTimeZone),
            sunsetText = timeNotation.formatClock(time.sunSetTime, time.ianaTimeZone),
            sunProgress = sunProgress.progress(time.sunRiseTime, time.sunSetTime, now),
            isPolarDay = time.arcticNightType == StudyForecastTime.ARCTIC_POLAR_DAY,
            isPolarNight = time.arcticNightType == StudyForecastTime.ARCTIC_POLAR_NIGHT,
        )
    }

    companion object {
        const val SUN = "Sun"
    }
}

/**
 * Corresponds conceptually to `…state.provider.DetailMoonCardStateProvider`.
 *
 * The phase is the index's LEVEL (one of eight); the lit fraction is its VALUE. They are
 * separate because the name changes in steps while the drawing changes continuously.
 */
class StudyDetailMoonCardStateProvider @Inject constructor(
    private val timeNotation: StudyTimeNotation,
    private val levelNotation: StudyLevelNotation,
    private val policyManager: StudyWeatherPolicyManager,
) {
    operator fun invoke(weather: StudyWeather): StudyDetailMoonCardState {
        val time = weather.currentObservation.time
        val phaseIndex = weather.currentObservation.condition.indexList
            .firstOrNull { it.type == StudyIndexType.MOON_PHASE }
        val illumination = phaseIndex?.value?.takeIf { it in 0.0..1.0 } ?: 0.0
        return StudyDetailMoonCardState(
            isVisible = policyManager.supportMoonCycle() && phaseIndex != null,
            moonriseText = timeNotation.formatClock(time.moonRiseTime, time.ianaTimeZone),
            moonsetText = timeNotation.formatClock(time.moonSetTime, time.ianaTimeZone),
            phase = phaseIndex?.level ?: 0,
            phaseText = levelNotation.formatMoonPhase(phaseIndex?.level ?: 0),
            illuminationFraction = illumination.toFloat(),
        )
    }

    companion object {
        const val MOON = "Moon"
    }
}

/**
 * Corresponds conceptually to `…state.provider.DetailInsightCardStateProvider`.
 *
 * Observed responsibility: the swipeable row of short statements. Two kinds arrive in
 * one list — the provider's own cards, and the ones `InsertIndexDescription` derives
 * locally from the current observation — and the derived ones are titled with the
 * measurement they came from, since their own title is only a level.
 */
class StudyDetailInsightCardStateProvider @Inject constructor(
    private val policyManager: StudyWeatherPolicyManager,
) {
    operator fun invoke(weather: StudyWeather): StudyDetailInsightCardState {
        if (!policyManager.supportInsightCard()) return StudyDetailInsightCardState()
        val items = weather.insightContent
            .filter { it.showDetail && it.card.content.isNotBlank() }
            .sortedBy { it.order }
            .map { insight ->
                StudyDetailInsightItemState(
                    insightType = insight.insightType,
                    titleText = derivedTitle(insight.insightType, insight.card.title) ?: insight.card.title,
                    contentText = insight.card.content,
                    timeText = insight.card.timeDescription,
                    webUrl = insight.card.url,
                )
            }
        return StudyDetailInsightCardState(isVisible = items.isNotEmpty(), items = items)
    }

    private fun derivedTitle(insightType: Int, level: String): String? {
        val measurement = when (insightType) {
            StudyInsightType.WIND_CONDITION -> "Wind"
            StudyInsightType.DEW_POINT_CONDITION -> "Dew point"
            StudyInsightType.PRESSURE_CONDITION -> "Pressure"
            StudyInsightType.UV_CONDITION -> "UV index"
            StudyInsightType.HUMIDITY_CONDITION -> "Humidity"
            StudyInsightType.VISIBILITY_CONDITION -> "Visibility"
            else -> return null
        }
        return if (level.isEmpty()) measurement else "$measurement: $level"
    }
}

/** Corresponds conceptually to `…state.provider.DetailAlertCardStateProvider`. */
class StudyDetailAlertCardStateProvider @Inject constructor(
    private val timeNotation: StudyTimeNotation,
    private val policyManager: StudyWeatherPolicyManager,
) {
    operator fun invoke(weather: StudyWeather): StudyDetailAlertCardState {
        if (!policyManager.supportAlert() || weather.alerts.isEmpty()) return StudyDetailAlertCardState()
        val zone = weather.currentObservation.time.ianaTimeZone
        return StudyDetailAlertCardState(
            isVisible = true,
            items = weather.alerts.map { alert ->
                StudyDetailAlertItemState(
                    titleText = alert.eventDescription,
                    issuedText = if (alert.expireTime > 0) {
                        "Until ${timeNotation.formatClock(alert.expireTime, zone)}"
                    } else {
                        ""
                    },
                    severityColor = severityColor(alert.severityCode),
                    webUrl = alert.linkURL,
                )
            },
        )
    }

    private fun severityColor(severity: Int) = when (severity) {
        StudyAlert.SEVERITY_EXTREME -> 0xFFD32F2F.toInt()
        StudyAlert.SEVERITY_SEVERE -> 0xFFF57C00.toInt()
        StudyAlert.SEVERITY_MODERATE -> 0xFFFBC02D.toInt()
        else -> 0xFF90A4AE.toInt()
    }
}

/**
 * Corresponds conceptually to `…state.provider.DetailTopInfoStateProvider`.
 *
 * Observed responsibility: the header's content — the part of the screen that is NOT a
 * card and collapses on scroll.
 */
class StudyDetailTopInfoStateProvider @Inject constructor(
    private val temperatureNotation: StudyTemperatureNotation,
    private val timeNotation: StudyTimeNotation,
) {
    operator fun invoke(weather: StudyWeather, tempScale: Int): StudyDetailTopInfoState {
        val condition = weather.currentObservation.condition
        return StudyDetailTopInfoState(
            cityName = weather.location.displayName,
            isCurrentLocation = weather.location.isCurrentLocation,
            temperature = temperatureNotation.format(condition.temperature, tempScale),
            weatherText = condition.weatherText,
            highLow = temperatureNotation.formatHighLow(condition.maxTemp, condition.minTemp, tempScale),
            feelsLike = temperatureNotation.format(condition.feelsLikeTemp, tempScale),
            iconNum = condition.iconNum,
            // the device's clock: this is when THIS device last fetched, not a forecast time
            updateTimeText = timeNotation.formatClock(weather.currentObservation.time.updateTime),
        )
    }
}

/** Corresponds conceptually to `…state.provider.DetailBackgroundStateProvider`. */
class StudyDetailBackgroundStateProvider @Inject constructor(
    private val backgroundProvider: StudyBackgroundProvider,
) {
    operator fun invoke(weather: StudyWeather): StudyDetailBackgroundState {
        val iconNum = weather.currentObservation.condition.iconNum
        val isDay = weather.currentObservation.time.isDayOrNight == StudyForecastTime.DAY
        val (start, end) = backgroundProvider.getGradientColors(iconNum, isDay)
        return StudyDetailBackgroundState(
            conditionCode = iconNum,
            isDay = isDay,
            gradientStartColor = start,
            gradientEndColor = end,
        )
    }
}

/**
 * Corresponds conceptually to `…state.provider.DetailIndicatorStateProvider`.
 *
 * The bottom card names where the data comes from. The reconstruction's gateway serves
 * Open-Meteo data, whose licence (CC BY 4.0) requires exactly this attribution.
 */
class StudyDetailIndicatorStateProvider @Inject constructor() {
    operator fun invoke(weather: StudyWeather): StudyDetailIndicatorState =
        StudyDetailIndicatorState(
            providerName = DATA_SOURCE,
            feedbackUrl = weather.links[LINK_ATTRIBUTION].orEmpty(),
        )

    private companion object {
        const val DATA_SOURCE = "Open-Meteo.com"
        const val LINK_ATTRIBUTION = "attribution"
    }
}

/**
 * Corresponds conceptually to `…state.provider.DetailItemStateListProvider`.
 *
 * Observed responsibility: assemble one location's page — every card's state, then the
 * ORDER, which comes from the policy package and keeps only the cards that are visible.
 * That filtered list is what `DetailAdapter` diffs.
 */
class StudyDetailItemStateListProvider @Inject constructor(
    private val topInfoProvider: StudyDetailTopInfoStateProvider,
    private val backgroundProvider: StudyDetailBackgroundStateProvider,
    private val indicatorProvider: StudyDetailIndicatorStateProvider,
    private val alertProvider: StudyDetailAlertCardStateProvider,
    private val insightProvider: StudyDetailInsightCardStateProvider,
    private val hourlyProvider: StudyDetailHourlyCardStateProvider,
    private val precipitationProvider: StudyDetailPrecipitationCardStateProvider,
    private val dailyProvider: StudyDetailDailyCardStateProvider,
    private val airIndexProvider: StudyDetailAirIndexCardStateProvider,
    private val indexProvider: StudyDetailIndexCardStateProvider,
    private val sunProvider: StudyDetailSunCardStateProvider,
    private val moonProvider: StudyDetailMoonCardStateProvider,
) {
    operator fun invoke(weather: StudyWeather, tempScale: Int): StudyDetailItemState {
        val cardStates = buildMap<StudyDetailCardType, StudyDetailCardState> {
            put(StudyDetailCardType.Alert, alertProvider(weather))
            put(StudyDetailCardType.Insight, insightProvider(weather))
            put(StudyDetailCardType.Hourly, hourlyProvider(weather, tempScale))
            put(StudyDetailCardType.Precipitation, precipitationProvider(weather))
            put(StudyDetailCardType.Daily, dailyProvider(weather, tempScale))
            put(StudyDetailCardType.AirIndex, airIndexProvider(weather))
            put(StudyDetailCardType.Index, indexProvider(weather, tempScale))
            put(StudyDetailCardType.Sun, sunProvider(weather))
            put(StudyDetailCardType.Moon, moonProvider(weather))
            put(
                StudyDetailCardType.Indicator,
                StudyDetailIndicatorCardState(indicator = indicatorProvider(weather)),
            )
        }
        return StudyDetailItemState(
            key = weather.location.key,
            topInfo = topInfoProvider(weather, tempScale),
            background = backgroundProvider(weather),
            indicator = indicatorProvider(weather),
            cardStates = cardStates,
            cardSortedList = sortCards(cardStates),
        )
    }

    private fun sortCards(
        cardStates: Map<StudyDetailCardType, StudyDetailCardState>,
    ): List<StudyDetailCardType> =
        StudyOrderingPolicy.DEFAULT_DETAIL_CARD_ORDER
            .mapNotNull { StudyDetailCardType.fromName(it) }
            .filter { cardStates[it]?.isVisible == true }
}

/**
 * Corresponds conceptually to `…state.provider.DetailStateProvider`.
 *
 * The top of the provider tree: all saved locations -> the whole screen state.
 */
class StudyDetailStateProvider @Inject constructor(
    private val itemStateListProvider: StudyDetailItemStateListProvider,
) {
    operator fun invoke(
        weathers: List<StudyWeather>,
        tempScale: Int,
        selectedKey: String,
        configuration: StudyDetailConfiguration,
    ): StudyDetailState {
        val details = weathers.map { itemStateListProvider(it, tempScale) }
        return StudyDetailState(
            screen = if (details.isEmpty()) StudyDetailScreenState.Empty else StudyDetailScreenState.Content,
            selectedKey = selectedKey.takeIf { key -> details.any { it.key == key } }
                ?: details.firstOrNull()?.key.orEmpty(),
            details = details,
            configuration = configuration,
        )
    }
}

private fun StudyCondition.find(type: Int): StudyIndex? =
    indexList.firstOrNull { it.type == type && it.value != StudyIndex.INVALID_VALUE }
